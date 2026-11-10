@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
package com.example.animepahedl

import androidx.core.net.toUri
import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext

/** Exposes only already-cached resources to FFmpeg, bound to loopback and protected by a random token. */
@androidx.annotation.OptIn(UnstableApi::class)
internal class CachedHlsServer(private val masterUri: String? = null, private val quality: String = "Best") : AutoCloseable {
    private val socket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val workers = java.util.concurrent.ThreadPoolExecutor(4, 4, 0, java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.ArrayBlockingQueue<Runnable>(8))
    private val clients = java.util.concurrent.ConcurrentHashMap.newKeySet<Socket>()
    private val token = UUID.randomUUID().toString()
    private val factory = CacheDataSource.Factory().setCache(OfflineDownloads.cache).setUpstreamDataSourceFactory(null)
    private val acceptor = Thread({
        while (!socket.isClosed) {
            val client = try { socket.accept() } catch (_: IOException) { break }
            clients += client
            try { workers.execute { try { serve(client) } finally { clients -= client } } }
            catch (_: java.util.concurrent.RejectedExecutionException) { clients -= client; client.close() }
        }
    }, "hls-cache-accept").apply { isDaemon = true; start() }
    fun url(uri: String): String = "http://127.0.0.1:${socket.localPort}/$token/" + Base64.getUrlEncoder().withoutPadding().encodeToString(uri.toByteArray(Charsets.UTF_8))
    private fun serve(client: Socket) {
        client.use { c ->
            var sent = false
            try {
                c.soTimeout = 15000
                val input = c.getInputStream()
                val deadline = System.nanoTime() + 15_000_000_000L
                fun line(): String? {
                    val bytes = java.io.ByteArrayOutputStream()
                    while (true) {
                        require(System.nanoTime() < deadline) { "Request deadline exceeded" }
                        c.soTimeout = ((deadline - System.nanoTime()) / 1_000_000L).coerceIn(1L, 15000L).toInt()
                        val value = input.read()
                        if (value < 0) return null
                        if (value == 10) return bytes.toString("ISO-8859-1").removeSuffix("\r")
                        require(bytes.size() < 8192) { "Header line too long" }
                        bytes.write(value)
                    }
                }
                val request = line()?.takeIf { it.length <= 8192 } ?: return
                val parts = request.split(' ')
                require(parts.first() in setOf("GET", "HEAD"))
                var range: String? = null
                var headerBytes = request.length
                while (true) {
                    val line = line() ?: return
                    headerBytes += line.length
                    require(headerBytes <= 16384)
                    if (line.isEmpty()) break
                    if (line.startsWith("Range:", true)) range = line.substringAfter(':').trim()
                }
                val path = parts.getOrNull(1) ?: return
                val prefix = "/$token/"
                require(path.startsWith(prefix))
                val uri = String(Base64.getUrlDecoder().decode(path.removePrefix(prefix)), Charsets.UTF_8)
                val dataSource = factory.createDataSource()
                try {
                    val length = dataSource.open(DataSpec.Builder().setUri((uri).toUri()).build())
                    val source = object : java.io.InputStream() {
                        override fun read(): Int { val one = ByteArray(1); return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 255 }
                        override fun read(buffer: ByteArray, offset: Int, size: Int) = dataSource.read(buffer, offset, size)
                        override fun close() = dataSource.close()
                    }
                    val peek = java.io.PushbackInputStream(source, 7)
                    val first = ByteArray(7)
                    var count = 0
                    while (count < first.size) { val n = peek.read(first, count, first.size - count); if (n < 0) break; count += n }
                    peek.unread(first, 0, count)
                    val playlist = count == 7 && String(first, Charsets.UTF_8) == "#EXTM3U"
                    val data = if (playlist) {
                        val bytes = peek.readBytesBounded(1024 * 1024)
                        val base = URI(uri)
                        fun rewrite(value: String) = url(base.resolve(value).toString())
                        val original = String(bytes, Charsets.UTF_8)
                        val selected = if (uri == masterUri) selectCachedHlsVariant(original, quality) else original
                        selected.lineSequence().joinToString("\n") { line ->
                            if (line.startsWith("#")) Regex("URI=\"([^\"]+)\"").replace(line) { "URI=\"${rewrite(it.groupValues[1])}\"" }
                            else if (line.isNotBlank()) rewrite(line.trim()) else line
                        }.toByteArray(Charsets.UTF_8)
                    } else null
                    val total = data?.size?.toLong() ?: length
                    require(total >= 0) { "Cached resource length unavailable" }
                    val selected = try { parseByteRange(range, total) } catch (_: IllegalArgumentException) {
                        c.getOutputStream().write("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$total\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                        return
                    }
                    val start = selected?.first ?: 0L
                    val size = selected?.let { it.last - it.first + 1 } ?: total
                    val header = if (selected != null) "HTTP/1.1 206 Partial Content\r\nContent-Range: bytes ${selected.first}-${selected.last}/$total\r\n" else "HTTP/1.1 200 OK\r\n"
                    val out = c.getOutputStream()
                    out.write((header + "Accept-Ranges: bytes\r\nContent-Length: $size\r\nConnection: close\r\n\r\n").toByteArray(Charsets.ISO_8859_1))
                    sent = true
                    if (parts.first() == "HEAD") return
                    if (data != null) out.write(data, start.toInt(), size.toInt())
                    else if (start == 0L) peek.copyExactly(out, size)
                    else {
                        // Reopen at the requested cache offset instead of loading or skipping the resource.
                        source.close()
                        DataSourceInputStream(factory.createDataSource(), DataSpec.Builder().setUri((uri).toUri())
                            .setPosition(start).setLength(size).build()).use { it.copyExactly(out, size) }
                    }
                } finally { dataSource.close() }
            } catch (_: Exception) {
                if (!sent) runCatching { c.getOutputStream().write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()) }
            }
        }
    }
    override fun close() {
        socket.close()
        clients.forEach { runCatching { it.close() } }
        workers.shutdownNow()
        acceptor.join(1000)
    }
}

