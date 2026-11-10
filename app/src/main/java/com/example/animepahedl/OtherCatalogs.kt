package com.example.animepahedl

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Narto's public English catalogue; the edge origin serves the same site without its JS gate. */
internal class NartoCatalog(cookieHeader: ((String) -> String)? = null) : SiteCatalog {
    private val http = OtherCatalogHttp(setOf("narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com"), cookieHeader)
    private val origin = "https://edge.narto-drama.com/".toHttpUrl()
    override suspend fun home() = page(origin.newBuilder().addQueryParameter("lang", "en-US").build().toString())
    override suspend fun search(query: String) = page(origin.newBuilder().encodedPath("/search")
        .addQueryParameter("lang", "en-US").addQueryParameter("q", query.trim()).build().toString())
    override suspend fun page(url: String): CatalogPage = withContext(Dispatchers.IO) {
        val doc = http.get(url)
        val items = linkedMapOf<String, CatalogItem>()
        // The server embeds the exact rendered catalogue as schema.org ItemList data.
        fun visit(node: JsonElement) {
            when {
                node.isJsonArray -> node.asJsonArray.forEach(::visit)
                node.isJsonObject -> {
                    val obj = node.asJsonObject
                    if (obj.get("@type")?.takeIf { it.isJsonPrimitive }?.asString == "ListItem") {
                        val target = obj.get("url")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                        val title = obj.get("name")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                        val image = obj.get("image")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                        val parsed = runCatching { target.toHttpUrl() }.getOrNull()
                        if (parsed != null && http.accepts(parsed) && parsed.encodedPath.startsWith("/detail/watch/") && title.isNotBlank())
                            items.putIfAbsent(parsed.encodedPath, CatalogItem(title, parsed.toString(), image))
                    }
                    obj.entrySet().forEach { visit(it.value) }
                }
            }
        }
        doc.select("script[type=application/ld+json]").forEach { script ->
            runCatching { JsonParser.parseString(script.data()) }.getOrNull()?.let(::visit)
        }
        if (items.isEmpty()) doc.select("a[href*=/detail/watch/]").forEach { link ->
            val target = runCatching { link.absUrl("href").toHttpUrl() }.getOrNull() ?: return@forEach
            if (!http.accepts(target)) return@forEach
            val image = link.selectFirst("img")
            val title = image?.attr("alt").orEmpty().ifBlank { link.attr("aria-label").removePrefix("Open ") }
                .ifBlank { link.selectFirst("h2,h3,h4")?.text().orEmpty() }
            if (title.isNotBlank() && title != "Watch Now") items.putIfAbsent(target.encodedPath,
                CatalogItem(title, target.toString(), image?.let { catalogImage(it, doc.location()) }.orEmpty()))
        }
        val next = doc.select("a.pager-link, a[rel=next]").firstOrNull {
            it.text().trim().equals("Next", true) || it.attr("rel") == "next"
        }?.absUrl("href")?.takeIf { runCatching { http.accepts(it.toHttpUrl()) }.getOrDefault(false) }
        CatalogPage(items.values.toList(), next)
    }
}

private fun catalogImage(image: org.jsoup.nodes.Element, base: String): String {
    val raw = sequenceOf("data-src", "data-lazy-src", "src").map(image::attr)
        .firstOrNull { it.isNotBlank() && !it.startsWith("data:") }.orEmpty()
    return runCatching { base.toHttpUrl().resolve(raw)?.toString().orEmpty() }.getOrDefault("")
}

private class OtherCatalogHttp(private val hosts: Set<String>, private val cookieHeader: ((String) -> String)?) {
    private val session = CatalogSessionCookies()
    private val client = OkHttpClient.Builder().cookieJar(session)
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(35, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            if (!accepts(request.url)) throw IOException("The catalogue redirected outside its source.")
            val builder = request.newBuilder().removeHeader("Cookie")
            // OkHttp supplies only cookies whose domain/path/secure flags match this request.
            // Merge fresh imported cookies by name without dropping the session's other cookies.
            val merged = linkedMapOf<String, String>()
            fun merge(header: String) {
                header.split(';').forEach { part ->
                    val name = part.substringBefore('=').trim()
                    if (name.isNotBlank() && '=' in part) merged[name] = part.substringAfter('=').trim()
                }
            }
            merge(request.header("Cookie").orEmpty())
            cookieHeader?.invoke(request.url.toString())?.let(::merge)
            if (merged.isNotEmpty()) builder.header("Cookie", merged.entries.joinToString("; ") { "${it.key}=${it.value}" })
            chain.proceed(builder.build())
        }.build()
    fun accepts(url: HttpUrl) = url.scheme == "https" && url.host in hosts && url.username.isEmpty() && url.password.isEmpty()
    suspend fun get(raw: String): Document {
        val url = runCatching { raw.toHttpUrl() }.getOrElse { throw IOException("Invalid catalogue URL.") }
        if (!accepts(url)) throw IOException("The catalogue URL does not belong to this source.")
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString())
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin").build()
        return client.newCall(request).consumeCancellable { response ->
            if (!response.isSuccessful) throw IOException("${url.host} returned HTTP ${response.code}. Refresh its session cookies or retry later.")
            val bytes = response.body.byteStream().use { it.readNBytes(8 * 1024 * 1024 + 1) }
            if (bytes.size > 8 * 1024 * 1024) throw IOException("The catalogue exceeds the size limit.")
            val html = bytes.toString(Charsets.UTF_8)
            if (html.contains("Checking your browser", true) || html.contains("Just a moment...", true))
                throw IOException("${url.host} requires browser verification. Import the site's session cookies and retry.")
            Jsoup.parse(html, response.request.url.toString())
        }
    }
}

/** Process-local browser session, intentionally never written into diagnostics or persisted. */
internal class CatalogSessionCookies : CookieJar {
    private val cookies = mutableListOf<Cookie>()
    @Synchronized override fun saveFromResponse(url: HttpUrl, incoming: List<Cookie>) {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt <= now }
        incoming.forEach { cookie ->
            cookies.removeAll { it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path }
            if (cookie.expiresAt > now) cookies += cookie
        }
    }
    @Synchronized override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt <= now }
        return cookies.filter { it.matches(url) }
    }
}
