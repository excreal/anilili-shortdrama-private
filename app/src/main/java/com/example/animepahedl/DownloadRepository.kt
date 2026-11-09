package com.example.animepahedl

import androidx.core.content.edit
import androidx.core.net.toUri
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface DownloadRepository {
    val rows: StateFlow<List<DownloadRow>>
    suspend fun refresh()
    suspend fun pause(id: String)
    suspend fun resume(id: String)
    suspend fun retry(id: String)
    suspend fun retrySave(id: String)
    suspend fun delete(id: String, deleteSavedFile: Boolean = true): ItemResult
    suspend fun deleteBatch(ids: Set<String>, confirmedSaved: Set<String> = ids): List<ItemResult> = ids.map { delete(it, it in confirmedSaved) }
    suspend fun enqueue(identity: DownloadIdentity, media: ResolvedMedia, previous: ExportRecord? = null): String
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class AndroidDownloadRepository(private val app: Context, private val network: AnimeRepository) : DownloadRepository {
    private val settings = AppSettings(app)
    override val rows = MutableStateFlow<List<DownloadRow>>(emptyList())
    override suspend fun refresh() {
        DownloadStore.awaitReady()
        val manager = OfflineDownloads.manager(app)
        val active = manager.currentDownloads.associateBy { it.request.id }
        val paused = manager.downloadsPaused
        val requirements = manager.notMetRequirements
        rows.value = withContext(Dispatchers.IO) { readDownloadRows(active, paused, requirements) }
    }
    private fun readDownloadRows(activeDownloads: Map<String, Download>, paused: Boolean, requirements: Int): List<DownloadRow> {
        val rows = mutableListOf<DownloadRow>()
        val manager = OfflineDownloads.manager(app)
        manager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) {
                val stored = cursor.download
                val download = activeDownloads[stored.request.id] ?: stored
                val status = when (download.state) {
                    Download.STATE_COMPLETED -> TransferState.COMPLETE
                    Download.STATE_FAILED -> TransferState.FAILED
                    Download.STATE_DOWNLOADING -> TransferState.DOWNLOADING
                    Download.STATE_QUEUED -> when {
                        paused -> TransferState.PAUSED
                        requirements != 0 -> TransferState.WAITING_NETWORK
                        else -> TransferState.QUEUED
                    }
                    Download.STATE_STOPPED -> TransferState.PAUSED
                    Download.STATE_REMOVING -> TransferState.REMOVING
                    else -> TransferState.QUEUED
                }
                rows += DownloadRow(download.request.id, String(download.request.data), status, download.percentDownloaded,
                    download.bytesDownloaded, download.contentLength)
            }
        }
        val exports = Exports.all(app)
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        DownloadStore.operations("direct").filterKeys { exports["direct:$it"]?.uri.isNullOrEmpty() }.forEach { (id, title) ->
            system.query(DownloadManager.Query().setFilterById(id.toLong())).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    rows += DownloadRow("direct:$id", title, when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> TransferState.COMPLETE
                        DownloadManager.STATUS_FAILED -> TransferState.FAILED
                        DownloadManager.STATUS_PAUSED -> TransferState.PAUSED
                        DownloadManager.STATUS_RUNNING -> TransferState.DOWNLOADING
                        else -> TransferState.QUEUED
                    }, if (total > 0) bytes * 100f / total else -1f, bytes, total)
                }
            }
        }
        val artwork = app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE)
        // Keep app-owned rows visible even if an engine has already removed its staging entry.
        val engineIds = rows.mapTo(HashSet()) { it.id }
        exports.filter { (id, record) -> record.uri.isEmpty() && id !in engineIds }.forEach { (id, record) ->
            rows += DownloadRow(id, record.title, TransferState.FAILED, -1f, explanation = "Temporary media is unavailable. Retry to resolve a fresh source.")
        }
        val challenges = app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).all.keys
        val deleting = DownloadStore.operations("delete")
        return rows.filter { exports[it.id]?.uri.isNullOrEmpty() }.map { row ->
            val record = exports[row.id]
            val metadata = row.copy(title = record?.episodeTitle() ?: row.title, thumbnail = artwork.getString(row.id.take(36), null) ?: record?.thumbnail.orEmpty(),
                audioCode = record?.audio, quality = record?.quality, showId = record?.identity?.anime?.session.orEmpty(), episodeId = record?.identity?.episode?.session.orEmpty(), showTitle = record?.identity?.anime?.title ?: record?.showTitle.orEmpty(), episodeNumber = record?.identity?.episode?.number ?: record?.episodeNumber.orEmpty())
            when {
                row.id in deleting -> metadata.copy(failure = FailureReason.DELETION, explanation = deleting[row.id].orEmpty().ifBlank { "Deleting…" }, deleting = true)
                row.id in challenges -> metadata.copy(failure = FailureReason.VERIFICATION)
                record?.error?.isNotEmpty() == true && row.explanation == null -> metadata.copy(save = SaveState.FAILED,
                    failure = classifyFailure(record.error), explanation = record.error)
                row.transfer == TransferState.COMPLETE && record != null -> Exports.progress[row.id]?.let {
                    metadata.copy(save = when (it.stage) {
                        "Preparing MP4", "Retrying MP4", "Preparing video" -> SaveState.PREPARING
                        "Checking MP4", "Checking saved video" -> SaveState.CHECKING
                        else -> SaveState.COPYING
                    }, percent = it.percent)
                } ?: metadata.copy(save = SaveState.WAITING, percent = -1f)
                else -> metadata
            }
        } + exports.filterValues { it.uri.isNotEmpty() }.map { (id, record) ->
            DownloadRow(id, record.episodeTitle(), TransferState.COMPLETE, 100f, record.bytes, record.bytes, record.uri,
                artwork.getString(id.take(36), null) ?: record.thumbnail.orEmpty(), record.audio, record.quality,
                availability = Availability.SAVED, failure = if (id in deleting) FailureReason.DELETION else if (record.error.isNotBlank()) FailureReason.FOLDER_ACCESS else null,
                explanation = deleting[id]?.ifBlank { "Deleting…" } ?: record.error.takeIf { it.isNotBlank() }, deleting = id in deleting, showId = record.identity?.anime?.session.orEmpty(), episodeId = record.identity?.episode?.session.orEmpty(), showTitle = record.identity?.anime?.title ?: record.showTitle.orEmpty(), episodeNumber = record.identity?.episode?.number ?: record.episodeNumber.orEmpty())
        }
    }

    override suspend fun pause(id: String) {
        if (Exports.deleting(id)) return
        DownloadService.sendSetStopReason(app, OfflineDownloadService::class.java, id, 1, true)
    }

    override suspend fun resume(id: String) {
        require(Exports.all(app)[id]?.identity?.isSiteDownload == true) { "This legacy download can no longer be resumed." }
        if (!hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null) throw OutputFolderAccessRequired()
        if (Exports.deleting(id)) return
        DownloadService.sendSetStopReason(app, OfflineDownloadService::class.java, id, Download.STOP_REASON_NONE, true)
        DownloadService.sendResumeDownloads(app, OfflineDownloadService::class.java, true)
    }

    private suspend fun retryLegacy(id: String) {
        if (Exports.deleting(id)) return
        val request = withContext(Dispatchers.IO) { OfflineDownloads.manager(app).downloadIndex.getDownload(id)?.request } ?: error("Temporary media is unavailable")
        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(id) }
        DownloadService.sendAddDownload(app, OfflineDownloadService::class.java, request, true)
        DownloadService.sendResumeDownloads(app, OfflineDownloadService::class.java, true)
    }
    override suspend fun delete(id: String, deleteSavedFile: Boolean): ItemResult {
        // Preserve the user's Cancel intent even if an existing file is adopted before deletion runs.
        if (!deleteSavedFile) DownloadStore.operation(id, "delete-keep-file", "1")
        val removePublished = deleteSavedFile && id !in DownloadStore.operations("delete-keep-file")
        DownloadStore.operation(id, "delete", "")
        return try {
            DownloadStore.flush()
            Exports.cancel(app, id)
            Exports.withItemLock(id) {
                val record = Exports.all(app)[id]
                withContext(Dispatchers.IO) {
                    listOfNotNull(record?.uri?.takeIf { removePublished && it.isNotBlank() }, record?.pendingUri?.takeIf { it.isNotBlank() && it != record.uri }).distinct()
                        .forEach { deleteOwnedMedia(app, (it).toUri()) }
                    if (id.startsWith("direct:")) {
                        val number = id.removePrefix("direct:")
                        (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(number.toLong())
                        DownloadStore.operation(number, "direct", null)
                    }
                }
                if (!id.startsWith("direct:")) {
                    // Media3 removes asynchronously; retain the tombstone until the index confirms absence.
                    DownloadService.sendRemoveDownload(app, OfflineDownloadService::class.java, id, true)
                    kotlinx.coroutines.withTimeout(30_000) {
                        while (withContext(Dispatchers.IO) { OfflineDownloads.manager(app).downloadIndex.getDownload(id) != null }) delay(100)
                    }
                }
                app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(id) }
                app.getSharedPreferences("download-retries", Context.MODE_PRIVATE).edit { remove(id) }
                Exports.remove(app, id)
                DownloadStore.operation(id, "delete", null)
                DownloadStore.operation(id, "delete-keep-file", null)
                DownloadStore.flush()
                ItemResult(id, true)
            }
        } catch (e: Exception) {
            val message = "Couldn’t delete: ${e.message ?: "storage unavailable"}. Tap Delete to retry."
            DownloadStore.operation(id, "delete", message)
            ItemResult(id, false, message)
        }
    }

    private companion object {
        val enqueueLocks = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()
    }
    override suspend fun enqueue(identity: DownloadIdentity, media: ResolvedMedia, previous: ExportRecord?): String {
        require(identity.isSiteDownload) { "Only supported source downloads can be queued here." }
        val lock = enqueueLocks.getOrPut(identity.key) { kotlinx.coroutines.sync.Mutex() }
        lock.lock()
        try { return enqueueLocked(identity, media, previous) } finally { lock.unlock() }
    }
    private suspend fun enqueueLocked(identity: DownloadIdentity, unresolved: ResolvedMedia, previous: ExportRecord?): String {
        if (!hasOutputFolderAccess(app)) throw OutputFolderAccessRequired()
        if (previous == null) Exports.all(app).entries.firstOrNull { (id, record) -> !Exports.deleting(id) && (record.identity?.key == identity.key || id == identity.key) }?.let { return it.key }
        val media = if (identity.isSiteDownload) MediaRequests.available(unresolved, SiteDownloadCookieStore(app)::headerFor) else {
            network.checkMedia(unresolved.url)
            unresolved.copy(headers = mapOf("Referer" to "https://kwik.cx/") + unresolved.headers.orEmpty())
        }
        val title = identity.anime.title + (if (identity.isSingleVideo) "" else " · Episode ${identity.episode.number}") + " · ${media.quality} · ${audioTag(media.audio)}"
        val record = previous?.copy(title = title, showTitle = identity.anime.title, episodeNumber = identity.episode.number,
            quality = media.quality, audio = media.audio, error = "", uri = "", identity = identity, media = media)
            ?: ExportRecord(title, settings.outputTree, settings.outputFolder, thumbnail = identity.anime.poster,
                audio = media.audio, quality = media.quality, showTitle = identity.anime.title, episodeNumber = identity.episode.number, identity = identity, media = media)
        if (media.isHls() || identity.isSiteDownload) {
            if (media.isHls()) require(java.io.File(app.applicationInfo.nativeLibraryDir, "libffmpeg_exec.so").exists()) { "Saving HLS requires an ARM64 device. This device cannot run the bundled exporter." }
            // Media3 also handles SiteDownload MP4s so its scoped interceptor controls every redirect.
            val id = identity.key
            val mime = if (media.isHls()) MimeTypes.APPLICATION_M3U8 else media.mimeType ?: MimeTypes.VIDEO_MP4
            val request = DownloadRequest.Builder(id, (media.url).toUri()).setMimeType(mime).setData(title.toByteArray()).build()
            Exports.put(app, id, record)
            DownloadStore.flush()
            DownloadService.sendAddDownload(app, OfflineDownloadService::class.java, request, true)
            Exports.start(app)
            return id
        }
        val stagingName = "${java.util.UUID.randomUUID()}.${if ((media.url).toUri().path.orEmpty().endsWith(".webm", true)) "webm" else "mp4"}"
        DownloadStore.operation(stagingName, "direct-enqueue", com.google.gson.Gson().toJson(record))
        DownloadStore.flush()
        val request = DownloadManager.Request((media.url).toUri()).setTitle(title)
            .setDestinationInExternalFilesDir(app, "direct-temp", stagingName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        mediaRequestHeaders(media).forEach { (name, value) -> request.addRequestHeader(name, value) }
        if (!identity.isSiteDownload && media.headers.orEmpty().keys.none { it.equals("Cookie", true) })
            CookieManager.getInstance().getCookie(media.url)?.let { request.addRequestHeader("Cookie", it) }
        if (settings.unmeteredOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
        request.setAllowedOverRoaming(false)
        val number = withContext(Dispatchers.IO) { (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request) }
        val id = "direct:$number"
        Exports.put(app, id, record)
        withContext(Dispatchers.IO) { DownloadStore.operation(number.toString(), "direct", title) }
        DownloadStore.flush()
        DownloadStore.operation(stagingName, "direct-enqueue", null)
        DownloadStore.flush()
        return id
    }
    override suspend fun retry(id: String) {
        require(Exports.all(app)[id]?.identity?.isSiteDownload == true) { "This legacy download can no longer be retried." }
        val stored = Exports.all(app)[id]
        stored?.identity?.let { identity ->
            val tree = stored.tree.takeIf { hasOutputFolderAccess(app, it) } ?: settings.outputTree
            val existing = withContext(Dispatchers.IO) { findExistingOutput(app, tree, stored.outputLocation("mp4")) }
            if (existing != null) { adoptExistingVideo(app, identity, existing, tree); refresh(); return }
        }
        Exports.withItemLock(id) {
            if (Exports.deleting(id)) return@withItemLock
            val record = Exports.all(app)[id]
            val identity = record?.identity
            if (identity == null) {
                if (!id.startsWith("direct:")) retryLegacy(id)
                else error("This older download has no source identity. Open the show and select the episode again.")
                return@withItemLock
            }
            val media = if (identity.isSiteDownload) {
                val locator = identity.locator?.takeIf { it.isNotBlank() }
                    ?: error("This video download has no source locator. Inspect its link again.")
                val cookies = SiteDownloadCookieStore(app)
                cookies.applyTo(appSiteDownloadRepository(cookieHeader = cookies::headerFor).resolve(locator, identity.quality, cookies.headerFor(locator)))
            } else network.playlist(identity.anime, identity.episode, identity.quality, identity.audio)
            val newId = enqueue(identity, media, record)
            app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(id) }
            if (newId != id) {
                if (id.startsWith("direct:")) withContext(Dispatchers.IO) {
                    val number = id.removePrefix("direct:")
                    (app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).remove(number.toLong())
                    DownloadStore.operation(number, "direct", null)
                }
                Exports.remove(app, id)
            }
        }
        refresh()
    }
    override suspend fun retrySave(id: String) {
        Exports.withItemLock(id) {
            if (Exports.deleting(id)) return@withItemLock
            Exports.all(app)[id]?.let { Exports.put(app, id, it.copy(error = "", tree = settings.outputTree, folder = settings.outputFolder)) }
            DownloadStore.flush()
            Exports.start(app)
        }
    }
}
