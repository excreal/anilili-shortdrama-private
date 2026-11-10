package com.example.animepahedl

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Keep
data class MediaSubtitle(val url: String, val label: String, val language: String,
    val mimeType: String = "text/vtt", val isDefault: Boolean = false)

internal fun unreferencedProviderSubtitleFile(url: String, directory: File, retained: Set<String>): File? {
    if (url in retained) return null
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
    if (uri.scheme != "file") return null
    val file = runCatching { File(uri.path.orEmpty()).canonicalFile }.getOrNull() ?: return null
    return file.takeIf { it.parentFile == directory.canonicalFile && it.extension == "vtt" }
}

/** Normalized files live outside cache so downloaded captions remain available offline. */
internal object ProviderSubtitles {
    private lateinit var context: Context
    @Volatile private var downloadsReady = false
    fun markDownloadsReady() { downloadsReady = true }
    private val active = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    fun init(context: Context) { this.context = context.applicationContext }
    fun hold(tracks: List<MediaSubtitle>) { active.addAll(tracks.map { it.url }) }
    fun release(tracks: List<MediaSubtitle>) {
        active.removeAll(tracks.map { it.url }.toSet())
        deleteUnreferenced("", tracks)
    }
    /** Remove app-local subtitle copies when their last downloaded-video reference is deleted. */
    fun deleteUnreferenced(downloadId: String, tracks: List<MediaSubtitle>) {
        if (!::context.isInitialized || tracks.isEmpty()) return
        val retained = DownloadStore.all().filterKeys { it != downloadId }.values
            .flatMap { it.media?.subtitles.orEmpty() }.map { it.url }.toSet() + active
        val directory = File(context.filesDir, "provider-subtitles").canonicalFile
        tracks.forEach { track ->
            unreferencedProviderSubtitleFile(track.url, directory, retained)?.delete()
        }
    }
    suspend fun localize(client: OkHttpClient, track: MediaSubtitle): MediaSubtitle = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "provider-subtitles").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256").digest(track.url.toByteArray()).joinToString("") { "%02x".format(it) }
        val target = File(directory, "$hash.vtt")
        if (!target.isFile || target.length() == 0L) {
            val request = providerSubtitleRequest(track)
            val bytes = fetchProviderSubtitleBytes(client, request, track.label)
            val content = normalizeProviderSubtitle(bytes.toString(Charsets.UTF_8), Uri.parse(track.url).path.orEmpty().substringAfterLast('.'))
            coroutineContext.ensureActive()
            val temporary = File(directory, "$hash.${java.util.UUID.randomUUID()}.tmp")
            try { temporary.writeText(content); check(temporary.renameTo(target)) { "Could not save subtitles." } }
            finally { temporary.delete() }
        }
        target.setLastModified(System.currentTimeMillis())
        track.copy(url = Uri.fromFile(target).toString(), mimeType = "text/vtt")
    }
    /** Keep downloaded tracks and active tracks; bound other recently streamed captions. */
    fun trim() {
        if (!downloadsReady) return
        val retained = DownloadStore.all().values.flatMap { it.media?.subtitles.orEmpty() }.map { it.url }.toSet() + active
        File(context.filesDir, "provider-subtitles").listFiles()?.filter { it.extension == "vtt" }
            ?.filter { Uri.fromFile(it).toString() !in retained }?.sortedByDescending { it.lastModified() }?.drop(64)?.forEach { it.delete() }
    }
}

/** Subtitle descriptors may point at third-party CDNs, so never attach a provider Referer. */
internal fun providerSubtitleRequest(track: MediaSubtitle): Request {
    return Request.Builder().url(track.url).header("User-Agent", AnimeNetwork.userAgent).build()
}

/** Download a bounded subtitle body while keeping coroutine cancellation connected to OkHttp. */
internal suspend fun fetchProviderSubtitleBytes(client: OkHttpClient, request: Request, label: String): ByteArray {
    val bytes = client.newCall(request).consumeCancellable { response ->
        check(response.isSuccessful) { "Could not download $label subtitles (${response.code})." }
        response.body.byteStream().use { it.readNBytes(4_000_001) }
    }
    require(bytes.size <= 4_000_000) { "Subtitle file is too large." }
    return bytes
}

/** Convert provider captions to VTT, tolerating legacy encrypted cues and cue positioning. */
internal fun normalizeProviderSubtitle(input: String, extension: String): String {
    val raw = input.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').trim()
    require(!raw.startsWith('<') && raw.contains("-->")) { "The source returned an invalid subtitle file." }
    if (raw.startsWith("WEBVTT") && extension.equals("vtt", true)) return raw + "\n"
    val keyAndIv = when (extension.lowercase()) {
        "txt" -> "8056483646328763" to "6852612370185273"
        "txt1" -> "AmSmZVcH93UQUezi" to "ReBKWW8cqdjPEnF6"
        "srt", "vtt" -> null
        else -> "sWODXX04QRTkHdlZ" to "8pwhapJeC4hrS9hO"
    }
    val output = StringBuilder("WEBVTT\n\n")
    var cues = 0
    raw.removePrefix("WEBVTT").trim().split(Regex("\n[ \\t]*\n")).forEach { block ->
        val lines = block.lines()
        val timestamp = lines.indexOfFirst { it.contains("-->") }
        if (timestamp < 0) return@forEach
        val time = Regex("^\\s*\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3}\\s+-->\\s+\\d{1,2}:\\d{2}:\\d{2}[,.]\\d{1,3}(?:\\s+.*)?$")
        if (!time.matches(lines[timestamp])) return@forEach
        var text = lines.drop(timestamp + 1).joinToString("\n").trim()
        if (text.isEmpty()) return@forEach
        if (keyAndIv != null) {
            // The website drops an unreadable encrypted cue and continues with the next one.
            // Keep that behavior so one truncated/corrupt block does not lose valid captions.
            text = try {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyAndIv.first.toByteArray(Charsets.UTF_8), "AES"),
                    IvParameterSpec(keyAndIv.second.toByteArray(Charsets.UTF_8)))
                val encoded = java.util.Base64.getMimeDecoder().decode(text)
                val body = if (encoded.size >= 16 && encoded.take(8).toByteArray().toString(Charsets.US_ASCII) == "Salted__") encoded.copyOfRange(16, encoded.size) else encoded
                cipher.doFinal(body).toString(Charsets.UTF_8).takeIf(String::isNotBlank) ?: return@forEach
            } catch (_: Exception) { return@forEach }
        }
        val atTop = text.contains('♫') || text.contains('♪') || text.contains('[') || text.contains("{\\an8}")
        text = text.replace("{\\an8}", "")
        output.append(lines[timestamp].replace(',', '.'))
        if (atTop && !lines[timestamp].contains("line:")) output.append(" line:1")
        output.append('\n').append(text).append("\n\n"); cues++
    }
    require(cues > 0) { "The source returned no readable subtitle cues." }
    return output.toString()
}
