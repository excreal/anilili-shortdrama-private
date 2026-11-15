package com.example.animepahedl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Server-rendered Huangguo library and search results. */
internal class HuangguoCatalog : SiteCatalog {
    private val host = "huangguodrama.ai"
    private val origin = "https://$host".toHttpUrl()
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS).followRedirects(true).build()

    override suspend fun home() = page("$origin/en/library/")
    override suspend fun search(query: String): CatalogPage {
        val url = origin.newBuilder().encodedPath("/en/search/").addQueryParameter("keyword", query.trim()).build()
        return page(url.toString())
    }

    override suspend fun page(url: String): CatalogPage = withContext(Dispatchers.IO) {
        val target = runCatching { url.toHttpUrl() }.getOrElse { throw IOException("Invalid Huangguo catalogue URL.") }
        if (!accepts(target)) throw IOException("The catalogue URL does not belong to Huangguo Drama.")
        val request = Request.Builder().url(target).header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml").header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "$origin/en/").build()
        client.newCall(request).consumeCancellable { response ->
            if (!response.isSuccessful) throw IOException("Huangguo Drama returned HTTP ${response.code}.")
            val bytes = response.body.byteStream().use { it.readNBytes(8 * 1024 * 1024 + 1) }
            if (bytes.size > 8 * 1024 * 1024) throw IOException("The Huangguo catalogue exceeds the size limit.")
            val doc = Jsoup.parse(bytes.toString(Charsets.UTF_8), response.request.url.toString())
            val items = linkedMapOf<String, CatalogItem>()
            doc.select("a.hg-poster__art[href], a[data-feed-kind][href]").forEach { link ->
                val detail = link.parent()?.selectFirst("a[href*=/detail/]")
                    ?: link.closest("article")?.selectFirst("a[href*=/detail/]") ?: link
                val detailUrl = response.request.url.resolve(detail.attr("href")) ?: return@forEach
                if (!accepts(detailUrl) || !detailUrl.encodedPath.matches(Regex("^/(?:en|zh|zh-hant|zh-hans|ja|ko|es|pt|id|th|vi)/detail/[0-9]+/?$"))) return@forEach
                val title = detail.attr("aria-label").ifBlank { detail.text() }.ifBlank { link.attr("aria-label") }.trim()
                if (title.isBlank()) return@forEach
                val image = link.selectFirst("img")
                val rawImage = image?.attr("srcSet")?.substringBefore(',')?.substringBeforeLast(' ')?.ifBlank { null }
                    ?: image?.attr("data-src")?.takeIf(String::isNotBlank) ?: image?.attr("src").orEmpty()
                val poster = response.request.url.resolve(rawImage)?.toString().orEmpty()
                items.putIfAbsent(detailUrl.toString(), CatalogItem(title, detailUrl.toString(), poster))
            }
            val next = doc.selectFirst("a[rel=next][href]")?.absUrl("href")?.takeIf { runCatching { accepts(it.toHttpUrl()) }.getOrDefault(false) }
            CatalogPage(items.values.toList(), next)
        }
    }

    private fun accepts(url: HttpUrl) = url.scheme == "https" && url.host in setOf(host, "www.$host") && url.username.isEmpty() && url.password.isEmpty()
}
