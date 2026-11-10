package com.example.animepahedl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.io.IOException
import java.io.StringReader
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal enum class SiteDownloadProvider { DRAMATIP, KISS18, NARTO, EROTICMV, KISSKH, DIRECT }
internal val SiteDownloadProvider.isShortDrama get() = this in setOf(SiteDownloadProvider.DRAMATIP, SiteDownloadProvider.KISS18, SiteDownloadProvider.NARTO)
/** Long-form and movie media use the original Anilili player; short dramas keep their dedicated player. */
internal val SiteDownloadProvider.usesReferencePlayer get() = !isShortDrama
internal val SiteDownloadProvider.contentLabel get() = if (isShortDrama) "Short drama" else "Video"
internal data class SiteDownloadEpisode(val id: String, val number: Int, val title: String, val url: String, val numberLabel: String? = null)
internal val SiteDownloadEpisode.displayNumber get() = numberLabel ?: number.toString()
internal data class SiteShowImage(val url: String, val role: String, val width: Int? = null, val height: Int? = null, val caption: String? = null)
internal data class SiteShowTerm(val name: String, val id: String? = null, val url: String? = null)
internal data class SiteShowRating(val value: Double? = null, val scale: Double? = null, val votes: Long? = null, val source: String? = null, val label: String? = null)
internal data class SiteShowPerson(val name: String, val role: String? = null, val url: String? = null)
internal data class SiteEpisodeMetadata(val number: String, val title: String, val description: String? = null,
    val durationSeconds: Long? = null, val releaseDate: String? = null, val subtitles: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap())
internal data class SiteShowMetadata(val description: String? = null, val alternativeTitles: List<String> = emptyList(),
    val images: List<SiteShowImage> = emptyList(), val genres: List<String> = emptyList(), val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(), val genreTerms: List<SiteShowTerm> = emptyList(),
    val categoryTerms: List<SiteShowTerm> = emptyList(), val tagTerms: List<SiteShowTerm> = emptyList(),
    val ratings: List<SiteShowRating> = emptyList(), val people: List<SiteShowPerson> = emptyList(),
    val countries: List<String> = emptyList(), val languages: List<String> = emptyList(), val studios: List<String> = emptyList(),
    val status: String? = null, val format: String? = null, val year: Int? = null, val episodeCount: Int? = null,
    val durationSeconds: Long? = null, val releaseDate: String? = null, val updatedAt: String? = null,
    val externalIds: Map<String, String> = emptyMap(), val attributes: Map<String, String> = emptyMap(),
    val episodes: List<SiteEpisodeMetadata> = emptyList())
internal data class SiteDownloadSeries(val provider: SiteDownloadProvider, val id: String, val title: String,
    val poster: String, val url: String, val episodes: List<SiteDownloadEpisode>, val qualities: List<String> = listOf("Best"),
    val description: String = "", val metadata: SiteShowMetadata? = null)
internal fun SiteDownloadSeries.overviewText(): String = metadata?.description?.takeIf(String::isNotBlank) ?: description
internal fun SiteDownloadSeries.posterThumbnail(): String = poster

/** A stale provider URL may be refreshed once for each selected episode, never in a retry loop. */
internal class MediaLinkRefreshGuard {
    private val refreshed = mutableSetOf<String>()
    fun shouldRefresh(provider: SiteDownloadProvider, episodeId: String): Boolean =
        provider != SiteDownloadProvider.DIRECT && refreshed.add("${provider.name}:$episodeId")
}
internal fun SiteDownloadSeries.matchesInspection(provider: SiteDownloadProvider, id: String, expectedGeneration: Int, generation: Int): Boolean =
    expectedGeneration == generation && this.provider == provider && this.id == id
internal fun mergeDiscoveredQualities(current: SiteDownloadSeries, expectedProvider: SiteDownloadProvider, expectedId: String,
    expectedGeneration: Int, generation: Int, qualities: List<String>): SiteDownloadSeries =
    if (current.matchesInspection(expectedProvider, expectedId, expectedGeneration, generation)) current.copy(qualities = qualities) else current
