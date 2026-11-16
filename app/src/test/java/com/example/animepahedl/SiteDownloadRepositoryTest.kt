package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SiteDownloadRepositoryTest {
    @Test fun nartoPublishedEpisodeMetadataSurvivesEmbeddedChallenge() = runBlocking {
        val repo = repository { """<title>Episode 26</title><div class="cf-turnstile"></div><script>const episodeItemsRaw = [{"route_episode_number":26,"play_url":"https://cdn.test/26.mp4"}];</script>""" }
        assertEquals("https://cdn.test/26.mp4", repo.resolve("https://edge.narto-drama.com/detail/watch/story/26").url)
        assertEquals(1, requests.size)
    }
    @Test fun nartoAdGateStopsWithAccurateErrorInsteadOfVerificationLoop() = runBlocking {
        val repo = repository { "<h1>Ad before watching</h1><p>6 of 6 ads required to unlock the stream</p>" }
        val failure = runCatching { repo.resolve("https://narto-drama.com/detail/watch/story/26") }.exceptionOrNull()
        assertTrue(failure is ProviderAdGate)
        assertEquals(listOf("narto-drama.com", "edge.narto-drama.com"), requests.map { it.url.host })
    }
    @Test fun nartoRealChallengeStillRequiresVerification() = runBlocking {
        val repo = repository { "<title>Just a moment</title>" }
        assertTrue(runCatching { repo.resolve("https://edge.narto-drama.com/detail/watch/story/26") }.exceptionOrNull() is VerificationRequired)
    }
    private val requests = mutableListOf<Request>()
    private fun repository(respond: (Request) -> String): SiteDownloadRepository {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(respond(request).toResponseBody("text/html".toMediaType())).build()
        }.build()
        return SiteDownloadRepository(client, "Mozilla/5.0")
    }
    private fun encrypted(url: String): String {
        val nonce = ByteArray(12) { 7 }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(Base64.getDecoder().decode("QC6Ir2trghxRAyyyWZEOEFR4GgLhnfQ4A19I3QBlQkc="), "AES"), GCMParameterSpec(128, nonce))
        return Base64.getEncoder().encodeToString(nonce + cipher.doFinal(url.toByteArray()))
    }
    @Test fun inspectThenResolveRetainsEpisodeAndDoesNotSendCookiesToCdn() = runBlocking {
        val payload = encrypted("https://cdn.test/one.mp4")
        val repo = repository { req ->
            if (req.url.encodedPath == "/series/story") """<title>A Story | Dramatip</title><a href="/series/story/episode-1">1</a>"""
            else """{"sourceFirst":{"chain":[{"type":"mp4","encByQuality":{"720":"$payload"}}]}}"""
        }
        val series = repo.inspect("https://dramatip.net/series/story", "session=secret")
        assertEquals("A Story", series.title)
        assertEquals(listOf(1), series.episodes.map { it.number })
        assertEquals(listOf("Best"), series.qualities)
        assertEquals(listOf("/series/story"), requests.map { it.url.encodedPath })
        assertEquals(listOf("Best", "720p"), repo.discoverQualities(series, cookie = "session=secret"))
        val media = repo.resolve(series.episodes.single().url, cookie = "session=secret")
        assertEquals("https://cdn.test/one.mp4", media.url)
        assertFalse(media.headers.containsKey("Cookie"))
        assertTrue(requests.all { it.header("Cookie") == "session=secret" })
        assertTrue(requests.all { it.url.host == "dramatip.net" })
    }

    @Test fun failedQualityProbeCannotBlockEpisodeSelection() = runBlocking {
        val repo = repository { req ->
            if (req.url.encodedPath == "/series/story") """<title>A Story | Dramatip</title><a href="/series/story/episode-1">1</a><a href="/series/story/episode-2">2</a>"""
            else throw java.io.IOException("Player unavailable")
        }
        val series = repo.inspect("https://dramatip.net/series/story")
        assertEquals(listOf("/series/story"), requests.map { it.url.encodedPath })
        assertEquals(listOf(1, 2), parseSiteDownloadSelection("1-2", series.episodes).map { it.number })
        assertTrue(runCatching { repo.discoverQualities(series, episodeNumber = 2) }.isFailure)
        assertEquals("/series/story/episode-2", requests.last().url.encodedPath)
        assertEquals(listOf(1, 2), parseSiteDownloadSelection("1-2", series.episodes).map { it.number })
    }

    @Test fun delayedQualityProbeRunsAfterInventoryAndSelectionAreReady() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val repo = repository { req ->
            if (req.url.encodedPath == "/series/story") """<title>A Story | Dramatip</title><a href="/series/story/episode-1">1</a><a href="/series/story/episode-2">2</a>"""
            else {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                """{"sourceFirst":{"chain":[{"type":"mp4","enc":"${encrypted("https://cdn.test/one.mp4")}"}]}}"""
            }
        }
        val series = repo.inspect("https://dramatip.net/series/story")
        val probe = async(Dispatchers.Default) { repo.discoverQualities(series) }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        assertEquals(listOf(1, 2), parseSiteDownloadSelection("1-2", series.episodes).map { it.number })
        release.countDown()
        assertEquals(listOf("Best", "Original"), probe.await())
    }

    @Test fun qualityResultForReplacedTitleIsDropped() {
        val old = SiteDownloadSeries(SiteDownloadProvider.DRAMATIP, "DRAMATIP:old", "Old", "", "https://dramatip.net/series/old",
            listOf(SiteDownloadEpisode("old-1", 1, "1", "https://dramatip.net/series/old/episode-1")))
        val replacement = old.copy(id = "DRAMATIP:new", title = "New", qualities = listOf("Best"))
        assertSame(replacement, mergeDiscoveredQualities(replacement, old.provider, old.id, 4, 5, listOf("Best", "720p")))
        assertEquals("New", replacement.title)
        assertEquals(listOf("Best"), replacement.qualities)
    }

    @Test fun metadataAndQualityMergesPreserveEitherCompletionOrder() {
        val inventory = SiteDownloadSeries(SiteDownloadProvider.DRAMATIP, "DRAMATIP:show", "Show", "", "https://dramatip.net/series/show", emptyList())
        val metadata = SiteShowMetadata(description = "Enriched")
        val qualityFirst = mergeInspectionMetadata(
            mergeDiscoveredQualities(inventory, inventory.provider, inventory.id, 2, 2, listOf("Best", "720p")),
            inventory.provider, inventory.id, 2, 2, metadata)
        val metadataFirst = mergeDiscoveredQualities(
            mergeInspectionMetadata(inventory, inventory.provider, inventory.id, 2, 2, metadata),
            inventory.provider, inventory.id, 2, 2, listOf("Best", "720p"))
        assertEquals(metadata, qualityFirst.metadata)
        assertEquals(listOf("Best", "720p"), qualityFirst.qualities)
        assertEquals(metadata, metadataFirst.metadata)
        assertEquals(listOf("Best", "720p"), metadataFirst.qualities)
    }
    @Test fun providerSameOriginMediaRetainsCookie() = runBlocking {
        val payload = encrypted("https://dramatip.net/media/one.mp4")
        val repo = repository { """{"sourceFirst":{"chain":[{"type":"mp4","enc":"$payload"}]}}""" }
        val media = repo.resolve("https://dramatip.net/series/story/episode-1", cookie = "session=private")
        assertEquals("session=private", media.headers["Cookie"])
    }
    @Test fun episodeRedirectMustNotSubstituteAnotherVideo() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val wrong = chain.request().newBuilder().url("https://kiss18.vip/watch/story/1").build()
            val schema = """<script type="application/ld+json">{"@type":"VideoObject","url":"/watch/story/1","contentUrl":"https://cdn.test/1.mp4"}</script>"""
            Response.Builder().request(wrong).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(schema.toResponseBody("text/html".toMediaType())).build()
        }.build()
        val result = runCatching { SiteDownloadRepository(client).resolve("https://kiss18.vip/watch/story/2") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message.orEmpty().contains("different episode"))
    }
    @Test fun httpErrorsDoNotExposeCookieOrSignedUrl() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(403).message("Forbidden")
                .body("private backend detail".toResponseBody()).build()
        }.build()
        val result = runCatching { SiteDownloadRepository(client).inspect("https://dramatip.net/series/story", "session=secret") }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message.orEmpty().contains("HTTP 403"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("secret"))
        assertFalse(result.exceptionOrNull()!!.message.orEmpty().contains("private backend"))
    }
    @Test fun directFilesPreserveTypesAndHttpLinks() = runBlocking {
        val repo = repository { error("Direct resolution should not fetch metadata") }
        assertEquals("video/webm", repo.resolve("https://cdn.test/video.webm").mimeType)
        assertEquals("http://cdn.test/video.mp4", repo.inspect("http://cdn.test/video.mp4").episodes.single().url)
        assertTrue(requests.isEmpty())
    }
}
