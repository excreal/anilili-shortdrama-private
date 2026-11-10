@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.animepahedl.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import com.anilili.ui.theme.LocalAppColors
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.animepahedl.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal data class DramaPlayerEpisode(val id: String, val historyKey: String, val label: String)

/** Shared online/offline presentation, with one bounded decoder and no neighbor preparation. */
@Composable
internal fun DramaPlayerSurface(
    title: String,
    seriesKey: String,
    episodes: List<DramaPlayerEpisode>,
    initialId: String?,
    onClose: () -> Unit,
    resolve: suspend (Int) -> MediaSource,
    external: ((Int) -> Unit)? = null,
    saved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    onPlaybackFailure: ((Int) -> Boolean)? = null,
) {
    if (episodes.isEmpty()) { LaunchedEffect(Unit) { onClose() }; return }
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val playerColors = LocalAppColors.current
    val history = remember(context) { PlaybackHistory(context) }
    val playbackPreferences = remember(context) { context.getSharedPreferences("app-settings", android.content.Context.MODE_PRIVATE) }
    val playOnOpen = remember { playbackPreferences.getBoolean("play-on-open", true) }
    val autoNext = remember { playbackPreferences.getBoolean("auto-next", true) }
    val initial = remember(seriesKey, initialId) {
        episodes.indexOfFirst { it.id == initialId }.takeIf { initialId != null && it >= 0 }
            ?: episodes.indexOfFirst { history.matches(history.lastEpisode(seriesKey), it.historyKey) }.coerceAtLeast(0)
    }
    val pager = rememberPagerState(initialPage = initial, pageCount = { episodes.size })
    val scope = rememberCoroutineScope()
    val latestResolve by rememberUpdatedState(resolve)
    val latestClose by rememberUpdatedState(onClose)
    val latestPlaybackFailure by rememberUpdatedState(onPlaybackFailure)
    val latestEpisodes by rememberUpdatedState(episodes)
    var retry by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var chooser by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var scrubbing by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    var range by remember { mutableIntStateOf(initial / 25) }
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var resumeAfterPause by remember { mutableStateOf(true) }
    var loadedReady by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf<DramaPlayerEpisode?>(null) }
    val creation = remember(context.applicationContext) { runCatching {
        ExoPlayer.Builder(context.applicationContext).setLoadControl(DefaultLoadControl.Builder()
            .setBufferDurationsMs(5_000, 15_000, 500, 1_000).setTargetBufferBytes(16 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false).build()).build().apply {
                setAudioAttributes(AudioAttributes.DEFAULT, true)
                setHandleAudioBecomingNoisy(true)
            }
    } }
    val player = creation.getOrNull()
    fun persist(completed: Boolean = player?.playbackState == Player.STATE_ENDED) {
        if (!loadedReady) return
        loaded?.let { history.save(seriesKey, it.historyKey, player?.currentPosition ?: 0L, completed, player?.duration ?: 0L) }
    }
    fun close() { persist(); latestClose() }
    BackHandler(onBack = ::close)

    DisposableEffect(player) {
        onDispose { persist(); player?.release() }
    }
    DisposableEffect(player, owner) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (!isPlaying) persist()
            }
            override fun onPlaybackStateChanged(state: Int) {
                loading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) loadedReady = true
                if (state == Player.STATE_ENDED) {
                    persist(true)
                    val next = pager.settledPage + 1
                    if (next < latestEpisodes.size && foreground && autoNext) scope.launch { pager.animateScrollToPage(next) }
                }
            }
            override fun onPlayerError(failure: PlaybackException) {
                if (latestPlaybackFailure?.invoke(pager.settledPage) == true) {
                    persist()
                    retry++
                    return
                }
                loading = false
                error = when (failure.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "Access to this video was lost. Relink its folder or try another player."
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "This video is missing. Refresh Downloads or relink its file."
                    else -> "Playback failed. Retry to reload this episode."
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { foreground = true; if (resumeAfterPause && error == null) player?.play() }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (foreground) resumeAfterPause = player?.playWhenReady == true
                    foreground = false; persist(); player?.pause()
                }
                else -> Unit
            }
        }
        player?.addListener(listener)
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); player?.removeListener(listener) }
    }
    LaunchedEffect(player, seriesKey) {
        while (true) { delay(5_000); if (player?.playbackState == Player.STATE_READY && loaded != null) persist() }
    }
    LaunchedEffect(player, seriesKey, retry) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collectLatest { page ->
            val episode = episodes.getOrNull(page) ?: return@collectLatest
            persist()
            loaded = null
            loadedReady = false
            player?.stop(); player?.clearMediaItems()
            error = creation.exceptionOrNull()?.let { "The video player could not start. Close and try again." }
            loading = player != null
            if (player != null) try {
                val source = latestResolve(page)
                loaded = episode
                player.setMediaSource(source, history.position(episode.historyKey))
                player.prepare()
                player.playWhenReady = foreground && playOnOpen
                resumeAfterPause = playOnOpen
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) {
                if (latestPlaybackFailure?.invoke(page) == true) {
                    retry++
                    return@collectLatest
                }
                loading = false
                error = failure.message?.takeIf(String::isNotBlank) ?: "Could not prepare this episode."
            }
        }
    }
    LaunchedEffect(playing, chooser, controlsVisible, scrubbing, interaction, foreground, loading, error) {
        if (loading || error != null) controlsVisible = true
        else if (!chooser && !scrubbing && controlsVisible && foreground) {
            delay(3_000)
            controlsVisible = false
        }
    }
    LaunchedEffect(pager.settledPage) { controlsVisible = true; interaction++ }
    val current = episodes[pager.settledPage.coerceIn(episodes.indices)]
    CompositionLocalProvider(LocalAppColors provides playerColors) {
    MaterialTheme(colorScheme = playerColors.scheme) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(pager, modifier = Modifier.fillMaxSize(), key = { episodes[it].id }, beyondViewportPageCount = 0) { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (page == pager.settledPage) {
                    if (player != null) AndroidView(
                        factory = { PlayerView(it).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; setKeepContentOnPlayerReset(false) } },
                        modifier = Modifier.fillMaxSize(),
                        update = { it.player = player; it.keepScreenOn = foreground && playing },
                        onRelease = { it.player = null; it.keepScreenOn = false },
                    )
                    Box(Modifier.fillMaxSize().clickable(enabled = error == null && !loading && player != null,
                        onClickLabel = if (!controlsVisible) "Show player controls" else if (playing) "Pause video" else "Play video") {
                        interaction++
                        if (!controlsVisible) controlsVisible = true
                        else if (player?.isPlaying == true) player.pause() else player?.play()
                    })
                    if (loading && error == null) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    else if (!playing && error == null) FilledIconButton(onClick = {
                        interaction++
                        controlsVisible = true
                        if (player?.playbackState == Player.STATE_ENDED) player.seekTo(0)
                        player?.play()
                    }, modifier = Modifier.size(56.dp), colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = playerColors.playerAccent, contentColor = playerColors.onPlayerAccent,
                    )) { AppSymbol(AppIcon.Play, Modifier.size(28.dp), "Play video") }
                    error?.let { message ->
                        Surface(Modifier.padding(24.dp), color = Color.Black.copy(alpha = .85f), shape = MaterialTheme.shapes.large) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(message, color = Color.White)
                                Button(onClick = { error = null; retry++ }) { Text("Retry") }
                                external?.let { TextButton(onClick = { persist(); player?.pause(); it(page) }) { Text("Open external player") } }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.TopCenter), enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .64f), 1f to Color.Transparent,
        ))) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 8.dp, top = 2.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompositionLocalProvider(LocalContentColor provides Color.White) {
                IconButton(onClick = ::close, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = .38f))) {
                    AppSymbol(AppIcon.Back, description = "Back")
                }
            }
            Text(title, Modifier.weight(1f).padding(horizontal = 6.dp), color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(shadow = Shadow(Color.Black.copy(alpha = .82f), Offset(0f, 2f), 7f)),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            onToggleSave?.let { toggle ->
                IconButton(onClick = { interaction++; toggle() }, colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = .38f))) {
                    AppSymbol(if (saved) AppIcon.BookmarkFilled else AppIcon.Bookmark, description = if (saved) "Unsave show" else "Save show")
                }
            }
            external?.let { callback -> TextButton(onClick = { persist(); player?.pause(); callback(pager.settledPage) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .38f))) { Text("External") } }
        }
        }
        }
        AnimatedVisibility(visible = controlsVisible, modifier = Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
        Column(Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.Transparent, .34f to Color.Black.copy(alpha = .22f), 1f to Color.Black.copy(alpha = .68f)))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(current.label, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${pager.settledPage + 1} / ${episodes.size}",
                        color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
                }
                if (episodes.size > 1) Surface(onClick = { range = pager.settledPage / 25; chooser = true; interaction++ },
                    shape = RoundedCornerShape(24.dp), color = Color.Black.copy(alpha = .42f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .24f)), contentColor = Color.White) {
                    Text("Episodes", Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            if (player != null) PlayerSeekControls(player, playerColors.playerAccent, onScrub = { scrubbing = it; interaction++ })
        }
    }
    }
    if (chooser) ModalBottomSheet(onDismissRequest = { chooser = false; controlsVisible = true; interaction++ }, containerColor = Color.Black) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Episodes", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            Text("${pager.settledPage + 1} / ${episodes.size}", style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = { chooser = false; interaction++ }) { AppSymbol(AppIcon.Close, description = "Close episode selector") }
        }
        val groups = episodes.indices.chunked(25)
        fun episodeNumber(index: Int) = Regex("""\d+(?:\.\d+)?""").find(episodes[index].label)?.value ?: (index + 1).toString()
        if (groups.size > 1) LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(groups.indices.toList()) { group ->
                FilterChip(selected = range == group, onClick = { range = group }, label = { Text("${episodeNumber(groups[group].first())}–${episodeNumber(groups[group].last())}") })
            }
        }
        val indices = groups[range.coerceIn(groups.indices)]
        LazyVerticalGrid(columns = GridCells.Adaptive(48.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(indices, key = { episodes[it].id }) { index ->
                val selected = index == pager.settledPage
                Surface(onClick = { chooser = false; controlsVisible = true; interaction++; scope.launch { pager.scrollToPage(index) } },
                    modifier = Modifier.aspectRatio(1f).heightIn(min = 48.dp).semantics { contentDescription = episodes[index].label },
                    shape = RoundedCornerShape(8.dp), color = if (selected) playerColors.playerAccent else Color(0xFF242424),
                    contentColor = if (selected) playerColors.onPlayerAccent else Color.White) {
                    Box(contentAlignment = Alignment.Center) { Text(episodeNumber(index), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding())
    }
    }
    }
}

/** Position ticking is confined to this small control subtree, not the player/pager. */
@Composable
private fun PlayerSeekControls(player: ExoPlayer, playedColor: Color, onScrub: (Boolean) -> Unit) {
    var position by remember(player) { mutableLongStateOf(0L) }
    var duration by remember(player) { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(player) { while (true) { position = player.currentPosition.coerceAtLeast(0L); duration = player.duration.coerceAtLeast(0L); delay(500) } }
    val fraction = if (dragging) target else if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val latestDuration by rememberUpdatedState(duration)
    fun fractionAt(x: Float, width: Float, inset: Float): Float = ((x - inset) / (width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
    fun seek(value: Float) {
        if (latestDuration > 0) player.seekTo((value.coerceIn(0f, 1f) * latestDuration).toLong())
    }
    Canvas(Modifier.fillMaxWidth().height(48.dp)
        .semantics {
            contentDescription = "Video position"
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            if (duration <= 0) disabled()
            setProgress { value -> if (latestDuration <= 0) false else { seek(value); true } }
        }
        .pointerInput(player, duration) {
            if (duration > 0) detectTapGestures { offset -> onScrub(false); seek(fractionAt(offset.x, size.width.toFloat(), 8.dp.toPx())) }
        }
        .pointerInput(player, duration) {
            if (duration > 0) detectHorizontalDragGestures(
                onDragStart = { offset -> dragging = true; onScrub(true); target = fractionAt(offset.x, size.width.toFloat(), 8.dp.toPx()) },
                onHorizontalDrag = { change, _ -> change.consume(); target = fractionAt(change.position.x, size.width.toFloat(), 8.dp.toPx()) },
                onDragEnd = { seek(target); dragging = false; onScrub(false) },
                onDragCancel = { dragging = false; onScrub(false) },
            )
        }) {
        val left = 8.dp.toPx()
        val right = (size.width - left).coerceAtLeast(left)
        val y = size.height / 2
        val x = left + (right - left) * fraction
        drawLine(Color.White.copy(alpha = .28f), Offset(left, y), Offset(right, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(playedColor, Offset(left, y), Offset(x, y), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(playedColor, radius = if (dragging) 6.dp.toPx() else 4.dp.toPx(), center = Offset(x, y))
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(playerTime(if (dragging) (target * duration).toLong() else position), color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall)
        Text(playerTime(duration), color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall)
    }
}
private fun playerTime(value: Long): String { val seconds = value / 1000; return "%d:%02d".format(seconds / 60, seconds % 60) }
