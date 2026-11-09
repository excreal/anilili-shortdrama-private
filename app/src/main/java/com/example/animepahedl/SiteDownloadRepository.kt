package com.example.animepahedl

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Provider HTTP is independent from AnimePahe's WebView cookie jar and Kwik defaults. */
internal class SiteDownloadRepository(
    private val client: OkHttpClient = OkHttpClient.Builder().cookieJar(CookieJar.NO_COOKIES)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS).build(),
    private val userAgent: String = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
    private val cookieHeader: ((String) -> String)? = null
) {
    private val providerSession = CatalogSessionCookies()
    private val kisskh = KisskhApi(client, cookieHeader)
    private data class Page(val text: String, val url: HttpUrl)

    suspend fun inspect(input: String, cookie: String = ""): SiteDownloadSeries = withContext(Dispatchers.IO) {
        val url = siteDownloadInput(input)
        val provider = siteDownloadProvider(url)
        when (provider) {
            SiteDownloadProvider.KISSKH -> kisskh.series(url.newBuilder().host("kisskh.do").build())
            SiteDownloadProvider.DIRECT -> {
                val title = url.pathSegments.lastOrNull()?.takeIf(String::isNotBlank) ?: "Direct video"
                SiteDownloadSeries(provider, "DIRECT:${url}", title, "", url.toString(), listOf(SiteDownloadEpisode(url.toString(), 1, title, url.toString())))
            }
            SiteDownloadProvider.EROTICMV -> {
                val initial = fetch(url, cookie = cookie)
                val (name, poster) = dramaMetadata(initial.text, initial.url)
                val title = cleanDramaTitle(name, provider).ifBlank { "Video" }
                val variants = eroticVariants(initial, cookie)
                SiteDownloadSeries(provider, "EROTICMV:${initial.url}", title, poster, initial.url.toString(),
                    listOf(SiteDownloadEpisode(initial.url.toString(), 1, title, initial.url.toString())), variantQualities(variants))
            }
            else -> {
                val (slug, _) = dramaRoute(url, provider)
                val target = when (provider) {
                    SiteDownloadProvider.DRAMATIP -> url.newBuilder().encodedPath("/series/$slug").query(null).build()
                    SiteDownloadProvider.KISS18 -> url.newBuilder().encodedPath("/title/$slug").query(null).build()
                    SiteDownloadProvider.NARTO -> url.newBuilder().encodedPath("/detail/watch/$slug").query(null).build()
                    else -> error("unreachable")
                }
                val page = if (provider == SiteDownloadProvider.NARTO) nartoPage(target, cookie) else fetch(target, cookie = cookie)
                parseDramaSeries(page.text, page.url, provider, slug)
            }
        }
    }

    /** Optional UI hint. Playback and downloads always resolve the selected episode again. */
    suspend fun discoverQualities(series: SiteDownloadSeries, episodeNumber: Int? = null, cookie: String = ""): List<String> = withContext(Dispatchers.IO) {
        if (!series.provider.isShortDrama) return@withContext series.qualities
        val episode = series.episodes.firstOrNull { it.number == episodeNumber } ?: series.episodes.firstOrNull() ?: return@withContext series.qualities
        variantQualities(episodeVariants(episode.url.toHttpUrl(), series.provider, cookie))
    }

    suspend fun resolve(locator: String, quality: String = "Best", cookie: String = ""): ResolvedMedia = withContext(Dispatchers.IO) {
        val input = siteDownloadInput(locator)
        val provider = siteDownloadProvider(input)
        val variants = episodeVariants(input, provider, cookie)
        val chosen = if (provider == SiteDownloadProvider.NARTO) chooseNartoVariant(variants, quality)
            else chooseDramaVariant(variants, quality)
        val headers = linkedMapOf("User-Agent" to userAgent)
        if (provider != SiteDownloadProvider.DIRECT) headers["Referer"] = input.toString()
        val mediaUrl = chosen.url.toHttpUrl()
        if (cookie.isNotBlank() && mediaUrl.scheme == input.scheme && mediaUrl.host == input.host && mediaUrl.port == input.port) headers["Cookie"] = cookie
        headers.putAll(chosen.headers)
        ResolvedMedia(chosen.url, chosen.quality, "und", headers, chosen.backupUrls, chosen.mimeType, chosen.subtitles)
    }

    suspend fun resolveChoices(locator: String, cookie: String = "", includeSubtitles: Boolean = true): List<ResolvedMedia> = withContext(Dispatchers.IO) {
        val input = siteDownloadInput(locator)
        val provider = siteDownloadProvider(input)
        episodeVariants(input, provider, cookie, includeSubtitles).distinctBy { it.url }.map { variant ->
            val headers = linkedMapOf("User-Agent" to userAgent)
            if (provider != SiteDownloadProvider.DIRECT) headers["Referer"] = input.toString()
            val mediaUrl = variant.url.toHttpUrl()
            if (cookie.isNotBlank() && mediaUrl.scheme == input.scheme && mediaUrl.host == input.host && mediaUrl.port == input.port) headers["Cookie"] = cookie
            headers.putAll(variant.headers)
            ResolvedMedia(variant.url, variant.quality, "und", headers, variant.backupUrls, variant.mimeType, variant.subtitles)
        }
    }

    private fun variantQualities(variants: List<DramaVariant>): List<String> = listOf("Best") + variants.map { it.quality }.distinct()
        .sortedByDescending { it.removeSuffix("p").toIntOrNull() ?: 0 }

    private suspend fun episodeVariants(url: HttpUrl, provider: SiteDownloadProvider, cookie: String,
        includeSubtitles: Boolean = true): List<DramaVariant> = when (provider) {
        SiteDownloadProvider.KISSKH -> kisskh.variants(url.newBuilder().host("kisskh.do").build(), includeSubtitles)
        SiteDownloadProvider.DIRECT -> listOf(DramaVariant(decodeEroticMedia(url.toString()), mimeType = if (isDramaHls(url)) "application/x-mpegURL" else if (url.encodedPath.endsWith(".webm", true) || url.queryParameter("mime_type") == "video/webm") "video/webm" else "video/mp4"))
        SiteDownloadProvider.DRAMATIP -> {
            val (slug, number) = dramaRoute(url, provider)
            val target = url.newBuilder().encodedPath("/series/$slug/episode-${number ?: 1}").query(null).build()
            val page = fetch(target, cookie = cookie)
            validateEpisodePage(target, page.url, provider)
            parseDramatipVariants(page.text)
        }
        SiteDownloadProvider.NARTO -> {
            val (_, number) = dramaRoute(url, provider)
            if (number == null) throw IOException("Select a Narto episode before downloading.")
            val page = nartoPage(url, cookie)
            validateEpisodePage(url, page.url, provider)
            parseNartoVariants(page.text, page.url, number)
        }
        SiteDownloadProvider.KISS18 -> kissVariants(url, cookie)
        SiteDownloadProvider.EROTICMV -> eroticVariants(fetch(url, cookie = cookie), cookie)
    }

    private suspend fun kissVariants(url: HttpUrl, cookie: String): List<DramaVariant> {
        val (slug, number) = dramaRoute(url, SiteDownloadProvider.KISS18)
        if (number == null) throw IOException("Select a Kiss18 episode before downloading.")
        val page = fetch(url, cookie = cookie)
        validateEpisodePage(url, page.url, SiteDownloadProvider.KISS18)
        parseKissPublicSource(page.text, page.url)?.let { source ->
            val target = source.toHttpUrl()
            if (isDramaHls(target) || isDramaFile(target)) return listOf(DramaVariant(source, mimeType = if (isDramaHls(target)) "application/x-mpegURL" else "video/mp4"))
        }
        val module = fetch(url.resolve("/catalogue-browse.mjs")!!, cookie = cookie, limit = 32 * 1024 * 1024)
        val episode = kissCatalogueEpisode(module.text, slug, number)
        val chunk = episode.dramaString("playbackChunk")
        require(chunk.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid Kiss18 playback catalogue." }
        val playback = JsonParser.parseString(fetch(url.resolve("/playback/$chunk.json")!!, cookie = cookie).text).asJsonObject.dramaObject("playback")
        val variants = episode.dramaArray("sources").filter { it.isJsonObject }.mapNotNull { candidate ->
            val id = candidate.asJsonObject.dramaString("id")
            if (id.isBlank()) null else runCatching { kissPlaybackVariant(playback.dramaObject(id), episode, id, url) }.getOrNull()
        }
        if (variants.isEmpty()) throw IOException("Kiss18 returned no supported media for this episode.")
        return variants
    }

    private suspend fun eroticVariants(initial: Page, cookie: String): List<DramaVariant> {
        var page = initial
        val seen = mutableSetOf<String>()
        repeat(3) {
            val raw = eroticVideoSource(page.text) ?: throw IOException("The player did not expose a supported video link.")
            val target = dramaAbsolute(page.url, decodeEroticMedia(raw)).toHttpUrl()
            if (isDramaHls(target) || isDramaFile(target)) return listOf(DramaVariant(target.toString(), mimeType = if (isDramaHls(target)) "application/x-mpegURL" else "video/mp4"))
            require(seen.add(target.toString())) { "Embedded player loop detected." }
            // An embedded host gets no provider cookies, including on redirect.
            page = fetch(target, referer = page.url.toString(), cookie = if (target.host == initial.url.host) cookie else "")
        }
        throw IOException("The embedded player is nested too deeply.")
    }

    private suspend fun nartoPage(url: HttpUrl, cookie: String): Page {
        fun english(target: HttpUrl) = target.newBuilder().removeAllQueryParameters("lang").addQueryParameter("lang", "en-US").fragment(null).build()
        val page = fetch(english(url), cookie = cookie, allowChallenge = true)
        if (!page.text.contains("Checking your browser", true)) return page
        if (url.host == "edge.narto-drama.com") throw IOException("Narto requires browser verification.")
        val edge = fetch(english(url.newBuilder().host("edge.narto-drama.com").build()), allowChallenge = true)
        if (edge.text.contains("Checking your browser", true)) throw IOException("Narto requires browser verification.")
        return edge
    }

    private fun validateEpisodePage(requested: HttpUrl, effective: HttpUrl, provider: SiteDownloadProvider) {
        require(siteDownloadProvider(effective) == provider && dramaRoute(effective, provider) == dramaRoute(requested, provider)) { "The provider redirected to a different episode. Refresh the series and retry." }
    }

    private suspend fun fetch(url: HttpUrl, referer: String? = null, cookie: String = "", limit: Int = 8 * 1024 * 1024, allowChallenge: Boolean = false): Page {
        require('\r' !in cookie && '\n' !in cookie) { "Invalid Cookie header." }
        // Re-evaluate imported cookies per request; explicit headers stay on their original origin.
        // Provider pages carry session context, so never let an HTTPS page redirect downgrade
        // that request (or imported cookies) to cleartext. Direct HTTP media bypasses fetch and
        // remains supported by the DIRECT path.
        if (url.scheme != "https") throw IOException("Provider pages must use HTTPS.")
        val scoped = client.newBuilder().followSslRedirects(false).cookieJar(providerSession).addNetworkInterceptor { chain ->
            val builder = chain.request().newBuilder()
            val target = chain.request().url
            if (cookieHeader != null) {
                builder.removeHeader("Cookie")
                val imported = cookieHeader.invoke(target.toString()).split(';').map(String::trim).filter { '=' in it }
                val names = imported.map { it.substringBefore('=') }.toSet()
                val received = providerSession.loadForRequest(target).filterNot { it.name in names }.map { "${it.name}=${it.value}" }
                (received + imported).joinToString("; ").takeIf(String::isNotBlank)?.let { builder.header("Cookie", it) }
            } else if (target.host != url.host || target.scheme != url.scheme || target.port != url.port) builder.removeHeader("Cookie")
            chain.proceed(builder.build())
        }.build()
        val request = Request.Builder().url(url).header("User-Agent", userAgent).header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json;q=0.8,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document").header("Sec-Fetch-Mode", "navigate").header("Sec-Fetch-Site", "same-origin")
        request.header("Referer", referer ?: url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString())
        if (cookie.isNotBlank()) request.header("Cookie", cookie)
        return scoped.newCall(request.build()).consumeCancellable { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} from ${response.request.url.host}. Try again or refresh provider cookies.")
            val body = response.body
            if (body.contentLength() > limit) throw IOException("Provider metadata exceeds the size limit.")
            val bytes = body.byteStream().use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(16384)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (output.size() + read > limit) throw IOException("Provider metadata exceeds the size limit.")
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            val text = bytes.toString(body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
            if (!allowChallenge && SessionChecks.isChallenge(text, response.header("cf-mitigated"))) throw IOException("${url.host} requires browser verification or fresh cookies.")
            Page(text, response.request.url)
        }
    }
}
