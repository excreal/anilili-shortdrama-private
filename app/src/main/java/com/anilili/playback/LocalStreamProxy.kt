package com.anilili.playback

import android.content.Context
import com.anilili.data.AppGraph
import com.anilili.diagnostics.DiagnosticsLog
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.HashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * A loopback HTTP server that lends this app's request headers to another player.
 *
 * The players this app hands episodes to fetch the stream themselves, and almost none of them can
 * be told to send a header. VLC in particular has no way at all: its whole external contract is
 * `item_location`, `subtitles_location`, `title`, `from_start`, `position`, `opened_position` and
 * `disable_hardware`, so the `Referer` these CDNs gate on cannot reach it and every gated source
 * answered it 403 while the same URL played here. Measured on one: bare and with a user agent alone
 * both 403, with `Referer: https://megaplay.buzz/` 200.
 *
 * So the other player is given a `127.0.0.1` address instead of the CDN's, and this serves it,
 * making the real request with the headers attached. Playlists are rewritten on the way through
 * ([rewriteHlsPlaylist]) so segments and keys come back here too — lending headers to the manifest
 * alone would only move the 403 one request along.
 *
 * **This is not an open proxy.** It binds the loopback interface, so only apps on this device can
 * reach it, and it will only fetch URLs this app has already registered under an unguessable token.
 * A request naming any other URL has nothing to name it with.
 */
internal object LocalStreamProxy {

    /** One upstream URL and the headers to fetch it with. */
    private data class Entry(val url: String, val headerContext: RootHeaderContext)

    private val random = SecureRandom()
    private val registry = BoundedUrlTokenRegistry<Entry>(
        maxEntries = REGISTRY_MAX_ENTRIES,
        maxApproximateBytes = REGISTRY_MAX_APPROXIMATE_BYTES,
    ) { ByteArray(16).also(random::nextBytes).joinToString("") { "%02x".format(it) } }
    private val lastRequestAt = AtomicLong(0L)
    private val lifecycleLock = Any()
    private var lifecycleGeneration = 0L
    private val activeResources = ProxyActiveResources()

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var port: Int = 0
    @Volatile private var appContext: Context? = null

