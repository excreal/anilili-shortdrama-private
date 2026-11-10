package com.example.animepahedl.metadata

import com.example.animepahedl.*

import androidx.annotation.Keep
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A data API independent of UI cards. Null/empty means unreported, never an inferred rating/tag. */
@Keep internal enum class MetadataSite { DRAMATIP, KISS18, NARTO }
@Keep internal data class MetadataImage(val url: String, val role: String = "image", val width: Int? = null,
    val height: Int? = null, val language: String? = null, val caption: String? = null)
@Keep internal data class MetadataTerm(val name: String, val id: String? = null, val url: String? = null,
    val count: Long? = null, val attributes: Map<String, String> = emptyMap())
@Keep internal data class MetadataRating(val value: Double? = null, val scale: Double? = null,
    val votes: Long? = null, val source: String? = null, val label: String? = null)
@Keep internal data class MetadataPerson(val name: String, val role: String? = null, val url: String? = null,
    val image: String? = null, val attributes: Map<String, String> = emptyMap())
@Keep internal data class MetadataLink(val url: String, val relation: String, val label: String? = null)
@Keep internal data class MetadataSubtitle(val url: String, val label: String, val language: String? = null,
    val format: String? = null, val isDefault: Boolean = false, val attributes: Map<String, String> = emptyMap())
/** Public source data only: no HTTP headers, account credentials or session tokens. JSON is retained
 * as text so callers can recover additional fields without a lossy JSON-to-string map conversion. */
@Keep internal data class MetadataPayload(val name: String, val url: String, val json: String)
@Keep internal data class MetadataRecord(val id: String, val url: String, val title: String,
    val alternativeTitles: List<String> = emptyList(), val description: String? = null,
    val images: List<MetadataImage> = emptyList(), val genres: List<MetadataTerm> = emptyList(),
    val categories: List<MetadataTerm> = emptyList(), val tags: List<MetadataTerm> = emptyList(),
    val ratings: List<MetadataRating> = emptyList(), val people: List<MetadataPerson> = emptyList(),
    val countries: List<String> = emptyList(), val languages: List<String> = emptyList(),
    val studios: List<MetadataTerm> = emptyList(), val status: String? = null, val format: String? = null,
    val year: Int? = null, val episodeCount: Int? = null, val durationSeconds: Long? = null,
    val releaseDate: String? = null, val updatedAt: String? = null,
    val externalIds: Map<String, String> = emptyMap(), val links: List<MetadataLink> = emptyList(),
    val attributes: Map<String, String> = emptyMap(), val payloads: List<MetadataPayload> = emptyList())
@Keep internal data class MetadataEpisode(val id: String, val number: String, val title: String, val url: String,
    val description: String? = null, val images: List<MetadataImage> = emptyList(),
    val durationSeconds: Long? = null, val releaseDate: String? = null,
    val subtitles: List<MetadataSubtitle> = emptyList(), val attributes: Map<String, String> = emptyMap(),
    val payloads: List<MetadataPayload> = emptyList())
@Keep internal data class MetadataSection(val name: String, val items: List<MetadataRecord>, val url: String? = null,
    val tabKey: String? = null, val page: Int? = null, val hasPrevious: Boolean? = null,
    val fallback: JsonElement? = null, val warnings: List<String> = emptyList(), val nextUrl: String? = null)
@Keep internal data class MetadataPage(val items: List<MetadataRecord>, val nextUrl: String? = null,
    val total: Long? = null, val sections: List<MetadataSection> = emptyList(),
    val filters: Map<String, List<MetadataTerm>> = emptyMap(), val payloads: List<MetadataPayload> = emptyList(),
    val tabPages: Map<String, Int> = emptyMap(), val providerFilters: Map<String, List<MetadataTerm>> = emptyMap())
@Keep internal data class MetadataDetails(val record: MetadataRecord, val episodes: List<MetadataEpisode> = emptyList(),
    val related: List<MetadataRecord> = emptyList(), val sections: List<MetadataSection> = emptyList(),
    val payloads: List<MetadataPayload> = emptyList())
@Keep internal data class MetadataTaxonomy(val groups: Map<String, List<MetadataTerm>> = emptyMap(),
    val payloads: List<MetadataPayload> = emptyList())
@Keep internal data class MetadataBrowseQuery(val query: String = "", val url: String? = null,
    val page: Int = 1, val filters: Map<String, String> = emptyMap())