private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(16384)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toByteArray()
        require(result.size() + count <= limit) { "Playlist exceeds size limit" }
        result.write(buffer, 0, count)
    }
}
private fun java.io.InputStream.copyExactly(output: java.io.OutputStream, size: Long) {
    val buffer = ByteArray(64 * 1024)
    var remaining = size
    while (remaining > 0) {
        val count = read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
        if (count < 0) throw IOException("Cached resource is incomplete")
        output.write(buffer, 0, count)
        remaining -= count
    }
}

/** Cache-span checks only: no network requests, media decoding, or second media scan. */
@androidx.annotation.OptIn(UnstableApi::class)
internal fun requireCachedHlsSegments(context: Context, id: String): Long {
    val request = OfflineDownloads.manager(context).downloadIndex.getDownload(id)?.request
        ?: error("Temporary download is unavailable")
    val cache = OfflineDownloads.cache
    val factory = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(null)
    fun playlist(uri: Uri) = DataSourceInputStream(factory.createDataSource(), DataSpec.Builder().setUri(uri).build()).use {
        androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser().parse(uri, it)
    }
    fun resource(base: String, path: String, offset: Long = 0, length: Long = androidx.media3.common.C.LENGTH_UNSET.toLong()) {
        val key = androidx.media3.common.util.UriUtil.resolve(base, path)
        val size = if (length != androidx.media3.common.C.LENGTH_UNSET.toLong()) length else
            androidx.media3.datasource.cache.ContentMetadata.getContentLength(cache.getContentMetadata(key)) - offset
        require(size > 0 && cache.isCached(key, offset, size)) {
            "Source segment is missing or incomplete; cached media retained. Retry download."
        }
    }
    val manifest = playlist(request.uri)
    val media = when (manifest) {
        is androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist -> listOf(manifest)
        is androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist -> {
            val selected = if (request.streamKeys.isEmpty()) manifest else manifest.copy(request.streamKeys)
            selected.mediaPlaylistUrls.map { playlist(it) as androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist }
        }
        else -> error("Unsupported playlist")
    }
    require(media.isNotEmpty()) { "Selected playlists are missing" }
    for (item in media) {
        require(item.hasEndTag && item.segments.isNotEmpty() && item.durationUs > 0) { "Incomplete HLS playlist; cached media retained" }
        val checked = mutableSetOf<Triple<String, Long, Long>>()
        fun segment(segment: androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist.Segment) {
            if (checked.add(Triple(segment.url, segment.byteRangeOffset, segment.byteRangeLength)))
                resource(item.baseUri, segment.url, segment.byteRangeOffset, segment.byteRangeLength)
            segment.fullSegmentEncryptionKeyUri?.let { key ->
                if (checked.add(Triple(key, 0L, -1L))) resource(item.baseUri, key)
            }
        }
        for (entry in item.segments) {
            entry.initializationSegment?.let { segment(it) }
            segment(entry)
        }
    }
    return media.first().durationUs
}