    // Socket reads and upstream streams can occupy a worker for their full lifetime. Keep these
    // request workers fixed and bounded; accept/reaper control tasks live on separate threads.
    private val requestWorkers = newBoundedProxyRequestExecutor(
        workerCount = REQUEST_WORKER_COUNT,
        queueCapacity = REQUEST_QUEUE_CAPACITY,
    )
    private val controlTasks = Executors.newFixedThreadPool(2, namedDaemonThreadFactory("anilili-stream-proxy-control"))
    private val writeWatchdogs = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "anilili-stream-proxy-watchdog").apply { isDaemon = true }
    }

    /**
     * Fetches with no call timeout and no cache, unlike [AppGraph.httpClient].
     *
     * An episode is a long read and the shared client's 45-second call timeout would cut a segment
     * off mid-body; the shared cache would keep megabytes of media that only ever passes straight
     * through to the other player.
     */
    private val client: OkHttpClient by lazy {
        AppGraph.httpClient.newBuilder()
            .cache(null)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            // A network interceptor runs once for each redirect hop. It is the last point at
            // which we can check the actual destination before private root headers are sent.
            .addNetworkInterceptor(RootHeaderPolicyInterceptor)
            .build()
    }

    /**
     * The address to hand another player for [url], or [url] itself when nothing would be gained.
     *
     * Standing in the middle of a stream that plays perfectly well without it would only add a
     * failure point, so a request with no headers to lend is passed through untouched.
     */
    fun publish(context: Context, url: String, headers: Map<String, String>): String {
        requireNoHttpUserInfo(url)
        val parsedRootUrl = url.toHttpUrlOrNull()
        val usable = headers.filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        if (!needsLocalStreamProxy(usable) || !url.startsWith("http", ignoreCase = true)) return url
        val rootUrl = parsedRootUrl ?: return url
        // Snapshot at publication time: callers may reuse or mutate their header map later.
        val headerContext = RootHeaderContext(HttpOrigin.from(rootUrl), usable.toMap())
        val endpoint = runCatching { ensureRunning() }.getOrElse { error ->
            // Better the original URL than none: without headers it may still be an ungated source.
            DiagnosticsLog.throwable("LocalStreamProxy could not bind, handing over the direct URL", error)
            return url
        }
        val published = registerRoot(url, headerContext, endpoint)
        appContext = context.applicationContext
        if (!LocalStreamProxyService.start(context)) {
            DiagnosticsLog.event("LocalStreamProxy foreground lifetime could not start; using direct URL")
            return url
        }
        return published
    }

    private data class ServerEndpoint(val port: Int, val generation: Long)

    private fun registerRoot(url: String, headerContext: RootHeaderContext, endpoint: ServerEndpoint): String =
        synchronized(lifecycleLock) {
            check(lifecycleGeneration == endpoint.generation && serverSocket?.isClosed == false) {
                "Local stream proxy stopped before the URL could be published"
            }
            lastRequestAt.set(System.nanoTime())
            register(url, headerContext, endpoint.port)
        }

    /** Registers one URL and returns its loopback address, reusing an identical entry's token. */
    private fun register(url: String, headerContext: RootHeaderContext, port: Int): String {
        val entry = Entry(url, headerContext)
        val token = registry.register(entry, approximateEntryBytes(entry))
        return localUrl(port, token)
    }

    private fun localUrl(port: Int, token: String) = "http://127.0.0.1:$port/$token"

    private fun approximateEntryBytes(entry: Entry): Long =
        entry.url.toByteArray(Charsets.UTF_8).size.toLong() +
            entry.headerContext.origin.scheme.toByteArray(Charsets.UTF_8).size +
            entry.headerContext.origin.host.toByteArray(Charsets.UTF_8).size +
            entry.headerContext.headers.entries.sumOf { (name, value) ->
                name.toByteArray(Charsets.UTF_8).size.toLong() + value.toByteArray(Charsets.UTF_8).size
            }

    private fun trackSocket(socket: Socket, generation: Long): Boolean = synchronized(lifecycleLock) {
        if (lifecycleGeneration != generation || serverSocket?.isClosed != false) false
        else {
            activeResources.trackSocket(socket)
            true
        }
    }

    private fun untrackSocket(socket: Socket) = synchronized(lifecycleLock) {
        activeResources.untrackSocket(socket)
        Unit
    }

    private fun trackCall(call: Call, generation: Long): Boolean = synchronized(lifecycleLock) {
        if (lifecycleGeneration != generation || serverSocket?.isClosed != false) false
        else {
            activeResources.trackCall(call)
            true
        }
    }

    private fun untrackCall(call: Call) = synchronized(lifecycleLock) {
        activeResources.untrackCall(call)
        Unit
    }

    private fun ensureRunning(): ServerEndpoint = synchronized(lifecycleLock) {
        serverSocket?.takeIf { !it.isClosed }?.let { return@synchronized ServerEndpoint(port, lifecycleGeneration) }
        val socket = ServerSocket()
        socket.reuseAddress = true
        // Android commonly returns ::1 from getLoopbackAddress(), while the URL handed to players
        // is deliberately 127.0.0.1. Binding the IPv6 socket and publishing the IPv4 address makes
        // every player fail with ECONNREFUSED even though this thread reports that it is listening.
        val ipv4Loopback = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
        socket.bind(InetSocketAddress(ipv4Loopback, 0))
        val generation = ++lifecycleGeneration
        serverSocket = socket
        port = socket.localPort
        lastRequestAt.set(System.nanoTime())
        controlTasks.execute { acceptLoop(socket, generation) }
        controlTasks.execute { reapWhenIdle(socket, generation) }
        DiagnosticsLog.event("LocalStreamProxy listening on 127.0.0.1:$port")
        ServerEndpoint(port, generation)
    }

    private fun acceptLoop(socket: ServerSocket, generation: Long) {
        while (!socket.isClosed) {
            val connection = runCatching { socket.accept() }.getOrElse { return }
            if (!trackSocket(connection, generation)) {
                runCatching { connection.close() }
                continue
            }
            dispatchAcceptedSocket(requestWorkers, connection, onRejected = {
                untrackSocket(connection)
            }) { accepted ->
                var watchdog: ScheduledFuture<*>? = null
                try {
                    accepted.soTimeout = SOCKET_TIMEOUT_MS
                    val output = ProgressOutputStream(BufferedOutputStream(accepted.getOutputStream()))
                    watchdog = writeWatchdogs.scheduleWithFixedDelay(
                        {
                            if (System.nanoTime() - output.lastWriteAt.get() >= WRITE_STALL_TIMEOUT_NS) {
                                runCatching { accepted.close() }
                            }
                        },
                        WRITE_STALL_TIMEOUT_MS,
                        WRITE_STALL_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS,
                    )
                    serve(accepted, output, generation)
                } catch (_: IOException) {
                    // A player that seeks or stops closes the socket mid-body constantly. That is
                    // the normal shape of this traffic, not a fault worth recording.
                } finally {
                    watchdog?.cancel(false)
                    runCatching { accepted.close() }
                    untrackSocket(accepted)
                }
            }
        }
    }

    /**
     * Closes the server once nothing has asked it for anything in a while.
     *
     * The hand-off has no completion to listen for — the viewer simply stops watching in the other
     * app — so idleness is the only signal that the socket and the URLs behind it can go.
     */
    private fun reapWhenIdle(socket: ServerSocket, generation: Long) {
        while (!socket.isClosed) {
            try {
                Thread.sleep(IDLE_CHECK_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
            if (System.nanoTime() - lastRequestAt.get() < IDLE_SHUTDOWN_NS) continue
            val didStop = synchronized(lifecycleLock) {
                if (serverSocket !== socket || lifecycleGeneration != generation) return
                // Accepted or queued work keeps the listener alive even if it has not produced
                // bytes recently. Starting a fresh idle interval avoids rechecking continuously.
                if (activeResources.hasSockets()) {
                    lastRequestAt.set(System.nanoTime())
                    false
                } else {
                    serverSocket = null
                    lifecycleGeneration++
                    registry.clear()
                    runCatching { socket.close() }
                    true
                }
            }
            if (!didStop) continue
            appContext?.let { context ->
                context.stopService(LocalStreamProxyService.intent(context))
            }
            DiagnosticsLog.event("LocalStreamProxy closed after 6 hours idle")
            return
        }
    }

    private fun serve(connection: Socket, output: OutputStream, generation: Long) {
        lastRequestAt.set(System.nanoTime())
        val input = connection.getInputStream().buffered()
        val request = readRequest(input) ?: return respond(output, 400, "Bad Request")
        val entry = registry.get(request.token) ?: return respond(output, 404, "Not Found")
        relay(request, entry, output, generation)
        output.flush()
    }

    private fun relay(request: ProxyRequest, entry: Entry, output: OutputStream, generation: Long) {
        val builder = Request.Builder().url(entry.url)
        entry.headerContext.headers.forEach { (name, value) -> builder.header(name, value) }
        builder.tag(RootHeaderContext::class.java, entry.headerContext)
        // The client's own conditional and range headers are the only ones forwarded. Everything
        // else is this app's to state: a Referer echoed back from the other player would overwrite
        // the one that makes the request work.
        request.forwardable.forEach { (name, value) -> builder.header(name, value) }
        if (request.method == "HEAD") builder.head()

        val call = client.newCall(builder.build())
        if (!trackCall(call, generation)) return respond(output, 503, "Service Unavailable")
        try {
        call.execute().use { response ->
            val contentType = response.header("Content-Type")
            val passthrough = passthroughHeaders(response.headers)
            if (request.method == "HEAD") {
                respondHeaders(output, response.code, contentType, null, passthrough)
                return
            }
            val body = response.body ?: run {
                respondHeaders(output, response.code, contentType, 0L, passthrough)
                return
            }

            // Sniff only a tiny prefix. An ordinary segment or direct file begins streaming after
            // these bytes; only a response that identifies itself as HLS is retained for rewriting.
            val stream = body.byteStream()
            val prefix = stream.readHead(PLAYLIST_SNIFF_BYTES)
            val hls = looksLikeHlsPlaylist(contentType, prefix.bytes)
            val buffered = if (hls && !prefix.complete) {
                stream.readBody(prefix.bytes, MAX_PLAYLIST_BYTES)
            } else {
                prefix
            }
            if (hls && buffered.complete) {
                val rewritten = runCatching {
                    synchronized(lifecycleLock) {
                        check(lifecycleGeneration == generation && serverSocket?.isClosed == false) {
                            "Local stream proxy stopped during playlist rewrite"
                        }
                        registry.transaction { registerEntry ->
                            rewriteHlsPlaylist(
                                playlist = buffered.bytes.decodeToString(),
                                playlistUrl = response.request.url.toString(),
                                register = { childUrl ->
                                    requireNoHttpUserInfo(childUrl)
                                    val child = Entry(childUrl, entry.headerContext)
                                    localUrl(port, registerEntry(child, approximateEntryBytes(child)))
                                },
                            ).encodeToByteArray()
                        }
                    }
                }.getOrElse {
                    val code = if (it is UrlRegistryCapacityException) 503 else 400
                    respond(output, code, if (code == 503) "Registry Full" else "Bad Request")
                    return
                }
                // The rewritten playlist is a different size than the one upstream sent, so its own
                // length is stated and upstream's is dropped.
                respondHeaders(output, response.code, contentType, rewritten.size.toLong(), emptyList())
                output.write(rewritten)
                return
            }

            val length = if (buffered.complete) buffered.bytes.size.toLong() else null
            respondHeaders(output, response.code, contentType, length, passthrough)
            output.write(buffered.bytes)
            if (!buffered.complete) stream.copyTo(output)
        }
        } finally {
            untrackCall(call)
        }
    }

    fun shutdown() {
        val (listener, resources) = synchronized(lifecycleLock) {
            lifecycleGeneration++
            val listener = serverSocket
            serverSocket = null
            port = 0
            registry.clear()
            listener to activeResources.takeAll()
        }
        listener?.let { runCatching { it.close() } }
        stopProxyResources(resources)
    }

    /** The response headers a player needs to seek: the range and length ones, and nothing else. */
    private fun passthroughHeaders(headers: okhttp3.Headers): List<Pair<String, String>> =
        PASSTHROUGH.mapNotNull { name -> headers[name]?.let { name to it } }

    private val PASSTHROUGH = listOf("Accept-Ranges", "Content-Range", "Content-Length")

    private fun respondHeaders(
        output: OutputStream,
        code: Int,
        contentType: String?,
        contentLength: Long?,
        extra: List<Pair<String, String>>,
    ) {
        val head = buildString {
            append("HTTP/1.1 ").append(code).append(' ').append(reason(code)).append(CRLF)
            contentType?.let { append("Content-Type: ").append(it).append(CRLF) }
            // Stated once, from whichever of the two sources is authoritative here: a rewritten
            // playlist is a different length than the one upstream sent.
            contentLength?.let { append("Content-Length: ").append(it).append(CRLF) }
            extra.filterNot { contentLength != null && it.first == "Content-Length" }
                .forEach { (name, value) -> append(name).append(": ").append(value).append(CRLF) }
            // Every response is close-delimited. It costs a TCP connection per segment, which on
            // loopback is nothing, and it removes any need to agree with the player about where a
            // body of unknown length ends.
            append("Connection: close").append(CRLF).append(CRLF)
        }
        output.write(head.toByteArray())
    }

    private fun respond(output: OutputStream, code: Int, reason: String) {
        output.write("HTTP/1.1 $code $reason${CRLF}Content-Length: 0${CRLF}Connection: close$CRLF$CRLF".toByteArray())
        output.flush()
    }

    private fun reason(code: Int): String = when (code) {
        200 -> "OK"
        206 -> "Partial Content"
        304 -> "Not Modified"
        else -> "Status $code"
    }

    private const val CRLF = "\r\n"
    private const val SOCKET_TIMEOUT_MS = 30_000
    private const val IDLE_CHECK_MS = 30_000L
    private const val IDLE_SHUTDOWN_NS = 6L * 60 * 60 * 1_000_000_000L
    private const val WRITE_STALL_TIMEOUT_MS = 60_000L
    private const val WRITE_STALL_TIMEOUT_NS = WRITE_STALL_TIMEOUT_MS * 1_000_000L
    private const val REQUEST_WORKER_COUNT = 8
    private const val REQUEST_QUEUE_CAPACITY = 32
    private const val REGISTRY_MAX_ENTRIES = 4_096
    private const val REGISTRY_MAX_APPROXIMATE_BYTES = 8L * 1024 * 1024
}

internal class UrlRegistryCapacityException : IllegalStateException("Local stream proxy URL registry is full")

internal data class TrackedProxyResources(val calls: List<Call>, val sockets: List<Socket>)

/** Thread-safe tracking shared by notification shutdown and tests. */
internal class ProxyActiveResources {
    private val calls = LinkedHashSet<Call>()
    private val sockets = LinkedHashSet<Socket>()

    @Synchronized fun trackCall(call: Call) { calls += call }
    @Synchronized fun untrackCall(call: Call) { calls -= call }
    @Synchronized fun trackSocket(socket: Socket) { sockets += socket }
    @Synchronized fun untrackSocket(socket: Socket) { sockets -= socket }
    @Synchronized fun hasSockets(): Boolean = sockets.isNotEmpty()

    @Synchronized
    fun takeAll(): TrackedProxyResources {
        val resources = TrackedProxyResources(calls.toList(), sockets.toList())
        calls.clear()
        sockets.clear()
        return resources
    }
}

internal fun stopProxyResources(resources: TrackedProxyResources) {
    resources.calls.forEach { it.cancel() }
    resources.sockets.forEach { runCatching { it.close() } }
}

/** Synchronized, non-evicting registry: a token remains valid until explicit clear/shutdown. */
internal class BoundedUrlTokenRegistry<V>(
    private val maxEntries: Int,
    private val maxApproximateBytes: Long,
    private val tokenFactory: () -> String,
) {
    private val valuesByToken = LinkedHashMap<String, V>()
    private val tokensByValue = HashMap<V, String>()
    private val bytesByToken = HashMap<String, Long>()
    private var approximateBytes = 0L

    init {
        require(maxEntries > 0 && maxApproximateBytes > 0)
    }

    @Synchronized
    fun register(value: V, approximateBytes: Long): String = registerInternal(value, approximateBytes)

    /** Adds all values atomically; a failed playlist rewrite leaves no orphan registrations. */
    @Synchronized
    fun <R> transaction(registerValues: (((V, Long) -> String)) -> R): R {
        val inserted = mutableListOf<String>()
        try {
            return registerValues { value, bytes ->
                val existed = tokensByValue.containsKey(value)
                registerInternal(value, bytes).also { token -> if (!existed) inserted += token }
            }
        } catch (error: Throwable) {
            inserted.forEach(::removeToken)
            throw error
        }
    }

    @Synchronized
    fun get(token: String): V? = valuesByToken[token]

    @Synchronized
    fun clear() {
        valuesByToken.clear()
        tokensByValue.clear()
        bytesByToken.clear()
        approximateBytes = 0L
    }

    @Synchronized
    fun size(): Int = valuesByToken.size

    @Synchronized
    fun approximateBytes(): Long = approximateBytes

    private fun registerInternal(value: V, bytes: Long): String {
        require(bytes >= 0L)
        tokensByValue[value]?.let { return it }
        if (valuesByToken.size >= maxEntries || bytes > maxApproximateBytes - approximateBytes) {
            throw UrlRegistryCapacityException()
        }
        var token: String
        do token = tokenFactory() while (token in valuesByToken)
        valuesByToken[token] = value
        tokensByValue[value] = token
        bytesByToken[token] = bytes
        approximateBytes += bytes
        return token
    }

    private fun removeToken(token: String) {
        val value = valuesByToken.remove(token) ?: return
        tokensByValue.remove(value)
        approximateBytes -= bytesByToken.remove(token) ?: 0L
    }
}

internal fun newBoundedProxyRequestExecutor(workerCount: Int, queueCapacity: Int): ThreadPoolExecutor {
    require(workerCount > 0 && queueCapacity > 0)
    return ThreadPoolExecutor(
        workerCount,
        workerCount,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(queueCapacity),
        namedDaemonThreadFactory("anilili-stream-proxy"),
        ThreadPoolExecutor.AbortPolicy(),
    )
}

internal fun dispatchAcceptedSocket(
    executor: ThreadPoolExecutor,
    socket: Socket,
    onRejected: () -> Unit = {},
    handler: (Socket) -> Unit,
) {
    try {
        executor.execute { handler(socket) }
    } catch (_: RejectedExecutionException) {
        // A saturated bounded pool must not leave accepted clients hanging in the kernel backlog.
        runCatching { socket.close() }
        onRejected()
    }
}

private fun namedDaemonThreadFactory(prefix: String) = ThreadFactory { runnable ->
    Thread(runnable, "$prefix-${System.nanoTime()}").apply { isDaemon = true }
}

/** A User-Agent alone works directly; only identity/gating headers justify a local proxy. */
internal fun needsLocalStreamProxy(headers: Map<String, String>): Boolean = headers.any { (name, value) ->
    name.isNotBlank() && value.isNotBlank() && !name.equals("User-Agent", ignoreCase = true)
}

internal fun requireNoHttpUserInfo(url: String) {
    val parsed = url.toHttpUrlOrNull() ?: return
    require(parsed.username.isEmpty() && parsed.password.isEmpty()) {
        "Local stream proxy does not accept HTTP URLs containing userinfo"
    }
}

/** A normalized HTTP origin, including the URL's effective (defaulted) port. */
internal data class HttpOrigin(val scheme: String, val host: String, val port: Int) {
    companion object {
        fun from(url: HttpUrl) = HttpOrigin(url.scheme.lowercase(), url.host.lowercase(), url.port)
    }
}

/** Immutable headers and origin captured when the root stream is published. */
internal data class RootHeaderContext(
    val origin: HttpOrigin,
    val headers: Map<String, String>,
)

/** Reapplies app-owned headers according to the origin of each actual network request. */
internal object RootHeaderPolicyInterceptor : Interceptor {
    private val SAFE_BROWSER_HEADERS = setOf(
        "referer", "origin", "user-agent", "accept", "accept-language",
    )
    private val RANGE_HEADERS = setOf("range", "if-range", "if-none-match", "if-modified-since")
    // These are generated or required by HTTP transport and are not site identity headers.
    private val TRANSPORT_HEADERS = setOf(
        "host", "connection", "accept-encoding", "content-length", "transfer-encoding",
        "te", "trailer", "upgrade", "proxy-connection",
    )

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val context = request.tag(RootHeaderContext::class.java)
            ?: return chain.proceed(request)
        if (HttpOrigin.from(request.url) == context.origin) return chain.proceed(request)

        val allowed = SAFE_BROWSER_HEADERS + RANGE_HEADERS + TRANSPORT_HEADERS
        val builder = request.newBuilder()
        request.headers.names().forEach { name ->
            if (name.lowercase() !in allowed) builder.removeHeader(name)
        }
        // Use only browser headers explicitly captured from this app. In particular, values
        // supplied by an external player can never become Cookie, Referer, or User-Agent here.
        context.headers.forEach { (name, value) ->
            if (name.lowercase() in SAFE_BROWSER_HEADERS) builder.header(name, value)
        }
        return chain.proceed(builder.build())
    }
}

