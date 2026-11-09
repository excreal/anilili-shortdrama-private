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

internal fun appendSitePreparation(current: SiteDownloadPreparation, added: SiteDownloadPreparation): SiteDownloadPreparation {
    val batches = current.batches().toMutableList()
    for (batch in added.batches()) {
        val index = batches.indexOfFirst { it.series.provider == batch.series.provider && it.series.id == batch.series.id && it.quality == batch.quality }
        if (index < 0) batches += batch.copy(episodes = batch.episodes.distinctBy { it.id })
        else {
            val previous = batches[index]
            batches[index] = previous.copy(episodes = (previous.episodes + batch.episodes).distinctBy { it.id })
        }
    }
    return current.withBatches(batches) ?: added
}

internal const val SITE_DOWNLOAD_PREPARATION_KEY = "sites"
