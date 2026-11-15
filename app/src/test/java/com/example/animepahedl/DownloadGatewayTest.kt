package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Test

class DownloadGatewayTest {
    @Test fun nartoDownloadsPreserveGatewayAndHeadersWithoutAProbe() = runBlocking {
        val identity = DownloadIdentity(
            Anime("sites:NARTO:story", "Story", "", "", "Series", ""),
            Episode("1", "1", "", ""), "Best", "und", "sites",
            "https://narto-drama.com/detail/watch/story/1",
        )
        for (mime in listOf("application/x-mpegURL", "video/mp4")) {
            // An unreachable address makes any accidental network preflight fail this test.
            val media = ResolvedMedia("https://127.0.0.1:1/gateway", "Best", "und",
                headers = mapOf("Referer" to identity.locator!!),
                backupUrls = listOf("https://127.0.0.1:1/backup"), mimeType = mime)
            assertSame(media, MediaRequests.forDownload(identity, media) {
                error("Preparing this gateway must not make an HTTP request")
            })
        }
    }
}
