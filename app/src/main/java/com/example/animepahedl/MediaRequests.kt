package com.example.animepahedl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Headers belong to one resolved episode, never to a mutable shared client. */
internal fun mediaRequestHeaders(media: ResolvedMedia, target: String = media.url): Map<String, String> {
    val origin = media.url.toHttpUrl()
    val destination = target.toHttpUrl()
    val credentialsAllowed = destination.host == origin.host && destination.isHttps == origin.isHttps && destination.port == origin.port
    return buildMap {
        put("User-Agent", AnimeNetwork.userAgent)
        media.headers.orEmpty().forEach { (name, value) ->
            if (name.lowercase() in setOf("host", "connection", "content-length", "range")) return@forEach
            if (!credentialsAllowed && name.lowercase() in setOf("cookie", "authorization", "proxy-authorization")) return@forEach
            keys.firstOrNull { it.equals(name, true) }?.let { remove(it) }
            put(name, value)
        }
    }
}

internal fun ResolvedMedia.isHls(): Boolean = mimeType?.substringBefore(';')?.trim()?.lowercase() in
    setOf("application/x-mpegurl", "application/vnd.apple.mpegurl", "application/mpegurl") ||
    url.toHttpUrl().encodedPath.endsWith(".m3u8", true)

internal object MediaRequests {
    // This client deliberately has no AnimePahe cookie jar or default AnimePahe Referer.
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build()

    fun client(media: ResolvedMedia, cookieHeader: ((String) -> String)? = null): OkHttpClient = client.newBuilder().addNetworkInterceptor { chain ->
        val builder = chain.request().newBuilder()
        val allowed = mediaRequestHeaders(media, chain.request().url.toString())
        // OkHttp follows redirects inside one application call. Check credentials on every hop.
        listOf("Cookie", "Authorization", "Proxy-Authorization").forEach { name ->
            if (allowed.keys.none { it.equals(name, true) }) builder.removeHeader(name)
        }
        if (cookieHeader != null) {
            builder.removeHeader("Cookie")
            cookieHeader.invoke(chain.request().url.toString()).takeIf { it.isNotBlank() }?.let { builder.header("Cookie", it) }
        }
        chain.proceed(builder.build())
    }.build()

    /** Probe alternatives before enqueueing, keeping the selected URL and its scoped headers durable. */
    suspend fun available(media: ResolvedMedia, cookieHeader: ((String) -> String)? = null): ResolvedMedia = withContext(Dispatchers.IO) {
        var lastFailure: Exception? = null
        for (candidate in (listOf(media.url) + media.backupUrls.orEmpty()).distinct()) {
            try {
                val headers = mediaRequestHeaders(media, candidate)
                val request = Request.Builder().url(candidate).apply {
                    headers.forEach { (name, value) -> header(name, value) }
                    if (!media.isHls() && !candidate.toHttpUrl().encodedPath.endsWith(".m3u8", true)) {
                        header("Range", "bytes=0-1023")
                    }
                }.build()
                val selected = client(media, cookieHeader).newCall(request).consumeCancellable { response ->
                    if (!response.isSuccessful) throw HttpFailure(candidate, candidate, response.code, "Media server rejected the request")
                    val type = response.header("Content-Type").orEmpty().substringBefore(';').trim().lowercase()
                    if (type.contains("text/html")) throw IOException("The media URL returned a web page, not a video or HLS playlist.")
                    val peek = response.peekBody(1024).string().trimStart('\uFEFF', ' ', '\n', '\r', '\t')
                    val inferred = when {
                        peek.startsWith("#EXTM3U") -> "application/x-mpegURL"
                        type in setOf("video/mp4", "video/webm", "application/x-mpegurl", "application/vnd.apple.mpegurl") -> type
                        else -> media.mimeType ?: type.takeIf { it.isNotBlank() }
                    }
                    val finalUrl = response.request.url.toString()
                    val finalHeaders = mediaRequestHeaders(media, finalUrl).toMutableMap()
                    if (cookieHeader != null) {
                        finalHeaders.keys.filter { it.equals("Cookie", true) }.forEach { finalHeaders.remove(it) }
                        cookieHeader.invoke(finalUrl).takeIf { it.isNotBlank() }?.let { finalHeaders["Cookie"] = it }
                    }
                    media.copy(url = finalUrl, headers = finalHeaders, mimeType = inferred ?: media.mimeType,
                        backupUrls = (listOf(media.url) + media.backupUrls.orEmpty()).distinct().filter { it != finalUrl })
                }
                return@withContext selected
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { lastFailure = failure }
        }
        throw lastFailure ?: IOException("No media URL is available")
    }
}