/** One parsed request: which registered URL it wants, and what to carry upstream with it. */
internal data class ProxyRequest(
    val method: String,
    val token: String,
    val forwardable: List<Pair<String, String>>,
)

/** The request headers that describe *which bytes* are wanted rather than who is asking. */
private val FORWARDABLE = setOf("range", "if-range", "if-none-match", "if-modified-since")

/**
 * Reads a request head, or null when it is not one this can answer.
 *
 * Only the head is read: this never has a request body to consume, and a player that sent one
 * would be doing something no player does.
 */
internal fun readRequest(input: InputStream): ProxyRequest? {
    val budget = RequestHeaderBudget()
    val requestLine = readLine(input, budget) ?: return null
    val parts = requestLine.split(' ')
    if (parts.size < 2) return null
    val method = parts[0].uppercase()
    if (method != "GET" && method != "HEAD") return null
    val token = parts[1].substringAfter('/').substringBefore('?').substringBefore('#')
    if (token.isEmpty()) return null

    val forwardable = mutableListOf<Pair<String, String>>()
    while (true) {
        // EOF, an over-limit line/head, and a completed blank line are different outcomes.
        // Only the blank line terminates a valid request head.
        val line = readLine(input, budget) ?: return null
        if (line.isEmpty()) break
        val name = line.substringBefore(':', missingDelimiterValue = "")
        if (name.lowercase() in FORWARDABLE) forwardable += name to line.substringAfter(':').trim()
    }
    return ProxyRequest(method, token, forwardable)
}

