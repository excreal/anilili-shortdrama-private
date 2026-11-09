package com.example.animepahedl.metadata

import com.example.animepahedl.*

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.StringReader
import java.text.Normalizer
import java.util.Locale

private const val DRAMATIP_MAX_PAGE = 100

/** Dramatip's public HTML/JSON-LD/React Flight API. No telemetry or account mutations. */
internal class DramatipMetadataApi(
    cookieHeader: ((String) -> String)? = null,
    fetch: (suspend (String) -> MetadataResponse)? = null
) : SiteMetadataApi {
    override val site = MetadataSite.DRAMATIP
    private val http = MetadataHttp(setOf("dramatip.net", "www.dramatip.net"), cookieHeader)
    private val get: suspend (String) -> MetadataResponse = fetch ?: { http.get(it) }
    private val origin = "https://dramatip.net/".toHttpUrl()

    override suspend fun browse(query: MetadataBrowseQuery): MetadataPage {
        require(query.page in 1..DRAMATIP_MAX_PAGE) { "Page must be between 1 and $DRAMATIP_MAX_PAGE." }
        val unsupported = query.filters.keys - setOf("category", "source", "language")
        require(unsupported.isEmpty()) { "Unsupported Dramatip filters: ${unsupported.joinToString()}" }
        require(!(query.filters.containsKey("category") && query.filters.containsKey("source"))) { "Choose a category or a source, not both." }
        require(query.query.isBlank() || query.filters.keys.none { it in setOf("category", "source") }) { "Dramatip search cannot be combined with category/source filters." }
        require(query.url == null || query.query.isBlank() && query.filters.isEmpty() && query.page == 1) { "Use an explicit URL or query/filter/page parameters, not both." }
        val locale = query.filters["language"].orEmpty().ifBlank { "en" }
        require(locale.matches(Regex("[a-z]{2,3}(?:-[A-Za-z]{2})?"))) { "Invalid language." }
        val prefix = if (locale == "en") "" else "/$locale"
        fun slug(name: String): String? = query.filters[name]?.also {
            require(it.matches(Regex("[a-zA-Z0-9_-]+"))) { "Invalid $name filter." }
        }
        val homepage = query.query.isBlank() && query.filters.keys.none { it in setOf("category", "source") }
        val route = when {
            query.query.isNotBlank() -> "$prefix/search"
            slug("category") != null -> "$prefix/category/${slug("category")}"
            slug("source") != null -> "$prefix/source/${slug("source")}"
            else -> prefix.ifBlank { "/" }
        }
        var target = query.url?.let(http::checked) ?: origin.newBuilder().encodedPath(
            if (query.page > 1 && !homepage && query.query.isBlank()) "${route.trimEnd('/')}/page/${query.page}" else route
        ).apply {
            if (query.query.isNotBlank()) addQueryParameter("q", query.query.trim())
            if (query.page > 1 && query.query.isNotBlank()) addQueryParameter("page", query.page.toString())
        }.build()
        var response = get(target.toString())
        dramatipRequireNotFoundAbsent(response.document())
        if (homepage && query.page > 1 && query.url == null) {
            // The homepage can advertise a different collection than its first section's See all link.
            val visited = linkedSetOf(target.toString(), response.url)
            val firstPage = parseDramatipMetadataPage(response.text, response.url)
            val advertised = firstPage.nextUrl?.let(http::checked)
                ?: throw java.io.IOException("Dramatip did not advertise a homepage continuation.")
            if (!visited.add(advertised.toString())) throw java.io.IOException("Dramatip repeated a homepage pagination URL.")
            target = if (query.page == 2) advertised else {
                val path = advertised.encodedPath
                require(advertised.encodedQuery == null && advertised.encodedFragment == null &&
                    Regex("^/(?:[A-Za-z]{2,3}/)?(?:category|source)/[A-Za-z0-9_-]+/page/2/?$").matches(path)) {
                    "Dramatip's homepage continuation does not expose a safe page-number route."
                }
                val pageSegment = advertised.pathSegments.indexOfLast { it.isNotEmpty() }
                advertised.newBuilder().setPathSegment(pageSegment, query.page.toString()).build()
                    .also { derived ->
                        if (!visited.add(derived.toString())) throw java.io.IOException("Dramatip repeated a homepage pagination URL.")
                    }
            }
            response = get(target.toString())
            dramatipRequireNotFoundAbsent(response.document())
            val finalUrl = http.checked(response.url).toString()
            if (finalUrl != target.toString() && !visited.add(finalUrl)) {
                throw java.io.IOException("Dramatip repeated a homepage pagination URL.")
            }
        }
        return parseDramatipMetadataPage(response.text, response.url)
    }

    override suspend fun details(url: String): MetadataDetails {
        val target = http.checked(url)
        dramaRoute(target, SiteDownloadProvider.DRAMATIP)
        val response = get(target.toString())
        dramatipRequireNotFoundAbsent(response.document())
        return parseDramatipMetadataDetails(response.text, response.url)
    }

    override suspend fun episode(url: String): MetadataEpisode {
        val target = http.checked(url)
        val (_, requested) = dramaRoute(target, SiteDownloadProvider.DRAMATIP)
        require(requested != null) { "Choose an episode URL." }
        val response = get(target.toString())
        dramatipRequireNotFoundAbsent(response.document())
        val parsed = parseDramatipMetadataDetails(response.text, response.url)
        require(dramaRoute(http.checked(response.url), SiteDownloadProvider.DRAMATIP) == dramaRoute(target, SiteDownloadProvider.DRAMATIP)) {
            "Dramatip redirected to another episode."
        }
        val episode = parsed.episodes.firstOrNull { it.number == requested.toString() }
            ?: throw java.io.IOException("Dramatip did not advertise this episode.")
        val objects = dramatipFlightObjects(response.document())
        val player = objects.firstOrNull { it.has("sourceFirst") }
        val source = player?.get("sourceFirst")?.takeIf { it.isJsonObject }?.asJsonObject
        val descriptor = source?.let { listOf(metadataPayload("episode-source-descriptor", response.url, it)) }.orEmpty()
        val schema = metadataFromDocument(response.document())
        return episode.copy(
            title = schema.title.takeIf { schema.format == "TVEpisode" && it.isNotBlank() } ?: episode.title,
            description = schema.description.takeIf { schema.format == "TVEpisode" } ?: episode.description,
            images = (episode.images + schema.images.filter { schema.format == "TVEpisode" }).distinctBy { it.url to it.role },
            durationSeconds = schema.durationSeconds.takeIf { schema.format == "TVEpisode" } ?: episode.durationSeconds,
            releaseDate = schema.releaseDate.takeIf { schema.format == "TVEpisode" } ?: episode.releaseDate,
            subtitles = (episode.subtitles + dramatipSubtitleTracks(source, response.url)).distinctBy { it.url },
            payloads = episode.payloads + descriptor,
            attributes = episode.attributes + source?.dramaString("source").orEmpty().takeIf(String::isNotBlank)
                ?.let { mapOf("source" to it) }.orEmpty())
    }

    override suspend fun taxonomy(): MetadataTaxonomy {
        val categories = get("https://dramatip.net/categories").document()
        val sources = get("https://dramatip.net/sources").document()
        val groups = mapOf("categories" to dramatipTerms(categories, "/category/"),
            "sources" to dramatipTerms(sources, "/source/"), "languages" to dramatipLanguages(sources))
        val payload = JsonObject().apply { groups.forEach { (key, terms) ->
            add(key, com.google.gson.Gson().toJsonTree(terms))
        } }
        return MetadataTaxonomy(groups, listOf(metadataPayload("taxonomy", "https://dramatip.net/categories", payload)))
    }
}

