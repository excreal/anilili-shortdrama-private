@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.example.animepahedl

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.anilili.data.library.EpisodeResume
import com.anilili.data.library.HistoryEntry
import com.anilili.data.model.EpisodeItem
import com.anilili.data.model.StreamItem
import com.anilili.data.model.SubtitleItem
import com.anilili.data.settings.SettingsStore
import com.anilili.errors.AnililiError
import com.anilili.errors.AnililiFailure
import com.anilili.errors.toFailure
import com.anilili.errors.asFailure
import com.anilili.playback.ExternalPlaybackSession
import com.anilili.playback.PlaybackService
import com.anilili.ui.adaptive.LocalAppDeviceProfile
import com.anilili.ui.adaptive.rememberAppDeviceProfile
import com.anilili.ui.components.ErrorBox
import com.anilili.ui.components.LoadingBox
import com.anilili.ui.theme.LocalAppColors
import com.anilili.ui.theme.ThemeSpec
import com.anilili.ui.theme.appColors
import com.anilili.ui.theme.wallpaperSeed
import com.anilili.ui.watch.*
import kotlinx.coroutines.*

private data class ReferenceResolved(val episode: SiteDownloadEpisode, val streams: List<StreamItem>,
    val subtitles: List<MediaSubtitle>, val local: Boolean, val localAvailable: Boolean)