/** One CRLF-terminated line, or null at end of stream. Header lines are short by definition. */
private class RequestHeaderBudget(var bytes: Int = 0, var lines: Int = 0)

private fun readLine(input: InputStream, budget: RequestHeaderBudget): String? {
    val line = StringBuilder()
    var lineBytes = 0
    while (true) {
        val next = input.read()
        if (next == -1) return null
        budget.bytes++
        lineBytes++
        if (budget.bytes > MAX_REQUEST_HEADER_BYTES || lineBytes > MAX_HEADER_LINE) return null
        if (next == '\n'.code) {
            budget.lines++
            if (budget.lines > MAX_HEADER_LINES) return null
            return line.toString().removeSuffix("\r")
        }
        line.append(next.toChar())
    }
}

private const val MAX_HEADER_LINE = 8 * 1024
private const val MAX_REQUEST_HEADER_BYTES = 32 * 1024
private const val MAX_HEADER_LINES = 100

/** The start of a body, and whether that turned out to be all of it. */
internal class BodyHead(val bytes: ByteArray, val complete: Boolean)

/**
 * Reads up to [limit] bytes, reporting whether the stream ended within them.
 *
 * [BodyHead.complete] is what decides whether the caller may treat this as a playlist: rewriting a
 * body it has only part of would hand the other player a truncated one, so anything longer is
 * streamed through as media instead — and the bytes already read are still returned, because the
 * stream cannot be rewound to send them again.
 */
