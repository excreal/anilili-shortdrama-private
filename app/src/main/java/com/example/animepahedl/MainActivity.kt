@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.animepahedl

import com.example.animepahedl.components.*
import androidx.compose.animation.animateContentSize

import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeState
import androidx.core.net.toUri
import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import coil.imageLoader
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.example.animepahedl.ui.theme.AnimepaheDLTheme

class MainActivity : ComponentActivity() {
    private lateinit var model: DownloaderViewModel
    private lateinit var siteDownload: SiteDownloadViewModel
    override fun onResume() {
        super.onResume()
        if (::model.isInitialized) model.setForeground(true)
    }
    override fun onPause() {
        if (::model.isInitialized) model.setForeground(false)
        super.onPause()
    }

    companion object { const val ACTION_DOWNLOADS = "com.example.animepahedl.OPEN_DOWNLOADS" }

    override fun onStart() {
        super.onStart()
        com.anilili.playback.PlaybackService.allowMediaButtonResume()
    }
    override fun onStop() {
        com.anilili.playback.PlaybackService.pauseActivePlayback()
        super.onStop()
    }
    override fun onPictureInPictureModeChanged(active: Boolean, config: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(active, config)
        ReferencePlaybackRegistry.inPictureInPicture.value = active
    }
    private fun openFromIntent(intent: Intent?) {
        intent?.getStringExtra(com.anilili.ui.nav.Routes.EXTRA_ROUTE)?.let { ReferencePlaybackRegistry.pendingRoute.value = it }
        AnnouncementManager.openFromNotification(this, intent?.getStringExtra(AnnouncementManager.EXTRA_ANNOUNCEMENT_ID))
        if (intent?.action == ACTION_DOWNLOADS) {
            model.showDownloads = true
            model.showSettings = false
            model.showAbout = false
            intent.action = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.anilili.playback.ExternalPlaybackReturns.register(this)
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(ReferencePlaybackRegistry.ready, com.anilili.data.settings.SettingsStore.pictureInPicture) { ready, enabled -> ready && enabled }
                .collect { autoEnter ->
                    if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                        runCatching { setPictureInPictureParams(android.app.PictureInPictureParams.Builder()
                            .setAspectRatio(android.util.Rational(16, 9)).setAutoEnterEnabled(autoEnter).setSeamlessResizeEnabled(true).build()) }
                    }
                }
        }
        enableEdgeToEdge()
        // Request the fastest same-resolution mode up to 120 Hz; system power policy still applies.
        display?.let { screen ->
            val current = screen.mode
            screen.supportedModes.filter { it.physicalWidth == current.physicalWidth &&
                it.physicalHeight == current.physicalHeight && it.refreshRate <= 120.1f }
                .maxByOrNull { it.refreshRate }?.let { mode ->
                    window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
                }
        }
        model = ViewModelProvider(this)[DownloaderViewModel::class.java]
        siteDownload = ViewModelProvider(this)[SiteDownloadViewModel::class.java]
        openFromIntent(intent)
        val images = imageLoader
        setContent { AnimepaheDLTheme {
            var launching by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
            Box(Modifier.fillMaxSize()) {
                DownloaderApp(model, siteDownload, images)
                AnnouncementDialog()
                androidx.compose.animation.AnimatedVisibility(launching, exit = androidx.compose.animation.fadeOut()) {
                    AppLaunchSplash(onFinished = { launching = false })
                }
            }
        } }
    }
}

