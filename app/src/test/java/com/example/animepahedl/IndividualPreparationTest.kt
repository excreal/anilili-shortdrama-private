package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class IndividualPreparationTest {
    private val episodes = (26..28).map { SiteDownloadEpisode("ep-$it", it, "Episode $it", "https://narto-drama.com/detail/watch/show/$it") }
    private val series = SiteDownloadSeries(SiteDownloadProvider.NARTO, "show", "Show", "", "https://narto-drama.com/detail/watch/show", episodes)

    @Test fun bulkSelectionSplitsIntoStableIndependentJournals() {
        val items = splitPreparation(SiteDownloadPreparation(series, episodes, "Best"))
        assertEquals(3, items.size)
        assertEquals(episodes, items.values.map { it.episodes.single() })
        assertTrue(items.values.all { it.batches().size == 1 })
        assertEquals(items.keys, splitPreparation(SiteDownloadPreparation(series, episodes, "Best")).keys)
    }

    @Test fun blockedEpisodeDoesNotMarkOtherRowsBlockedOrStopAggregate() {
        val ids = episodes.map { pendingEpisodeRowId(series, it) }
        val states = mapOf(
            ids[0] to PreparationStatus(true, paused = true, label = "Ad gate"),
            ids[1] to PreparationStatus(true, running = true, activeRowId = ids[1]),
            ids[2] to PreparationStatus(true))
        val aggregate = aggregatePreparationStatus(states)
        assertTrue(aggregate.automatic)
        assertFalse(aggregate.paused)
        val rows = mergePreparedQueueRows(emptyList(), SiteDownloadPreparation(series, episodes, "Best"), aggregate)
        assertEquals(TransferState.PAUSED, rows[0].transfer)
        assertEquals("Ad gate", rows[0].explanation)
        assertEquals("Preparing download link", rows[1].explanation)
        assertFalse(rows[2].explanation.orEmpty().contains("Ad gate"))
    }

    @Test fun migrationIsolatesFailureButPreservesExplicitPauseAndFolderNeeds() {
        assertTrue(inheritLegacyPreparationState("preparation-failures", first = true, failed = true))
        assertFalse(inheritLegacyPreparationState("preparation-failures", first = false, failed = true))
        assertFalse(inheritLegacyPreparationState("preparation-paused", first = false, failed = true))
        assertTrue(inheritLegacyPreparationState("preparation-paused", first = false, failed = false))
        assertFalse(inheritLegacyPreparationState("preparation-verification", first = false, failed = false))
        assertTrue(inheritLegacyPreparationState("preparation-folder", first = false, failed = true))
    }

    @Test fun qualitiesRemainSeparateAndJournalsSurviveSerialization() {
        val record = SiteDownloadPreparation(series, episodes, "720p", listOf(SiteDownloadPreparationBatch(series, episodes, "1080p")))
        val gson = com.google.gson.Gson()
        val restored = gson.fromJson(gson.toJson(record), SiteDownloadPreparation::class.java)
        val items = splitPreparation(restored)
        assertEquals(6, items.size)
        assertEquals(setOf("720p", "1080p"), items.values.map { it.quality }.toSet())
    }

    @Test fun verificationOnOneItemDoesNotStopAnother() {
        val aggregate = aggregatePreparationStatus(mapOf("a" to PreparationStatus(true, verificationUrl = series.url), "b" to PreparationStatus(true)))
        assertTrue(aggregate.automatic)
        assertEquals(series.url, aggregate.verificationUrl)
        assertFalse(aggregate.items.getValue("a").automatic)
        assertTrue(aggregate.items.getValue("b").automatic)
    }
}
