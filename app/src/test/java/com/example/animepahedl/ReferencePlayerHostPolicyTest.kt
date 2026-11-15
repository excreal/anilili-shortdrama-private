package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferencePlayerHostPolicyTest {
    @Test
    fun everyProviderUsesTheShortDramaBoundaryForHostPolicy() {
        SiteDownloadProvider.entries.forEach { provider ->
            val shortDrama = provider in setOf(SiteDownloadProvider.DRAMATIP, SiteDownloadProvider.KISS18, SiteDownloadProvider.NARTO, SiteDownloadProvider.HUANGGUO)
            assertEquals(shortDrama, provider.isShortDrama)
            assertEquals(!shortDrama, provider.usesReferencePlayer)
            assertEquals(!shortDrama, ReferencePlayerHostPolicy.startsFullscreen(provider, isTv = false))
            assertTrue(ReferencePlayerHostPolicy.startsFullscreen(provider, isTv = true))
            assertEquals(shortDrama, ReferencePlayerHostPolicy.allowsFullscreenToggle(provider))
            assertEquals(!shortDrama, ReferencePlayerHostPolicy.closesOnBack(
                provider, fullscreen = true, isTv = false, inPip = false))
            assertTrue(ReferencePlayerHostPolicy.closesOnBack(
                provider, fullscreen = false, isTv = false, inPip = false))
            assertEquals(!shortDrama, ReferencePlayerHostPolicy.forcesLandscape(
                provider, fullscreen = false, isTv = false))
            assertFalse(ReferencePlayerHostPolicy.forcesLandscape(provider, fullscreen = true, isTv = true))
        }
    }

    @Test
    fun onlyPersistedContentAndFileUrisUseTheLibraryOfflineRoute() {
        assertTrue(isLocalPlaybackUri("content://downloads/video/42"))
        assertTrue(isLocalPlaybackUri("file:///storage/emulated/0/Movies/video.mp4"))
        assertFalse(isLocalPlaybackUri("https://kisskh.do/episode/42"))
    }

    @Test
    fun legacyNonSiteRowsBuildOfflineLandscapeQueueFromSavedUris() {
        val row = DownloadRow("row-1", "Saved movie", TransferState.COMPLETE, 100f,
            savedUri = "content://downloads/movie.mp4", availability = Availability.SAVED, showId = "", episodeNumber = "1")
        val identity = legacyIdentity("animepahe:movie-1")
        val records = mapOf(row.id to ExportRecord(row.title, "", "", uri = row.savedUri, identity = identity))

        val series = savedLandscapeFallbackFromDownloads(listOf(row), records)

        assertEquals(SiteDownloadProvider.DIRECT, series?.provider)
        assertEquals("content://downloads/movie.mp4", series?.episodes?.single()?.url)
        assertEquals("row-1", series?.episodes?.single()?.id)
        listOf(SiteDownloadProvider.DRAMATIP, SiteDownloadProvider.KISS18, SiteDownloadProvider.NARTO, SiteDownloadProvider.HUANGGUO).forEach { provider ->
            assertNull(savedLandscapeFallbackFromDownloads(
                listOf(row.copy(showId = "sites:${provider.name}:known")), emptyMap()))
        }
    }

    @Test
    fun groupedLegacyEpisodesKeepStableSeriesIdentityAndOrder() {
        val first = DownloadRow("row-1", "Episode 1", TransferState.COMPLETE, 100f,
            savedUri = "content://downloads/one.mp4", availability = Availability.SAVED, episodeNumber = "1")
        val second = DownloadRow("row-2", "Episode 2", TransferState.COMPLETE, 100f,
            savedUri = "content://downloads/two.mp4", availability = Availability.SAVED, episodeNumber = "2")
        val records = mapOf(
            first.id to ExportRecord(first.title, "", "", uri = first.savedUri, identity = legacyIdentity("legacy:show-1")),
            second.id to ExportRecord(second.title, "", "", uri = second.savedUri, identity = legacyIdentity("legacy:show-1")),
        )

        val playlist = savedDownloadPlaylist(second, listOf(second, first), records)
        val series = savedLandscapeFallbackFromDownloads(playlist, records)
        val reversedSeries = savedLandscapeFallbackFromDownloads(playlist.reversed(), records)

        assertEquals(listOf("row-1", "row-2"), playlist.map { it.id })
        assertEquals("saved:legacy:show-1", series?.id)
        assertEquals(series?.id, reversedSeries?.id)
        assertEquals(listOf("row-1", "row-2"), series?.episodes?.map { it.id })
    }

    private fun legacyIdentity(session: String) = DownloadIdentity(
        Anime(session, "Legacy series", "", "", "Series", ""),
        Episode("episode", "1", "", ""), "Original", "und")
}