/** Public parser entry points also permit inspecting saved responses without networking. */
internal fun parseDramatipMetadataPage(html: String, url: String): MetadataPage {
    val doc = org.jsoup.Jsoup.parse(html, url)
    dramatipRequireNotFoundAbsent(doc)
    val items = dramatipCards(doc)
    val sections = doc.select("section").mapNotNull { section ->
        val cards = dramatipCards(section)
        val name = section.selectFirst(":scope > h2, :scope > h3, :scope > header h2, :scope > header h3, :scope > div > h2, :scope > div > h3")
            ?.text().orEmpty().ifBlank { section.attr("aria-label") }
        if (cards.isEmpty() || name.isBlank()) null else MetadataSection(name, cards,
            section.select("a[href]").firstOrNull { it.text().contains("See all", true) }?.absUrl("href"))
    }
    val next = doc.select("a[href]").firstOrNull { anchor ->
        anchor.attr("rel").split(Regex("\\s+")).any { it.equals("next", true) } ||
            dramatipIsNextLabel(anchor.attr("aria-label")) || dramatipIsNextLabel(anchor.text())
    }?.absUrl("href")?.takeIf(::dramatipMetadataUrl)
    val pagination = JsonObject().apply {
        doc.select("a[aria-label][href]").filter {
            val label = it.attr("aria-label").trim().lowercase()
            label.contains("page") || label.contains("seite") || dramatipPaginationLabels.contains(label)
        }
            .forEach { addProperty(it.attr("aria-label"), it.absUrl("href")) }
    }
    return MetadataPage(items, next, sections = sections, filters = mapOf(
        "categories" to dramatipTerms(doc, "/category/"), "sources" to dramatipTerms(doc, "/source/"),
        "languages" to dramatipLanguages(doc)), payloads = listOf(metadataPayload("pagination", url, pagination)) +
        metadataFromDocument(doc).payloads)
}

