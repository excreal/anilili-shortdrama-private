package com.example.animepahedl

import com.example.animepahedl.metadata.*

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import okhttp3.HttpUrl.Companion.toHttpUrl

class SiteMetadataApiTest {
    @Test fun extractedMetadataFlowsIntoPersistableAppDetailModel() {
        val details = MetadataDetails(
            MetadataRecord("id", "https://sample.test/title", "Title", description = "Overview", year = 2024,
                genres = listOf(MetadataTerm("Drama")), ratings = listOf(MetadataRating(8.5, 10.0, 120, "Source")),
                people = listOf(MetadataPerson("Lee", "director"))),
            episodes = listOf(MetadataEpisode("ep1", "1", "Episode 1", "https://sample.test/ep1",
                description = "Episode summary", subtitles = listOf(MetadataSubtitle("https://sample.test/sub.vtt", "English", "en"))))
        ).toAppMetadata()
        assertEquals("Overview", details.description)
        assertEquals(2024, details.year)
        assertEquals("Drama", details.genres.single())
        assertEquals(8.5, details.ratings.single().value!!, 0.0)
        assertEquals("Lee", details.people.single().name)
        assertEquals("Episode summary", details.episodes.single().description)
        assertEquals("English · en", details.episodes.single().subtitles.single())
    }

    @Test fun schemaParsingKeepsPrimaryTitleSeparateFromRelatedTitle() {
        val html = """
            <html><head><link rel="canonical" href="https://sample.test/title/1">
            <script type="application/ld+json">{"@graph":[
              {"@type":"TVSeries","url":"https://sample.test/title/1","name":"Main","genre":["Mystery"],"numberOfEpisodes":12,"aggregateRating":{"ratingValue":"8.2","bestRating":"10","ratingCount":"450"}},
              {"@type":"Movie","url":"https://sample.test/title/2","name":"Related","genre":"Romance","aggregateRating":{"ratingValue":"3.1"}}
            ]}</script></head><body><h1>Main</h1></body></html>
        """.trimIndent()
        val result = metadataFromHtml(html, "https://sample.test/title/1")
        assertEquals("Main", result.title)
        assertEquals(listOf("Mystery"), result.genres.map { it.name })
        assertEquals(8.2, result.ratings.single().value!!, 0.0)
        assertEquals(450L, result.ratings.single().votes)
        assertEquals(12, result.episodeCount)
    }

    @Test fun publicPayloadRetainsUnknownFieldsButStripsCredentialFields() {
        val json = JsonParser.parseString("""{"newField":[1,{"label":"ok"}],"cookie":"secret","csrfToken":"private","password":"pw","id":"public"}""")
        val payload = metadataPayload("fixture", "https://sample.test/api?token=secret&id=5", json)
        assertTrue(payload.json.contains("newField"))
        assertTrue(payload.json.contains("public"))
        assertFalse(payload.json.contains("cookie"))
        assertFalse(payload.json.contains("csrfToken"))
        assertFalse(payload.json.contains("password"))
        assertFalse(payload.url.contains("token"))
    }

    @Test fun dramatipCardsAndPaginationAreMapped() {
        val html = """<html><body><main><h2>Popular</h2><section><a href="/series/demo"><img src="/poster.jpg" alt="Demo"></a></section></main><a rel="next" href="/category/popular/page/2">Next</a></body></html>"""
        val page = parseDramatipMetadataPage(html, "https://dramatip.net/category/popular")
        assertEquals("Demo", page.items.single().title)
        assertEquals("https://dramatip.net/poster.jpg", page.items.single().images.single().url)
        assertTrue(page.nextUrl!!.endsWith("/category/popular/page/2"))
    }

