package com.example.animepahedl

import com.example.animepahedl.metadata.*
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DramatipMetadataFixtureTest {
    @Test fun detailGenresBecomeSafeBrowseCategoryLinks() {
        val fixture = """
            <html><head><title>Test drama</title></head><body>
              <h1>Test drama</h1>
              <div id="series-info"><dl><div><dt>Genres</dt><dd>
                <span>Hidden Identity</span><span>Café &amp; Revenge</span>
              </dd></div></dl></div>
            </body></html>
        """.trimIndent()

        val genres = parseDramatipMetadataDetails(fixture, "https://dramatip.net/series/test-drama").record.genres

        assertEquals(listOf("Hidden Identity", "Café & Revenge"), genres.map { it.name })
        assertEquals(
            listOf("https://dramatip.net/category/hidden-identity", "https://dramatip.net/category/cafe-revenge"),
            genres.map { it.url },
        )
    }

    @Test fun heroAndCardHeadingsStayDistinctAndGermanNavigationIsRetained() {
        val fixture = """
            <html lang="de"><body>
              <section aria-label="Beliebt">
                <a href="/series/demo"><img src="/demo.jpg" alt="Demo"><h2>Demo</h2></a>
                <a href="/category/popular">See all</a>
              </section>
              <nav><a rel="next" aria-label="Nächste Seite" href="/category/new-releases/page/2">Weiter</a></nav>
            </body></html>
        """.trimIndent()
        val page = parseDramatipMetadataPage(fixture, "https://dramatip.net/de")
        assertEquals("Beliebt", page.sections.single().name)
        assertEquals("Demo", page.sections.single().items.single().title)
        assertEquals("https://dramatip.net/category/popular", page.sections.single().url)
        assertEquals("https://dramatip.net/category/new-releases/page/2", page.nextUrl)
        val pagination = page.payloads.first { it.name == "pagination" }.json
        assertTrue(pagination.contains("Nächste Seite"))
        assertTrue(pagination.contains("/category/new-releases/page/2"))
    }

    @Test fun homepagePageTwoFetchesItsOwnNextUrlInsteadOfFirstSectionSeeAll() = runBlocking {
        val homepage = """
            <html><body>
              <section><h2>Popular</h2>
                <a href="/series/popular"><img src="/popular.jpg" alt="Popular"></a>
                <a href="/category/popular">See all</a>
              </section>
              <a rel="next" href="/category/new-releases/page/2">Next</a>
            </body></html>
        """.trimIndent()
        val newReleasesPage = """
            <html><body><a href="/series/new-release">
              <img src="/new.jpg" alt="New Release">
            </a></body></html>
        """.trimIndent()
        val requested = mutableListOf<String>()
        val api = DramatipMetadataApi(fetch = { url ->
            requested += url
            when (url) {
                "https://dramatip.net/" -> MetadataResponse(url, homepage, "text/html")
                "https://dramatip.net/category/new-releases/page/2" -> MetadataResponse(url, newReleasesPage, "text/html")
                else -> error("Unexpected Dramatip request: $url")
            }
        })

        val page = api.browse(MetadataBrowseQuery(page = 2))

        assertEquals(listOf("https://dramatip.net/", "https://dramatip.net/category/new-releases/page/2"), requested)
        assertEquals(listOf("New Release"), page.items.map { it.title })
        assertFalse(page.items.any { it.title == "Popular" })
    }

    @Test fun homepagePageFiveChangesOnlyAdvertisedPageSegment() = runBlocking {
        val homepage = """<html><body><a rel="next" href="/category/new-releases/page/2">Next</a></body></html>"""
        val pageFive = """<html><body><a href="/series/page-five"><img src="/five.jpg" alt="Page Five"></a></body></html>"""
        val requested = mutableListOf<String>()
        val api = DramatipMetadataApi(fetch = { url ->
            requested += url
            when (url) {
                "https://dramatip.net/" -> MetadataResponse(url, homepage, "text/html")
                "https://dramatip.net/category/new-releases/page/5" -> MetadataResponse(url, pageFive, "text/html")
                else -> error("Unexpected Dramatip request: $url")
            }
        })

        val page = api.browse(MetadataBrowseQuery(page = 5))

        assertEquals(listOf("https://dramatip.net/", "https://dramatip.net/category/new-releases/page/5"), requested)
        assertEquals("Page Five", page.items.single().title)
    }

    @Test fun outOfRangePageIsRejectedWithoutFetching() = runBlocking {
        var requests = 0
        val api = DramatipMetadataApi(fetch = {
            requests++
            error("No request expected")
        })

        assertTrue(runCatching { api.browse(MetadataBrowseQuery(page = 101)) }.isFailure)
        assertEquals(0, requests)
    }

    @Test fun homepageRepeatedNextUrlIsRejected() = runBlocking {
        var requests = 0
        val homepage = """<html><body><a rel="next" href="/">Next</a></body></html>"""
        val api = DramatipMetadataApi(fetch = { url ->
            requests++
            MetadataResponse(url, homepage, "text/html")
        })

        assertTrue(runCatching { api.browse(MetadataBrowseQuery(page = 2)) }.exceptionOrNull() is java.io.IOException)
        assertEquals(1, requests)
    }

    @Test fun sourceIndexUsesSemanticNameAndKeepsCountsSeparate() {
        val fixture = """
            <html><body><a href="/source/mymuse">
              <span aria-hidden="true">MY</span><span>MyMuse</span><span>1,234 titles</span>
            </a></body></html>
        """.trimIndent()
        val page = parseDramatipMetadataPage(fixture, "https://dramatip.net/sources")
        val source = page.filters.getValue("sources").single()
        assertEquals("MyMuse", source.name)
        assertEquals(1234L, source.count)
    }

    @Test fun subtitleDescriptorsNormalizeFormatsDefaultsAndRejectUnsafeUrls() {
        val source = JsonObject().apply {
            add("subtitle", com.google.gson.JsonParser.parseString(
                """[
                  {"url":"../captions/main.vtt","lang":"de","format":"text/vtt","default":true},
                  {"url":"http://captions.test/sub.srt","lang":"en","default":false},
                  {"url":"https://user:pass@captions.test/private.vtt","lang":"en"},
                  {"url":"javascript:alert(1)","lang":"en"}
                ]"""
            ))
        }
        val tracks = dramatipSubtitleTracks(source, "https://dramatip.net/series/a/episode-1")
        assertEquals(1, tracks.size)
        assertEquals("https://dramatip.net/series/captions/main.vtt", tracks.single().url)
        assertEquals("vtt", tracks.single().format)
        assertTrue(tracks.single().isDefault)
        assertEquals("de", tracks.single().language)
    }

    @Test fun encryptedSubtitleDescriptorUsesEncFormatAndLanguageFields() {
        val encryptedUrl = encryptDramaSubtitleFixture("https://mydramawave.test/subtitles/episode.srt")
        val source = JsonObject().apply {
            add("subtitle", JsonObject().apply {
                addProperty("enc", encryptedUrl)
                addProperty("format", "srt")
                addProperty("language", "de")
            })
        }
        val track = dramatipSubtitleTracks(source, "https://dramatip.net/series/demo/episode-1").single()
        assertEquals("https://mydramawave.test/subtitles/episode.srt", track.url)
        assertEquals("srt", track.format)
        assertEquals("de", track.language)
        assertFalse(track.isDefault)
    }

    @Test fun http200NotFoundPagesAreRejected() {
        val notFound = org.jsoup.Jsoup.parse("<html><head><title>404 - Page Not Found</title></head><body><h1>Not Found</h1></body></html>")
        assertTrue(runCatching { dramatipRequireNotFoundAbsent(notFound) }.exceptionOrNull() is java.io.IOException)
        val valid = org.jsoup.Jsoup.parse("<html><head><title>Drama</title></head><body><h1>Drama</h1></body></html>")
        dramatipRequireNotFoundAbsent(valid)
    }

    @Test fun episodeSchemaFactsAreAvailableFromItsOwnCanonicalDocument() {
        val fixture = """
            <html><head><link rel="canonical" href="https://dramatip.net/series/demo/episode-1">
              <script type="application/ld+json">{
                "@type":"TVEpisode","url":"https://dramatip.net/series/demo/episode-1",
                "name":"Demo Episode 1","description":"Episode synopsis","duration":"PT24M",
                "uploadDate":"2026-10-01","thumbnailUrl":"https://img.test/episode.jpg"
              }</script>
            </head><body><h1>Episode page</h1></body></html>
        """.trimIndent()
        val episode = metadataFromHtml(fixture, "https://dramatip.net/series/demo/episode-1")
        assertEquals("Demo Episode 1", episode.title)
        assertEquals("Episode synopsis", episode.description)
        assertEquals(1440L, episode.durationSeconds)
        assertEquals("2026-10-01", episode.releaseDate)
        assertTrue(episode.images.any { it.url == "https://img.test/episode.jpg" })
    }
}

/** Produces an in-memory descriptor matching Dramatip's AES-GCM enc field; no ciphertext fixture is stored. */
private fun encryptDramaSubtitleFixture(url: String): String {
    val key = Base64.getDecoder().decode("QC6Ir2trghxRAyyyWZEOEFR4GgLhnfQ4A19I3QBlQkc=")
    val iv = ByteArray(12).also(java.security.SecureRandom()::nextBytes)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
    return Base64.getEncoder().encodeToString(iv + cipher.doFinal(url.toByteArray(Charsets.UTF_8)))
}
