package com.example.animepahedl

import com.example.animepahedl.metadata.*
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

/** Opt-in replay of fresh public HTML; signed media URLs stay outside the repository. */
class NartoLiveCaptureTest {
    @Test fun liveNartoInventoryBrowseSearchProviderTaxonomyAndEpisodeResolution() = runBlocking {
        val directory = System.getenv("NARTO_CAPTURE_DIR")
        assumeNotNull(directory)
        fun capture(name: String) = File(directory!!, "narto-live-$name").readText()
        val detail = "https://edge.narto-drama.com/detail/watch/my-x-ray-vision-is-too-op"
        val detailHtml = capture("detail.html")
        val episodeHtml = capture("episode.html")
        val requests = mutableListOf<String>()
        val api = NartoMetadataApi(injectedGet = { url ->
            requests += url
            val path = url.toHttpUrl().encodedPath
            val body = when {
                path == "/" -> capture("home.html")
                path == "/search" -> capture("search.json")
                path == "/home/providers/sections" -> capture("provider.json")
                path == "/detail/watch/my-x-ray-vision-is-too-op" -> detailHtml
                else -> episodeHtml
            }
            MetadataResponse(url, body, "text/html")
        })
        assertTrue(api.browse().items.isNotEmpty())
        assertTrue(api.browse(MetadataBrowseQuery(query = "My X-Ray Vision is Too OP")).items.any { it.title == "My X-Ray Vision is Too OP" })
        assertTrue(api.browse(MetadataBrowseQuery(filters = mapOf("provider" to "gorogoro"))).items.isNotEmpty())
        assertTrue(api.taxonomy().groups.getValue("providers").isNotEmpty())
        requests.clear()
        val details = api.details(detail)
        assertEquals("My X-Ray Vision is Too OP", details.record.title)
        assertEquals((1..59).map(Int::toString), details.episodes.map { it.number })
        assertEquals(59, details.episodes.map { it.url }.distinct().size)
        assertEquals(2, requests.size)
        val episode = api.episode("$detail/26")
        assertEquals("26", episode.number)
        assertTrue(episode.subtitles.isNotEmpty())
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val isEpisode = dramaRoute(request.url, SiteDownloadProvider.NARTO).second != null
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body((if (isEpisode) episodeHtml else detailHtml).toResponseBody()).build()
        }.build()
        val repository = SiteDownloadRepository(client, "test")
        val series = repository.inspect(detail)
        assertEquals((1..59).toList(), series.episodes.map { it.number })
        assertTrue(series.poster.isNotBlank())
        val media = repository.resolve(series.episodes[25].url)
        assertEquals("application/x-mpegURL", media.mimeType)
        assertTrue(media.subtitles.orEmpty().isNotEmpty())
        assertFalse(media.headers.containsKey("Cookie"))
    }
}
