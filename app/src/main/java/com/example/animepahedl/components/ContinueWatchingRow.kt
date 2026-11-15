package com.example.animepahedl.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import coil.ImageLoader
import com.example.animepahedl.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ContinueWatchingRow(
    series: List<SiteDownloadSeries>,
    images: ImageLoader,
    onOpen: (SiteDownloadSeries) -> Unit,
    onRemove: (SiteDownloadSeries) -> Unit = {},
    onMarkCompleted: (SiteDownloadSeries) -> Unit = {},
) {
    if (series.isEmpty()) return
    var actionsFor by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<SiteDownloadSeries?>(null) }
    // The containing Home/Library feed already owns vertical spacing. Extra padding here stacked
    // with the rail's own 12dp inset and the next section's header padding, leaving a large blank
    // band below the cards compared with Anilili's Continue Watching rail.
    Column(Modifier.fillMaxWidth()) {
        Text("Continue Watching", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(series, key = { it.provider.name + it.id }) { item ->
                val summary = rememberSeriesPlaybackSummary(item)
                Column(Modifier.width(174.dp).combinedClickable(onClick = { onOpen(item) }, onLongClick = { actionsFor = item })) {
                    Box {
                        Thumbnail(item.posterThumbnail(), item.title, images, Modifier.fillMaxWidth().aspectRatio(16f / 9f), cornerRadius = 12.dp)
                        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .30f)))
                        AppSymbol(AppIcon.Play, Modifier.align(Alignment.Center))
                        IconButton(onClick = { actionsFor = item }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions for ${item.title}",
                                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.offset(x = 6.dp, y = (-6).dp))
                        }
                    }
                    Text(item.title, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge, maxLines = 2)
                    WatchProgressBar(summary.fraction, Modifier.fillMaxWidth().padding(top = 4.dp))
                    Text(summary.resumeLabel ?: "Resume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary.downloadLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    actionsFor?.let { item ->
        val summary = rememberSeriesPlaybackSummary(item)
        ModalBottomSheet(
            onDismissRequest = { actionsFor = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
                Text("Continue watching", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(item.title, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, maxLines = 2)
                Text(summary.resumeLabel ?: summary.watchLabel, Modifier.padding(top = 2.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ListItem(
                    headlineContent = { Text("Mark as completed") },
                    supportingContent = { Text("Move this title out of Continue Watching") },
                    leadingContent = { AppSymbol(AppIcon.CheckCircle, Modifier.size(22.dp)) },
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                        actionsFor = null
                        onMarkCompleted(item)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                ListItem(
                    headlineContent = { Text("Remove from Continue Watching") },
                    supportingContent = { Text("Keep it in Saved shows") },
                    leadingContent = { AppSymbol(AppIcon.Close, Modifier.size(22.dp)) },
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                        actionsFor = null
                        onRemove(item)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