@OptIn(kotlinx.coroutines.InternalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
internal suspend fun ffmpegRemuxCachedHls(context: Context, id: String, output: File, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
    val download = OfflineDownloads.manager(context).downloadIndex.getDownload(id) ?: error("Temporary download is unavailable")
    val executable = File(context.applicationInfo.nativeLibraryDir, "libffmpeg_exec.so")
    require(executable.exists()) { "FFmpeg is available only on arm64 devices" }
    val durationUs = requireCachedHlsSegments(context, id)
    val record = Exports.all(context)[id]
    val siteDownload = record?.identity?.isSiteDownload == true
    val server = CachedHlsServer(download.request.uri.toString().takeIf { siteDownload }, record?.quality ?: "Best")
    val errors = File(context.cacheDir, "ffmpeg-${id.hashCode().toUInt()}.log")
    try {
        val args = listOf(executable.absolutePath, "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-stats_period", "0.25", "-progress", "pipe:1",
            "-protocol_whitelist", "http,tcp,crypto,data", "-allowed_extensions", "ALL", "-extension_picky", "0", "-f", "hls", "-i", server.url(download.request.uri.toString()),
            "-map", "0:v:0", "-map", if (!siteDownload && record?.audio?.isNotBlank() == true) "0:a:0" else "0:a:0?", "-c", "copy", "-f", "mp4", output.absolutePath)
        val process = ProcessBuilder(args).redirectError(errors).start()
        val lastProgress = java.util.concurrent.atomic.AtomicLong(System.nanoTime())
        var elapsedBefore = -1L
        var percentBefore = -1
        val watchdog = Executors.newSingleThreadScheduledExecutor()
        val stalled = java.util.concurrent.atomic.AtomicBoolean(false)
        watchdog.scheduleWithFixedDelay({
            if (System.nanoTime() - lastProgress.get() > 120_000_000_000L) { stalled.set(true); process.destroyForcibly() }
        }, 10, 10, java.util.concurrent.TimeUnit.SECONDS)
        val cancel = coroutineContext[Job]?.invokeOnCompletion(onCancelling = true, invokeImmediately = true) { if (it != null) process.destroyForcibly() }
        try {
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach { line ->
                coroutineContext.ensureActive()
                if (line.startsWith("out_time_us=")) {
                    val elapsed = line.substringAfter('=').toLongOrNull() ?: 0L
                    if (elapsed > elapsedBefore) { elapsedBefore = elapsed; lastProgress.set(System.nanoTime()) }
                    val percent = (elapsed * 100 / durationUs).coerceIn(0, 99).toInt()
                    if (percent > percentBefore) { percentBefore = percent; onProgress(percent) }
                }
            } }
            coroutineContext.ensureActive()
            if (process.waitFor() != 0) throw IOException(if (stalled.get()) "FFmpeg made no progress for two minutes. Retry save; the source is retained." else "FFmpeg could not remux the cached HLS stream")
            onProgress(100)
        } finally { watchdog.shutdownNow(); cancel?.dispose(); if (process.isAlive) process.destroyForcibly() }
    } finally { server.close(); errors.delete() }
}