/** Backend/window adapter only. PlayerSurface and its controls/sheets are the original source. */
@Composable
internal fun ReferenceVideoPlayer(series: SiteDownloadSeries, quality: String, onClose: () -> Unit,
    initialId: String? = null, downloadedOnly: Boolean = false,
    offlineRows: List<DownloadRow>? = null,
    onDownload: (SiteDownloadSeries, SiteDownloadEpisode) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val profile = rememberAppDeviceProfile()
    val landscapeOnlyHost = series.provider.usesReferencePlayer
    // Legacy offline queues can be reconstructed as DIRECT series whose episode URLs
    // are persisted content:// or file:// URIs. Library resume must play those URIs
    // locally instead of sending them through the online source resolver.
    val localOnlySeries = downloadedOnly || (series.provider == SiteDownloadProvider.DIRECT &&
        series.episodes.isNotEmpty() && series.episodes.all { isLocalPlaybackUri(it.url) })
    val history = remember(context) { PlaybackHistory(context) }
    val registryId = remember(series.provider, series.id, localOnlySeries) { ReferencePlaybackRegistry.register(series, localOnlySeries) }
    val cookies = remember(context) { SiteDownloadCookieStore(context) }
    val repository = remember(cookies) { SiteDownloadRepository(cookieHeader = cookies::headerFor) }
    val savedShows = remember(context) { SavedShowsStore(context) }
    val savedRevision by SavedShowsStore.changes.collectAsState()
    val isSaved = remember(savedRevision, series) { savedShows.contains(series) }
    var historyRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("playback-history", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> historyRevision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val inPip by ReferencePlaybackRegistry.inPictureInPicture.collectAsState()
    var index by rememberSaveable(series.provider.name, series.id) { mutableIntStateOf(
        series.episodes.indexOfFirst { it.id == initialId }.takeIf { initialId != null && it >= 0 }
            ?: series.episodes.indexOfFirst { history.matches(history.lastEpisode("series:${series.provider}:${series.id}"), it.url) }.coerceAtLeast(0)) }
    var fullscreen by rememberSaveable(series.provider.name, series.id) {
        mutableStateOf(ReferencePlayerHostPolicy.startsFullscreen(series.provider, profile.isTv))
    }
    val playerFullscreen = landscapeOnlyHost || fullscreen
    var controlsVisible by remember { mutableStateOf(true) }
    var attempt by remember { mutableIntStateOf(0) }
    var autoAdvanced by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf<ReferenceResolved?>(null) }
    var failure by remember { mutableStateOf<AnililiFailure?>(null) }
    val onlineOnly = remember(series.id) { mutableSetOf<String>() }
    val refreshedLinks = remember(series.provider, series.id) { MediaLinkRefreshGuard() }
    val seriesKey = "series:${series.provider}:${series.id}"
    val current = series.episodes.getOrNull(index) ?: return
    var nartoStreamFallbackIndex by remember(current.id, attempt) { mutableIntStateOf(0) }
    val colors = remember { appColors(ThemeSpec(), wallpaperSeed(context)) }

    fun close() {
        ReferencePlaybackRegistry.capture()
        PlaybackService.pauseActivePlayback()
        onClose()
    }
    fun select(target: Int, automatic: Boolean = false) {
        if (target !in series.episodes.indices) return
        ReferencePlaybackRegistry.capture()
        PlaybackService.stopActivePlayback()
        autoAdvanced = automatic
        index = target
        ReferencePlaybackRegistry.beginEpisode(registryId, series.episodes[target])
        attempt++
    }
    BackHandler {
        if (ReferencePlayerHostPolicy.closesOnBack(series.provider, fullscreen, profile.isTv, inPip)) close()
        else fullscreen = false
    }
    DisposableEffect(playerFullscreen, profile.isTv) {
        if (activity != null && ReferencePlayerHostPolicy.forcesLandscape(series.provider, playerFullscreen, profile.isTv)) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (activity != null && !profile.isTv) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            if (activity != null) {
                if (!profile.isTv) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    DisposableEffect(landscapeOnlyHost) {
        onDispose {
            if (landscapeOnlyHost) {
                ReferencePlaybackRegistry.capture()
                PlaybackService.pauseActivePlayback()
            }
        }
    }
    PlayerSystemBarsEffect(activity, playerFullscreen, controlsVisible, profile.isTv)
    DisposableEffect(Unit) { onDispose { ReferencePlaybackRegistry.ready.value = false } }
    LaunchedEffect(series, localOnlySeries) { if (localOnlySeries) history.registerLocal(series) else history.register(series) }
    LaunchedEffect(current.id, attempt) {
        ReferencePlaybackRegistry.beginEpisode(registryId, current)
        loaded = null; failure = null; ReferencePlaybackRegistry.ready.value = false
        try {
            com.anilili.data.AppGraph.awaitReady()
            DownloadStore.awaitReady()
            val result = withContext(Dispatchers.IO) {
                val records = DownloadStore.all()
                val savedRow = offlineRows?.firstOrNull { it.id == current.id }
                val localRecord = if (offlineRows != null) records[current.id] else records.values.firstOrNull { record ->
                    val identity = record.identity
                    identity?.isSiteDownload == true && record.uri.isNotBlank() &&
                        identity.anime.session == "sites:${series.provider}:${series.id}" && identity.episode.session == current.id &&
                        runCatching { context.contentResolver.openAssetFileDescriptor(record.uri.toUri(), "r")?.use { it.length != 0L } == true }.getOrDefault(false)
                }
                if (offlineRows != null) {
                    val uri = savedRow?.savedUri?.takeIf(String::isNotBlank) ?: localRecord?.uri?.takeIf(String::isNotBlank)
                    check(!uri.isNullOrBlank()) { "This saved video is missing. Relink it in Downloads." }
                    val tracks = localRecord?.media?.subtitles.orEmpty().filter { track ->
                        if (!track.url.startsWith("file:")) true else java.io.File(track.url.toUri().path.orEmpty()).isFile
                    }
                    ReferenceResolved(current, listOf(StreamItem(uri, "mp4", savedRow?.quality ?: localRecord?.quality,
                        savedRow?.audioCode ?: localRecord?.audio, null, true, null, null)), tracks, true, true)
                } else if (localOnlySeries) {
                    check(isLocalPlaybackUri(current.url)) { "This saved video is missing. Relink it in Downloads." }
                    val uri = current.url.toUri()
                    check(runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length != 0L } == true }.getOrDefault(false) ||
                        (uri.scheme == "file" && java.io.File(uri.path.orEmpty()).isFile)) {
                        "This saved video is missing. Relink it in Downloads."
                    }
                    val matchingRecord = records.values.firstOrNull { it.uri == current.url }
                    val tracks = matchingRecord?.media?.subtitles.orEmpty().filter { track ->
                        if (!track.url.startsWith("file:")) true else java.io.File(track.url.toUri().path.orEmpty()).isFile
                    }
                    ReferenceResolved(current, listOf(StreamItem(current.url, "mp4", matchingRecord?.quality,
                        matchingRecord?.audio, null, true, null, null)), tracks, true, true)
                } else if (localRecord != null && current.id !in onlineOnly) {
                    val tracks = localRecord.media?.subtitles.orEmpty()
                    ReferenceResolved(current, listOf(StreamItem(localRecord.uri, "mp4", localRecord.quality, localRecord.audio,
                        null, true, null, null)), tracks.filter { track ->
                        if (!track.url.startsWith("file:")) true else java.io.File(track.url.toUri().path.orEmpty()).isFile
                    }, true, true)
                } else {
                    check(!localOnlySeries) { "This saved video is missing. Relink it in Downloads." }
                    val options = repository.resolveChoices(current.url, cookies.headerFor(current.url), includeSubtitles = false).map(cookies::applyTo)
                    check(options.isNotEmpty()) { "This episode has no playable video." }
                    val streams = options.map { media ->
                        val height = media.quality.removeSuffix("p").toIntOrNull()
                        StreamItem(media.url, if (media.isHls()) "hls" else if (media.mimeType == "application/dash+xml") "dash" else "mp4",
                            media.quality, media.audio, media.headers.entries.firstOrNull { it.key.equals("Referer", true) }?.value,
                            true, null, height, headers = media.headers)
                    }.sortedByDescending { it.height ?: 0 }
                    val preferred = streams.firstOrNull { it.quality == quality } ?: streams.first()
                    ReferenceResolved(current, listOf(preferred) + streams.filterNot { it.url == preferred.url },
                        options.flatMap { it.subtitles.orEmpty() }.distinctBy { it.url }, false, localRecord != null)
                }
            }
            result.streams.forEach { ReferencePlaybackRegistry.bind(registryId, current, it.url) }
            if (result.local) fullscreen = true
            loaded = result
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (!localOnlySeries && refreshedLinks.shouldRefresh(series.provider, current.id)) {
                attempt++
            } else failure = error.toFailure(AnililiError.SOURCE_RESOLVE_FAILED)
        }
    }
    val result = loaded?.takeIf { it.episode.id == current.id }
    LaunchedEffect(current.id, attempt, result?.episode?.id) {
        val ready = result ?: return@LaunchedEffect
        if (series.provider != SiteDownloadProvider.KISSKH || ready.subtitles.isNotEmpty()) return@LaunchedEffect
        val episodeId = current.id
        val resolvingAttempt = attempt
        val tracks = try {
            withContext(Dispatchers.IO) { KisskhApi(cookies = cookies::headerFor).playbackSubtitles(episodeId) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { emptyList() }
        if (tracks.isEmpty()) return@LaunchedEffect
        val latest = loaded
        if (series.episodes.getOrNull(index)?.id != episodeId || attempt != resolvingAttempt || latest?.episode?.id != episodeId) return@LaunchedEffect
        loaded = latest.copy(subtitles = tracks)
        if (ready.local) withContext(Dispatchers.IO) {
            val rows = DownloadStore.all()
            val entry = rows.entries.firstOrNull { it.value.uri == ready.streams.firstOrNull()?.url }
            if (entry != null) {
                val record = entry.value
                DownloadStore.put(entry.key, record.copy(media =
                    (record.media ?: ResolvedMedia(record.uri, record.quality ?: "Original", "und")).copy(subtitles = tracks)))
            }
        }
    }
    DisposableEffect(result?.subtitles) {
        val tracks = result?.subtitles.orEmpty(); ProviderSubtitles.hold(tracks)
        onDispose { ProviderSubtitles.release(tracks) }
    }
    fun persist(url: String, position: Long, duration: Long, completed: Boolean = false) {
        ReferencePlaybackRegistry.progressForStream(url, position, duration, completed)
    }
    val activeStream = result?.streams?.getOrNull(nartoStreamFallbackIndex) ?: result?.streams?.firstOrNull()
    fun playbackFailure(error: AnililiFailure, url: String, position: Long) {
        if (position > 0) persist(url, position, history.duration(current.url))
        ReferencePlaybackRegistry.capture()
        PlaybackService.stopActivePlayback()
        if (series.provider == SiteDownloadProvider.NARTO && result != null) {
            val failedIndex = result.streams.indexOfFirst { it.url == url }
                .takeIf { it >= 0 } ?: nartoStreamFallbackIndex
            val nextIndex = failedIndex + 1
            if (nextIndex < result.streams.size) {
                ReferencePlaybackRegistry.ready.value = false
                failure = null
                nartoStreamFallbackIndex = nextIndex
                return
            }
        }
        when {
            result?.local == true && !localOnlySeries && onlineOnly.add(current.id) -> attempt++
            !localOnlySeries && refreshedLinks.shouldRefresh(series.provider, current.id) -> { failure = null; attempt++ }
            else -> failure = error
        }
    }
    val snapshots = remember(historyRevision, series) { series.episodes.associate { episode ->
        episode.displayNumber to EpisodeResume(history.position(episode.url), history.duration(episode.url))
    } }
    val historyEntry = HistoryEntry(registryId, series.title, series.poster, current.displayNumber.toDoubleOrNull() ?: current.number.toDouble(),
        provider = series.provider.name, category = "sub", positionMs = history.position(current.url), durationMs = history.duration(current.url),
        watchedEpisodes = series.episodes.filter { history.watched(it.url) }.mapNotNull { it.displayNumber.toDoubleOrNull() }.toSet(), resumePoints = snapshots)
    val episodeItems = series.episodes.map { episode -> EpisodeItem(episode.id, episode.displayNumber.toDoubleOrNull() ?: episode.number.toDouble(),
        episode.title.takeIf { it.isNotBlank() }, series.poster, false) }
    val downloadAction = PlayerDownloadAction(result?.localAvailable == true) { onDownload(series, current) }
    CompositionLocalProvider(LocalAppDeviceProfile provides profile, LocalAppColors provides colors,
        LocalPlayerInPictureInPicture provides inPip, LocalPlayerDownload provides downloadAction) {
        MaterialTheme(colorScheme = colors.scheme) {
            Column(Modifier.fillMaxSize().background(Color.Black)) {
                if (!playerFullscreen && !inPip) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = ::close) { AppSymbol(AppIcon.Back, description = "Back") }
                        Text(series.title, Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { savedShows.toggle(series) }) {
                            AppSymbol(if (isSaved) AppIcon.BookmarkFilled else AppIcon.Bookmark, description = "Save show")
                        }
                    }
                }
                val pane = if (playerFullscreen || inPip) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                Box(pane.background(Color.Black)) {
                    when {
                        failure != null -> ErrorBox(failure!!, onRetry = { failure = null; attempt++ })
                        result == null -> LoadingBox()
                        else -> key(current.id, attempt, nartoStreamFallbackIndex) {
                            PlayerSurface(
                                stream = activeStream ?: result.streams.first(), qualityStreams = result.streams,
                                subtitles = result.subtitles.map { SubtitleItem(it.url, it.label, it.language, it.mimeType) },
                                skip = null, seriesTitle = series.title,
                                episodeTitle = if (series.isSingleVideo) series.title else "Episode ${current.displayNumber}",
                                pauseDescription = series.description, artworkUrl = series.poster, animeId = registryId,
                                provider = series.provider.name, category = "sub", episode = current.displayNumber,
                                contentScaleOverride = if (series.provider == SiteDownloadProvider.KISSKH)
                                    com.anilili.data.settings.PlayerContentScale.FIT else null,
                                autoAdvanced = autoAdvanced,
                                onEnded = { activeStream?.let { persist(it.url, history.duration(current.url), history.duration(current.url), true) } },
                                onNextEpisode = { select(index + 1) }, onPreviousEpisode = { select(index - 1) },
                                onAutoAdvance = { select(index + 1, true) }, onError = ::playbackFailure,
                                onStalled = { url, position -> playbackFailure(AnililiError.PLAYBACK_SOURCE_UNREACHABLE.asPlayerFailure(), url, position) },
                                modifier = Modifier.fillMaxSize(), onToggleFullscreen = {
                                    if (ReferencePlayerHostPolicy.allowsFullscreenToggle(series.provider)) fullscreen = !fullscreen
                                    else close()
                                },
                                startPositionMs = history.position(current.url),
                                onProgress = { url, position, duration -> persist(url, position, duration) },
                                onFinalProgress = { url, position, duration -> ReferencePlaybackRegistry.progressForStream(url, position, duration, force = true) },
                                onControlsVisibilityChanged = { controlsVisible = it },
                                onPlaybackActiveChanged = { ReferencePlaybackRegistry.ready.value = it },
                                hasNextEpisode = index + 1 < series.episodes.size, hasPreviousEpisode = index > 0,
                                isFullscreen = playerFullscreen, episodes = episodeItems, currentIndex = index, historyEntry = historyEntry,
                                onSelectEpisode = { select(it) }, onAddToList = { savedShows.toggle(series) },
                                externalPlaybackSession = { duration -> ExternalPlaybackSession(registryId,
                                    current.displayNumber.toDoubleOrNull() ?: current.number.toDouble(), duration, series.episodes.size,
                                    series.episodes.getOrNull(index + 1)?.displayNumber?.toDoubleOrNull()) },
                                sourceOptions = if (!localOnlySeries && result.localAvailable) listOf(
                                    PlayerSourceOption("local", "Downloaded video", null, result.local, true, { onlineOnly.remove(current.id); ReferencePlaybackRegistry.capture(); PlaybackService.stopActivePlayback(); attempt++ }),
                                    PlayerSourceOption("online", series.provider.name, "Online video", !result.local, true, { onlineOnly.add(current.id); ReferencePlaybackRegistry.capture(); PlaybackService.stopActivePlayback(); attempt++ })
                                ) else emptyList(),
                                allowStallFailover = !localOnlySeries,
                            )
                        }
                    }
                }
                if (!playerFullscreen && !inPip) Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (series.isSingleVideo) series.title else "Episode ${current.displayNumber}", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { fullscreen = true }) { Text("Fullscreen") }
                        OutlinedButton(onClick = downloadAction.onClick) { Text(if (downloadAction.downloaded) "Downloaded" else "Download") }
                    }
                    if (series.description.isNotBlank()) Text(series.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

internal fun isLocalPlaybackUri(value: String): Boolean =
    value.startsWith("content:", ignoreCase = true) || value.startsWith("file:", ignoreCase = true)

private fun AnililiError.asPlayerFailure(): AnililiFailure = this.asFailure()