internal fun mergeInspectionMetadata(current: SiteDownloadSeries, expectedProvider: SiteDownloadProvider, expectedId: String,
    expectedGeneration: Int, generation: Int, metadata: SiteShowMetadata): SiteDownloadSeries =
    if (current.matchesInspection(expectedProvider, expectedId, expectedGeneration, generation)) current.copy(metadata = metadata) else current
internal val SiteDownloadSeries.isSingleVideo get() = provider == SiteDownloadProvider.DIRECT
internal data class DramaVariant(val url: String, val quality: String = "Original", val backupUrls: List<String> = emptyList(),
    val mimeType: String = "video/mp4", val headers: Map<String, String> = emptyMap(), val width: Int = 0, val height: Int = 0, val subtitles: List<MediaSubtitle> = emptyList())

internal fun dramaHost(host: String, domain: String) = host == domain || host.endsWith(".$domain")
internal fun siteDownloadInput(raw: String): HttpUrl {
    val url = runCatching { raw.trim().toHttpUrl() }.getOrElse { throw IOException("Enter a supported HTTP(S) video or series link.") }
    require(url.username.isEmpty() && url.password.isEmpty()) { "Links must not contain credentials." }
    return url
}
internal fun siteDownloadProvider(url: HttpUrl): SiteDownloadProvider = when {
    dramaHost(url.host, "tiktok.com") -> throw IOException("TikTok is not supported by this downloader.")
    dramaHost(url.host, "dramatip.net") -> SiteDownloadProvider.DRAMATIP
    url.host in setOf("kiss18.vip", "www.kiss18.vip") -> SiteDownloadProvider.KISS18
    url.host in setOf("narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com") -> SiteDownloadProvider.NARTO
    dramaHost(url.host, "eroticmv.com") -> throw IOException("Movie sources aren't included in this edition.")
    url.host in setOf("kisskh.do", "www.kisskh.do") -> throw IOException("K-drama sources aren't included in this edition.")
    isDramaHls(url) || isDramaFile(url) -> SiteDownloadProvider.DIRECT
    else -> throw IOException("Unsupported link. Use Dramatip, Kiss18, Narto Drama, or a direct HLS/MP4/WebM URL.")
}
internal fun isDramaHls(url: HttpUrl) = url.encodedPath.contains(".m3u8", true)
internal fun isDramaFile(url: HttpUrl) = listOf(".mp4", ".m4v", ".webm").any { url.encodedPath.endsWith(it, true) } ||
    url.queryParameter("mime_type") in setOf("video_mp4", "video/mp4", "video/webm")
internal fun dramaAbsolute(base: HttpUrl, raw: String): String = base.resolve(raw.trim())?.takeIf { it.username.isEmpty() && it.password.isEmpty() }?.toString()
    ?: throw IOException("The provider returned an invalid media link.")
