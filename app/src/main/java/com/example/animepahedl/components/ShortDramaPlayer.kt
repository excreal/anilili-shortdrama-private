@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
package com.example.animepahedl.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.animepahedl.DownloadRow
import com.example.animepahedl.DownloadStore
import com.example.animepahedl.SavedShowsStore
import com.example.animepahedl.savedSeriesFromDownloads

/** Saved files use the same controls and locator-based progress as their online episodes. */
@Composable
internal fun ShortDramaPlayer(rows: List<DownloadRow>, initialId: String, onClose: () -> Unit, onExternal: (DownloadRow) -> Unit) {
    LaunchedEffect(Unit) { com.anilili.playback.PlaybackService.stopActivePlayback() }
    val context = LocalContext.current
    val playlist = remember(rows) { rows.filter { it.complete && !it.deleting && it.savedUri.isNotBlank() }.distinctBy { it.id } }
    val savedShows = remember(context) { SavedShowsStore(context) }
    val savedSeries = remember(playlist) { savedSeriesFromDownloads(playlist) }
    val history = remember(context) { com.example.animepahedl.PlaybackHistory(context) }
    LaunchedEffect(savedSeries) { savedSeries?.let(history::registerLocal) }
    val savedRevision by SavedShowsStore.changes.collectAsState()
    val isSaved = remember(savedRevision, savedSeries) { savedSeries?.let(savedShows::contains) == true }
    val records = remember(playlist) { DownloadStore.all() }
    val episodes = remember(playlist, records) { playlist.map { row ->
        DramaPlayerEpisode(row.id, records[row.id]?.identity?.locator?.takeIf(String::isNotBlank) ?: row.savedUri,
            row.episodeNumber.takeIf(String::isNotBlank)?.let { "Episode $it" } ?: row.title)
    } }
    val first = playlist.firstOrNull()
    val identity = first?.let { records[it.id]?.identity }
    val seriesKey = identity?.anime?.session?.removePrefix("sites:")?.let { "series:$it" }
        ?: "saved:${first?.showId.orEmpty().ifBlank { first?.showTitle.orEmpty() }}"
    DramaPlayerSurface(first?.let { it.showTitle.ifBlank { it.title } }.orEmpty(), seriesKey, episodes, initialId.takeIf(String::isNotBlank), onClose,
        saved = isSaved, onToggleSave = savedSeries?.let { series -> { savedShows.toggle(series) } },
        resolve = { index ->
            val uri = playlist[index].savedUri.toUri()
            require(uri.scheme == "content") { "Relink this saved video before playing it." }
            val mimeType = context.contentResolver.getType(uri)?.takeIf { it.startsWith("video/") } ?: MimeTypes.VIDEO_MP4
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                require(descriptor.length != 0L) { "This saved video is empty. Relink it in Downloads." }
            } ?: error("This saved video is unavailable. Relink it in Downloads.")
            val mediaItem = MediaItem.Builder().setUri(uri).setMimeType(mimeType).build()
            ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context.applicationContext))
                .createMediaSource(mediaItem)
        }, external = { onExternal(playlist[it]) })
}
