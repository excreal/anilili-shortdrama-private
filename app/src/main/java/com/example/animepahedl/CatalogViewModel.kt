package com.example.animepahedl

import androidx.compose.runtime.*
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import okhttp3.HttpUrl.Companion.toHttpUrl
internal enum class CatalogSource(val label: String) { DRAMATIP("Dramatip"), KISS18("Kiss18"), NARTO("Narto Drama") }

internal object ShortDramaCatalogConfig {
    val sources = listOf(CatalogSource.DRAMATIP, CatalogSource.KISS18, CatalogSource.NARTO)
    val initialSource = CatalogSource.DRAMATIP
    val supportedProviders = setOf(SiteDownloadProvider.DRAMATIP, SiteDownloadProvider.KISS18, SiteDownloadProvider.NARTO, SiteDownloadProvider.DIRECT)
}

internal data class CatalogSourceOrder(val order: List<CatalogSource>, val enabled: Set<CatalogSource>)

/** User-owned order and visibility for the source pills shown on Home and Browse. */
internal object CatalogSourcePreferences {
    private const val FILE = "catalog-sources"
    private const val ORDER = "order"
    private const val DISABLED = "disabled"
    private var prefs: android.content.SharedPreferences? = null
    private val defaults get() = CatalogSourceOrder(ShortDramaCatalogConfig.sources, ShortDramaCatalogConfig.sources.toSet())
    val state = kotlinx.coroutines.flow.MutableStateFlow(defaults)

    @Synchronized fun attach(context: android.content.Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
        val stored = prefs ?: return
        val order = (stored.getString(ORDER, null)?.split(',').orEmpty().mapNotNull { raw ->
            ShortDramaCatalogConfig.sources.firstOrNull { it.name == raw }
        } + ShortDramaCatalogConfig.sources).distinct()
        val disabled = stored.getStringSet(DISABLED, emptySet()).orEmpty()
            .mapNotNullTo(mutableSetOf()) { raw -> ShortDramaCatalogConfig.sources.firstOrNull { it.name == raw } }
        val enabled = ShortDramaCatalogConfig.sources.filterNotTo(linkedSetOf()) { it in disabled }
        if (enabled.isEmpty()) enabled += ShortDramaCatalogConfig.initialSource
        state.value = CatalogSourceOrder(order, enabled)
        persist(state.value)
    }

    @Synchronized fun move(source: CatalogSource, offset: Int) {
        val current = state.value
        val from = current.order.indexOf(source)
        val to = (from + offset).coerceIn(current.order.indices)
        if (from < 0 || from == to) return
        val order = current.order.toMutableList().apply { add(to, removeAt(from)) }
        publish(current.copy(order = order))
    }

    @Synchronized fun setEnabled(source: CatalogSource, enabled: Boolean) {
        val current = state.value
        val updated = current.enabled.toMutableSet().apply { if (enabled) add(source) else remove(source) }
        if (updated.isEmpty()) return
        publish(current.copy(enabled = updated))
    }

    private fun publish(value: CatalogSourceOrder) {
        state.value = value
        persist(value)
    }

    private fun persist(value: CatalogSourceOrder) {
        prefs?.edit()
            ?.putString(ORDER, value.order.joinToString(",") { it.name })
            ?.putStringSet(DISABLED, ShortDramaCatalogConfig.sources.filterNot { it in value.enabled }.mapTo(mutableSetOf()) { it.name })
            ?.apply()
    }
}

private const val MAX_CATALOG_CACHE_BYTES = 4_000_000L

/** Serialize the complete loaded prefix or decline to cache it; a shortened prefix cannot keep a tail cursor. */
internal fun catalogCacheJson(page: CatalogPage, gson: com.google.gson.Gson = com.google.gson.Gson()): String? =
    gson.toJson(page).takeIf { it.toByteArray(Charsets.UTF_8).size <= MAX_CATALOG_CACHE_BYTES }

/** Reject cached catalogues tagged with a source outside this edition. */
internal fun catalogPageForSource(source: CatalogSource, page: CatalogPage): CatalogPage =
    page.also { require(source in ShortDramaCatalogConfig.sources) }

/** Results may survive a failed forced refresh only while the displayed query/source identity is unchanged. */
internal fun shouldPreserveCatalogResultsOnRefresh(force: Boolean, displayedKey: String?, requestKey: String): Boolean =
    force && displayedKey == requestKey

