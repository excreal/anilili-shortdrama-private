package com.example.animepahedl

/** Placeholder IDs describe source episodes, not resolved quality or signed media URLs. */
internal fun pendingEpisodeRowId(series: SiteDownloadSeries, episode: SiteDownloadEpisode): String {
    val stable = "${series.provider.name}\u0000${series.id}\u0000${episode.id}"
    return "preparing:" + java.security.MessageDigest.getInstance("SHA-256")
        .digest(stable.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

internal fun isPreparedQueueRow(id: String) = id.startsWith("preparing:")

/** Every selected item is visible immediately, while real downloads replace placeholders by source identity. */
internal fun mergePreparedQueueRows(actual: List<DownloadRow>, pending: SiteDownloadPreparation?, status: PreparationStatus): List<DownloadRow> {
    if (pending == null) return actual
    val queued = pending.batches().flatMap { batch ->
        val showId = "sites:${batch.series.provider.name}:${batch.series.id}"
        val showRows = actual.filter { it.showId == showId }
        val existing = showRows.map { it.episodeId }.filter { it.isNotBlank() }.toHashSet()
        val existingNumbers = showRows.filter { it.episodeId.isBlank() }.map { it.episodeNumber }.toHashSet()
        batch.episodes.filterNot { it.id in existing || it.number.toString() in existingNumbers }.map { episode ->
            DownloadRow(
                id = pendingEpisodeRowId(batch.series, episode),
                title = batch.series.title + (if (batch.series.isSingleVideo) "" else " · Episode ${episode.displayNumber}") + " · ${batch.quality}",
                transfer = if (status.paused) TransferState.PAUSED else TransferState.QUEUED,
                percent = -1f,
                thumbnail = batch.series.poster,
                quality = batch.quality,
                explanation = when {
                    status.needsFolderAccess -> "Queued · choose downloads folder"
                    status.paused -> "Queued · link preparation paused"
                    status.verificationUrl != null -> "Queued · session verification needed"
                    else -> "Queued · preparing link in background"
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
