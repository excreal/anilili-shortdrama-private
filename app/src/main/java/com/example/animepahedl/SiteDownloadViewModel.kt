package com.example.animepahedl

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.example.animepahedl.metadata.SiteMetadataApis
import com.example.animepahedl.metadata.toAppMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SiteDownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val cookies = SiteDownloadCookieStore(application)
    private val repository = appSiteDownloadRepository(cookieHeader = cookies::headerFor)
    private val metadataApis by lazy { SiteMetadataApis.forApp(application) }
    private val gson = Gson()
    private var inspectJob: Job? = null
    private var metadataJob: Job? = null
    private var qualityJob: Job? = null
    private var generation = 0
    var submitting by mutableStateOf(false)
        private set
    var input by mutableStateOf("")
        private set
    var series by mutableStateOf<SiteDownloadSeries?>(null)
        private set
    var inspecting by mutableStateOf(false)
        private set
    var selection by mutableStateOf("")
        private set
    var selectedEpisodes by mutableStateOf(emptyList<SiteDownloadEpisode>())
        private set
    var selectionError by mutableStateOf<String?>(null)
        private set
    var quality by mutableStateOf("Best")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var hasCookies by mutableStateOf(cookies.hasCookies)
        private set
    var pending by mutableStateOf<SiteDownloadPreparation?>(null)
        private set
    var preparing by mutableStateOf(false)
        private set
    var discarding by mutableStateOf(false)
        private set
    var preparationStatus by mutableStateOf(PreparationStatus())
        private set
    var preparationLabel by mutableStateOf("")
        private set
    var ready by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            try {
                DownloadStore.awaitReady()
                pending = DownloadStore.operations("preparation")[SITE_DOWNLOAD_PREPARATION_KEY]?.let {
                    runCatching { gson.fromJson(it, SiteDownloadPreparation::class.java) }.getOrNull()
                }
                ready = true
                PreparationJobs.observe(app, PreparationJobs.SITES).collect { status ->
                    val remaining = PreparationJobs.payload(PreparationJobs.SITES)?.let {
                        runCatching { gson.fromJson(it, SiteDownloadPreparation::class.java) }.getOrNull()
                    }
                    if (submitting && remaining == null) return@collect
                    pending = remaining
                    preparationStatus = status
                    preparing = status.automatic
                    preparationLabel = status.label
                }
            } catch (_: Exception) { error = "Could not load pending downloads. Reopen the app to try again." }
        }
    }

    fun libraryRows(actual: List<DownloadRow>): List<DownloadRow> = mergePreparedQueueRows(actual, pending, preparationStatus)
    fun isPendingRow(id: String) = isPreparedQueueRow(id)
    fun pausePending(id: String) { if (isPendingRow(id)) pausePreparation() }
    fun resumePending(id: String) { if (isPendingRow(id)) resumePreparation() }
    fun removePending(ids: Set<String>) {
        if (ids.none(::isPreparedQueueRow)) return
        viewModelScope.launch {
            try { PreparationJobs.removeSiteEpisodes(app, ids) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not remove queued episodes. Try again." }
        }
    }

    fun updateInput(value: String) {
        generation++
        inspectJob?.cancel()
        metadataJob?.cancel()
        qualityJob?.cancel()
        inspecting = false
        input = value
        series = null
        selectedEpisodes = emptyList()
        selection = ""
        selectionError = null
        error = null
    }

    fun inspect() {
        if (input.isBlank()) return
        val request = input.trim()
        val current = ++generation
        inspectJob?.cancel()
        metadataJob?.cancel()
        qualityJob?.cancel()
        inspecting = true
        error = null
        series = null
        selectedEpisodes = emptyList()
        inspectJob = viewModelScope.launch {
            try {
                val result = repository.inspect(request, cookies.headerFor(request))
                ensureActive()
                if (result.provider !in ShortDramaCatalogConfig.supportedProviders) {
                    error = "This short-drama edition supports Dramatip, Kiss18, Narto Drama, and direct media links."
                    return@launch
                }
                if (generation == current) {
                    series = result
                    if (result.provider != SiteDownloadProvider.DIRECT) metadataJob = viewModelScope.launch {
                        try {
                            val metadata = metadataApis.forProvider(result.provider).details(request).toAppMetadata()
                            ensureActive()
                            val latest = series
                            if (latest != null) series = mergeInspectionMetadata(latest, result.provider, result.id, current, generation, metadata)
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* A metadata challenge must not block playback or downloads. */ }
                    }
                    val requestedEpisode = runCatching {
                        dramaRoute(siteDownloadInput(request), result.provider).second
                    }.getOrNull()
                    if (result.provider.isShortDrama) qualityJob = viewModelScope.launch {
                        try {
                            val qualities = repository.discoverQualities(result, requestedEpisode, cookies.headerFor(request))
                            ensureActive()
                            val latest = series
                            if (latest != null) series = mergeDiscoveredQualities(latest, result.provider, result.id, current, generation, qualities)
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* Optional qualities do not affect episode selection. */ }
                    }
                    quality = "Best"
                    updateSelection(requestedEpisode?.toString().orEmpty())
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (generation == current) error = "Could not inspect this link. Check the URL and connection, or import a session for this site."
            } finally { if (generation == current) inspecting = false }
        }
    }

    fun cancelInspect() { generation++; inspectJob?.cancel(); metadataJob?.cancel(); qualityJob?.cancel(); inspecting = false }
    fun chooseQuality(value: String) { if (value in (series?.qualities.orEmpty() + "Best")) quality = value }
    fun updateSelection(value: String) {
        selection = value
        try {
            selectedEpisodes = parseSiteDownloadSelection(value, series?.episodes.orEmpty())
            selectionError = null
        } catch (_: Exception) {
            selectedEpisodes = emptyList()
            selectionError = "Use episode numbers and ranges, for example 1-5,9. Enter none to clear."
        }
    }
    fun toggleEpisode(episode: SiteDownloadEpisode) {
        val ids = selectedEpisodes.map { it.id }.toMutableSet()
        if (!ids.add(episode.id)) ids.remove(episode.id)
        val selected = series?.episodes.orEmpty().filter { it.id in ids }
        updateSelection(if (selected.isEmpty()) "none" else selected.joinToString(",") { it.displayNumber })
    }
    fun saveCookies(input: String): Boolean = try {
        cookies.save(input)
        hasCookies = cookies.hasCookies
        notice = if (hasCookies) "Session cookies saved privately." else "Session cookies removed."
        true
    } catch (_: IllegalArgumentException) { error = "Import a Netscape cookie file with domain information (up to 256 KiB)."; false }
    fun clearCookies() { cookies.clear(); hasCookies = false; notice = "Session cookies removed." }
    fun dismissNotice() { notice = null }
    fun dismissError() { error = null }
    fun downloadEpisode(show: SiteDownloadSeries, episode: SiteDownloadEpisode, selectedQuality: String = "Best"): Boolean {
        if (!ready || submitting || discarding) return false
        runPreparation(SiteDownloadPreparation(show, listOf(episode), selectedQuality))
        return true
    }
    fun downloadSelected() {
        val preview = series ?: return
        if (!ready || submitting || discarding || selectionError != null || selectedEpisodes.isEmpty()) return
        runPreparation(SiteDownloadPreparation(preview, selectedEpisodes.toList(), quality))
    }
    fun resumePreparation() { if (!discarding) viewModelScope.launch {
        try { PreparationJobs.resume(app, PreparationJobs.SITES) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Could not resume preparation. Try again." }
    } }
    fun pausePreparation() { viewModelScope.launch {
        try { PreparationJobs.pause(app, PreparationJobs.SITES) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Could not pause preparation. Try again." }
    } }
    fun discardPreparation() {
        if (discarding || !ready) return
        discarding = true
        viewModelScope.launch {
            try { PreparationJobs.discard(app, PreparationJobs.SITES) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Could not discard preparation. Try again." }
            finally { discarding = false }
        }
    }
    private fun runPreparation(record: SiteDownloadPreparation) {
        if (submitting || discarding || !ready) return
        val append = pending != null
        if (!append) preparing = true
        submitting = true
        if (!append) {
            pending = record
        val queuedCount = record.batches().sumOf { it.episodes.size }
        preparationStatus = PreparationStatus(pending = true, label = "$queuedCount queued · preparing links in background", totalSelected = queuedCount)
        }
        error = null
        viewModelScope.launch {
            try {
                if (append) PreparationJobs.appendSites(app, record) else PreparationJobs.submit(app, PreparationJobs.SITES, record)
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                preparing = false
                pending = PreparationJobs.payload(PreparationJobs.SITES)?.let {
                    runCatching { gson.fromJson(it, SiteDownloadPreparation::class.java) }.getOrNull()
                }
                error = if (append) "Could not add these downloads to the preparation queue. Your current queue is kept." else
                    "Could not schedule preparation. Your selection is kept."
            } finally { submitting = false }
        }
    }
}
