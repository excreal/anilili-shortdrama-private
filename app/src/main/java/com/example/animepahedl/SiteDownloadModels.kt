package com.example.animepahedl

/** Separate from AnimePahe's "episodes" preparation record. Contains no cookie input. */
internal data class SiteDownloadPreparation(
    val series: SiteDownloadSeries,
    val episodes: List<SiteDownloadEpisode>,
    val quality: String,
    /** Later shows appended while this preparation is running. Nullable for old Gson journals. */
    val queuedBatches: List<SiteDownloadPreparationBatch>? = null,
) {
    fun batches(): List<SiteDownloadPreparationBatch> =
        (if (episodes.isNotEmpty()) listOf(SiteDownloadPreparationBatch(series, episodes, quality)) else emptyList()) +
            queuedBatches.orEmpty()

    fun withBatches(batches: List<SiteDownloadPreparationBatch>): SiteDownloadPreparation? =
        batches.firstOrNull()?.let { first ->
            copy(series = first.series, episodes = first.episodes, quality = first.quality, queuedBatches = batches.drop(1))
        }
}

internal data class SiteDownloadPreparationBatch(
    val series: SiteDownloadSeries,
    val episodes: List<SiteDownloadEpisode>,
    val quality: String,
)

/** Identity matches the library row; bulk requests are only an input convenience. */
internal fun splitPreparation(record: SiteDownloadPreparation): Map<String, SiteDownloadPreparation> =
    buildMap {
        for (batch in record.batches()) for (episode in batch.episodes)
            putIfAbsent(pendingEpisodeRowId(batch.series, episode, batch.quality),
                SiteDownloadPreparation(batch.series, listOf(episode), batch.quality))
    }

internal fun aggregatePreparationStatus(items: Map<String, PreparationStatus>): PreparationStatus {
    val values = items.values
    val running = values.count { it.running }
    val waiting = values.count { it.automatic && !it.running }
    val blocked = values.count { !it.automatic }
    return PreparationStatus(pending = items.isNotEmpty(), paused = items.isNotEmpty() && values.all { it.paused },
        label = "$running preparing · $waiting waiting · $blocked need attention",
        verificationUrl = values.firstNotNullOfOrNull { it.verificationUrl },
        needsFolderAccess = values.any { it.needsFolderAccess }, totalSelected = items.size,
        running = running > 0, items = items)
}

internal fun inheritLegacyPreparationState(kind: String, first: Boolean, failed: Boolean): Boolean =
    first || kind == "preparation-folder" || (kind == "preparation-paused" && !failed)
