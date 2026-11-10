package com.example.animepahedl

import androidx.core.net.toUri
import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class StorageReport(val available: Long, val retained: Long, val failed: Int, val inaccessible: Int)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal suspend fun inspectStorage(context: Context): StorageReport = withContext(Dispatchers.IO) {
    val root = context.getExternalFilesDir(null) ?: context.filesDir
    val records = Exports.all(context)
    val inaccessible = records.values.count { record -> record.uri.isNotBlank() && runCatching {
        context.contentResolver.openFileDescriptor((record.uri).toUri(), "r")?.use { true } ?: false
    }.getOrDefault(false).not() }
    StorageReport(StatFs(root.absolutePath).availableBytes, OfflineDownloads.cache.cacheSpace +
        listOf("direct-temp", "export-temp").sumOf { java.io.File(root, it).walkTopDown().filter { f -> f.isFile }.sumOf { f -> f.length() } },
        records.values.count { it.error.isNotBlank() }, inaccessible)
}

/** Validate the complete copied stream, including providers that cannot report a length. */
internal suspend fun verifyCopy(context: Context, uri: Uri, expectedBytes: Long, expectedDigest: ByteArray, onBytes: (Long) -> Unit = {}) {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val size = context.contentResolver.openInputStream(uri)?.use { input ->
        copyCancellable(input, object : java.io.OutputStream() {
            override fun write(value: Int) { digest.update(value.toByte()) }
            override fun write(bytes: ByteArray, offset: Int, length: Int) { digest.update(bytes, offset, length) }
        }, onBytes)
    } ?: error("Cannot read saved file")
    require(size == expectedBytes && digest.digest().contentEquals(expectedDigest)) { "Saved file failed integrity verification; source retained" }
}

internal fun diagnostics(context: Context): String {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val rows = Exports.all(context).values
    return buildString {
        appendLine("Anilili ShortDrama diagnostic summary")
        appendLine("Version: ${info.versionName} (${info.longVersionCode})")
        appendLine("Android API: ${android.os.Build.VERSION.SDK_INT}")
        appendLine("ABIs: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
        appendLine("Saved: ${rows.count { it.uri.isNotBlank() }}")
        appendLine("Unfinished: ${rows.count { it.uri.isBlank() }}")
        appendLine("Failures by category:")
        rows.filter { it.error.isNotBlank() }.groupingBy { classifyFailure(it.error) }.eachCount().forEach { (category, count) -> appendLine("$category: $count") }
        appendLine("Pending deletions: ${DownloadStore.operations("delete").size}")
        appendLine("Pending enqueue recovery: ${DownloadStore.operations("direct-enqueue").size}")
        appendLine("No titles, file paths, cookies, or media URLs are included.")
    }
}

internal object QueueOrder {
    fun ordered(context: Context, ids: List<String>): List<String> {
        val preferred = (DownloadStore.operations("queue")["order"] ?: context.getSharedPreferences("queue-order", Context.MODE_PRIVATE).getString("ids", "").orEmpty()).split('\n')
        val rank = preferred.withIndex().associate { it.value to it.index }
        return ids.sortedBy { rank[it] ?: Int.MAX_VALUE }
    }
    suspend fun first(context: Context, id: String, ids: List<String>) {
        val order = listOf(id) + ordered(context, ids).filterNot { it == id }
        DownloadStore.operation("order", "queue", order.joinToString("\n"))
        DownloadStore.flush()
    }
}
