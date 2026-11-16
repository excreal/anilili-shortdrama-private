package com.example.animepahedl

/** Placeholder IDs use source episodes and requested quality, never signed media URLs. */
internal fun pendingEpisodeRowId(series: SiteDownloadSeries, episode: SiteDownloadEpisode, quality: String = "Best"): String {
    val stable = "${series.provider.name}\u0000${series.id}\u0000${episode.id}\u0000$quality"
    return "preparing:" + java.security.MessageDigest.getInstance("SHA-256")
        .digest(stable.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

internal fun isPreparedQueueRow(id: String) = id.startsWith("preparing:")

/** Every selected item is visible immediately, while real downloads replace placeholders by source identity. */
internal fun mergePreparedQueueRows(actual: List<DownloadRow>, pending: SiteDownloadPreparation?, status: PreparationStatus): List<DownloadRow> {
    if (pending == null) return actual
    val queued = pending.batches().flatMap { batch ->
        val showId = "sites:${batch.series.provider.name}:${batch.series.id}"
        val showRows = actual.filter { it.showId == showId && (batch.quality == "Best" || it.quality == batch.quality) }
        val existing = showRows.map { it.episodeId }.filter { it.isNotBlank() }.toHashSet()
        val existingNumbers = showRows.filter { it.episodeId.isBlank() }.map { it.episodeNumber }.toHashSet()
        batch.episodes.filterNot { it.id in existing || it.number.toString() in existingNumbers }.map { episode ->
            val status = status.items[pendingEpisodeRowId(batch.series, episode, batch.quality)] ?: status
            DownloadRow(
                id = pendingEpisodeRowId(batch.series, episode, batch.quality),
                title = batch.series.title + (if (batch.series.isSingleVideo) "" else " · Episode ${episode.displayNumber}") + " · ${batch.quality}",
                transfer = if (status.paused || status.verificationUrl != null) TransferState.PAUSED else TransferState.QUEUED,
                percent = -1f,
                failure = when {
                    status.needsFolderAccess -> FailureReason.FOLDER_ACCESS
                    else -> null
                },
                thumbnail = batch.series.poster,
                quality = batch.quality,
                explanation = when {
                    status.needsFolderAccess -> "Queued · choose downloads folder"
                    status.paused -> status.label.ifBlank { "Queued · link preparation paused" }
                    status.verificationUrl != null -> "Could not prepare download link. Retry this episode."
                    status.running && status.activeRowId == pendingEpisodeRowId(batch.series, episode, batch.quality) -> "Preparing download link"
                    status.running -> "Queued · waiting for earlier episodes"
                    else -> "Queued · " + status.label.ifBlank { "waiting to prepare link" }
                },
                showId = showId,
                episodeId = episode.id,
                showTitle = batch.series.title,
                episodeNumber = episode.displayNumber,
            )
        }
    }
    return if (queued.isEmpty()) actual else actual + queued
}