internal fun InputStream.readHead(limit: Int): BodyHead {
    val buffer = ByteArray(limit)
    var filled = 0
    while (filled < limit) {
        val read = read(buffer, filled, limit - filled)
        if (read == -1) return BodyHead(buffer.copyOf(filled), complete = true)
        if (read == 0) continue
        filled += read
    }
    return BodyHead(buffer, complete = false)
}

/** Continues an already-sniffed body, retaining at most [limit] bytes plus one overflow byte. */
internal fun InputStream.readBody(prefix: ByteArray, limit: Int): BodyHead {
    require(prefix.size <= limit)
    val output = ByteArrayOutputStream(minOf(limit, prefix.size + DEFAULT_BUFFER_SIZE))
    output.write(prefix)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (output.size() <= limit) {
        val remaining = limit + 1 - output.size()
        val read = read(buffer, 0, minOf(buffer.size, remaining))
        if (read == -1) return BodyHead(output.toByteArray(), complete = true)
        if (read == 0) continue
        output.write(buffer, 0, read)
    }
    return BodyHead(output.toByteArray(), complete = false)
}

/** Lets a watchdog close a socket whose receiver has stopped consuming bytes. */
private class ProgressOutputStream(output: OutputStream) : FilterOutputStream(output) {
    val lastWriteAt = AtomicLong(System.nanoTime())

    override fun write(value: Int) = track { out.write(value) }

    override fun write(buffer: ByteArray, offset: Int, length: Int) =
        track { out.write(buffer, offset, length) }

    override fun flush() = track { out.flush() }

    private inline fun track(block: () -> Unit) {
        block()
        lastWriteAt.set(System.nanoTime())
    }
}
