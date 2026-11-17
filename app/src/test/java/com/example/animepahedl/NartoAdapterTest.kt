package com.example.animepahedl

import com.example.animepahedl.metadata.*
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class NartoAdapterTest {
    private val detail = "https://edge.narto-drama.com/detail/watch/story"
    private fun inventory() = """<title>Story - Free Streaming</title><h1 class="movie-title">Story</h1>
        <div class="cf-turnstile"></div><meta property="og:image" content="/cover.jpg">
        <a class="episode-item" href="$detail/2">2</a><a class="episode-item" href="$detail/1">1</a>
        <a class="episode-item" href="https://narto-drama.com/detail/watch/story/3">3</a>
        <a class="episode-item" href="/detail/watch/other/4">4</a>
        <a class="episode-item" href="https://bad.test/detail/watch/story/5">5</a>"""
    private fun client(body: String, effective: String? = null, status: Int = 200, mitigated: Boolean = false) =
        OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(effective?.let { chain.request().newBuilder().url(it).build() } ?: chain.request())
                .protocol(Protocol.HTTP_1_1).code(status).message("fixture")
                .apply { if (mitigated) header("cf-mitigated", "challenge") }
                .body(body.toResponseBody()).build()
        }.build()

    @Test fun detailWidgetsDoNotBlockInventoryAndOnlyMatchingProviderLinksSurvive() = runBlocking {
        val requests = mutableListOf<Request>()
        val transport = client(inventory()).newBuilder().apply {
            interceptors().add(0, okhttp3.Interceptor { chain ->
                requests += chain.request(); chain.proceed(chain.request())
            })
        }.build()
        val result = SiteDownloadRepository(transport, "test").inspect("$detail/26?lang=id")
        assertEquals(listOf(1, 2, 3), result.episodes.map { it.number })
        assertEquals("Story", result.title)
        assertEquals("https://edge.narto-drama.com/cover.jpg", result.poster)
        assertEquals(1, requests.size)
        assertEquals(detail + "?lang=en-US", result.url)
    }

    @Test fun metadataTransportAcceptsWidgetsButRejectsActualChallengeAndHttpErrors() = runBlocking {
        suspend fun fetch(body: String, status: Int = 200, mitigated: Boolean = false) = MetadataHttp(
            setOf("edge.narto-drama.com"), allowEmbeddedWidgets = true,
            client = client(body, status = status, mitigated = mitigated)).get(detail)
        assertEquals(inventory(), fetch(inventory()).text)
        for (failure in listOf(
            runCatching { fetch("<title>Just a moment</title>") }.exceptionOrNull(),
            runCatching { fetch(inventory(), mitigated = true) }.exceptionOrNull(),
            runCatching { fetch(inventory(), status = 503) }.exceptionOrNull())) {
            assertTrue(failure is java.io.IOException)
            assertFalse(failure is VerificationRequired)
        }
    }

    @Test fun partialHydrationRetainsInventoryAndBuildsDistinctMissingWatchUrls() = runBlocking {
        val requests = mutableListOf<String>()
        val api = NartoMetadataApi(injectedGet = { url ->
            requests += url
            MetadataResponse(url, if (url == detail) inventory() else """<script>const episodeItemsRaw = [
              {"route_episode_number":2,"title":"Enriched 2"},
              {"route_episode_number":3,"watch_url":"/detail/watch/other/3","title":"Wrong show"}
            ];</script>""", "text/html")
        })
        val result = api.details(detail)
        assertEquals(listOf("1", "2", "3"), result.episodes.map { it.number })
        assertEquals("$detail/2", result.episodes[1].url)
        assertEquals("Enriched 2", result.episodes[1].title)
        assertFalse(result.episodes.any { it.title == "Wrong show" })
        assertEquals(listOf(detail, "$detail/1"), requests)
    }

    @Test fun embeddedEpisodeMetadataAvoidsExtraHydrationRequest() = runBlocking {
        var calls = 0
        val api = NartoMetadataApi(injectedGet = { url ->
            calls++
            MetadataResponse(url, inventory() + """<script>const episodeItemsRaw = [{"route_episode_number":1,"title":"Enriched"}];</script>""", "text/html")
        })
        assertEquals("Enriched", api.details(detail).episodes.first().title)
        assertEquals(1, calls)
    }

    @Test fun redirectsCannotReplaceSeriesOrEpisodeAndFailedHydrationKeepsAnchors() = runBlocking {
        val api = NartoMetadataApi(injectedGet = { url ->
            MetadataResponse(if (url == detail) url else "$detail/2", inventory(), "text/html")
        })
        assertEquals(3, api.details(detail).episodes.size)
        assertTrue(runCatching { api.episode("$detail/1") }.isFailure)
        val wrong = "https://edge.narto-drama.com/detail/watch/other"
        assertTrue(runCatching { SiteDownloadRepository(client(inventory(), wrong), "test").inspect(detail) }.isFailure)
        assertTrue(runCatching { NartoMetadataApi(injectedGet = { MetadataResponse(wrong, inventory(), "text/html") }).details(detail) }.isFailure)
    }

    @Test fun metadataOnlyDetailsRetainEveryPublishedEpisodeAndEmptyPagesFail() = runBlocking {
        val body = """<h1>Story</h1><script>const episodeItemsRaw = [{"route_episode_number":1},{"route_episode_number":2}];</script>"""
        val api = NartoMetadataApi(injectedGet = { MetadataResponse(it, body, "text/html") })
        assertEquals(listOf("$detail/1", "$detail/2"), api.details(detail).episodes.map { it.url })
        assertTrue(runCatching { NartoMetadataApi(injectedGet = { MetadataResponse(it, "<h1>Unavailable</h1>", "text/html") }).details(detail) }.isFailure)
        val invalidCards = NartoMetadataApi(injectedGet = { MetadataResponse(it,
            """{"ok":true,"items":[{"title":"Missing URL"},{"title":"Wrong route","url":"/login"}]}""", "application/json") })
        assertTrue(invalidCards.browse().items.isEmpty())
    }

    @Test fun playbackRejectsPublishedMediaForAnotherShow() {
        val body = """<script>const episodeItemsRaw = [{"route_episode_number":26,"watch_url":"/detail/watch/other/26","play_url":"https://cdn.test/wrong.mp4"}];</script>"""
        assertTrue(runCatching { parseNartoVariants(body, "$detail/26".toHttpUrl(), 26) }.isFailure)
    }

    @Test fun subtitlesFollowExactEpisodeAndRejectInsecureTracks() {
        val body = """<script>const episodeItemsRaw = [
          {"route_episode_number":1,"play_url":"https://cdn.test/1.mp4"},
          {"route_episode_number":26,"play_url":"https://cdn.test/26","browser_prefetch_mode":"hls",
           "multi_subtitles":[{"subtitle_url":"/26.vtt","language_code":"en","label":"English"},{"subtitle_url":"http://bad.test/sub.vtt"}],
           "subtitle_url":"/26.vtt","direct_subtitle_url":"https://cdn.test/26.srt"}];</script>"""
        val variant = parseNartoVariants(body, "$detail/26".toHttpUrl(), 26).single()
        assertEquals("https://cdn.test/26", variant.url)
        assertEquals("application/x-mpegURL", variant.mimeType)
        assertEquals(listOf("https://edge.narto-drama.com/26.vtt", "https://cdn.test/26.srt"), variant.subtitles.map { it.url })
        assertEquals("English", variant.subtitles.first().label)
    }
}
