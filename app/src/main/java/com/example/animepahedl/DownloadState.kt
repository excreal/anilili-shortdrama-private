package com.example.animepahedl

enum class TransferState { QUEUED, DOWNLOADING, PAUSED, WAITING_NETWORK, COMPLETE, FAILED, REMOVING }
enum class SaveState { NONE, WAITING, PREPARING, CHECKING, COPYING, FAILED }
enum class Availability { STAGING, SAVED }
enum class DownloadAction { PAUSE, RESUME, RETRY, RETRY_SAVE, VERIFY, DELETE }
enum class FailureReason { NETWORK, VERIFICATION, EXPIRED_SOURCE, STORAGE_FULL, FOLDER_ACCESS, INCOMPATIBLE_EXPORT, DELETION, UNKNOWN }

data class DownloadRow(val id: String, val title: String, val transfer: TransferState, val percent: Float,
    val bytes: Long = 0, val totalBytes: Long = -1, val savedUri: String = "",
    val thumbnail: String = "", val audioCode: String? = null, val quality: String? = null,
    val save: SaveState = SaveState.NONE, val availability: Availability = Availability.STAGING,
    val failure: FailureReason? = null, val explanation: String? = null, val deleting: Boolean = false, val showId: String = "", val episodeId: String = "", val showTitle: String = "", val episodeNumber: String = "") {
    val complete get() = availability == Availability.SAVED
    val saving get() = save in setOf(SaveState.WAITING, SaveState.PREPARING, SaveState.CHECKING, SaveState.COPYING)
    val failed get() = transfer == TransferState.FAILED || save == SaveState.FAILED || failure != null
    val actions: Set<DownloadAction> get() = buildSet {
        add(DownloadAction.DELETE)
        if (deleting) return@buildSet
        if (failure == FailureReason.VERIFICATION) add(DownloadAction.VERIFY)
        if (save == SaveState.FAILED) add(DownloadAction.RETRY_SAVE)
        else if (transfer == TransferState.FAILED && !complete) add(DownloadAction.RETRY)
        if (!id.startsWith("direct:") && !complete && save == SaveState.NONE && failure == null) {
            if (transfer == TransferState.PAUSED) add(DownloadAction.RESUME)
            if (transfer in setOf(TransferState.QUEUED, TransferState.DOWNLOADING, TransferState.WAITING_NETWORK)) add(DownloadAction.PAUSE)
        }
    }
    val status get() = explanation ?: when {
        complete -> "Ready"
        failure == FailureReason.VERIFICATION -> "Session verification needed"
        save == SaveState.FAILED -> "Couldn’t save video"
        save == SaveState.WAITING -> "Waiting to save"
        save == SaveState.PREPARING -> "Preparing video"
        save == SaveState.CHECKING -> "Checking video"
        save == SaveState.COPYING -> "Saving to folder"
        else -> when (transfer) {
            TransferState.QUEUED -> "Waiting to download"
            TransferState.DOWNLOADING -> "Downloading"
            TransferState.PAUSED -> "Paused"
            TransferState.WAITING_NETWORK -> "Waiting for internet"
            TransferState.COMPLETE -> "Ready offline"
            TransferState.FAILED -> "Download failed"
            TransferState.REMOVING -> "Removing"
        }
    }
}

internal data class DownloadIdentity(val anime: Anime, val episode: Episode, val quality: String, val audio: String,
    val source: String? = null, val locator: String? = null) {
    val isSiteDownload get() = source == "sites"
    val isSingleVideo get() = isSiteDownload && anime.type == "Video"
    // Absent fields in old Gson records stay null. Never change an AnimePahe download key.
    val key get() = if (!isSiteDownload) "${anime.session}-${episode.session}-$quality-$audio" else {
        val value = listOf(source, anime.session, episode.session, quality, audio).joinToString("\u0000")
        "sites:" + java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
    companion object {
        fun siteDownload(series: SiteDownloadSeries, episode: SiteDownloadEpisode, quality: String, audio: String): DownloadIdentity {
            val poster = series.posterThumbnail()
            return DownloadIdentity(Anime("sites:${series.provider.name}:${series.id}", series.title, poster,
                "", if (series.isSingleVideo) "Video" else "Series", ""), Episode(episode.id, episode.displayNumber, poster, ""),
                quality, audio, "sites", episode.url)
        }
    }
}

internal fun classifyFailure(message: String): FailureReason = when {
    message.contains("space", true) || message.contains("ENOSPC", true) -> FailureReason.STORAGE_FULL
    message.contains("permission", true) || message.contains("folder", true) || message.contains("SecurityException") -> FailureReason.FOLDER_ACCESS
    message.contains("FFmpeg", true) || message.contains("track", true) || message.contains("duration", true) -> FailureReason.INCOMPATIBLE_EXPORT
    message.contains("403") || message.contains("410") -> FailureReason.EXPIRED_SOURCE
    message.contains("network", true) || message.contains("timeout", true) -> FailureReason.NETWORK
    else -> FailureReason.UNKNOWN
}

internal fun savedFileLocationStatus(row: DownloadRow): String? = if (!row.complete) null else when (row.failure) {
    FailureReason.DELETION -> "Delete failed"
    FailureReason.FOLDER_ACCESS -> "Folder access needed"
    else -> "In Downloads folder"
}

internal data class PreparationRecord(val anime: Anime, val episodes: List<Episode>, val quality: String, val audio: String)
