package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Test

class SiteDownloadPreparationQueueTest {
    private fun series(provider: SiteDownloadProvider, id: String, title: String = id) =
        SiteDownloadSeries(provider, id, title, "", "https://example.test/$id", emptyList())

    private fun episode(id: String, number: Int = 1) =
        SiteDownloadEpisode(id, number, "Episode $number", "https://example.test/episode/$id")

    private fun batch(
        provider: SiteDownloadProvider,
        seriesId: String,
        episodes: List<SiteDownloadEpisode>,
        quality: String = "Best",
    ) = SiteDownloadPreparation(series(provider, seriesId), episodes, quality)

    @Test fun appendKeepsExistingWorkAheadOfNewTitles() {
        val first = batch(SiteDownloadProvider.DRAMATIP, "first", listOf(episode("first-1")))
        val second = batch(SiteDownloadProvider.NARTO, "second", listOf(episode("second-1")))

        val queued = appendSitePreparation(first, second).batches()

        assertEquals(listOf("first", "second"), queued.map { it.series.id })
        assertEquals(listOf("first-1", "second-1"), queued.flatMap { it.episodes }.map { it.id })
    }

    @Test fun appendDeduplicatesEpisodesForSameProviderShowAndQuality() {
        val first = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("1"), episode("2")))
        val added = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("2", 200), episode("3")))

        val queued = appendSitePreparation(first, added).batches()

        assertEquals(1, queued.size)
        assertEquals(listOf("1", "2", "3"), queued.single().episodes.map { it.id })
        assertEquals(1, queued.single().episodes[1].number)
    }

    @Test fun appendKeepsSameIdsFromDifferentProvidersAndQualitiesSeparate() {
        val first = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("1")))
        val fromAnotherProvider = batch(SiteDownloadProvider.NARTO, "show", listOf(episode("1")))
        val otherQuality = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("1")), "720p")

        val queued = appendSitePreparation(appendSitePreparation(first, fromAnotherProvider), otherQuality).batches()

        assertEquals(3, queued.size)
        assertEquals(
            listOf("DRAMATIP:show:Best", "NARTO:show:Best", "DRAMATIP:show:720p"),
            queued.map { "${it.series.provider}:${it.series.id}:${it.quality}" },
        )
    }
}