@Keep
internal interface SiteMetadataApi {
    val site: MetadataSite
    suspend fun browse(query: MetadataBrowseQuery = MetadataBrowseQuery()): MetadataPage
    suspend fun details(url: String): MetadataDetails
    suspend fun episode(url: String): MetadataEpisode
    suspend fun taxonomy(): MetadataTaxonomy
}

internal data class MetadataResponse(val url: String, val text: String, val contentType: String) {
    fun document(): Document = Jsoup.parse(text, url)
    fun json(): JsonElement = JsonParser.parseString(text)
    fun payload(name: String) = metadataPayload(name, url, json())
}

/** Bounded, cancellable, same-provider requests. Imported/session cookies are scoped on every hop. */
internal class MetadataHttp(private val hosts: Set<String>, private val cookieHeader: ((String) -> String)? = null,
    private val accept: String = "application/json,text/html;q=0.9,*/*;q=0.8",
    client: OkHttpClient = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS).build()) {
    private val sessions = CatalogSessionCookies()
    private val http = client.newBuilder().cookieJar(sessions).addNetworkInterceptor { chain ->
        val request = chain.request()
        checked(request.url.toString())
        val merged = linkedMapOf<String, String>()
        fun merge(value: String) { value.split(';').forEach { part ->
            val name = part.substringBefore('=').trim()
            if (name.isNotEmpty() && '=' in part) merged[name] = part.substringAfter('=').trim()
        } }
        merge(request.header("Cookie").orEmpty()); merge(cookieHeader?.invoke(request.url.toString()).orEmpty())
        val builder = request.newBuilder().removeHeader("Cookie")
        if (merged.isNotEmpty()) builder.header("Cookie", merged.entries.joinToString("; ") { "${it.key}=${it.value}" })
        chain.proceed(builder.build())
    }.build()
    fun checked(raw: String) = raw.toHttpUrl().also {
        require(it.scheme == "https" && it.host in hosts && it.username.isEmpty() && it.password.isEmpty()) { "URL does not belong to this metadata source." }
    }
    suspend fun get(url: String, limit: Int = 8 * 1024 * 1024): MetadataResponse = withContext(Dispatchers.IO) {
        val target = checked(url)
        val request = Request.Builder().url(target).header("User-Agent", AnimeNetwork.userAgent)
            .header("Accept", accept)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", target.newBuilder().encodedPath("/").query(null).fragment(null).build().toString()).build()
        http.newCall(request).consumeCancellable { response ->
            val bytes = response.body.byteStream().use { it.readNBytes(limit + 1) }
            if (bytes.size > limit) throw IOException("Source metadata exceeds the response size limit.")
            val text = bytes.toString(response.body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
            if (SessionChecks.isChallenge(text, response.header("cf-mitigated"))) throw VerificationRequired(target.toString())
            if (!response.isSuccessful) throw IOException("${target.host} metadata returned HTTP ${response.code}.")
            MetadataResponse(response.request.url.toString(), text, response.header("Content-Type").orEmpty())
        }
    }
}

internal fun metadataPayload(name: String, url: String, value: JsonElement): MetadataPayload {
    fun clean(node: JsonElement): JsonElement = when {
        node.isJsonObject -> JsonObject().apply { node.asJsonObject.entrySet().forEach { (key, child) ->
            if (!key.matches(Regex("(?i)(password|authorization|cookie|set-cookie|csrf|csrf.?token|token|access.?token|refresh.?token|session.?token|secret|secretKey|privateKey|kkey)"))) add(key, clean(child))
        } }
        node.isJsonArray -> com.google.gson.JsonArray().apply { node.asJsonArray.forEach { add(clean(it)) } }
        else -> node.deepCopy()
    }
    val source = runCatching {
        val parsed = url.toHttpUrl()
        parsed.newBuilder().apply {
            parsed.queryParameterNames.filter { it.matches(Regex("(?i)(kkey|token|access_token|refresh_token|csrf|csrf_token|authorization)")) }
                .forEach(::removeAllQueryParameters)
        }.build().toString()
    }.getOrDefault(url)
    return MetadataPayload(name, source, clean(value).toString())
}

internal fun CatalogItem.metadataRecord() = MetadataRecord(url, url, title,
    images = poster.takeIf(String::isNotBlank)?.let { listOf(MetadataImage(it, "poster")) }.orEmpty())
internal fun SiteDownloadEpisode.metadataEpisode() = MetadataEpisode(id, displayNumber, title, url)
