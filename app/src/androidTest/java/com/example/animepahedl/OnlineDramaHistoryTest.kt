package com.example.animepahedl

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineDramaHistoryTest {
    @Test fun registeredOnlineShortDramaAppearsInContinueWatchingAfterProgressIsSaved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val history = PlaybackHistory(context, "playback-history-test-${System.nanoTime()}")
        val series = SiteDownloadSeries(
            provider = SiteDownloadProvider.DRAMATIP,
            id = "DRAMATIP:continue-${System.nanoTime()}",
            title = "Online drama fixture",
            poster = "https://dramatip.net/poster.jpg",
            url = "https://dramatip.net/series/continue-fixture",
            episodes = listOf(
                SiteDownloadEpisode("episode-1", 1, "Episode 1", "https://dramatip.net/series/continue-fixture/episode-1"),
            ),
        )

        // RemoteDramaPlayer registers the inventory before DramaPlayerSurface resolves media.
        history.register(series)
        history.save("series:${series.provider.name}:${series.id}", series.episodes.single().url,
            position = 24_000L, completed = false, duration = 100_000L)

        assertEquals(listOf(series), history.recent())
        assertTrue(history.canResume(series))
        assertNotNull(history.metadata(series))
        assertEquals(24_000L, history.position(series.episodes.single().url))
    }

    @Test fun markCompletedHidesSeriesAndRemoveKeepsResumePosition() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val history = PlaybackHistory(context, "playback-history-actions-${System.nanoTime()}")
        val series = SiteDownloadSeries(SiteDownloadProvider.NARTO, "NARTO:actions", "Action fixture", "", "https://narto-drama.com/detail/watch/actions",
            listOf(SiteDownloadEpisode("one", 1, "Episode 1", "https://narto-drama.com/detail/watch/actions/1")))
        val key = "series:${series.provider.name}:${series.id}"
        val episode = series.episodes.single().url
        history.register(series)
        history.save(key, episode, 1_000, duration = 10_000)
        history.markCompleted(series.copy(episodes = emptyList()))
        assertTrue(history.recent().isEmpty())
        assertTrue(history.watched(episode))
        history.setWatched(episode, false)
        history.save(key, episode, 1_000, duration = 10_000)
        history.removeFromContinueWatching(series)
        assertTrue(history.recent().isEmpty())
        assertEquals(1_000L, history.position(episode))
    }
}
