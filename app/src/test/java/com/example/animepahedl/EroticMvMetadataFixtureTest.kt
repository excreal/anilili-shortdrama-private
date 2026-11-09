package com.example.animepahedl

import com.example.animepahedl.metadata.parseEroticMvMetadataDetails
import com.example.animepahedl.metadata.parseEroticMvMetadataFeed
import com.example.animepahedl.metadata.parseEroticMvMetadataPage
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

/** Sanitized snapshots of public theme markup. Scripts are represented as data only by the parser. */
class EroticMvMetadataFixtureTest {
    @Test fun emptySearchMarkerIsAValidEmptyResult() {
        val html = """<html><body><div class="none-content-info search-df">No movies found</div></body></html>"""
        assertTrue(parseEroticMvMetadataPage(html, "https://eroticmv.com/?s=notfound").items.isEmpty())
    }

    @Test fun movieFactsRatingsAndPeopleStayInsidePrimaryArticle() {
        val html = """<!doctype html><html><head><title>Sample Film (2024)</title></head><body>
          <main id="primary">
            <article id="post-42" class="post-item">
              <h1 class="entry-title">Sample Film (2024)</h1>
              <div class="entry-content">
                <p>Language: Korean</p><p>Country: Korea</p><p>Year: 2024</p>
                <p>Runtime: 95 minutes</p><p>Studio: Example Studio</p><p>Original title: Example Original</p>
                <p>A synopsis of the movie.</p>
                <div class="categories-wrap"><a href="https://eroticmv.com/category/genre/drama/">Drama</a></div>
                <div class="person-item"><img src="https://eroticmv.com/wp-content/lead.jpg" alt="Lead"><a href="https://eroticmv.com/actor/lead/">Lead</a></div>
              </div>
              <div class="yasr-visitor-votes"><span class="yasr-rater-stars-vv" data-rating="4.2"></span><span id="yasr-vv-votes-number-container">7</span></div>
              <div data-imdbdata='{"id":"tt1234567","rating":"7.8"}'></div>
            </article>
            <article class="post-item person-item"><h2>Actor archive decoy</h2><a href="/actor/other/">Other</a>
              <div class="yasr-rater-stars-vv" data-rating="1.1"></div><div data-imdbdata='{"id":"tt9999999","rating":"1.0"}'></div>
            </article>
            <article class="post-item"><h2 class="entry-title"><a href="/related/">Related film</a></h2></article>
          </main>
          <script>var vidorev_jav_js_object = {"single_media_sources":[{"src":"https://cdn.example/media.m3u8"}],"single_media_subtitles":[{"src":"/subtitles/en.vtt","label":"English","srclang":"en","kind":"captions","default":true}],"vid_vtt_preview_sprite_image":"https://cdn.example/sprite.jpg"};</script>
        </body></html>"""
        val result = parseEroticMvMetadataDetails(html, "https://eroticmv.com/sample-film-2024/")
        val record = result.record
        assertEquals("Sample Film (2024)", record.title)
        assertEquals("https://eroticmv.com/sample-film-2024/", record.url)
        assertEquals("A synopsis of the movie.", record.description)
        assertEquals(listOf("Korean"), record.languages)
        assertEquals(listOf("Korea"), record.countries)
        assertEquals(2024, record.year)
        assertEquals(5700L, record.durationSeconds)
        assertEquals(listOf("Example Studio"), record.studios.map { it.name })
        assertEquals(listOf("Lead"), record.people.map { it.name })
        assertEquals("https://eroticmv.com/wp-content/lead.jpg", record.people.single().image)
        assertEquals(listOf("tt1234567"), record.externalIds.values.toList())
        assertEquals(setOf("EroticMV", "IMDb"), record.ratings.mapNotNull { it.source }.toSet())
        assertEquals("originalTitle", record.attributes.keys.first { it == "originalTitle" })
        val player = JsonParser.parseString(result.episodes.single().payloads.single().json).asJsonObject
        assertTrue(player.has("single_media_sources"))
        assertEquals("https://cdn.example/sprite.jpg", player.get("vid_vtt_preview_sprite_image").asString)
        val subtitle = result.episodes.single().subtitles.single()
        assertEquals("text/vtt", subtitle.format)
        assertEquals("captions", subtitle.attributes["kind"])
        assertTrue(subtitle.isDefault)
    }

    @Test fun emptySearchAndFeedRoutesRejectUnsafeSourceUrls() {
        assertTrue(runCatching { parseEroticMvMetadataPage("<html></html>", "http://eroticmv.com/") }.isFailure)
        assertTrue(runCatching { parseEroticMvMetadataFeed("<rss><channel/></rss>", "https://example.com/feed/") }.isFailure)
    }

    @Test fun feedDropsUnsafePermalinksAndKeepsSafeOnes() {
        val xml = """<rss version="2.0"><channel><title>Feed</title>
          <item><title>Good</title><link>https://eroticmv.com/good/</link><guid>https://eroticmv.com/?p=7</guid></item>
          <item><title>Bad</title><link>https://example.com/bad/</link></item>
        </channel></rss>"""
        val result = parseEroticMvMetadataFeed(xml, "https://eroticmv.com/feed/")
        assertEquals(listOf("Good"), result.items.map { it.title })
    }

}