internal fun dramaRoute(url: HttpUrl, provider: SiteDownloadProvider): Pair<String, Int?> {
    val path = url.pathSegments.filter(String::isNotEmpty)
    val offset = when (provider) {
        SiteDownloadProvider.KISS18 -> { require(path.firstOrNull() in setOf("title", "watch")); 1 }
        SiteDownloadProvider.NARTO -> { require(path.take(2) == listOf("detail", "watch")); 2 }
        SiteDownloadProvider.DRAMATIP -> { path.indexOf("series").also { require(it in 0..1) } + 1 }
        else -> throw IOException("This provider does not use episode routes.")
    }
    require(path.size in (offset + 1)..(offset + 2)) { "Expected a series or episode link." }
    val slug = path[offset]
    require(slug.isNotBlank())
    val number = path.getOrNull(offset + 1)?.let { it.removePrefix(if (provider == SiteDownloadProvider.DRAMATIP) "episode-" else "").toIntOrNull()?.takeIf { n -> n in 1..10000 }
        ?: throw IOException("Invalid episode number.") }
    return slug to number
}
internal fun parseSiteDownloadSelection(input: String, episodes: List<SiteDownloadEpisode>): List<SiteDownloadEpisode> {
    val selection = input.trim()
    if (selection.isBlank()) return episodes
    if (selection.equals("none", true)) return emptyList()
    require(!selection.trimStart().startsWith(',') && !selection.trimEnd().endsWith(',') && !Regex(",\\s*,").containsMatchIn(selection)) {
        "Episode selections cannot contain empty items."
    }
    if (episodes.any { it.numberLabel != null }) {
        val labels = episodes.associateWith { it.displayNumber.toBigDecimal() }
        val selected = linkedSetOf<SiteDownloadEpisode>()
        selection.replace(Regex("\\s*-\\s*"), "-").split(Regex("[,\\s]+")).forEach { token ->
            val range = token.split('-').map { it.toBigDecimalOrNull() ?: throw IOException("Use episode numbers or ranges, such as 1-5,9.") }
            require(range.size in 1..2 && range.first() <= range.last()) { "Invalid episode selection." }
            require(labels.values.any { it.compareTo(range.first()) == 0 } && labels.values.any { it.compareTo(range.last()) == 0 }) { "This episode is unavailable." }
            selected += episodes.filter { labels.getValue(it) >= range.first() && labels.getValue(it) <= range.last() }
        }
        return episodes.filter { it in selected }
    }
    val available = episodes.map { it.number }.toSet()
    val numbers = mutableSetOf<Int>()
    for (token in selection.replace(Regex("\\s*-\\s*"), "-").split(Regex("[,\\s]+"))) {
        val parts = token.trim().split('-').map { it.trim().toIntOrNull() ?: throw IOException("Use episode numbers or ranges, such as 1-5,9.") }
        require(parts.size in 1..2 && parts.all { it in 1..10000 }) { "Invalid episode selection." }
        val first = parts.first(); val last = parts.last()
        require(first <= last) { "Episode ranges must be ascending." }
        for (number in first..last) {
            require(number in available) { "Episode $number is unavailable." }
            numbers += number
        }
    }
    return episodes.filter { it.number in numbers }
}

internal fun dramaMetadata(html: String, url: HttpUrl): Pair<String, String> {
    val doc = Jsoup.parse(html)
    val title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty().ifBlank { doc.title().trim() }
    val image = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
    return title to if (image.isBlank()) "" else runCatching { dramaAbsolute(url, image) }.getOrDefault("")
}
internal fun cleanDramaTitle(raw: String, provider: SiteDownloadProvider): String = when (provider) {
    SiteDownloadProvider.DRAMATIP -> raw.replace(Regex("(?i)(\\s+-\\s+watch (?:all\\s+\\d+\\s+episodes free|free online))?\\s*\\|\\s*dramatip\\s*$"), "").replace(Regex("(?i)\\s+(?:episode\\s+|E)\\d+$"), "")
    SiteDownloadProvider.KISS18 -> raw.removeSuffix(" | Kiss18").removeSuffix(" — Kiss18").removeSuffix(" — Watch Episodes").removeSuffix(" · Story & Episodes")
    SiteDownloadProvider.NARTO -> raw.removeSuffix(" - Free Streaming").replace(Regex(" Episode \\d+$"), "")
    else -> raw
}.trim()
internal fun parseDramaSeries(html: String, url: HttpUrl, provider: SiteDownloadProvider, slug: String): SiteDownloadSeries {
    val (rawTitle, poster) = dramaMetadata(html, url)
    val title = cleanDramaTitle(rawTitle, provider).ifBlank { slug }
    val episodes = sortedMapOf<Int, SiteDownloadEpisode>()
    val base = if (provider == SiteDownloadProvider.DRAMATIP) url.newBuilder().encodedPath(url.encodedPath.trimEnd('/') + "/").build() else url
    val selector = if (provider == SiteDownloadProvider.NARTO) "a.episode-item[href]" else "a[href]"
    Jsoup.parse(html).select(selector).forEach { a ->
        val target = base.resolve(a.attr("href")) ?: return@forEach
        if (target.host != url.host) return@forEach
        val route = runCatching { dramaRoute(target, provider) }.getOrNull() ?: return@forEach
        val number = route.second ?: return@forEach
        if (route.first == slug) episodes[number] = SiteDownloadEpisode(target.toString(), number, "$title · Episode $number", target.toString())
    }
    if (provider == SiteDownloadProvider.DRAMATIP) {
        val plain = unescapeDramaFlight(html)
        val numbers = Regex("\"serial_number\"\\s*:\\s*(\\d+)").findAll(plain).mapNotNull { it.groupValues[1].toIntOrNull()?.takeIf { n -> n in 1..10000 } }.toList()
        val count = if (numbers.isEmpty() && episodes.isEmpty()) Regex("\"epCount\"\\s*:\\s*(\\d+)").find(plain)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 10000) ?: 0 else 0
        (numbers + (1..count)).forEach { n ->
            val target = url.newBuilder().encodedPath("/series/$slug/episode-$n").query(null).build().toString()
            episodes.putIfAbsent(n, SiteDownloadEpisode(target, n, "$title · Episode $n", target))
        }
    }
    if (episodes.isEmpty()) throw IOException("The provider returned no available episodes.")
    return SiteDownloadSeries(provider, "${provider.name}:$slug", title, poster, url.toString(), episodes.values.toList(), description = Jsoup.parse(html).selectFirst("meta[property=og:description],meta[name=description]")?.attr("content").orEmpty())
}
internal fun dramaJsonAfter(text: String, marker: Regex): JsonElement? {
    val match = marker.find(text) ?: return null
    return runCatching { JsonParser.parseReader(JsonReader(StringReader(text.substring(match.range.last + 1)))) }.getOrNull()
}
internal fun JsonObject.dramaString(vararg names: String): String = names.firstNotNullOfOrNull { name ->
    get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
}.orEmpty()
internal fun JsonObject.dramaInt(vararg names: String): Int = dramaString(*names).toIntOrNull() ?: 0
internal fun JsonObject.dramaObject(name: String): JsonObject = get(name)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
internal fun JsonObject.dramaArray(name: String): List<JsonElement> = get(name)?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty()
internal fun unescapeDramaFlight(raw: String) = raw.replace("\\\"", "\"").replace("\\\\", "\\")

