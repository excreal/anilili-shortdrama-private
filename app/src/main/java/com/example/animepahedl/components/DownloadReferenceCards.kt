@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.animepahedl.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.animepahedl.AppIcon
import com.example.animepahedl.AppSymbol

/** Rendering port of Anilili's DownloadSeriesCard; download state is supplied by the adapter. */
internal data class DownloadReferenceSeries(
    val title: String, val artworkUrl: String, val countLabel: String,
    val bytesLabel: String, val activityLabel: String?, val subtitle: String,
)

@Composable
internal fun DownloadReferenceSeriesCard(
    series: DownloadReferenceSeries,
    images: ImageLoader,
    onOpen: () -> Unit,
    onRemoveSeries: () -> Unit,
    modifier: Modifier = Modifier,
    selectionActive: Boolean = false,
    selected: Boolean = false,
    onSelect: () -> Unit = {},
) {
    Column(modifier.combinedClickable(onClickLabel = "Open ${series.title}",
        onLongClickLabel = "Select ${series.title}", onClick = onOpen, onLongClick = onSelect)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            AsyncImage(model = series.artworkUrl, imageLoader = images, contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            ReferenceCornerBadge(series.countLabel, Modifier.align(Alignment.TopStart).padding(5.dp))
            Box(Modifier.align(Alignment.TopEnd).size(48.dp)
                .clickable(role = Role.Button,
                    onClickLabel = if (selectionActive) "Select ${series.title}" else "Remove every video of ${series.title}",
                    onClick = if (selectionActive) onSelect else onRemoveSeries),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(28.dp).offset(x = 6.dp, y = (-6).dp).clip(CircleShape).background(Color.Black.copy(alpha = .6f)), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides Color.White) {
                        if (selectionActive && !selected) Canvas(Modifier.size(16.dp)) {
                            drawCircle(Color.White, style = Stroke(width = 2.dp.toPx()))
                        } else AppSymbol(if (selectionActive) AppIcon.Check else AppIcon.Delete,
                            Modifier.size(16.dp), if (selectionActive) "Select ${series.title}" else "Remove every video of ${series.title}")
                    }
                }
            }
            if (series.bytesLabel.isNotBlank()) ReferenceCornerBadge(series.bytesLabel, Modifier.align(Alignment.BottomStart).padding(5.dp))
            series.activityLabel?.let { ReferenceCornerBadge(it, Modifier.align(Alignment.BottomEnd).padding(5.dp)) }
        }
        Text(series.title, Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(series.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal data class DownloadReferenceEpisode(
    val title: String, val subtitle: String, val artworkUrl: String, val status: String,
    val playable: Boolean, val failed: Boolean, val progress: Float?, val indeterminate: Boolean = false,
    val watched: Boolean = false, val watchProgress: Float = 0f,
)

/** Phone EpisodeRow layout from Anilili, retaining the source's artwork, text and action geometry. */
@Composable
internal fun DownloadReferenceEpisodeRow(
    episode: DownloadReferenceEpisode,
    images: ImageLoader,
    selectionActive: Boolean,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    controlIcon: AppIcon? = null,
    controlLabel: String? = null,
    onControl: () -> Unit = {},
    actions: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val secondary = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Card(shape = shape, colors = CardDefaults.cardColors(
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth().clip(shape).combinedClickable(
            onClickLabel = if (selectionActive) "Select ${episode.title}" else if (episode.playable) "Play offline" else null,
            onLongClickLabel = "Select episodes", onClick = {
                if (selectionActive) onToggleSelected() else if (episode.playable) onPlay()
            }, onLongClick = onToggleSelected)) {
        BoxWithConstraints {
            val compact = maxWidth < 420.dp
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(if (compact) 84.dp else 104.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                    AsyncImage(model = episode.artworkUrl, imageLoader = images, contentDescription = null,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = if (selectionActive) .72f else .55f)),
                        contentAlignment = Alignment.Center) {
                        CompositionLocalProvider(LocalContentColor provides Color.White) {
                            if (selectionActive && !selected) Canvas(Modifier.size(20.dp)) { drawCircle(Color.White, style = Stroke(width = 2.dp.toPx())) }
                            else AppSymbol(when { selectionActive -> AppIcon.Check; episode.playable -> AppIcon.Play; else -> AppIcon.Download }, Modifier.size(20.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (episode.watched) {
                            AppSymbol(AppIcon.CheckCircle, Modifier.size(16.dp), "Watched")
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(episode.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            color = if (episode.watched) secondary else content, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (episode.subtitle.isNotBlank()) Text(episode.subtitle, style = MaterialTheme.typography.bodySmall,
                        color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (episode.progress != null) LinearProgressIndicator(progress = { episode.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    else if (episode.indeterminate) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!episode.watched) WatchProgressBar(episode.watchProgress, Modifier.fillMaxWidth())
                    Text(episode.status, style = MaterialTheme.typography.labelSmall,
                        color = if (episode.failed) MaterialTheme.colorScheme.error else secondary,
                        maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis)
                }
                if (!selectionActive) {
                    // The action menu contains the same pause/resume/retry actions, so drop the
                    // duplicate shortcut on narrow cards to preserve room for episode identity.
                    if (!compact && controlIcon != null) IconButton(onClick = onControl) { AppSymbol(controlIcon, description = controlLabel) }
                    actions()
                }
            }
        }
    }
}

@Composable
private fun ReferenceCornerBadge(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Row(modifier.clip(shape).background(Color.Black.copy(alpha = .78f))
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .6f), shape)
        .padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WatchProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    if (fraction <= 0.01f) return
    Box(
        modifier
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White.copy(alpha = 0.25f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