@Composable
private fun DownloaderApp(model: DownloaderViewModel, siteDownload: SiteDownloadViewModel, images: ImageLoader) {
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val chromeBackdrop = remember { HazeState() }
    val playbackScope = rememberCoroutineScope()
    var historyRevision by remember { mutableIntStateOf(0) }
    var openingPlayer by remember { mutableStateOf(false) }
    var playingSaved by remember { mutableStateOf<DownloadRow?>(null) }
    var playingRemote by remember { mutableStateOf<SiteDownloadSeries?>(null) }
    var referenceRequest by remember { mutableStateOf<ReferencePlayerRequest?>(null) }
    val referenceRoute by ReferencePlaybackRegistry.pendingRoute.collectAsState()
    LaunchedEffect(referenceRoute) {
        val route = referenceRoute ?: return@LaunchedEffect
        if (route.startsWith("reference-player/")) {
            val parts = route.split('/')
            val id = parts.getOrNull(1)?.toIntOrNull()
            val series = id?.let(ReferencePlaybackRegistry::series)
            if (series != null) {
                playingSaved = null; playingRemote = null
                referenceRequest = ReferencePlayerRequest(series,
                    series.episodes.firstOrNull { it.displayNumber == parts.getOrNull(2) }?.id,
                    ReferencePlaybackRegistry.isOffline(id))
            }
        }
        ReferencePlaybackRegistry.pendingRoute.value = null
    }
    var remoteEpisode by remember { mutableStateOf<String?>(null) }
    var errorDetails by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<String?>(null) }
    var confirmBulkDelete by remember { mutableStateOf(false) }
    var bulkMenu by remember { mutableStateOf(false) }
    var enqueueAfterFolder by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val displayedDownloads = remember(model.downloads, siteDownload.pending, siteDownload.preparationStatus) { siteDownload.libraryRows(model.downloads) }
    val outputPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val action = enqueueAfterFolder
        enqueueAfterFolder = null
        if (uri != null) model.chooseOutputTree(uri) {
            if (action == "sites") siteDownload.downloadSelected()
            if (action == "player") siteDownload.resumePreparation()
        }
    }
    fun chooseOutputFolder(action: String? = null) {
        enqueueAfterFolder = action
        val initial = model.outputTree.takeIf { it.isNotBlank() }?.toUri() ?: android.provider.DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents", "primary:Download/${model.outputFolder}")
        outputPicker.launch(initial)
    }
    val savedShows = remember(context) { SavedShowsStore(context) }
    val savedRevision by SavedShowsStore.changes.collectAsState()
    val history = remember(context) { PlaybackHistory(context) }
    LaunchedEffect(playingRemote, playingSaved) { playingRemote?.let(history::register); if (playingRemote == null && playingSaved == null) historyRevision++ }
    val continueWatching = remember(historyRevision) { history.recent() }
    LaunchedEffect(displayedDownloads) {
        if (model.selectionMode) {
            val kept = model.selectedDownloadIds.intersect(displayedDownloads.map { it.id }.toSet())
            if (kept != model.selectedDownloadIds) model.selectDownloads(kept)
        }
    }
    val catalog = remember { ViewModelProvider(context as ComponentActivity)[CatalogViewModel::class.java] }
    var showLinkDetails by remember { mutableStateOf(false) }
    var browsing by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    BackHandler(enabled = showLinkDetails && !model.showDownloads && !model.showSettings) { showLinkDetails = false }
    val screenState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    LaunchedEffect(Unit) { model.selectExploreSource(true) }
    BackHandler(enabled = model.selectionMode && model.showDownloads) { model.exitSelection() }
    BackHandler(enabled = !model.selectionMode && (model.showAbout || model.showSettings || model.showDownloads || showHistory)) {
        if (model.selectionMode && model.showDownloads) model.exitSelection() else if (model.showAbout) model.showAbout = false else if (model.showSettings) model.showSettings = false else if (model.showDownloads) model.showDownloads = false else if (showHistory) showHistory = false else model.back()
    }
    Scaffold(
        contentWindowInsets = if (!model.showDownloads && !model.showSettings && !model.showAbout && (!showLinkDetails || siteDownload.series != null) && !browsing && !showHistory) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        topBar = {
            if (model.showAbout || (showLinkDetails && siteDownload.series == null) || model.selectionMode) TopAppBar(
                expandedHeight = 56.dp,
                title = { if (!model.showDownloads && !model.showSettings && !model.showAbout && !showLinkDetails) {
                    Text(if (browsing) "Browse" else "DOWNLOADER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                } else Text(if (model.showDownloads && model.selectionMode) "${model.selectedDownloadIds.size} selected" else if (model.showAbout) "About" else if (model.showSettings) "Settings" else if (model.showDownloads) "Downloads" else if (showLinkDetails) "Details" else "Explore", maxLines = 1) },
                navigationIcon = {
                    if (model.showDownloads && model.selectionMode) IconButton(onClick = model::exitSelection) {
                        AppSymbol(AppIcon.Close, description = "Exit selection")
                    } else if (model.showAbout || (showLinkDetails && !model.showDownloads && !model.showSettings)) IconButton(onClick = {
                        if (model.showAbout) model.showAbout = false else showLinkDetails = false
                    }) { AppSymbol(AppIcon.Back, description = "Back") }
                },
                actions = {
                    if (model.showDownloads && model.selectionMode) {
                        IconButton(onClick = {
                            val all = displayedDownloads.map { it.id }.toSet()
                            if (model.selectedDownloadIds.containsAll(all)) model.clearDownloadSelection() else model.selectDownloads(all)
                        }) {
                            AppSymbol(AppIcon.Check, description = "Select all downloads")
                        }
                        IconButton(onClick = { confirmBulkDelete = true }, enabled = model.selectedDownloadIds.isNotEmpty() && !model.deletingDownloads) {
                            AppSymbol(AppIcon.Delete, description = "Delete selected downloads")
                        }
                        Box {
                            IconButton(onClick = { bulkMenu = true }) { AppSymbol(AppIcon.More, description = "More selection actions") }
                            DropdownMenu(bulkMenu, onDismissRequest = { bulkMenu = false }) {
                                val selected = displayedDownloads.filter { it.id in model.selectedDownloadIds }
                                listOf(DownloadAction.PAUSE to "Pause", DownloadAction.RESUME to "Resume",
                                    DownloadAction.RETRY to "Retry failed", DownloadAction.RETRY_SAVE to "Retry saves").forEach { (action, label) ->
                                    if (selected.any { action in it.actions }) DropdownMenuItem(text = { Text(label) }, onClick = {
                                        bulkMenu = false
                                        if (selected.any { siteDownload.isPendingRow(it.id) && action in it.actions }) {
                                            val id = selected.first { siteDownload.isPendingRow(it.id) }.id
                                            if (action == DownloadAction.PAUSE) siteDownload.pausePending(id)
                                            if (action == DownloadAction.RESUME) siteDownload.resumePending(id)
                                        }
                                        if (selected.any { !siteDownload.isPendingRow(it.id) && action in it.actions }) model.performSelectionAction(action)
                                    })
                                }
                                DropdownMenuItem(text = { Text("Clear selection") }, enabled = selected.isNotEmpty(), onClick = {
                                    bulkMenu = false; model.clearDownloadSelection()
                                })
                            }
                        }
                    } else if (showLinkDetails && siteDownload.series != null) {
                        val series = siteDownload.series!!
                        val selected = remember(savedRevision, series) { savedShows.contains(series) }
                        IconButton(onClick = { savedShows.toggle(series) }) { AppSymbol(if (selected) AppIcon.BookmarkFilled else AppIcon.Bookmark, description = if (selected) "Unsave show" else "Save show") }
                    } else if (model.showDownloads) {
                        IconButton(onClick = { model.enterSelection() }, enabled = displayedDownloads.isNotEmpty()) {
                            AppSymbol(AppIcon.Check, description = "Select downloads")
                        }
                    }
                }

            )
        },
        bottomBar = {
            Column {
            DownloaderNavigation(selected = if (model.showSettings) 4 else if (model.showDownloads) 2 else if (showHistory) 3 else if (browsing) 1 else 0, backdrop = chromeBackdrop) { tab ->
                model.showAbout = false; showLinkDetails = false
                model.showDownloads = tab == 2; model.showSettings = tab == 4; showHistory = tab == 3
                if (tab < 2) { browsing = tab == 1; if (tab == 0 && catalog.query.isNotBlank()) catalog.edit("") }
            }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).hazeSource(chromeBackdrop)) {
        val noticeHeight = 88.dp
        Column(Modifier.fillMaxSize()) {
        Column(Modifier.heightIn(max = noticeHeight).verticalScroll(rememberScrollState())) {
            if (model.downloadVerificationCount > 0) Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${model.downloadVerificationCount} download(s) need session verification", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { model.showAbout = false; model.showSettings = false; model.showDownloads = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_view)) }
                }
            }
            model.error?.let { message ->
                AppCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_request_couldn_t_finish), style = MaterialTheme.typography.titleSmall)
                        Text(message, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        FlowRow {
                            TextButton(onClick = model::retry) { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry)) }
                            TextButton(onClick = { errorDetails = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_details)) }
                            IconButton(onClick = model::clearError) { AppSymbol(AppIcon.Close, description = "Dismiss error") }
                        }
                    }
                }
            }
        }
            when {
                model.showAbout -> AboutScreen()
                model.showSettings -> SettingsScreen(model, onAbout = { model.showAbout = true }, siteDownload = siteDownload)
                model.showDownloads -> screenState.SaveableStateProvider("downloads") { DownloadsScreen(model, siteDownload, images, onChooseFolder = { chooseOutputFolder() }, onPlay = { row ->
                    playingSaved = row
                }, onRemove = { removeId = it }) }
                showHistory -> LibraryScreen(savedShows, continueWatching, images,
                    onDetails = { series -> showHistory = false; siteDownload.updateInput(series.url); siteDownload.inspect(); showLinkDetails = true },
                    onResume = { series -> remoteEpisode = null; playingRemote = series },
                    onRemoveFromContinueWatching = { series -> history.removeFromContinueWatching(series); historyRevision++ },
                    onMarkCompleted = { series -> history.markCompleted(series); historyRevision++ })
                !showLinkDetails -> CatalogScreen(catalog, images, browseMode = browsing, onExpand = { browsing = true }, onOpen = { url -> siteDownload.updateInput(url); siteDownload.inspect(); showLinkDetails = true }, onLink = { siteDownload.updateInput(""); showLinkDetails = true }, continueWatching = { ContinueWatchingRow(continueWatching, images, onOpen = { series -> remoteEpisode = null; playingRemote = series }, onRemove = { series -> history.removeFromContinueWatching(series); historyRevision++ }, onMarkCompleted = { series -> history.markCompleted(series); historyRevision++ }) }, backdrop = chromeBackdrop, onDownloads = { model.showDownloads = true })
                else -> screenState.SaveableStateProvider("sites") {
                    SiteDownloadScreen(siteDownload, model, images,
                        onBack = { showLinkDetails = false },
                        saved = remember(savedRevision, siteDownload.series) { siteDownload.series?.let(savedShows::contains) == true },
                        onToggleSave = { siteDownload.series?.let(savedShows::toggle) }, onWatch = { remoteEpisode = null; playingRemote = siteDownload.series }, onWatchEpisode = { id -> remoteEpisode = id; playingRemote = siteDownload.series }, onEnqueue = {
                        if (model.outputAccessGranted && !model.outputCheckRequired) { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS); true }
                        else { chooseOutputFolder("sites"); false }
                    }, onExploreMetadata = { kind, term ->
                        siteDownload.series?.let { catalog.explore(it.provider, kind, term) }
                        showLinkDetails = false
                        browsing = true
                    })
                }
            }
        }
    }
    }
    fun downloadFromPlayer(series: SiteDownloadSeries, episode: SiteDownloadEpisode) {
        if (model.downloads.any { it.complete && it.showId == "sites:${series.provider}:${series.id}" && it.episodeId == episode.id }) {
            playingSaved = null; playingRemote = null; referenceRequest = null; model.showDownloads = true
        } else if (siteDownload.downloadEpisode(series, episode)) {
            if (model.outputAccessGranted && !model.outputCheckRequired) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else chooseOutputFolder("player")
            android.widget.Toast.makeText(context, "Episode added to Downloads", android.widget.Toast.LENGTH_SHORT).show()
        } else android.widget.Toast.makeText(context, "A download batch is being prepared. Manage it in Downloads.", android.widget.Toast.LENGTH_LONG).show()
    }
    playingSaved?.let { initial ->
        val savedRecords = remember(model.downloads) { DownloadStore.all() }
        val playlist = remember(initial.id, model.downloads, savedRecords) { savedDownloadPlaylist(initial, model.downloads, savedRecords) }
        val series = remember(playlist) { savedSeriesFromDownloads(playlist) }
        val fallbackSeries = remember(playlist, series) {
            if (series == null) savedLandscapeFallbackFromDownloads(playlist, savedRecords) else null
        }
        val landscapeSeries = series?.takeIf { it.provider.usesReferencePlayer } ?: fallbackSeries
        if (landscapeSeries != null) {
            ReferenceVideoPlayer(landscapeSeries, "Best", onClose = { playingSaved = null; historyRevision++ },
                initialId = if (fallbackSeries != null) initial.id else initial.episodeId,
                downloadedOnly = true, offlineRows = playlist.takeIf { fallbackSeries != null }, onDownload = ::downloadFromPlayer)
        } else androidx.compose.ui.window.Dialog(onDismissRequest = { playingSaved = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            ShortDramaPlayer(playlist, initial.id, onClose = { playingSaved = null }, onExternal = { row ->
                playingSaved = null
                playbackScope.launch { openSavedVideo(context, row)?.let { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show() } }
            })
        }
    }
    playingRemote?.let { series ->
        RemoteDramaPlayer(series, siteDownload.quality, onClose = { playingRemote = null; historyRevision++ }, startEpisodeId = remoteEpisode, onDownload = ::downloadFromPlayer)
    }
    referenceRequest?.let { request -> ReferenceVideoPlayer(request.series, "Best", onClose = { referenceRequest = null; historyRevision++ },
        initialId = request.episodeId, downloadedOnly = request.offline, onDownload = ::downloadFromPlayer) }
    if (confirmBulkDelete) {
        val selected = displayedDownloads.filter { it.id in model.selectedDownloadIds }
        AlertDialog(onDismissRequest = { confirmBulkDelete = false }, title = { Text("Delete ${selected.size} items?") },
            text = { Text("This deletes ${selected.count { it.complete }} saved videos and cancels ${selected.count { !it.complete }} unfinished downloads.") },
            confirmButton = { TextButton(onClick = { confirmBulkDelete = false; siteDownload.removePending(selected.filter { siteDownload.isPendingRow(it.id) }.map { it.id }.toSet()); model.removeDownloads(selected.filterNot { siteDownload.isPendingRow(it.id) }.map { it.id }.toSet(), selected.filter { it.complete }.map { it.id }.toSet()) }, enabled = selected.isNotEmpty()) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmBulkDelete = false }) { Text("Keep") } })
    }
    if (errorDetails) AlertDialog(onDismissRequest = { errorDetails = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_request_details)) },
        text = { Text(model.error.orEmpty(), Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { errorDetails = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close)) } })
    model.verification?.let { url -> SessionBrowser(url, onClose = model::closeVerification) }
    removeId?.let { id ->
        val row = displayedDownloads.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { removeId = null },
            title = { Text(if (row?.complete == true) "Delete saved video?" else "Cancel download?") },
            text = { Text("${row?.title.orEmpty()}\n\n${if (row?.complete == true) "This permanently deletes the saved video." else "This cancels the download and removes its partial files."}") },
            confirmButton = { TextButton(onClick = { if (siteDownload.isPendingRow(id)) siteDownload.removePending(setOf(id)) else model.removeDownload(id, deleteSavedFile = row?.complete == true); removeId = null }) {
                Text(if (row?.complete == true) "Delete" else "Cancel download")
            } },
            dismissButton = { TextButton(onClick = { removeId = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_keep)) } })
    }
}

private data class ReferencePlayerRequest(val series: SiteDownloadSeries, val episodeId: String?, val offline: Boolean)