internal fun decryptDramaSource(payload: String): String {
    val data = Base64.getDecoder().decode(payload)
    require(data.size >= 28) { "Invalid encrypted provider source." }
    val key = Base64.getDecoder().decode("QC6Ir2trghxRAyyyWZEOEFR4GgLhnfQ4A19I3QBlQkc=")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, data.copyOfRange(0, 12)))
    return cipher.doFinal(data.copyOfRange(12, data.size)).toString(Charsets.UTF_8).trim().toHttpUrl().toString()
}
internal fun dramaProxy(url: String): String = if (url.startsWith("https://cdn.dramadizilerim.xyz/") || url.startsWith("https://hls-proxy-i4ic7.b-cdn.net/")) url
    else "https://cdn.dramadizilerim.xyz/" + url.replaceFirst("://", "/")
internal fun dramaProxyBackup(url: String): String? {
    val proxy = url.toHttpUrl()
    if (proxy.host != "cdn.dramadizilerim.xyz") return null
    val path = proxy.encodedPath.removePrefix("/")
    val scheme = path.substringBefore('/')
    if (scheme !in setOf("https", "http")) return null
    val original = "$scheme://${path.substringAfter('/')}" + (proxy.encodedQuery?.let { "?$it" } ?: "")
    return "https://hls-proxy-i4ic7.b-cdn.net/".toHttpUrl().newBuilder().addQueryParameter("url", original).build().toString()
}
internal fun parseDramatipVariants(page: String): List<DramaVariant> {
    val plain = unescapeDramaFlight(page)
    val source = dramaJsonAfter(plain, Regex("\"sourceFirst\"\\s*:")) ?: run {
        require(!plain.contains("nextSourceFirst") && !plain.contains("prevSourceFirst")) { "Current episode source is missing." }
        val chain = dramaJsonAfter(plain, Regex("\"chain\"\\s*:")) ?: throw IOException("No supported provider source found.")
        JsonObject().also { it.add("chain", chain) }
    }
    val result = mutableListOf<DramaVariant>()
    source.asJsonObject.dramaArray("chain").filter { it.isJsonObject }.forEach { value ->
        val entry = value.asJsonObject
        val candidates = listOf("" to entry.dramaString("enc")) + entry.dramaObject("encByQuality").entrySet().map { it.key to it.value.asString }
        candidates.forEach candidate@ { (quality, encrypted) ->
            val payload = if (encrypted.startsWith('$')) {
                val ref = Regex("(?:^|[^0-9A-Za-z]|\\\\n)${Regex.escape(encrypted.drop(1))}:T([0-9a-fA-F]+),").find(page)
                ref?.let { page.substring(it.range.last + 1).take(it.groupValues[1].toInt(16)) }
            } else encrypted
            if (payload.isNullOrBlank()) return@candidate
            var url = runCatching { decryptDramaSource(payload) }.getOrNull() ?: return@candidate
            val type = entry.dramaString("type")
            val proxy = type in setOf("dramawave", "shorttv")
            if (proxy) url = dramaProxy(url)
            val isHls = isDramaHls(url.toHttpUrl()) || (type == "goodshort" && url.toHttpUrl().encodedPath.startsWith("/hls/")) || type == "hls"
            if (!isHls && !isDramaFile(url.toHttpUrl()) && type != "mp4") return@candidate
            result += DramaVariant(url, quality.trimEnd('p').toIntOrNull()?.let { "${it}p" } ?: "Original",
                listOfNotNull(dramaProxyBackup(url)), if (isHls) "application/x-mpegURL" else "video/mp4",
                if (proxy) mapOf("Origin" to "https://dramatip.net") else emptyMap())
        }
    }
    if (result.isEmpty()) throw IOException("No supported current-episode HLS or MP4 source found.")
    return result.distinctBy { it.url }
}

