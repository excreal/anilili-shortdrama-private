package com.example.animepahedl

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine

/** Provider notifications cover scoped Downloads access; foreground polling covers quiet SAF providers. */
internal class SavedFilesWatcher(private val context: Context, private val onChanged: () -> Unit) {
    private var running = false
    private var roots = emptySet<String>()
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = onChanged()
    }
    fun start() { running = true; updateRoots(force = true) }
    fun updateRoots(force: Boolean = false) {
        if (!running) return
        val next = Exports.all(context).values.mapNotNull { it.tree.takeIf(String::isNotBlank) }.toSet() +
            listOfNotNull(AppSettings(context).outputTree.takeIf(String::isNotBlank))
        if (!force && next == roots) return
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
        roots = next
        // MediaStore may notify its parent rather than the specific Downloads table.
        runCatching { context.contentResolver.registerContentObserver(Uri.parse("content://media"), true, observer) }
        for (root in roots) runCatching {
            context.contentResolver.registerContentObserver(root.toUri(), true, observer)
        }
    }
    fun stop() {
        running = false
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
    }
}

private suspend fun <T> querySavedFiles(context: Context, uri: Uri, projection: Array<String>,
    selection: String? = null, args: Array<String>? = null, notFound: T? = null, read: (Cursor) -> T): T? = suspendCancellableCoroutine { continuation ->
    val signal = CancellationSignal()
    continuation.invokeOnCancellation { signal.cancel() }
    Dispatchers.IO.dispatch(continuation.context, Runnable {
        try {
            val result = context.contentResolver.query(uri, projection, selection, args, null, signal)?.use(read)
            continuation.resumeWith(Result.success(result))
        } catch (_: java.io.FileNotFoundException) {
            continuation.resumeWith(Result.success(notFound))
        } catch (_: Exception) {
            // Revoked grants, unavailable volumes, and provider failures are not proof of deletion.
            continuation.resumeWith(Result.success(null))
        }
    })
}

/** A missing document can produce a null cursor instead of an empty one.
 * Check the descriptor in that case, but never mistake provider errors or revoked
 * permissions for deletion. FileNotFoundException is the only definitive fallback.
 */
private suspend fun savedFilePresent(context: Context, uri: Uri): Boolean? = kotlinx.coroutines.withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    try {
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor != null) return@withContext cursor.use { it.moveToFirst() }
        resolver.openAssetFileDescriptor(uri, "r")?.use { true }
    } catch (_: java.io.FileNotFoundException) {
        false
    } catch (_: SecurityException) {
        null
    } catch (_: Exception) {
        null
    }
}

/** Only confirmed absent rows are removed; queries are off-main and provider work is cancellable. */
internal suspend fun missingSavedFiles(context: Context, records: Map<String, ExportRecord>): Map<String, ExportRecord> {
    val missing = mutableMapOf<String, ExportRecord>()
    val collections = records.entries.filter {
        val uri = it.value.uri.toUri()
        uri.authority == MediaStore.AUTHORITY && uri.pathSegments.size == 3 && uri.pathSegments[1] == "downloads" &&
            uri.lastPathSegment?.toLongOrNull() != null
    }.groupBy { it.value.uri.toUri().buildUpon().path(it.value.uri.toUri().path!!.substringBeforeLast('/')).build() }
    val batched = collections.values.flatten().mapTo(HashSet()) { it.key }
    for ((collection, entries) in collections) for (chunk in entries.chunked(128)) {
        val ids = chunk.map { it.value.uri.toUri().lastPathSegment!! }
        val existing = querySavedFiles<Set<String>>(context, collection, arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",") { "?" }})", ids.toTypedArray()) { cursor ->
            buildSet<String> { while (cursor.moveToNext()) add(cursor.getLong(0).toString()) }
        }
        // A provider that cannot answer a batch query still needs a per-file
        // check. Otherwise every record in that batch remains stale forever.
        if (existing == null) {
            for (entry in chunk) if (savedFilePresent(context, entry.value.uri.toUri()) == false) {
                missing[entry.key] = entry.value
            }
            continue
        }
        for (entry in chunk) if (entry.value.uri.toUri().lastPathSegment !in existing) {
            // Recheck individual grants: a collection query can hide files shared by another app.
            if (savedFilePresent(context, entry.value.uri.toUri()) == false) missing[entry.key] = entry.value
        }
    }
    val treeAccessible = mutableMapOf<String, Boolean>()
    for ((id, record) in records) if (id !in batched) {
        val uri = record.uri.toUri()
        if (uri.scheme != "content") continue
        if (record.tree.isNotBlank()) {
            val accessible = treeAccessible[record.tree] ?: run {
                val root = runCatching { DocumentsContract.buildDocumentUriUsingTree(record.tree.toUri(), DocumentsContract.getTreeDocumentId(record.tree.toUri())) }.getOrNull()
                val exists = root?.let { querySavedFiles(context, it, arrayOf(OpenableColumns.DISPLAY_NAME)) { cursor -> cursor.moveToFirst() } } == true
                treeAccessible[record.tree] = exists
                exists
            }
            if (!accessible) continue
        }
        if (savedFilePresent(context, uri) == false) missing[id] = record
    }
    return missing
}
