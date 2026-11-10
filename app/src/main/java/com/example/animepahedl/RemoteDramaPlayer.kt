@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.example.animepahedl

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.animepahedl.components.DramaPlayerEpisode
import com.example.animepahedl.components.DramaPlayerSurface

/** Online episodes resolve only when selected; the shared surface owns the sole decoder. */
@Composable
internal fun RemoteDramaPlayer(series: SiteDownloadSeries, quality: String, onClose: () -> Unit, startEpisodeId: String? = null, onDownload: (SiteDownloadSeries, SiteDownloadEpisode) -> Unit = { _, _ -> }) {
    if (series.provider.usesReferencePlayer) {
        ReferenceVideoPlayer(series, quality, onClose, startEpisodeId, onDownload = onDownload)
        return
    }
    LaunchedEffect(Unit) { com.anilili.playback.PlaybackService.stopActivePlayback() }
    val app = LocalContext.current.applicationContext
    val playbackHistory = remember(app) { PlaybackHistory(app) }
    // Register the online series before the surface starts resolving episodes. Progress writes
    // alone cannot populate Continue Watching because recent() intentionally reads registered
    // metadata records only.
    LaunchedEffect(series.provider, series.id) { playbackHistory.register(series) }
    val savedShows = remember(app) { SavedShowsStore(app) }
    val savedRevision by SavedShowsStore.changes.collectAsState()
    val isSaved = remember(savedRevision, series) { savedShows.contains(series) }
    val cookies = remember(app) { SiteDownloadCookieStore(app) }
    val repository = remember(cookies) { appSiteDownloadRepository(cookieHeader = cookies::headerFor) }
    // A failed local copy is bypassed only for this playback session.
    val onlineOnly = remember(series.provider, series.id) { mutableSetOf<Int>() }
    val localPlaying = remember(series.provider, series.id) { mutableSetOf<Int>() }
    val refreshedLinks = remember(series.provider, series.id) { MediaLinkRefreshGuard() }
    val episodes = remember(series) { series.episodes.map {
        DramaPlayerEpisode(it.id, it.url, if (series.isSingleVideo) series.title else "Episode ${it.displayNumber}")
    } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DramaPlayerSurface(series.title, "series:${series.provider.name}:${series.id}", episodes, startEpisodeId, onClose,
            saved = isSaved, onToggleSave = { savedShows.toggle(series) },
            onPlaybackFailure = { index ->
                when {
                    localPlaying.remove(index) -> { onlineOnly.add(index); true }
                    refreshedLinks.shouldRefresh(series.provider, series.episodes.getOrNull(index)?.id.orEmpty()) -> true
                    else -> false
                }
            },
            resolve = { index ->
                val episode = series.episodes[index]
                DownloadStore.awaitReady()
                val local = if (index in onlineOnly) null else withContext(Dispatchers.IO) {
                    DownloadStore.all().values.asSequence().filter { record ->
                        val identity = record.identity
                        record.uri.isNotBlank() && identity?.isSiteDownload == true &&
                            ((identity.anime.session == "sites:${series.provider.name}:${series.id}" && identity.episode.session == episode.id) ||
                                identity.locator == episode.url)
                    }.mapNotNull { record ->
                        runCatching {
                            val uri = record.uri.toUri()
                            require(uri.scheme == "content")
                            app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                                require(descriptor.length != 0L)
                                uri
                            }
                        }.getOrNull()
                    }.firstOrNull()
                }
                if (local != null) {
                    localPlaying.add(index)
                    DefaultMediaSourceFactory(app).createMediaSource(MediaItem.fromUri(local))
                } else {
                localPlaying.remove(index)
                val media = cookies.applyTo(repository.resolve(episode.url, quality, cookies.headerFor(episode.url)))
                val factory = OkHttpDataSource.Factory(MediaRequests.client(media, cookies::headerFor))
                    .setDefaultRequestProperties(mediaRequestHeaders(media))
                val item = MediaItem.Builder().setUri(media.url)
                    .apply { media.mimeType?.takeIf(String::isNotBlank)?.let(::setMimeType) }.build()
                DefaultMediaSourceFactory(factory).createMediaSource(item)
                }
            })
    }
}