internal fun parseNartoVariants(body: String, url: HttpUrl, number: Int): List<DramaVariant> {
    val items = dramaJsonAfter(body, Regex("const\\s+episodeItemsRaw\\s*=\\s*"))?.takeIf { it.isJsonArray }?.asJsonArray
        ?: throw IOException("Narto episode metadata is missing.")
    val episode = items.firstOrNull { it.isJsonObject && it.asJsonObject.dramaInt("route_episode_number") == number }?.asJsonObject
        ?: throw IOException("Requested Narto episode is unavailable.")
    return listOf("direct_play_url", "play_url", "schema_content_url").mapNotNull { key ->
        val raw = episode.dramaString(key).takeIf(String::isNotBlank) ?: return@mapNotNull null
        val media = dramaAbsolute(url, raw)
        val target = media.toHttpUrl()
        if (target.scheme != "https") return@mapNotNull null
        val hls = isDramaHls(target) || (key == "play_url" && episode.dramaString("browser_prefetch_mode") == "hls") ||
            (key == "direct_play_url" && episode.get("direct_play_is_hls")?.toString() == "true")
        // Narto can publish playable provider URLs as opaque paths with browser_prefetch_mode=none.
        // That mode only disables neighbor prefetch; the site's player still treats play_url as a
        // native video. Preserve that behavior and infer MP4 unless HLS is explicitly advertised.
        val mime = when {
            hls -> "application/x-mpegURL"
            target.encodedPath.endsWith(".webm", true) || target.queryParameter("mime_type") == "video/webm" -> "video/webm"
            else -> "video/mp4"
        }
        DramaVariant(media, mimeType = mime)
    }.distinctBy { it.url }.also { if (it.isEmpty()) throw IOException("Narto returned no supported media source.") }
}
internal fun kissVideoObject(value: JsonElement): Sequence<JsonObject> = sequence {
    when {
        value.isJsonArray -> value.asJsonArray.forEach { yieldAll(kissVideoObject(it)) }
        value.isJsonObject -> {
            val obj = value.asJsonObject
            if (obj.dramaString("@type") == "VideoObject") yield(obj)
            obj.get("@graph")?.let { yieldAll(kissVideoObject(it)) }
        }
    }
}
internal fun parseKissPublicSource(page: String, url: HttpUrl): String? {
    for (script in Jsoup.parse(page).select("script[type=application/ld+json]")) {
        val json = runCatching { JsonParser.parseString(script.data()) }.getOrNull() ?: continue
        for (video in kissVideoObject(json)) {
            val route = video.dramaString("url")
            if (route.isNotBlank() && runCatching { dramaRoute(url.resolve(route)!!, SiteDownloadProvider.KISS18) }.getOrNull() != dramaRoute(url, SiteDownloadProvider.KISS18)) continue
            val source = video.dramaString("contentUrl")
            if (source.isNotBlank()) return dramaAbsolute(url, source)
        }
    }
    return null
}
internal fun kissCatalogueEpisode(module: String, slug: String, number: Int): JsonObject {
    val catalogue = dramaJsonAfter(module, Regex("export\\s+const\\s+snapshot\\s*=\\s*"))?.asJsonObject ?: throw IOException("Kiss18 catalogue is missing.")
    val story = catalogue.dramaArray("items").filter { it.isJsonObject }.map { it.asJsonObject }.firstOrNull {
        it.dramaString("public_slug") == slug || it.dramaArray("public_slug_aliases").any { alias -> alias.asString == slug }
    } ?: throw IOException("Kiss18 story is unavailable.")
    return story.dramaArray("episodes").filter { it.isJsonObject }.map { it.asJsonObject }.firstOrNull {
        it.dramaInt("ordinal") == number && it.get("playable")?.toString() == "true"
    } ?: throw IOException("This Kiss18 episode is unavailable.")
}
internal fun kissPlaybackVariant(entry: JsonObject, episode: JsonObject, candidate: String, url: HttpUrl): DramaVariant {
    require(entry.dramaString("episodeId") == episode.dramaString("id") && entry.dramaString("candidateId") == candidate) { "Kiss18 playback identity mismatch." }
    val format = entry.dramaString("format")
    require(format in setOf("hls", "mp4")) { "Unsupported Kiss18 playback format." }
    val resolver = entry.dramaObject("resolver")
    val source = when {
        entry.get("refreshable")?.toString() == "true" && resolver.dramaString("kind") == "hdrama" -> {
            require(resolver.dramaString("slug").isNotBlank() && resolver.dramaInt("ep") > 0)
            url.resolve("/api/resolve")!!.newBuilder().addQueryParameter("slug", resolver.dramaString("slug"))
                .addQueryParameter("ep", resolver.dramaInt("ep").toString()).addQueryParameter("proxy", "1").build().toString()
        }
        entry.get("gateway")?.toString() == "true" -> url.resolve("/api/media")!!.newBuilder().addQueryParameter("candidate", candidate).build().toString()
        else -> dramaAbsolute(url, entry.dramaString("url").also { require(it.isNotBlank()) })
    }
    return DramaVariant(source, mimeType = if (format == "hls") "application/x-mpegURL" else "video/mp4")
}
internal fun chooseDramaVariant(variants: List<DramaVariant>, quality: String): DramaVariant {
    return chooseDramaVariant(variants, quality, preferHlsOnTie = true)
}

internal fun chooseNartoVariant(variants: List<DramaVariant>, quality: String): DramaVariant =
    chooseDramaVariant(variants, quality, preferHlsOnTie = false)

private fun chooseDramaVariant(variants: List<DramaVariant>, quality: String, preferHlsOnTie: Boolean): DramaVariant {
    require(variants.isNotEmpty()) { "No supported media variants found." }
    val target = quality.removeSuffix("p").toIntOrNull()
    if (quality !in setOf("Best", "Original") && target == null) throw IOException("Invalid quality selection.")
    val candidates = if (target == null) variants else variants.filter { dramaHeight(it) <= target }
    if (candidates.isEmpty()) return variants.minByOrNull { dramaHeight(it) }!!
    return candidates.maxWithOrNull(compareBy<DramaVariant> { dramaHeight(it) }
        .thenBy { if ((it.mimeType == "application/x-mpegURL") == preferHlsOnTie) 1 else 0 })!!
}
private fun dramaHeight(variant: DramaVariant): Int = variant.quality.removeSuffix("p").toIntOrNull() ?: minOf(variant.width, variant.height).coerceAtLeast(0)