    @Test fun kiss18LiteralReaderAcceptsDataOnlyAndRejectsExecutableCode() {
        val type = Class.forName("com.example.animepahedl.metadata.Kiss18Literal")
        val constructor = type.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType).apply { isAccessible = true }
        val instance = constructor.newInstance("{title:cleanText('Kiss18'),count:417,tags:['Drama','Mystery',],}", 0)
        val parsed = type.getDeclaredMethod("read").apply { isAccessible = true }.invoke(instance) as JsonObject
        assertEquals("Kiss18", parsed.get("title").asString)
        assertEquals(417, parsed.get("count").asInt)
        val bad = constructor.newInstance("globalThis.alert(1)", 0)
        try { type.getDeclaredMethod("read").apply { isAccessible = true }.invoke(bad); fail("Executable JS must not be interpreted") }
        catch (_: java.lang.reflect.InvocationTargetException) { }
    }

    @Test fun kiss18FetchedModuleFixturesRetainDeclaredGenreMembershipAndEnabledRows() {
        // Sanitized data-only excerpts matching the public module shapes; no sessions or private data.
        fun literal(source: String): JsonObject {
            val type = Class.forName("com.example.animepahedl.metadata.Kiss18Literal")
            val constructor = type.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType).apply { isAccessible = true }
            return type.getDeclaredMethod("read").apply { isAccessible = true }
                .invoke(constructor.newInstance(source, 0)) as JsonObject
        }
        val campaign = literal("""{collections:[{slug:'mafia-romance',title:'Mafia Romance Short Dramas',members:[{titleId:'chartdrama:shortpeak:bound-by-betrayal',reason:'Declared campaign member'}]},{slug:'werewolf-romance',title:'Werewolf Romance Short Dramas',members:[{titleId:'chartdrama:hdrama:a-mates-curse',reason:'Declared campaign member'}]}]}""")
        assertEquals(listOf("mafia-romance", "werewolf-romance"), campaign.getAsJsonArray("collections").map { it.asJsonObject.get("slug").asString })
        assertEquals("chartdrama:shortpeak:bound-by-betrayal", campaign.getAsJsonArray("collections")[0].asJsonObject.getAsJsonArray("members")[0].asJsonObject.get("titleId").asString)
        val editorial = literal("""{sections:[{enabled:true,label:'Curated stories',titles:['public:id:one','public:id:two']},{enabled:false,label:'Hidden row',titles:['public:id:three']}]}""")
        val enabled = editorial.getAsJsonArray("sections").map { it.asJsonObject }.filter { it.get("enabled").asBoolean }
        assertEquals("Curated stories", enabled.single().get("label").asString)
        assertEquals(2, enabled.single().getAsJsonArray("titles").size())
    }

    @Test fun kiss18SearchNormalizationTreatsApostropheVariantsEqually() {
        val owner = Class.forName("com.example.animepahedl.metadata.Kiss18MetadataApiKt")
        val normalize = owner.getDeclaredMethod("normalized", String::class.java).apply { isAccessible = true }
        assertEquals(normalize.invoke(null, "The Don’s Bride"), normalize.invoke(null, "The Don's Bride"))
        assertEquals("the dons bride", normalize.invoke(null, "The Don’s Bride"))
    }

    @Test fun kiss18WebsiteSearchFlagsAndContinuationRetainCompleteFilters() {
        val supported = setOf("kind", "partial", "unavailable", "category", "tag", "genre")
        val url = "https://kiss18.vip/search?kind=all&partial=1&unavailable=1&category=Test%20media&tag=CEO%20%26%20Revenge&genre=mafia-romance".toHttpUrl()
        val filters = kiss18UrlFilterValues(url, supported)
        assertEquals("all", filters["kind"])
        assertEquals("1", filters["partial"])
        assertTrue(kiss18FlagEnabled(filters["partial"]))
        assertTrue(kiss18FlagEnabled(filters["unavailable"]))
        assertEquals("Test media", filters["category"])
        assertEquals("CEO & Revenge", filters["tag"])
        assertEquals("mafia-romance", filters["genre"])
        assertFalse(kiss18FlagEnabled("0"))
        assertFalse(kiss18FlagEnabled("false"))

        val next = kiss18ContinuationUrl("https://kiss18.vip/", 2, 48, "wolf’s bride", filters).toHttpUrl()
        assertEquals("all", next.queryParameter("kind"))
        assertEquals("1", next.queryParameter("partial"))
        assertEquals("1", next.queryParameter("unavailable"))
        assertEquals("Test media", next.queryParameter("category"))
        assertEquals("CEO & Revenge", next.queryParameter("tag"))
        assertEquals("mafia-romance", next.queryParameter("genre"))
        assertEquals("wolf’s bride", next.queryParameter("q"))
    }

    @Test fun kiss18AvailabilityMatchesWebsiteCompletenessAndSearchRules() {
        data class Fixture(val id: String, val listed: List<Int>, val playable: Set<Int>, val finalEpisode: Int? = null) {
            val status get() = kiss18Availability(listed, playable, finalEpisode)
        }
        val fixtures = listOf(
            Fixture("complete", listOf(1, 2, 3), setOf(1, 2, 3)),
            Fixture("partial", listOf(1, 2, 3), setOf(1, 2)),
            Fixture("episode-1-missing", listOf(1, 2, 3), setOf(2, 3)),
            Fixture("gap", listOf(1, 2, 3, 4), setOf(1, 3, 4)),
            Fixture("unavailable", listOf(1, 2), emptySet()),
            Fixture("verified-finale-gap", listOf(1, 2, 3), setOf(1, 2, 3), finalEpisode = 4),
        )
        fun result(includePartial: Boolean = false, includeUnavailable: Boolean = false, searching: Boolean = false) =
            fixtures.filter { kiss18AvailabilityMatches(it.status, includePartial, includeUnavailable, searching) }.map { it.id }.toSet()

        assertEquals(setOf("complete"), result())
        assertEquals(setOf("complete", "partial", "episode-1-missing", "gap", "verified-finale-gap"), result(includePartial = true))
        assertEquals(setOf("complete", "unavailable"), result(includeUnavailable = true))
        assertEquals(setOf("complete", "partial", "episode-1-missing", "gap", "verified-finale-gap", "unavailable"),
            result(includePartial = true, includeUnavailable = true))
        assertEquals(setOf("complete", "partial", "episode-1-missing", "gap", "verified-finale-gap"), result(searching = true))
        assertFalse(kiss18Availability(listOf(1, 2, 3), setOf(1, 2, 3), 4).readyToStart)
        assertFalse(kiss18Availability(listOf(1, 2, 3, 4), setOf(1, 3, 4), null).readyToStart)
    }

    @Test fun nartoSearchCardsPreserveTagsAndPublicAttributes() {
        val method = NartoMetadataApi::class.java.getDeclaredMethod("jsonCard", com.google.gson.JsonElement::class.java, String::class.java).apply { isAccessible = true }
        val record = method.invoke(NartoMetadataApi(), JsonParser.parseString("""{"book_id":"42","title":"Story","watch_url":"/detail/watch/story","description":"Summary","poster_url":"/cover.jpg","category_name":"Romance","tag_names":["CEO","Sweet"],"is_adult":false}"""), "https://edge.narto-drama.com/") as MetadataRecord
        assertEquals(setOf("CEO", "Sweet"), record.tags.map { it.name }.toSet())
        assertEquals("Romance", record.categories.single().name)
        assertEquals("false", record.attributes["is_adult"])
        assertTrue(record.payloads.single().json.contains("book_id"))
    }

    @Test fun persistedStringTermsAndLinkedTermObjectsBothDeserialize() {
        val gson = com.google.gson.Gson()
        val old = gson.fromJson("""{"genres":["Drama"],"categories":["Romance"],"tags":["Slow burn"]}""", SiteShowMetadata::class.java)
        assertEquals(listOf("Drama"), old.genres)
        assertEquals(listOf("Romance"), old.categories)
        assertEquals(listOf("Slow burn"), old.tags)
        val newer = gson.fromJson("""{"genres":["Drama"],"genreTerms":[{"name":"Drama","url":"https://eroticmv.com/category/genre/drama/"}]}""", SiteShowMetadata::class.java)
        assertEquals("Drama", newer.genres.single())
        assertEquals("https://eroticmv.com/category/genre/drama/", newer.genreTerms.single().url)
    }

    @Test fun nartoLinkedTaxonomyTermsAreClassifiedByTheirRealRoutes() {
        val groups = classifyNartoLinkedTerms(listOf(
            MetadataTerm("Romance", url = "https://edge.narto-drama.com/genre/romance/"),
            MetadataTerm("Trending", url = "https://edge.narto-drama.com/category/trending/"),
            MetadataTerm("CEO", url = "https://edge.narto-drama.com/tag/ceo/")
        ))
        assertEquals(listOf("Romance"), groups.genres.map { it.name })
        assertEquals(listOf("Trending"), groups.categories.map { it.name })
        assertEquals(listOf("CEO"), groups.tags.map { it.name })
    }

    @Test fun similarTitleRoutesStayOnProviderTaxonomyPaths() {
        assertTrue(isSafeMetadataFilterRoute(CatalogSource.DRAMATIP, "genre", "https://dramatip.net/category/hidden-identity"))
        assertTrue(isSafeMetadataFilterRoute(CatalogSource.DRAMATIP, "category", "https://dramatip.net/category/hidden-identity"))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.DRAMATIP, "category", "http://dramatip.net/category/action/"))
        assertFalse(isSafeMetadataFilterRoute(CatalogSource.DRAMATIP, "genre", "https://evil.test/category/action/"))
    }

    @Test fun durationParserHandlesClockAndIsoValues() {
        assertEquals(3723L, metadataDurationSeconds("PT1H2M3S"))
        assertEquals(3723L, metadataDurationSeconds("1:02:03"))
        assertNull(metadataDurationSeconds("unknown"))
    }
}
