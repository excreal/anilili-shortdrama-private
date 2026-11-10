package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import okhttp3.OkHttpClient

class EditionProviderBoundaryTest {
    @Test fun shortDramaEditionRejectsMovieLinksBeforeAnyNetworkRequest() = runBlocking {
        val repository = appSiteDownloadRepository()
        val inspectError = runCatching { repository.inspect("https://kisskh.do/Drama/Some-Show?id=123") }.exceptionOrNull()
        val playbackError = runCatching { repository.resolve("https://eroticmv.com/video/example") }.exceptionOrNull()

        assertTrue(inspectError is IOException)
        assertEquals("K-drama sources aren't included in this edition.", inspectError?.message)
        assertTrue(playbackError is IOException)
        assertEquals("Movie sources aren't included in this edition.", playbackError?.message)
    }

    @Test fun directlyConstructedRepositoryAlsoRejectsMovieLinksBeforeNetwork() = runBlocking {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor {
            requests++
            throw IOException("Unexpected request")
        }.build()
        val repository = SiteDownloadRepository(client = client)
        val errors = listOf(
            runCatching { repository.inspect("https://kisskh.do/Drama/Some-Show?id=123") }.exceptionOrNull(),
            runCatching { repository.resolve("https://eroticmv.com/video/example") }.exceptionOrNull(),
        )
        assertTrue(errors.all { it is IOException })
        assertEquals(0, requests)
    }
}