internal fun parseDramatipMetadataDetails(html: String, url: String): MetadataDetails {
    val doc = org.jsoup.Jsoup.parse(html, url)
    dramatipRequireNotFoundAbsent(doc)
    val (slug, _) = dramaRoute(url.toHttpUrl(), SiteDownloadProvider.DRAMATIP)
    val objects = dramatipFlightObjects(doc)
    val player = objects.firstOrNull { it.has("sourceFirst") }
    val source = player?.get("sourceFirst")?.takeIf { it.isJsonObject }?.asJsonObject
    val base = metadataFromDocument(doc)
    val info = doc.selectFirst("#series-info")
    val attributes = linkedMapOf<String, String>()
    info?.select("dl > div")?.forEach { row ->
        val label = row.selectFirst("dt")?.text().orEmpty()
        val value = row.selectFirst("dd")?.text().orEmpty()
        if (label.isNotBlank() && value.isNotBlank()) attributes[label] = value
    }
    val voteDisplay = doc.select("div").firstOrNull { it.ownText().trim().matches(Regex("[0-9.,]+[KMB]?\\s+votes", RegexOption.IGNORE_CASE)) }?.ownText()
    voteDisplay?.let { attributes["votesDisplay"] = it }
    player?.dramaString("bookId")?.takeIf(String::isNotBlank)?.let { attributes["bookId"] = it }
    player?.dramaString("locale")?.takeIf(String::isNotBlank)?.let { attributes["locale"] = it }
    val rating = info?.select("span")?.firstOrNull { it.text() == "Rating" }?.parent()?.select("span")?.lastOrNull()?.text()?.toDoubleOrNull()
    val genreRow = info?.select("dl > div")?.firstOrNull { it.selectFirst("dt")?.text() == "Genres" }
    val genres = linkDramatipGenres(genreRow?.select("dd span")?.map { MetadataTerm(it.text()) }.orEmpty().ifEmpty { base.genres })
    val sourceLink = info?.selectFirst("a[href*=/source/]")
    val sourceName = sourceLink?.text().orEmpty().ifBlank { source?.dramaString("source").orEmpty() }
    val images = (base.images + listOfNotNull(
        player?.dramaString("poster")?.takeIf(String::isNotBlank)?.let { MetadataImage(it, "player-poster") },
        player?.dramaString("posterFallback")?.takeIf(String::isNotBlank)?.let { MetadataImage(it, "poster-fallback") }
    ) + info?.select("img")?.flatMap { dramatipImages(it, "poster") }.orEmpty()).distinctBy { it.url to it.role }
    val episodeObjects = objects.filter { it.has("chapter_id") && it.has("serial_number") }
    val episodes = episodeObjects.mapNotNull { obj ->
        val number = obj.dramaInt("serial_number").takeIf { it > 0 } ?: return@mapNotNull null
        val route = url.toHttpUrl().newBuilder().encodedPath(
            (if (url.toHttpUrl().pathSegments.firstOrNull() in setOf("series", null)) "" else "/${url.toHttpUrl().pathSegments.first()}") +
                "/series/$slug/episode-$number").query(null).fragment(null).build().toString()
        MetadataEpisode(obj.dramaString("chapter_id"), number.toString(), "Episode $number", route,
            images = listOfNotNull(obj.dramaString("cover").takeIf(String::isNotBlank)?.let { MetadataImage(it, "thumbnail") },
                obj.dramaString("cover_fallback").takeIf(String::isNotBlank)?.let { MetadataImage(it, "thumbnail-fallback") }),
            durationSeconds = obj.dramaString("duration").toLongOrNull()?.takeIf { it > 0 },
            attributes = obj.entrySet().filter { it.value.isJsonPrimitive }.associate { it.key to it.value.asString },
            payloads = listOf(metadataPayload("episode", url, obj)))
    }.distinctBy { it.number }.sortedBy { it.number.toIntOrNull() }
    val fallbackEpisodes = if (episodes.isEmpty()) runCatching { parseDramaSeries(html, url.toHttpUrl(), SiteDownloadProvider.DRAMATIP, slug).episodes.map { it.metadataEpisode() } }.getOrDefault(emptyList()) else episodes
    val payloads = listOfNotNull(player?.let { metadataPayload("player-props", url, it) })
    val links = base.links + sourceLink?.absUrl("href")?.takeIf(String::isNotBlank)?.let { listOf(MetadataLink(it, "source", sourceName)) }.orEmpty()
    val record = base.copy(id = player?.dramaString("bookId")?.takeIf(String::isNotBlank) ?: slug,
        title = doc.selectFirst("h1")?.text()?.takeIf(String::isNotBlank) ?: base.title,
        images = images, genres = genres,
        ratings = (base.ratings + listOfNotNull(rating?.let { MetadataRating(it, 10.0, source = "Dramatip", label = voteDisplay) })).distinctBy { it.value to it.source },
        studios = base.studios,
        episodeCount = base.episodeCount ?: attributes["Episodes"]?.toIntOrNull() ?: fallbackEpisodes.size.takeIf { it > 0 },
        attributes = base.attributes + attributes + sourceName.takeIf(String::isNotBlank)?.let { mapOf("distributionSource" to it) }.orEmpty(), links = links, payloads = base.payloads + payloads)
    val related = dramatipCards(doc).filter { it.url.toHttpUrl().encodedPath.trimEnd('/') != url.toHttpUrl().encodedPath.substringBefore("/episode-").trimEnd('/') }
    return MetadataDetails(record, fallbackEpisodes, related = related,
        sections = parseDramatipMetadataPage(html, url).sections, payloads = payloads)
}

