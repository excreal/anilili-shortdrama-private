package com.example.animepahedl

import android.content.Context
import android.content.ContentUris
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException

private val outputSubtitleExtensions = setOf("srt", "vtt", "ass", "ssa", "sub", "idx", "ttml", "smi")

internal fun isOutputSubtitleSidecar(candidate: String, mediaFileName: String): Boolean {
    val mediaStem = mediaFileName.substringBeforeLast('.', mediaFileName)
    val candidateExtension = candidate.substringAfterLast('.', "").lowercase()
    val candidateStem = candidate.substringBeforeLast('.', candidate)
    return candidateExtension in outputSubtitleExtensions && candidateStem.equals(mediaStem, ignoreCase = true)
}

/** Unknown provider state is never treated as absence (especially after a revoked grant). */
internal fun deleteOwnedMedia(context: Context, uri: Uri) {
    val resolver = context.contentResolver
    fun absent(): Boolean = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { !it.moveToFirst() } ?: false
    if (absent()) return
    val deleted = if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.deleteDocument(resolver, uri)
        else resolver.delete(uri, null, null) > 0
    if (!deleted && !absent()) throw IOException("The storage provider refused deletion. Restore folder access and tap Delete again.")
}

/** Delete only same-basename subtitle sidecars and an empty per-show directory. */
internal fun deleteOutputCompanions(context: Context, record: ExportRecord) {
    val location = record.outputLocation("mp4")
    val tree = record.tree.takeIf(String::isNotBlank)?.let(Uri::parse)
    if (tree != null) {
        runCatching {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
            val rootName = context.contentResolver.query(root, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
            val folderIsRoot = rootName.equals(location.showFolder, ignoreCase = true)
            // A selected tree can itself be the show folder. Never delete the user's granted root.
            val folder = if (folderIsRoot) root else {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
                context.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)
                    ?.use { cursor ->
                        var found: Uri? = null
                        while (cursor.moveToNext()) if (cursor.getString(1).equals(location.showFolder, ignoreCase = true) &&
                            cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            found = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)); break
                        }
                        found
                    } ?: return@runCatching
            }
            val folderId = DocumentsContract.getDocumentId(folder)
            val contents = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId)
            context.contentResolver.query(contents, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                val sidecars = mutableListOf<Uri>()
                var hasOtherFiles = false
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    if (isOutputSubtitleSidecar(cursor.getString(1), location.fileName)) sidecars += DocumentsContract.buildDocumentUriUsingTree(tree, id)
                    else hasOtherFiles = true
                }
                sidecars.forEach { runCatching { DocumentsContract.deleteDocument(context.contentResolver, it) } }
                if (!hasOtherFiles && !folderIsRoot) runCatching { DocumentsContract.deleteDocument(context.contentResolver, folder) }
            }
        }
    } else {
        val relativePath = listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder)
            .filter(String::isNotBlank).joinToString("/").trimEnd('/') + "/"
        runCatching {
            val files = MediaStore.Files.getContentUri("external")
            val sidecars = mutableListOf<Uri>()
            context.contentResolver.query(files,
                arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(relativePath), null)?.use { cursor ->
                while (cursor.moveToNext()) if (isOutputSubtitleSidecar(cursor.getString(1), location.fileName)) {
                    sidecars += ContentUris.withAppendedId(files, cursor.getLong(0))
                }
            }
            sidecars.forEach { context.contentResolver.delete(it, null, null) }
        }
        runCatching {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
            val folder = File(downloads, listOf(record.folder, location.showFolder).filter(String::isNotBlank).joinToString("/")).canonicalFile
            if (folder.toPath().startsWith(downloads.toPath()) && folder.isDirectory && folder.list()?.isEmpty() == true) folder.delete()
        }
    }
}

internal data class ItemResult(val id: String, val succeeded: Boolean, val message: String? = null)
