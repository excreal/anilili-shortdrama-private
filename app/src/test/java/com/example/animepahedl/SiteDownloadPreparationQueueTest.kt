package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Test

class SiteDownloadPreparationQueueTest {
    @Test fun onlyActiveEpisodeShowsPreparingAndPauseOverridesStaleProgress() {
        val pending = batch(SiteDownloadProvider.NARTO, "show", listOf(episode("1"), episode("2", 2)))
        val status = PreparationStatus(pending = true, running = true,
            activeRowId = pendingEpisodeRowId(pending.series, pending.episodes.first()))
        val rows = mergePreparedQueueRows(emptyList(), pending, status)
        assertEquals("Preparing download link", rows[0].explanation)
        assertEquals("Queued · waiting for earlier episodes", rows[1].explanation)
        val paused = mergePreparedQueueRows(emptyList(), pending,
            status.copy(paused = true, label = "Preparation stopped after repeated failures"))
        assertEquals(listOf("Preparation stopped after repeated failures", "Preparation stopped after repeated failures"),
            paused.map { it.explanation })
    }

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

    @Test fun splitKeepsExistingWorkAheadOfNewTitles() {
        val first = batch(SiteDownloadProvider.DRAMATIP, "first", listOf(episode("first-1")))
        val second = batch(SiteDownloadProvider.NARTO, "second", listOf(episode("second-1")))

        val queued = splitPreparation(first.withBatches(first.batches() + second.batches())!!).values.toList()

        assertEquals(listOf("first", "second"), queued.map { it.series.id })
        assertEquals(listOf("first-1", "second-1"), queued.flatMap { it.episodes }.map { it.id })
    }

    @Test fun splitDeduplicatesEpisodesForSameProviderShowAndQuality() {
        val first = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("1"), episode("2")))
        val added = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("2", 200), episode("3")))

        val queued = splitPreparation(first.withBatches(first.batches() + added.batches())!!).values.toList()

        assertEquals(3, queued.size)
        assertEquals(listOf("1", "2", "3"), queued.map { it.episodes.single().id })
        assertEquals(1, queued[1].episodes.single().number)
    }

    @Test fun splitKeepsSameIdsFromDifferentProvidersAndQualitiesSeparate() {
        val first = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("1")))
        val fromAnotherProvider = batch(SiteDownloadProvider.NARTO, "show", listOf(episode("1")))
        val otherQuality = batch(SiteDownloadProvider.DRAMATIP, "show", listOf(episode("1")), "720p")

        val queued = splitPreparation(first.withBatches(first.batches() + fromAnotherProvider.batches() + otherQuality.batches())!!).values.toList()

        assertEquals(3, queued.size)
        assertEquals(
            listOf("DRAMATIP:show:Best", "NARTO:show:Best", "DRAMATIP:show:720p"),
            queued.map { "${it.series.provider}:${it.series.id}:${it.quality}" },
        )
    }
}
