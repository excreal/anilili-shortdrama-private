package com.example.animepahedl

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogViewModelTest {
    @Test fun nartoFiltersMustMatchSupportedRouteKind() {
        val category = "https://edge.narto-drama.com/category/romance"
        assertTrue(isSafeMetadataFilterRoute(CatalogSource.NARTO, "category", category))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.NARTO, "actor", category))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.NARTO, "director", category))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.NARTO, "genre", category))
        assertTrue(isSafeMetadataFilterRoute(CatalogSource.NARTO, "genre", "https://edge.narto-drama.com/genre/romance"))
    }

    @Test fun shortDramaEditionExcludesMovieAndKDramaProviders() {
        assertEquals(
            setOf(SiteDownloadProvider.DRAMATIP, SiteDownloadProvider.KISS18, SiteDownloadProvider.NARTO, SiteDownloadProvider.HUANGGUO, SiteDownloadProvider.DIRECT),
            ShortDramaCatalogConfig.supportedProviders,
        )
    }

    @Test fun shortDramaCatalogContainsItsSupportedSources() {
        assertEquals(setOf(CatalogSource.DRAMATIP, CatalogSource.KISS18, CatalogSource.NARTO, CatalogSource.HUANGGUO), ShortDramaCatalogConfig.sources.toSet())
        assertEquals(ShortDramaCatalogConfig.sources.toSet(), CatalogSource.entries.toSet())
    }

    @Test fun cacheKeepsEveryCachedItemWithItsMatchingTailContinuation() {
        val items = (0 until 750).map { CatalogItem("Title $it", "https://source.test/$it", "") }
        val original = CatalogPage(items, "https://source.test/page/16")

        val json = catalogCacheJson(original)!!
        val restored = Gson().fromJson(json, CatalogPage::class.java)

        assertEquals(items, restored.items)
        assertEquals(original.nextUrl, restored.nextUrl)
    }

    @Test fun shortDramaCatalogCacheDoesNotChangePosterShape() {
        val original = CatalogPage(listOf(CatalogItem("Story", "https://dramatip.net/series/story", "poster")))

        assertEquals(original, catalogPageForSource(CatalogSource.DRAMATIP, original))
    }

    @Test fun failedRefreshRetainsResultsAndContinuationForSameQueryAndSource() {
        val items = listOf(CatalogItem("Current", "https://source.test/current", ""))

        val failure = catalogFailureState(items, "https://source.test/page/2", "DRAMATIP:query", append = false,
            force = true, requestKey = "DRAMATIP:query")

        assertEquals(items, failure.items)
        assertEquals("https://source.test/page/2", failure.nextUrl)
        assertEquals("DRAMATIP:query", failure.displayedKey)
    }

    @Test fun failedRefreshDoesNotRetainResultsFromAnotherQueryOrSource() {
        val items = listOf(CatalogItem("Old", "https://source.test/old", ""))

        val failure = catalogFailureState(items, "https://source.test/page/2", "DRAMATIP:old", append = false,
            force = true, requestKey = "NARTO:new")

        assertTrue(failure.items.isEmpty())
        assertNull(failure.nextUrl)
        assertNull(failure.displayedKey)
    }
}
