package com.example.animepahedl

import androidx.core.content.edit
import androidx.core.net.toUri
import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloaderViewModel(application: Application, private val repository: AnimeRepository) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, AnimeRepository())
    private val downloadRepository: DownloadRepository = AndroidDownloadRepository(application, repository)
    private val libraryPreferences = application.getSharedPreferences("personal-library", Context.MODE_PRIVATE)
    var bookmarks by mutableStateOf(loadBookmarks())
        private set
    var recentSearches by mutableStateOf(libraryPreferences.getString("recent", "").orEmpty().split("\n").filter { it.isNotBlank() })
        private set
    private fun loadBookmarks(): List<Anime> = runCatching {
        com.google.gson.Gson().fromJson(libraryPreferences.getString("bookmarks", "[]"), Array<Anime>::class.java).toList()
    }.getOrDefault(emptyList())
    fun toggleBookmark(anime: Anime) {
        bookmarks = if (bookmarks.any { it.session == anime.session }) bookmarks.filterNot { it.session == anime.session } else bookmarks + anime
        libraryPreferences.edit { putString("bookmarks", com.google.gson.Gson().toJson(bookmarks)) }
    }
    fun clearRecentSearches() { recentSearches = emptyList(); libraryPreferences.edit { remove("recent") } }
    var siteDownloadSelected by mutableStateOf(false)
        private set
    fun selectExploreSource(siteDownload: Boolean) {
        siteDownloadSelected = siteDownload
        if (siteDownload) leaveExplore() else presentSearchVerification()
    }
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf(emptyList<Anime>())
        private set
    var searched by mutableStateOf(false)
        private set
    var searching by mutableStateOf(false)
        private set
    var selectedAnime by mutableStateOf<Anime?>(null)
        private set
    var details by mutableStateOf<AnimeDetails?>(null)
        private set
    var loadingDetails by mutableStateOf(false)
        private set
    var selectedEpisodes by mutableStateOf(emptySet<String>())
        private set
    var sort by mutableStateOf(ResultSort.RELEVANCE)
    var quality by mutableStateOf("Best")
    var audio by mutableStateOf("Original")
    var availableSources by mutableStateOf(emptyList<EpisodeSource>())
        private set
    var loadingSources by mutableStateOf(false)
        private set
    var sourceChoicesLabel by mutableStateOf("")
        private set
    val availableAudio get() = availableSources.map { it.audio }.distinct()
    val availableQualities get() = availableSources.filter { it.audio == audio }.map { it.quality }.distinct()
        .sortedByDescending { it.removeSuffix("p").toInt() }
    private val sourceCache = mutableMapOf<String, List<EpisodeSource>>()
    private var sourceJob: Job? = null
    private var sourceGeneration = 0
    private var artworkJob: Job? = null
    private val attemptedArtwork = mutableSetOf<String>()
    private val failures = androidx.compose.runtime.mutableStateMapOf<OperationId, OperationFailure>()
    private var otherError by mutableStateOf<String?>(null)
    internal val searchFailure get() = failures[OperationId.Search(searchGeneration)]
    val downloadVerificationCount get() = failures.entries.count { (id, failure) -> id is OperationId.Download && relevant(id) && failure.verificationNeeded }
    val error get() = currentFailure()?.message ?: otherError
    var verification by mutableStateOf<String?>(null)
        private set
    private var verificationHost: String? = null
    private var foreground = false
    private val searchGate = SearchVerificationGate()
    private var idleJob: Job? = null
    private val verifiedOperations = mutableSetOf<OperationId>()
    private val verificationOperations = mutableSetOf<OperationId>()
    private val exploreVisible get() = !siteDownloadSelected && !showDownloads && !showSettings && !showAbout && selectedAnime == null
    fun setForeground(value: Boolean) {
        foreground = value
        outputAccessGranted = hasOutputFolderAccess(app)
        outputCheckRequired = !outputAccessGranted || DownloadStore.operations("folder-access")["transfers"] != null
        if (!value) {
            searchGate.invalidate()
            savedFilesWatcher.stop()
            savedFilesPolling?.cancel()
            savedFilesRefresh?.cancel()
        } else {
            presentSearchVerification()
            savedFilesWatcher.start()
            refreshDownloads()
            savedFilesPolling?.cancel()
            savedFilesPolling = viewModelScope.launch {
                try {
                    DownloadStore.awaitReady()
                    savedFilesWatcher.updateRoots()
                    refreshSavedFiles()
                    while (true) {
                        delay(30_000)
                        savedFilesWatcher.updateRoots()
                        refreshSavedFiles()
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { DebugTrace.event("saved_files_watch_deferred") }
            }
        }
    }
    private fun leaveExplore() { searchGate.invalidate(); idleJob?.cancel() }
    var resolving by mutableStateOf(false)
        private set
    var resolvingLabel by mutableStateOf("")
        private set
    var downloadNotice by mutableStateOf<String?>(null)
        private set
    fun dismissDownloadNotice() { downloadNotice = null }
    var downloads by mutableStateOf(emptyList<DownloadRow>())
        private set
    private var downloadsVisible by mutableStateOf(false)
    var showDownloads: Boolean
        get() = downloadsVisible
        set(value) {
            downloadsVisible = value
            if (value) {
                leaveExplore()
                refreshSavedFiles()
            }
        }
    private var settingsVisible by mutableStateOf(false)
    var showSettings: Boolean
        get() = settingsVisible
        set(value) { settingsVisible = value; if (value) leaveExplore() }
    private var aboutVisible by mutableStateOf(false)
    var showAbout: Boolean
        get() = aboutVisible
        set(value) { aboutVisible = value; if (value) leaveExplore() }
    private val settings = AppSettings(application)
    var unmeteredOnly by mutableStateOf(settings.unmeteredOnly)
        private set
    fun setUnmetered(value: Boolean) {
        settings.unmeteredOnly = value
        unmeteredOnly = value
        OfflineDownloads.manager(app).setRequirements(androidx.media3.exoplayer.scheduler.Requirements(if (value) androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED else androidx.media3.exoplayer.scheduler.Requirements.NETWORK))
        settingsMessage = "Network policy saved. Existing direct downloads keep their original policy."
    }
    var defaultAudio by mutableStateOf(settings.defaultAudio)
        private set
    var connections by androidx.compose.runtime.mutableIntStateOf(settings.connections)
        private set
    var simultaneousDownloads by androidx.compose.runtime.mutableIntStateOf(settings.simultaneousDownloads)
        private set
    var outputFolder by mutableStateOf(settings.outputFolder)
        private set
    var outputTree by mutableStateOf(settings.outputTree)
        private set
    var outputAccessGranted by mutableStateOf(hasOutputFolderAccess(application))
        private set
    var outputCheckRequired by mutableStateOf(!hasOutputFolderAccess(application))
        private set
    var checkingExistingFiles by mutableStateOf(false)
        private set
    var clearingSession by mutableStateOf(false)
        private set
    var settingsMessage by mutableStateOf<String?>(null)
        private set
    private var searchJob: Job? = null
    private var searchGeneration = 0
    private var detailJob: Job? = null
    private val app get() = getApplication<Application>()

    private var savedFilesRefresh: Job? = null
    private var savedFilesPolling: Job? = null
    private var savedFilesDirty = false
    private val savedFilesWatcher by lazy { SavedFilesWatcher(app, ::refreshSavedFiles) }

    fun refreshSavedFiles() {
        if (!foreground) return
        savedFilesDirty = true
        if (savedFilesRefresh?.isActive == true) return
        savedFilesRefresh = viewModelScope.launch {
            try {
                DownloadStore.awaitReady()
                do {
                    savedFilesDirty = false
                    delay(500)
                    val records = Exports.all(app).filterValues { it.uri.isNotEmpty() }
                    val missing = missingSavedFiles(app, records)
                    var removed = false
                    for ((id, record) in missing) Exports.withItemLock(id) {
                        ensureActive()
                        if (!Exports.deleting(id) && Exports.all(app)[id] == record) {
                            // Only forget a confirmed deleted file. Never delete another file or a transfer.
                            Exports.remove(app, id)
                            if (id.startsWith("direct:")) DownloadStore.operation(id.removePrefix("direct:"), "direct", null)
                            removed = true
                        }
                    }
                    if (removed) DownloadStore.flush()
                    refreshDownloads()
                } while (savedFilesDirty && foreground)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { DebugTrace.event("saved_files_refresh_deferred") }
        }
    }

    override fun onCleared() {
        savedFilesWatcher.stop()
        super.onCleared()
    }

    private fun hasActiveDirectDownload(): Boolean {
        val exports = Exports.all(app)
        val ids = DownloadStore.operations("direct").keys.filter { exports["direct:$it"]?.uri.isNullOrEmpty() }
        if (ids.isEmpty()) return false
        val system = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return ids.any { id ->
            system.query(DownloadManager.Query().setFilterById(id.toLong())).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) in
                    listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED)
            }
        }
    }

    fun updateQuery(value: String) {
        if (clearingSession) return
        query = value
        searchGeneration = searchGate.edit(value, android.os.SystemClock.elapsedRealtime())
        failures.keys.filterIsInstance<OperationId.Search>().forEach { failures.remove(it) }
        verifiedOperations.removeAll { it is OperationId.Search }
        idleJob?.cancel()
        idleJob = viewModelScope.launch { delay(1200); presentSearchVerification() }
        startSearch(400)
    }

    fun search() { searchGate.submit(); startSearch(0) }

    private fun startSearch(debounceMs: Long) {
        val generation = searchGeneration
        val operation = OperationId.Search(generation)
        searchJob?.cancel()
        otherError = null
        val term = query.trim()
        if (term.isEmpty()) {
            results = emptyList(); searched = false; searching = false
            return
        }
        results = emptyList()
        searched = false
        searching = true
        searchJob = viewModelScope.launch {
            try {
                delay(debounceMs)
                val found = repository.search(term)
                ensureActive()
                if (generation == searchGeneration) {
                    recentSearches = (listOf(term) + recentSearches).distinct().take(20)
                    libraryPreferences.edit { putString("recent", recentSearches.joinToString("\n")) }
                    results = found
                    searched = true
                    failures.remove(operation)
                    recordSourceHealth("Search succeeded")
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (generation == searchGeneration) fail(e, operation)
            } finally { if (generation == searchGeneration) searching = false }
        }
    }

    private fun presentSearchVerification() {
        if (verification == null && searchGate.shouldPresent(android.os.SystemClock.elapsedRealtime(), exploreVisible, foreground)) {
            val op = OperationId.Search(searchGeneration)
            failures[op]?.browserUrl?.let { openVerification(it, op) }
            searchGate.presented()
        }
    }

    fun open(anime: Anime) {
        detailJob?.cancel()
        sourceGeneration++
        sourceJob?.cancel()
        loadingSources = false
        sourceCache.clear()
        availableSources = emptyList()
        app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE).edit { putString(anime.session, anime.poster) }
        audio = defaultAudio
        leaveExplore()
        selectedAnime = anime
        details = null
        selectedEpisodes = emptySet()
        otherError = null
        detailJob = viewModelScope.launch {
            loadingDetails = true
            try { details = repository.detailsIncremental(anime) { page -> withContext(Dispatchers.Main) { details = page } }; failures.remove(OperationId.Details(anime.session)); updateSourceChoices()
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { fail(e, OperationId.Details(anime.session))
            } finally { loadingDetails = false }
        }
    }

    fun back() {
        detailJob?.cancel()
        sourceGeneration++
        sourceJob?.cancel()
        loadingSources = false
        selectedAnime = null
        details = null
        otherError = null
    }

    fun toggle(episode: Episode) {
        selectedEpisodes = if (episode.session in selectedEpisodes) selectedEpisodes - episode.session
            else selectedEpisodes + episode.session
        updateSourceChoices()
    }

    fun selectAll() {
        val ids = details?.episodes.orEmpty().map { it.session }.toSet()
        selectedEpisodes = if (selectedEpisodes == ids) emptySet() else ids
        updateSourceChoices()
    }

    fun clearEpisodes() { selectedEpisodes = emptySet(); updateSourceChoices() }
    fun selectEpisodeRange(start: String, end: String) {
        val first = start.toDoubleOrNull()
        val last = end.toDoubleOrNull()
        if (first == null || last == null || first > last) { otherError = "Enter a valid episode range."; return }
        selectedEpisodes = details?.episodes.orEmpty().filter { it.number.toDoubleOrNull()?.let { n -> n in first..last } == true }.map { it.session }.toSet()
        updateSourceChoices()
    }
    private var episodeStates = emptyMap<Pair<String, String>, DownloadRow>()
    fun episodeAvailability(episode: Episode): String? = episodeStates[selectedAnime?.session.orEmpty() to episode.session]?.status

    fun selectMissingEpisodes() {
        selectedEpisodes = details?.episodes.orEmpty().filter { episode ->
            downloads.none { it.showId == selectedAnime?.session && it.episodeId == episode.session && !it.failed && (quality == "Best" || it.quality == quality) && (audio == "Original" || it.audioCode == audio) }
        }.map { it.session }.toSet()
        updateSourceChoices()
    }

    fun chooseAudio(code: String) {
        audio = code
        if (quality !in availableQualities) quality = availableQualities.firstOrNull().orEmpty()
    }

    private fun updateSourceChoices() {
        val retried = verifiedOperations.any { it is OperationId.Sources && it.session == selectedAnime?.session }
        val generation = ++sourceGeneration
        selectedAnime?.let { if (retried) verifiedOperations += OperationId.Sources(it.session, generation) }
        sourceJob?.cancel()
        val data = details ?: return
        val episodes = data.episodes.filter { it.session in selectedEpisodes }.ifEmpty { data.episodes.take(1) }
        availableSources = emptyList()
        loadingSources = episodes.isNotEmpty()
        sourceChoicesLabel = "Loading available sources…"
        sourceJob = viewModelScope.launch {
            try {
                delay(200)
                var checked = 0
                val options = coroutineScope {
                    val iterator = episodes.iterator()
                    val found = mutableMapOf<String, List<EpisodeSource>>()
                    List(minOf(3, episodes.size)) { async {
                        while (iterator.hasNext()) {
                            val episode = iterator.next()
                            val sources = sourceCache[episode.session] ?: repository.sources(data.anime, episode)
                            ensureActive()
                            sourceCache[episode.session] = sources
                            found[episode.session] = sources
                            checked++
                            sourceChoicesLabel = "Checking sources $checked/${episodes.size}"
                        }
                    } }.awaitAll()
                    episodes.map { found.getValue(it.session) }
                }
                ensureActive()
                if (generation != sourceGeneration) return@launch
                failures.remove(OperationId.Sources(data.anime.session, generation))
                availableSources = commonSourceOptions(options)
                if (audio !in availableAudio) audio = availableAudio.firstOrNull { it == defaultAudio }
                    ?: availableSources.firstOrNull { it.preferred }?.audio ?: availableAudio.firstOrNull().orEmpty()
                if (quality !in availableQualities) quality = availableQualities.firstOrNull().orEmpty()
                sourceChoicesLabel = if (availableSources.isEmpty()) "No shared quality and audio options for these episodes. Select fewer episodes."
                    else if (selectedEpisodes.isEmpty()) "Available for episode ${episodes.first().number}. Select episodes to check their options."
                    else "Available for all ${episodes.size} selected episode${if (episodes.size == 1) "" else "s"}."
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { sourceChoicesLabel = "Could not load available sources."; fail(e, OperationId.Sources(data.anime.session, generation))
            } finally { if (generation == sourceGeneration) loadingSources = false }
        }
    }

    internal var pendingPreparation by mutableStateOf<PreparationRecord?>(null)
        private set
    fun downloadSelected() {
        val data = details ?: return
        val episodes = data.episodes.filter { it.session in selectedEpisodes }
        if (episodes.isEmpty() || resolving || pendingPreparation != null || loadingSources || availableSources.isEmpty()) return
        runPreparation(PreparationRecord(data.anime, episodes, quality, audio))
    }
    internal var preparationStatus by mutableStateOf(PreparationStatus())
        private set
    fun resumePreparation() { viewModelScope.launch {
        try { PreparationJobs.resume(app, PreparationJobs.ANIME) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { downloadNotice = "Could not schedule preparation. Reopen the app to retry." }
    } }
    fun discardPreparation() { viewModelScope.launch {
        try { PreparationJobs.discard(app, PreparationJobs.ANIME) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { downloadNotice = "Could not discard preparation. Try again." }
    } }
    fun verifyPreparation() {
        val url = preparationStatus.verificationUrl ?: return
        openVerification(url, pendingPreparation?.let { OperationId.Preparation(it.anime.session) })
    }
    private fun runPreparation(preparation: PreparationRecord) {
        if (resolving || pendingPreparation != null) return
        resolving = true
        viewModelScope.launch {
            try { PreparationJobs.submit(app, PreparationJobs.ANIME, preparation) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { resolving = false; downloadNotice = "Could not schedule preparation. Your selection is kept." }
        }
    }
    fun cancelPreparation() { viewModelScope.launch {
        try { PreparationJobs.pause(app, PreparationJobs.ANIME) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { downloadNotice = "Could not pause preparation. Try again." }
    } }
    private val retryingDownloads = mutableSetOf<String>()
    fun retryDownload(id: String) {
        if (!retryingDownloads.add(id)) return
        viewModelScope.launch {
            try { downloadRepository.retry(id); failures.remove(OperationId.Download(id)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e, OperationId.Download(id)); downloadNotice = "Retry failed: ${e.message}" }
            finally { retryingDownloads -= id }
            refreshDownloads()
        }
    }
    fun retryOfflineDownload(id: String) = retryDownload(id)
    fun retryExport(id: String) = runDownloadAction(id) { downloadRepository.retrySave(id); refreshDownloads() }
    fun pauseDownload(id: String) = runDownloadAction(id) { downloadRepository.pause(id) }
    fun resumeDownload(id: String) = runDownloadAction(id) { downloadRepository.resume(id) }
    private fun runDownloadAction(id: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(e, OperationId.Download(id)); downloadNotice = e.message ?: "Download action failed" }
        }
    }
    fun performSelectionAction(action: DownloadAction) {
        val eligible = downloads.filter { it.id in selectedDownloadIds && action in it.actions }
        eligible.forEach { row -> when (action) {
            DownloadAction.PAUSE -> pauseDownload(row.id)
            DownloadAction.RESUME -> resumeDownload(row.id)
            DownloadAction.RETRY -> retryDownload(row.id)
            DownloadAction.RETRY_SAVE -> retryExport(row.id)
            else -> Unit
        } }
        downloadNotice = "${eligible.size} eligible items: ${action.name.lowercase()} requested"
    }

    internal var storageReport by mutableStateOf<StorageReport?>(null)
        private set
    var sourceHealth by mutableStateOf(app.getSharedPreferences("source-health", Context.MODE_PRIVATE).getString("last", "No requests recorded").orEmpty())
        private set
    fun sourceHealthHistory(): String = app.getSharedPreferences("source-health", Context.MODE_PRIVATE).getString("history", sourceHealth).orEmpty()
    private fun recordSourceHealth(status: String) {
        sourceHealth = "$status · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}"
        val preferences = app.getSharedPreferences("source-health", Context.MODE_PRIVATE)
        val history = (listOf(sourceHealth) + preferences.getString("history", "").orEmpty().split("\n").filter { it.isNotBlank() }).take(20)
        preferences.edit { putString("last", sourceHealth); putString("history", history.joinToString("\n")) }
    }
    fun inspectLibrary() = runDownloadAction("library") {
        storageReport = inspectStorage(app)
        refreshDownloads()
    }
    fun prioritize(id: String) = runDownloadAction(id) {
        withContext(Dispatchers.IO) { QueueOrder.first(app, id, downloads.filter { !it.complete }.map { it.id }) }
        val eligible = downloads.filter { !it.complete && !it.id.startsWith("direct:") && !it.saving && !it.failed && !it.deleting }
        eligible.filter { it.id != id }.forEach { downloadRepository.pause(it.id) }
        if (eligible.any { it.id == id }) downloadRepository.resume(id)
        downloadNotice = "Priority saved. Other HLS transfers are paused; resume them when ready. Direct files are managed by Android."
    }
    fun forgetMissing(id: String) = runDownloadAction(id) {
        Exports.withItemLock(id) {
            val record = Exports.all(app)[id] ?: return@withItemLock
            require(record.uri.isNotBlank()) { "Only saved records can be removed without deleting media" }
            Exports.remove(app, id)
            DownloadStore.flush()
        }
        refreshDownloads()
    }
    fun relink(id: String, uri: Uri) = runDownloadAction(id) {
        app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val size = kotlinx.coroutines.runInterruptible(Dispatchers.IO) {
            val size = app.contentResolver.openFileDescriptor(uri, "r")?.use { require(it.statSize != 0L) { "Selected file is empty" }; it.statSize } ?: error("Cannot read selected video")
            val metadata = android.media.MediaMetadataRetriever()
            try { metadata.setDataSource(app, uri); require(metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") { "Selected file has no readable video track" } }
            finally { metadata.release() }
            size
        }
        Exports.withItemLock(id) {
            require(!Exports.deleting(id)) { "Deletion in progress" }
            val record = Exports.all(app)[id] ?: error("Download no longer exists")
            require(record.uri.isNotBlank()) { "Finish or cancel the transfer first" }
            Exports.put(app, id, record.copy(uri = uri.toString(), error = "", bytes = size.takeIf { it >= 0 } ?: record.bytes))
            DownloadStore.flush()
        }
        refreshDownloads()
    }
    fun retryFailedSaves() { downloads.filter { DownloadAction.RETRY_SAVE in it.actions }.forEach { retryExport(it.id) } }
    private fun relevant(op: OperationId): Boolean = when (op) {
        is OperationId.Search -> op.generation == searchGeneration && query.isNotBlank()
        is OperationId.Details -> selectedAnime?.session == op.session
        is OperationId.Sources -> selectedAnime?.session == op.session && sourceGeneration == op.generation
        is OperationId.Preparation -> pendingPreparation?.anime?.session == op.session
        is OperationId.Download -> downloads.any { it.id == op.id }
    }

    private fun currentOperation(): OperationId? = failures.keys.lastOrNull { relevant(it) && when (it) {
        is OperationId.Search -> false
        is OperationId.Download -> false
        else -> !siteDownloadSelected && !showDownloads && !showSettings
    } }
    private fun currentFailure() = currentOperation()?.let { failures[it] }

    private fun fail(e: Exception, operation: OperationId) {
        if (!relevant(operation)) return
        val challenge = e as? VerificationRequired
        val url = challenge?.url ?: (e as? HttpFailure)?.url
        val browser = challenge?.browserUrl ?: (e as? HttpFailure)?.browserUrl
        val repeated = operation in verifiedOperations
        failures[operation] = OperationFailure(
            if (challenge != null && repeated) "Verification finished, but the server still rejected the session. Use Verify or Retry to try again."
            else if (challenge != null) "Session verification needed" else e.message ?: "The request failed. Please try again.",
            url, browser, challenge != null)
        recordSourceHealth("${operation.javaClass.simpleName}: ${if (challenge != null) "Verification required" else if (e is HttpFailure) "HTTP ${e.status}" else "Request failed"}")
        DebugTrace.event("operation_failed", "type" to e.javaClass.simpleName, "url" to url?.let(DebugTrace::safeUrl))
        if (challenge != null) {
            if (operation is OperationId.Search && !repeated && exploreVisible && foreground) {
                searchGate.challenge(operation.generation)
                presentSearchVerification()
            } else if (operation !is OperationId.Download && operation !is OperationId.Search && operation !is OperationId.Preparation && !repeated && foreground && !siteDownloadSelected && verification == null) {
                openVerification(challenge.browserUrl, operation)
            }
        }
    }

    private fun openVerification(url: String, operation: OperationId? = null) {
        verificationHost = (url).toUri().host
        verificationOperations.clear()
        failures.filter { (op, failure) -> relevant(op) && failure.verificationNeeded &&
            failure.browserUrl?.toUri()?.host == verificationHost }.keys.forEach { verificationOperations += it }
        operation?.let { verificationOperations += it }
        verification = verificationPage(url)
    }
    fun verifySearch() { searchGate.suppress(); searchFailure?.browserUrl?.let { openVerification(it, OperationId.Search(searchGeneration)) } }
    fun verifyDownload(id: String) { failures[OperationId.Download(id)]?.browserUrl?.let { openVerification(it, OperationId.Download(id)) } }
    fun verifyManually() {
        if (exploreVisible && searchFailure?.verificationNeeded == true) verifySearch()
        else currentFailure()?.browserUrl?.let { openVerification(it, currentOperation()) }
            ?: openVerification(AnimeNetwork.HOST)
    }
    fun closeVerification(retry: Boolean) {
        verification = null
        CookieManager.getInstance().flush()
        if (retry) { sourceHealth = "Browser verification completed · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}. HTTP acceptance is checked on retry."; app.getSharedPreferences("source-health", Context.MODE_PRIVATE).edit { putString("last", sourceHealth) } }
        val operations = verificationOperations.filter(::relevant).toMutableSet()
        if (verificationOperations.any { it is OperationId.Search } && query.isNotBlank() && exploreVisible) operations += OperationId.Search(searchGeneration)
        verificationOperations.clear()
        if (retry) operations.forEach { op -> verifiedOperations += op; resumeOperation(op) }
    }
    private fun resumeOperation(op: OperationId) {
        if (!relevant(op)) return
        failures.remove(op)
        when (op) {
            is OperationId.Search -> startSearch(0)
            is OperationId.Details -> selectedAnime?.let(::open)
            is OperationId.Sources -> updateSourceChoices()
            is OperationId.Preparation -> resumePreparation()
            is OperationId.Download -> retryDownload(op.id)
        }
    }
    fun retrySearch() = resumeOperation(OperationId.Search(searchGeneration))
    fun retry() { currentOperation()?.let(::resumeOperation) }
    private fun clearFailure() { currentOperation()?.let { failures.remove(it) }; otherError = null }
    fun clearError() = clearFailure()

    private var refreshJob: Job? = null
    private fun refreshDownloads() {
        if (!foreground) { refreshAgain = true; return }
        if (refreshJob?.isActive == true) { refreshAgain = true; return }
        refreshJob = viewModelScope.launch {
            try {
            do {
                refreshAgain = false
                delay(if (showDownloads) 500 else 1500)
                if (!foreground) break
                downloadRepository.refresh()
                val updated = downloadRepository.rows.value
                if (downloads != updated) {
                    downloads = updated
                    episodeStates = updated.associateBy { it.showId to it.episodeId }
                }
                selectedDownloadIds = selectedDownloadIds.filter { it.startsWith("preparing:") || updated.any { row -> row.id == it } }.toSet()
                checkMediaVerification()
                loadMissingArtwork()
                refreshStorageUsage()
            } while (refreshAgain && foreground)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                downloadNotice = "Couldn’t refresh downloads. Try reopening Downloads."
                DebugTrace.event("downloads_refresh_failed", "reason" to e.toString())
            }
        }
    }
    var temporaryBytes by androidx.compose.runtime.mutableLongStateOf(0L)
        private set
    private var lastUsageCheck = 0L
    private suspend fun refreshStorageUsage() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastUsageCheck < 30_000) return
        lastUsageCheck = now
        temporaryBytes = withContext(Dispatchers.IO) {
            val root = app.getExternalFilesDir(null) ?: app.filesDir
            OfflineDownloads.cache.cacheSpace + listOf("direct-temp", "export-temp").sumOf { directory ->
                java.io.File(root, directory).walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        }
    }
    private var refreshAgain = false
    private fun checkMediaVerification() {
        val mediaEvents = app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).all.keys
        failures.keys.filterIsInstance<OperationId.Download>().filter { !relevant(it) || (failures[it]?.verificationNeeded == true && it.id !in mediaEvents) }.forEach { failures.remove(it) }
        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).all.values.forEach { value ->
            val event = runCatching { com.google.gson.Gson().fromJson(value.toString(), MediaVerification::class.java) }.getOrNull() ?: return@forEach
            val op = OperationId.Download(event.id)
            if (relevant(op) && failures[op] == null) fail(VerificationRequired(event.url, event.browserUrl), op)
        }
    }

    private val animeDownloadId = Regex("^[a-fA-F0-9-]{36}-.*")
    private fun loadMissingArtwork() {
        if (artworkJob?.isActive == true) return
        val artwork = app.getSharedPreferences("download-artwork", Context.MODE_PRIVATE)
        val missing = downloads.filter { row ->
            val poster = artwork.getString(row.id.take(36), null)
            row.thumbnail.isBlank() && poster.isNullOrEmpty() && row.id.take(36) !in attemptedArtwork && row.id.matches(animeDownloadId)
        }.distinctBy { it.id.take(36) }
        artworkJob = viewModelScope.launch {
            missing.forEach { row ->
                val session = row.id.take(36)
                attemptedArtwork += session
                try {
                    val poster = repository.poster(session)
                    if (poster.isNotBlank()) artwork.edit { putString(session, poster) }
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) { /* Existing downloads remain usable if artwork is unavailable. */ }
            }
        }
    }

    var selectionMode by mutableStateOf(false)
        private set
    var selectedDownloadIds by mutableStateOf(emptySet<String>())
        private set
    var deletingDownloads by mutableStateOf(false)
        private set
    fun enterSelection(id: String? = null) { selectionMode = true; if (id != null) selectedDownloadIds = selectedDownloadIds + id }
    fun exitSelection() { selectionMode = false; selectedDownloadIds = emptySet() }
    fun toggleDownload(id: String) { selectedDownloadIds = if (id in selectedDownloadIds) selectedDownloadIds - id else selectedDownloadIds + id }
    fun selectDownloads(ids: Set<String>) { selectionMode = true; selectedDownloadIds = ids }
    fun clearDownloadSelection() { selectedDownloadIds = emptySet() }
    fun removeDownload(id: String, deleteSavedFile: Boolean = downloads.any { it.id == id && it.complete }) =
        removeDownloads(setOf(id), if (deleteSavedFile) setOf(id) else emptySet())
    fun removeDownloads(ids: Set<String>, confirmedSaved: Set<String> = downloads.filter { it.id in ids && it.complete }.map { it.id }.toSet()) {
        if (deletingDownloads || ids.isEmpty()) return
        deletingDownloads = true
        viewModelScope.launch {
            val outcomes = withContext(kotlinx.coroutines.NonCancellable) { downloadRepository.deleteBatch(ids, confirmedSaved) }
            outcomes.filter { it.succeeded }.forEach { failures.remove(OperationId.Download(it.id)) }
            val failed = outcomes.filterNot { it.succeeded }.map { it.id }.toSet()
            selectedDownloadIds = failed
            if (failed.isEmpty()) selectionMode = false
            downloadNotice = "${outcomes.size - failed.size} deleted · ${failed.size} couldn’t be deleted"
            deletingDownloads = false
            refreshDownloads()
        }
    }
    private suspend fun recoverDeletions() {
        DownloadStore.operations("delete").filterValues { it.isBlank() }.keys.forEach { downloadRepository.delete(it) }
    }
    fun updateDefaultAudio(code: String) {
        settings.defaultAudio = code
        defaultAudio = code
        settingsMessage = "Default audio saved. Applies when opening a show; unavailable languages use the source default."
    }

    fun setDownloadConnections(count: Int) {
        settings.connections = count
        connections = settings.connections
        OfflineDownloads.setConnections(connections)
        settingsMessage = "Download speed setting saved."
    }

    fun updateSimultaneousDownloads(count: Int) {
        settings.simultaneousDownloads = count
        simultaneousDownloads = settings.simultaneousDownloads
        OfflineDownloads.setSimultaneousDownloads(simultaneousDownloads)
        settingsMessage = "Download limit saved."
    }

    fun saveOutputFolder(input: String): Boolean {
        val folder = normalizeOutputFolder(input) ?: return false
        settings.outputTree = ""
        outputTree = ""
        outputAccessGranted = false
        OfflineDownloads.manager(app).pauseDownloads()
        DownloadStore.operation("transfers", "folder-access", "1")
        settings.outputFolder = folder
        outputFolder = folder
        settingsMessage = "Output folder saved. Applies to new downloads."
        return true
    }

    fun chooseOutputTree(uri: Uri, onReady: () -> Unit = {}): Boolean {
        try {
            app.contentResolver.takePersistableUriPermission(uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            settings.outputTree = uri.toString()
            outputTree = uri.toString()
            outputAccessGranted = hasOutputFolderAccess(app)
            if (!outputAccessGranted) { settingsMessage = "Choose a folder with read and write access."; return false }
            checkingExistingFiles = true
            settingsMessage = "Checking existing videos before resuming downloads…"
            viewModelScope.launch {
                try {
                    reconcileExistingVideos(app)
                    settingsMessage = "Folder linked. Matching videos are reused; remaining downloads can continue."
                    onReady()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { settingsMessage = "Could not check this folder. Choose the original downloads folder again." }
                finally { checkingExistingFiles = false; refreshDownloads() }
            }
            return true
        } catch (_: Exception) {
            settingsMessage = "Cannot access that folder. Choose the folder containing your saved show folders."
            return false
        }
    }

    fun clearSessionData() {
        if (clearingSession || resolving || searching || loadingDetails || loadingSources || verification != null) return
        if (downloads.any { !it.complete && !it.failed } || retryingDownloads.isNotEmpty() || artworkJob?.isActive == true) {
            settingsMessage = "Pause or finish active downloads before clearing the session."; return
        }
        clearingSession = true
        settingsMessage = null
        AnimeNetwork.invalidateSession()
        android.webkit.WebStorage.getInstance().deleteAllData()
        android.webkit.WebView(app).apply { clearCache(true); clearHistory(); clearFormData(); destroy() }
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            verifiedOperations.clear()
            failures.clear()
            clearFailure()
            settingsMessage = "Session cleared. Verify again the next time the site asks."
            clearingSession = false
        }
    }
    // Main.immediate may run synchronously: initialize every state delegate before recovery.
    init {
        viewModelScope.launch {
            try {
            DownloadStore.awaitReady()
            withContext(Dispatchers.IO) { OfflineDownloads.prepareCache(app) }
            withContext(Dispatchers.IO) { reconcileDirectEnqueues(app) }
            val manager = OfflineDownloads.manager(app)
            val unfinished = withContext(Dispatchers.IO) { manager.downloadIndex.getDownloads(
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING, Download.STATE_FAILED
            ).use { it.moveToNext() } }
            if (unfinished && outputAccessGranted) runCatching { DownloadService.startForeground(app, OfflineDownloadService::class.java) }.onFailure {
                downloadNotice = "Downloads could not start. Reopen the app and tap Resume. ${it.javaClass.simpleName}"
            }
            DownloadStore.operations("service-timeout").values.firstOrNull()?.let { downloadNotice = it }
            withContext(Dispatchers.IO) { manager.downloadIndex.getDownloads(Download.STATE_COMPLETED).use { cursor ->
                while (cursor.moveToNext()) {
                    val request = cursor.download.request
                    if (!Exports.all(app).containsKey(request.id)) Exports.prepare(app, request.id, String(request.data))
                }
            }
            }
            recoverDeletions()
            Exports.start(app)
            refreshDownloads()
            checkMediaVerification()
            viewModelScope.launch { DownloadStore.revisions.collect {
                outputCheckRequired = !hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null
                if (foreground) savedFilesWatcher.updateRoots()
                refreshDownloads()
            } }
            viewModelScope.launch { DownloadStore.health.collect { failure ->
                if (failure != null) downloadNotice = "Download state could not be saved ($failure). Free storage and reopen the app; media is retained."
                refreshDownloads()
            } }
            viewModelScope.launch { Exports.updates.collect { refreshDownloads(); checkMediaVerification() } }
            viewModelScope.launch { OfflineDownloads.updates.collect { refreshDownloads(); checkMediaVerification() } }
            // System DownloadManager exposes no progress callbacks for direct MP4/WebM files.
            viewModelScope.launch {
                while (true) {
                    delay(1000)
                    if (foreground && withContext(Dispatchers.IO) { hasActiveDirectDownload() }) refreshDownloads()
                }
            }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                downloadNotice = "Your downloads couldn’t be loaded. Close and reopen the app to retry."
                DebugTrace.event("startup_failed", "reason" to e.toString())
            }
        }
    }
}