/** Dramatip displays genres as plain text, while its browse pages expose them as /category/{slug}. */
private fun linkDramatipGenres(terms: List<MetadataTerm>): List<MetadataTerm> = terms.mapNotNull { term ->
    val name = term.name.trim()
    val slug = term.id?.takeIf { it.matches(Regex("[a-zA-Z0-9_-]+")) }
        ?: Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace("&", " ")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .takeIf { it.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*")) }
        ?: return@mapNotNull null
    val url = term.url?.takeIf { raw ->
        dramatipMetadataUrl(raw) && runCatching { raw.toHttpUrl().encodedPath.matches(Regex("^/(?:[a-z]{2}/)?category/[a-zA-Z0-9_-]+/?$")) }.getOrDefault(false)
    }
        ?: "https://dramatip.net/category/$slug"
    MetadataTerm(name, id = slug, url = url, count = term.count, attributes = term.attributes)
}.distinctBy { it.url }

private fun dramatipMetadataUrl(raw: String): Boolean = runCatching { raw.toHttpUrl().let {
    it.scheme == "https" && it.host in setOf("dramatip.net", "www.dramatip.net") && it.username.isEmpty() && it.password.isEmpty()
} }.getOrDefault(false)

private fun dramatipCards(root: Element): List<MetadataRecord> = root.select("a[href]").mapNotNull { anchor ->
    val target = anchor.absUrl("href")
    val parsed = runCatching { target.toHttpUrl() }.getOrNull() ?: return@mapNotNull null
    if (!dramatipMetadataUrl(target) || !Regex("^/(?:[a-z]{2,3}/)?series/[^/]+/?$").matches(parsed.encodedPath)) return@mapNotNull null
    val image = anchor.select("img").firstOrNull { it.attr("aria-hidden") != "true" && !it.attr("src").contains("/flags/") } ?: return@mapNotNull null
    val title = image.attr("alt").ifBlank { anchor.selectFirst("h2,h3,h4")?.text().orEmpty() }.trim()
    if (title.isEmpty()) return@mapNotNull null
    val text = anchor.text()
    val number = Regex("\\bEP\\s+(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
    val rating = anchor.select("span").firstOrNull { span -> span.select("svg").isNotEmpty() && span.ownText().trim().toDoubleOrNull() != null }?.ownText()?.trim()?.toDoubleOrNull()
    val languages = anchor.select("span").map { it.ownText().trim() }.filter { it in setOf("English", "Deutsch", "Español", "Русский", "Français", "العربية", "한국어", "ไทย", "Português", "Italiano", "日本語", "Bahasa Indonesia", "Tiếng Việt", "Filipino", "हिन्दी", "Bahasa Melayu") }.distinct()
    MetadataRecord(parsed.pathSegments.lastOrNull { it.isNotBlank() }.orEmpty(), target, title,
        images = dramatipImages(image, "poster"), episodeCount = number, languages = languages,
        ratings = listOfNotNull(rating?.let { MetadataRating(it, 10.0, source = "Dramatip") }),
        attributes = mapOf("cardText" to text) + image.attributes().associate { "image.${it.key}" to it.value })
}.distinctBy { it.url }

private fun dramatipImages(image: Element, role: String): List<MetadataImage> {
    val result = mutableListOf<MetadataImage>()
    for (attr in listOf("src", "data-src", "data-lazy-src", "data-original")) {
        val raw = image.attr(attr)
        if (raw.isNotBlank() && !raw.startsWith("data:")) runCatching {
            image.baseUri().toHttpUrl().resolve(raw)?.toString()?.let { MetadataImage(it, role, caption = image.attr("alt")) }
        }.getOrNull()?.let(result::add)
    }
    for (attr in listOf("srcset", "data-srcset")) image.attr(attr).split(Regex(",\\s*(?=https?://|/|\\.\\.?/)"))
        .forEach { candidate ->
            val parts = Regex("^(.+?)\\s+(\\d+)w$").matchEntire(candidate.trim()) ?: return@forEach
            image.baseUri().toHttpUrl().resolve(parts.groupValues[1])?.let {
                result += MetadataImage(it.toString(), role, width = parts.groupValues[2].toIntOrNull(), caption = image.attr("alt"))
            }
        }
    return result.distinctBy { it.url }
}

private fun dramatipTerms(doc: Document, prefix: String): List<MetadataTerm> = doc.select("a[href]").mapNotNull { anchor ->
    val raw = anchor.absUrl("href")
    val url = runCatching { raw.toHttpUrl() }.getOrNull() ?: return@mapNotNull null
    if (!dramatipMetadataUrl(raw) || !url.encodedPath.matches(Regex("^(?:/[a-z]{2,3})?${Regex.escape(prefix)}[^/]+/?$"))) return@mapNotNull null
    val semanticSpans = anchor.select("span").map { it.text().trim() }.filter {
        it.isNotBlank() && !it.matches(Regex("(?i)([A-Z]{1,3}|[0-9,]+\\s+titles|[0-9,]+)"))
    }
    val cleaned = anchor.clone().apply { select("img,svg,[aria-hidden=true]").remove() }.text()
        .replace(Regex("(?i)[0-9,]+\\s+titles?"), "").trim()
    val name = semanticSpans.lastOrNull() ?: cleaned
    if (name.isBlank()) return@mapNotNull null
    val count = Regex("([0-9,]+)\\s+titles").find(anchor.text())?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
    MetadataTerm(name, url.pathSegments.lastOrNull { it.isNotBlank() }, raw, count,
        anchor.selectFirst("img")?.absUrl("src")?.takeIf(String::isNotBlank)?.let { mapOf("icon" to it) }.orEmpty())
}.groupBy { it.url }.values.map { variants -> variants.firstOrNull { it.count != null } ?: variants.first() }

private fun dramatipLanguages(doc: Document): List<MetadataTerm> = doc.select("a[hreflang][href]").mapNotNull { anchor ->
    val raw = anchor.absUrl("href")
    if (!dramatipMetadataUrl(raw)) null else MetadataTerm(anchor.text().ifBlank { anchor.attr("hreflang") }, anchor.attr("hreflang"), raw)
}.distinctBy { it.id }

internal fun dramatipSubtitleTracks(source: JsonObject?, pageUrl: String): List<MetadataSubtitle> {
    val value = source?.get("subtitle") ?: return emptyList()
    val nodes = if (value.isJsonArray) value.asJsonArray.toList() else listOf(value)
    return nodes.mapNotNull { node ->
        if (!node.isJsonObject) return@mapNotNull null
        val obj = node.asJsonObject
        val encoded = obj.dramaString("enc", "url", "src", "file", "data")
        if (encoded.isBlank()) return@mapNotNull null
        // Subtitle URLs use the same AES-GCM wrapper as media sources. Keep already-public
        // URLs intact, but never let a decoded descriptor bypass the HTTPS/credential checks.
        val raw = if (runCatching { encoded.toHttpUrl() }.isSuccess || encoded.startsWith("/") ||
            encoded.startsWith("./") || encoded.startsWith("../")) encoded
        else runCatching { decryptDramaSource(encoded) }.getOrNull() ?: return@mapNotNull null
        val url = pageUrl.toHttpUrl().resolve(raw)?.takeIf {
            it.scheme == "https" && it.host.isNotBlank() && it.username.isEmpty() && it.password.isEmpty()
        } ?: return@mapNotNull null
        val language = obj.dramaString("lang", "language").takeIf(String::isNotBlank)
        val extension = url.encodedPath.substringAfterLast('.', "").lowercase().takeIf { it in setOf("vtt", "srt", "ass", "ssa", "ttml", "xml") }
        val declaredFormat = obj.dramaString("format", "type", "mimeType", "mime_type").lowercase()
            .substringAfterLast('/').substringBefore(';').takeIf { it in setOf("vtt", "srt", "ass", "ssa", "ttml", "xml") }
        val isDefault = listOf("default", "isDefault", "is_default").any { key ->
            obj.get(key)?.let { it.isJsonPrimitive && (it.asJsonPrimitive.isBoolean && it.asBoolean || it.asString.equals("true", true) || it.asString == "1") } == true
        }
        MetadataSubtitle(url.toString(), obj.dramaString("label").ifBlank { language ?: "Subtitles" }, language,
            declaredFormat ?: extension,
            isDefault,
            obj.entrySet().filter { it.value.isJsonPrimitive }.associate { it.key to it.value.asString })
    }
}

private val dramatipNextLabels = setOf("next", "weiter", "nächste", "naechste", "nächste seite", "naechste seite")
private val dramatipPaginationLabels = dramatipNextLabels + setOf("last", "previous", "prev", "zurück", "zurueck", "letzte", "vorherige", "vorherige seite", "letzte seite")
private fun dramatipIsNextLabel(raw: String): Boolean = raw.trim().lowercase()
    .removeSuffix(" page").removeSuffix(" seite") in dramatipNextLabels

internal fun dramatipRequireNotFoundAbsent(doc: Document) {
    val title = doc.title().trim()
    val heading = doc.selectFirst("h1")?.text().orEmpty().trim()
    val text = doc.body().text().take(20_000).lowercase()
    val titleSignals = listOf(title, heading).any { it.matches(Regex("(?i)(404|not found|page not found|seite nicht gefunden|nicht gefunden)(?:\\s*[-|:].*)?")) }
    val bodySignal = listOf("404 - page not found", "page not found", "seite wurde nicht gefunden", "seite nicht gefunden", "title not found", "series not found").any(text::contains)
    if (titleSignals || bodySignal || doc.selectFirst("#__next[data-not-found=true], [data-testid=not-found]") != null) {
        throw java.io.IOException("Dramatip page was not found.")
    }
}

/** Decode JSON string chunks before parsing Flight records; do not regex unescape nested content. */
private fun dramatipFlightObjects(doc: Document): List<JsonObject> {
    val chunks = doc.select("script").mapNotNull { script ->
        val data = script.data().trim()
        val prefix = "self.__next_f.push("
        if (!data.startsWith(prefix)) return@mapNotNull null
        val end = data.lastIndexOf(')')
        if (end <= prefix.length) return@mapNotNull null
        val array = runCatching { JsonParser.parseString(data.substring(prefix.length, end)).asJsonArray }.getOrNull() ?: return@mapNotNull null
        (if (array.size() > 1) array[1] else null)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    }.joinToString("")
    val result = mutableListOf<JsonObject>()
    fun visit(node: JsonElement) {
        when {
            node.isJsonObject -> { result += node.asJsonObject; node.asJsonObject.entrySet().forEach { visit(it.value) } }
            node.isJsonArray -> node.asJsonArray.forEach(::visit)
        }
    }
    chunks.lineSequence().forEach { line ->
        val match = Regex("^[0-9a-fA-F]+:(?=[{\\[])").find(line) ?: return@forEach
        runCatching { JsonParser.parseReader(JsonReader(StringReader(line.substring(match.range.last + 1)))) }.getOrNull()?.let(::visit)
    }
    return result
}