internal data class CatalogFailureState(val items: List<CatalogItem>, val nextUrl: String?, val displayedKey: String?)

internal fun catalogFailureState(
    items: List<CatalogItem>, nextUrl: String?, displayedKey: String?,
    append: Boolean, force: Boolean, requestKey: String
): CatalogFailureState = if (append || shouldPreserveCatalogResultsOnRefresh(force, displayedKey, requestKey)) {
    CatalogFailureState(items, nextUrl, displayedKey)
} else {
    CatalogFailureState(emptyList(), null, null)
}

internal class CatalogViewModel(application: Application) : AndroidViewModel(application) {
    private val cookies = SiteDownloadCookieStore(application)
    private val sourcePreferences = CatalogSourcePreferences.also { it.attach(application) }
    var availableSources by mutableStateOf(sourcePreferences.state.value.order.filter { it in sourcePreferences.state.value.enabled }); private set
    var source by mutableStateOf(availableSources.firstOrNull() ?: ShortDramaCatalogConfig.initialSource); private set
    var query by mutableStateOf(""); private set
    var items by mutableStateOf(emptyList<CatalogItem>()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var nextUrl by mutableStateOf<String?>(null); private set
    var filterLabel by mutableStateOf<String?>(null); private set
    private var activeFilter: Pair<String, SiteShowTerm>? = null
    private var job: Job? = null
    private var displayedKey: String? = null
    private data class Cached(val time: Long, val page: CatalogPage)
    private val memory = linkedMapOf<String, Cached>()
    private val cacheDir = java.io.File(application.cacheDir, "catalog-pages").apply { mkdirs() }
    private val gson = com.google.gson.Gson()
    private fun cacheFile(key: String) = java.io.File(cacheDir, java.security.MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) } + ".json")
    private suspend fun readCache(key: String, source: CatalogSource): Cached? {
        val cached = memory[key] ?: withContext(Dispatchers.IO) {
            runCatching { val file = cacheFile(key); if (!file.exists() || file.length() > MAX_CATALOG_CACHE_BYTES) null else Cached(file.lastModified(), gson.fromJson(file.readText(), CatalogPage::class.java)) }.getOrNull()
        } ?: return null
        val normalized = cached.copy(page = catalogPageForSource(source, cached.page))
        memory[key] = normalized
        if (memory.size > 8) memory.remove(memory.keys.first())
        return normalized
    }
    private suspend fun saveCache(key: String, page: CatalogPage) {
        val json = catalogCacheJson(page, gson)
        if (json == null) {
            memory.remove(key)
            withContext(Dispatchers.IO) { runCatching { cacheFile(key).delete() } }
            return
        }
        memory[key] = Cached(System.currentTimeMillis(), page)
        if (memory.size > 8) memory.remove(memory.keys.first())
        withContext(Dispatchers.IO) { runCatching {
            val file = cacheFile(key); val temp = java.io.File(file.path + ".tmp")
            temp.writeText(json); temp.renameTo(file)
            cacheDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(30)?.forEach { it.delete() }
        } }
    }
    private val adapters by lazy { ShortDramaCatalogConfig.sources.associateWith { source -> when(source) {
        CatalogSource.DRAMATIP -> DramatipCatalog(cookieHeader = cookies::headerFor)
        CatalogSource.KISS18 -> Kiss18Catalog()
        CatalogSource.NARTO -> NartoCatalog(cookieHeader = cookies::headerFor)
    } } }
    init {
        load(false)
        viewModelScope.launch {
            sourcePreferences.state.collect { settings ->
                availableSources = settings.order.filter { it in settings.enabled }
                if (source !in settings.enabled) {
                    val replacement = availableSources.firstOrNull() ?: ShortDramaCatalogConfig.initialSource
                    if (source != replacement) {
                        source = replacement; query = ""; activeFilter = null; filterLabel = null; load(false)
                    }
                }
            }
        }
    }
    fun select(value: CatalogSource) { if (source == value || value !in availableSources) return; source = value; query = ""; activeFilter = null; filterLabel = null; load(false) }
    fun edit(value: String) { activeFilter = null; filterLabel = null; query = value; load(false, 400) }
    fun explore(provider: SiteDownloadProvider, kind: String, term: SiteShowTerm) {
        val mapped = when (provider) {
            SiteDownloadProvider.DRAMATIP -> CatalogSource.DRAMATIP
            SiteDownloadProvider.KISS18 -> CatalogSource.KISS18
            SiteDownloadProvider.NARTO -> CatalogSource.NARTO
            else -> return
        }
        if (mapped == CatalogSource.KISS18) {
            if (kind !in setOf("category", "tag", "genre") || term.name.isBlank() || term.name.length > 100) return
        } else if (!isSafeMetadataFilterRoute(mapped, kind, term.url)) return
        source = mapped; query = ""; activeFilter = kind to term; filterLabel = "${term.name} · ${mapped.label}"; load(false)
    }
    fun reset() { activeFilter = null; filterLabel = null; query = ""; load(false) }
    fun refresh() = load(false, force = true)
    fun more() { if (!loading && nextUrl != null) load(true) }
    private fun load(append: Boolean, delayMs: Long = 0, force: Boolean = false) {
        job?.cancel()
        val selectedSource = source
        val adapter = adapters.getValue(selectedSource); val term = query.trim(); val next = nextUrl; val filter = activeFilter
        val key = selectedSource.name + ":" + (filter?.let { "${it.first}:${it.second.url ?: it.second.name}" } ?: term.lowercase())
        if (!append && !shouldPreserveCatalogResultsOnRefresh(force, displayedKey, key)) {
            items = emptyList(); nextUrl = null; displayedKey = null
        }
        error = null; loading = true
        job = viewModelScope.launch {
            try {
                delay(delayMs)
                if (!append) {
                    readCache(key, selectedSource)?.let { cached ->
                        ensureActive(); items = cached.page.items; nextUrl = cached.page.nextUrl; displayedKey = key
                        if (!force && System.currentTimeMillis() - cached.time < 15 * 60_000) return@launch
                    }
                }
                var page = if (append && next != null) adapter.page(next) else if (filter != null) adapter.filter(filter.first, filter.second) else if (term.isBlank()) adapter.home() else adapter.search(term)
                if (!append && term.isBlank() && filter == null) {
                    items = page.items; nextUrl = page.nextUrl; displayedKey = key
                    val visited = mutableSetOf<String>()
                    repeat(2) {
                        val url = page.nextUrl
                        if (url != null && visited.add(url)) {
                            try { val extra = adapter.page(url); page = CatalogPage((page.items + extra.items).distinctBy { it.url }, extra.nextUrl); ensureActive(); items = page.items; nextUrl = page.nextUrl; displayedKey = key }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { /* Keep a successfully loaded first page. */ }
                        }
                    }
                }
                ensureActive()
                items = (if (append) items + page.items else page.items).distinctBy { it.url }
                displayedKey = key
                nextUrl = page.nextUrl?.takeUnless { it == next && append }
                saveCache(key, CatalogPage(items, nextUrl))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val failed = catalogFailureState(items, nextUrl, displayedKey, append, force, key)
                items = failed.items; nextUrl = failed.nextUrl; displayedKey = failed.displayedKey
                error = e.message ?: "Could not load this source."
            }
            finally { if (currentCoroutineContext().isActive) loading = false }
        }
    }

}

internal fun isSafeMetadataFilterRoute(source: CatalogSource, kind: String, raw: String?): Boolean {
        if (raw == null || kind !in setOf("genre", "category", "tag", "actor", "director")) return false
        val url = runCatching { raw.toHttpUrl() }.getOrNull() ?: return false
        if (url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty() || url.querySize > 0 || url.fragment != null) return false
        return when (source) {
            CatalogSource.DRAMATIP -> url.host in setOf("dramatip.net", "www.dramatip.net") && kind in setOf("category", "genre") && url.encodedPath.matches(Regex("^/(?:[a-z]{2}/)?category/[a-zA-Z0-9_-]+(?:/page/\\d+)?/?$"))
            CatalogSource.NARTO -> url.host in setOf("narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com") &&
                url.encodedPath.matches(Regex("^/(?:genre|tag|category)/[a-zA-Z0-9_-]+(?:/page/\\d+)?/?$")) &&
                url.encodedPath.startsWith(if (kind == "genre") "/genre/" else if (kind == "tag") "/tag/" else "/category/")
            CatalogSource.KISS18 -> false
        }
}
