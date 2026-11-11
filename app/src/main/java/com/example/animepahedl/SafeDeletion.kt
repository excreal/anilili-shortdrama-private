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
    fun absent(): Boolean = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { !it.moveToFirst() } ?: false
    } catch (_: java.io.FileNotFoundException) { true }
    if (absent()) return
    val deleted = try {
        if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.deleteDocument(resolver, uri)
        else resolver.delete(uri, null, null) > 0
    } catch (_: java.io.FileNotFoundException) { true }
    if (!deleted && !absent()) throw IOException("The storage provider refused deletion. Restore folder access and tap Delete again.")
}

/** Delete only same-basename subtitle sidecars and an empty per-show directory. */
internal fun deleteOutputCompanions(context: Context, record: ExportRecord) {
    val location = record.outputLocation("mp4")
    val tree = record.tree.takeIf(String::isNotBlank)?.let(Uri::parse)
    if (tree != null) {
        val resolver = context.contentResolver
        fun query(uri: Uri, projection: Array<String>) = resolver.query(uri, projection, null, null, null)
            ?: throw IOException("Could not inspect the selected download folder. Restore folder access and tap Delete again.")
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val rootName = query(root, arrayOf(OpenableColumns.DISPLAY_NAME)).use {
            if (it.moveToFirst()) it.getString(0) else throw IOException("The selected download folder is no longer available.")
        }
        val folderIsRoot = rootName.equals(location.showFolder, ignoreCase = true)
        // A selected tree can itself be the show folder. Never delete the user's granted root.
        val folder = if (folderIsRoot) root else {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)).use { cursor ->
                var found: Uri? = null
                while (cursor.moveToNext()) if (cursor.getString(1).equals(location.showFolder, ignoreCase = true) &&
                    cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    found = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)); break
                }
                found
            }
        } ?: return
        val folderId = DocumentsContract.getDocumentId(folder)
        val contents = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folderId)
        query(contents, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME)).use { cursor ->
            val sidecars = mutableListOf<Uri>()
            var hasOtherFiles = false
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                if (isOutputSubtitleSidecar(cursor.getString(1), location.fileName)) sidecars += DocumentsContract.buildDocumentUriUsingTree(tree, id)
                else hasOtherFiles = true
            }
            sidecars.forEach { deleteOwnedMedia(context, it) }
            if (!hasOtherFiles && !folderIsRoot) deleteOwnedMedia(context, folder)
        }
    } else {
        val relativePath = listOf(Environment.DIRECTORY_DOWNLOADS, record.folder, location.showFolder)
            .filter(String::isNotBlank).joinToString("/").trimEnd('/') + "/"
        val files = MediaStore.Files.getContentUri("external")
        val sidecars = mutableListOf<Uri>()
        val cursor = context.contentResolver.query(files,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME),
            "${MediaStore.MediaColumns.RELATIVE_PATH}=?", arrayOf(relativePath), null)
            ?: throw IOException("Could not inspect the output folder. Restore storage access and tap Delete again.")
        cursor.use {
            while (cursor.moveToNext()) if (isOutputSubtitleSidecar(cursor.getString(1), location.fileName)) {
                sidecars += ContentUris.withAppendedId(files, cursor.getLong(0))
            }
        }
        sidecars.forEach { deleteOwnedMedia(context, it) }
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canonicalFile
        val folder = File(downloads, listOf(record.folder, location.showFolder).filter(String::isNotBlank).joinToString("/")).canonicalFile
        if (!folder.toPath().startsWith(downloads.toPath())) throw IOException("The output folder is outside Downloads.")
        if (folder.isDirectory) {
            val children = folder.list() ?: throw IOException("Could not inspect the output folder for cleanup.")
            if (children.isEmpty() && !folder.delete()) throw IOException("Could not remove the empty show folder.")
        }
    }
}

internal data class ItemResult(val id: String, val succeeded: Boolean, val message: String? = null)
