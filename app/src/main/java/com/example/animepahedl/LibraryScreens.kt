@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.animepahedl

import com.example.animepahedl.components.*
import androidx.compose.animation.animateContentSize

import androidx.core.net.toUri
import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil.ImageLoader
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anilili.data.settings.SettingsStore
import com.anilili.data.settings.ThemePreset

internal fun friendlyDownloadStatus(status: String): String = when (status) {
    "Downloading HLS", "Downloading file" -> "Downloading"
    "Queued" -> "Waiting to download"
    "Waiting to save" -> "Waiting to save"
    "Preparing MP4", "Retrying MP4" -> "Preparing video"
    "Checking MP4" -> "Checking video"
    "Copying to folder" -> "Saving to folder"
    "Ready offline", "Saved to Downloads", "Saved to folder" -> "Ready"
    else -> if (status.startsWith("Save failed")) "Couldn’t save video" else if (status.startsWith("Failed")) "Download failed" else status
}

internal fun outputLabel(model: DownloaderViewModel): String = if (model.outputTree.isBlank())
    "Downloads" + model.outputFolder.takeIf { it.isNotEmpty() }?.let { "/$it" }.orEmpty()
    else runCatching { DocumentsContract.getTreeDocumentId((model.outputTree).toUri()).replace(':', '/') }.getOrDefault("Selected folder")

/** Settings detail row follows Anilili's title/description/value/chevron layout. */
@Composable
private fun CatalogSourceSettingRow(title: String, description: String, value: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            AppSymbol(AppIcon.Next, Modifier.size(20.dp), description = null)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun DownloadsScreen(model: DownloaderViewModel, siteDownload: SiteDownloadViewModel, images: ImageLoader, onChooseFolder: () -> Unit, onPlay: (DownloadRow) -> Unit, onRemove: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var openGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteGroup by remember { mutableStateOf<List<DownloadRow>?>(null) }
    androidx.activity.compose.BackHandler(enabled = openGroup != null && !model.selectionMode) { openGroup = null }
    deleteGroup?.let { rows ->
        AlertDialog(onDismissRequest = { deleteGroup = null }, title = { Text("Remove ${rows.size} videos?") },
            text = { Text("Saved files will be deleted and unfinished downloads cancelled.") },
            confirmButton = { TextButton(onClick = {
                deleteGroup = null
                siteDownload.removePending(rows.filter { siteDownload.isPendingRow(it.id) }.map { it.id }.toSet())
                model.removeDownloads(rows.filterNot { siteDownload.isPendingRow(it.id) }.map { it.id }.toSet(), rows.filter { it.complete }.map { it.id }.toSet())
            }) { Text("Remove", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteGroup = null }) { Text("Keep") } })
    }
    var relinkId by rememberSaveable { mutableStateOf<String?>(null) }
    var forgetId by rememberSaveable { mutableStateOf<String?>(null) }
    val relinkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = relinkId
        relinkId = null
        if (uri != null && id != null) model.relink(id, uri)
    }
    forgetId?.let { id -> AlertDialog(onDismissRequest = { forgetId = null }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_remove_library_record)) },
        text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_the_video_file_is_kept_you_can_add_it_again_by_downloading_or_rel)) },
        confirmButton = { TextButton(onClick = { model.forgetMissing(id); forgetId = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_remove_record)) } },
        dismissButton = { TextButton(onClick = { forgetId = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_keep)) } }) }
    var detailRow by remember { mutableStateOf<DownloadRow?>(null) }
    detailRow?.let { row -> AlertDialog(onDismissRequest = { detailRow = null }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_download_details)) },
        text = { Text("${row.title}\n\n${row.status}\n\n${outputLabel(model)}", Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { detailRow = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_close)) } }) }
    val gridListState = rememberLazyListState()
    val episodeListState = rememberLazyListState()
    val listState = if (openGroup == null) gridListState else episodeListState
    LaunchedEffect(openGroup) { if (openGroup != null) episodeListState.scrollToItem(0) }
    var downloadSort by rememberSaveable { mutableStateOf("Show title") }
    var downloadView by rememberSaveable { mutableStateOf("All downloads") }
    var downloadAudio by rememberSaveable { mutableStateOf("Any audio") }
    val widthDp = LocalConfiguration.current.screenWidthDp
    val pagePadding = if (widthDp >= 840) 32.dp else if (widthDp >= 600) 24.dp else 16.dp
    val posterWidth = if (widthDp >= 840) 132f else if (widthDp >= 600) 120f else ((widthDp - 62) / 3.35f).coerceIn(108f, 150f)
    val posterColumns = ((widthDp - pagePadding.value * 2 + 12) / (posterWidth + 12)).toInt().coerceAtLeast(1)
    val allRows = remember(model.downloads, siteDownload.pending, siteDownload.preparationStatus) { siteDownload.libraryRows(model.downloads) }
    val snapshot by produceState(DownloadLibrarySnapshot(), allRows, query, downloadSort, downloadView, downloadAudio, openGroup) {
        val previous = value
        value = withContext(Dispatchers.Default) {
            val filtered = if (openGroup != null) allRows else allRows.groupBy { it.showId.ifBlank { it.showTitle.ifBlank { it.title.substringBefore(" · ") } } }
                .values.filter { rows ->
                    (downloadAudio == "Any audio" || rows.any { it.audioCode?.let(::audioTag) == downloadAudio }) && when (downloadView) {
                        "Downloading" -> rows.any { !it.complete && !it.failed && it.transfer != TransferState.PAUSED }
                        "Saved" -> rows.any { it.complete }
                        "Failed" -> rows.any { it.failed }
                        else -> true
                    }
                }.flatten()
            downloadLibrarySnapshot(filtered, if (openGroup == null) query else "", downloadSort, previous).copy(source = allRows)
        }
    }
    val ready = snapshot.ready
    val active = snapshot.unfinished
    val visible = snapshot.rows
    val context = LocalContext.current
    var playbackRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val preferences = context.applicationContext.getSharedPreferences("playback-history", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> playbackRevision++ }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val playbackHistory = remember(context, playbackRevision) { PlaybackHistory(context) }
    val savedRecords = remember(allRows) { DownloadStore.all() }
    val openRows = snapshot.groups.firstOrNull { it.first == openGroup }?.second.orEmpty()
    val heading = if (openGroup == null) "Downloads" else openRows.firstOrNull()?.showTitle?.ifBlank { openRows.firstOrNull()?.title.orEmpty() }.orEmpty()
    Column(Modifier.fillMaxSize()) {
        if (!model.selectionMode) Row(Modifier.fillMaxWidth().padding(start = if (openGroup == null) pagePadding else 4.dp,
            end = pagePadding, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (openGroup != null) IconButton(onClick = { openGroup = null; model.exitSelection() }) {
                AppSymbol(AppIcon.Back, description = "Back to downloaded series")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(heading, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 46.sp),
                    fontWeight = FontWeight.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(if (openGroup == null) "${snapshot.groups.size} titles · $ready saved · ${Formatter.formatShortFileSize(context, snapshot.savedBytes)}"
                    else "${openRows.size} videos · ${openRows.count { it.complete }} saved · ${Formatter.formatShortFileSize(context, openRows.sumOf { it.bytes })}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            IconButton(onClick = { model.enterSelection() }, enabled = allRows.isNotEmpty()) { AppSymbol(AppIcon.Check, description = "Select downloads") }
        }
    LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("downloads-list"), state = listState,
        contentPadding = PaddingValues(start = pagePadding, end = pagePadding, top = 8.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(if (openGroup == null) 10.dp else 8.dp)) {
        if (!model.outputAccessGranted || model.outputCheckRequired || model.checkingExistingFiles) item(key = "output-folder-access", contentType = "folder-access") {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (model.checkingExistingFiles) CircularProgressIndicator(Modifier.padding(end = 8.dp).size(16.dp), strokeWidth = 2.dp)
                Text(if (model.checkingExistingFiles) "Checking existing videos…" else "Link your downloads folder to reuse saved videos.",
                    Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                if (!model.checkingExistingFiles) TextButton(onClick = onChooseFolder) { Text("Choose folder") }
            }
        }
        siteDownload.pending?.let { pending -> item(key = "site-preparation", contentType = "preparation-status") {
            PreparationStatusRow(siteDownload.preparationStatus, pending.batches().sumOf { it.episodes.size }, siteDownload::pausePreparation,
                siteDownload::resumePreparation, siteDownload::discardPreparation, onChooseFolder = onChooseFolder)
        } }
        model.downloadNotice?.let { notice -> item(contentType = "download-notice") {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(notice, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = model::dismissDownloadNotice) { AppSymbol(AppIcon.Close, description = "Dismiss notice") }
                }
            }
        } }
        if (openGroup == null && allRows.isNotEmpty()) item(contentType = "download-filters") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(query, { query = it; model.clearDownloadSelection() },
                    modifier = Modifier.fillMaxWidth().testTag("downloads-search").semantics { contentDescription = "Search downloads" },
                    placeholder = { Text("Search downloads") }, singleLine = true,
                    leadingIcon = { AppSymbol(AppIcon.Browse) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; model.clearDownloadSelection() }) { AppSymbol(AppIcon.Close, description = "Clear search") } },
                    shape = RoundedCornerShape(50), colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.outline))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DownloadFilterPill(downloadView, listOf("All downloads", "Downloading", "Saved", "Failed"), Modifier.weight(1f)) { downloadView = it }
                    DownloadFilterPill(downloadAudio, listOf("Any audio") + allRows.mapNotNull { it.audioCode?.let(::audioTag) }.distinct().sorted(), Modifier.weight(1f)) { downloadAudio = it }
                    Box {
                        var sortMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { sortMenu = true }) { AppSymbol(AppIcon.Sort, description = "Sort downloads, $downloadSort") }
                        DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                            listOf("Show title", "Status", "Largest").forEach { option -> DropdownMenuItem(text = { Text(option) },
                                onClick = { downloadSort = option; sortMenu = false }, trailingIcon = { if (downloadSort == option) AppSymbol(AppIcon.Check) }) }
                        }
                    }
                }
                Text("${snapshot.groups.size} titles", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (visible.isEmpty() && snapshot.source === allRows) item(contentType = "download-empty-state") {
            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (query.isNotBlank()) "No matching downloads" else "Nothing downloaded yet", style = MaterialTheme.typography.titleMedium)
                Text(if (query.isNotBlank()) "Try a title or episode number." else "Save videos from Explore to keep them here.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (query.isBlank()) TextButton(onClick = { model.showDownloads = false }) { Text("Explore videos") }
            }
        }
        if (openGroup == null) {
            items(snapshot.groups.chunked(posterColumns), key = { "poster-row:${it.first().first}" }, contentType = { "download-series-row" }) { groups ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    groups.forEach { (groupId, episodes) ->
                        val title = episodes.first().showTitle.ifBlank { episodes.first().title.substringBefore(" · ") }
                        val progress = downloadGroupProgress(episodes, siteDownload.preparing &&
                            siteDownload.pending?.series?.let { "sites:${it.provider.name}:${it.id}" } == groupId)
                        val selected = episodes.count { it.id in model.selectedDownloadIds }
                        val bytes = episodes.sumOf { it.bytes }
                        val lastEpisode = playbackHistory.lastEpisode("series:${groupId.removePrefix("sites:")}")
                        val continued = episodes.firstOrNull { row ->
                            val key = savedRecords[row.id]?.identity?.locator?.takeIf(String::isNotBlank) ?: row.savedUri
                            key.isNotBlank() && playbackHistory.matches(lastEpisode, key)
                        }
                        DownloadReferenceSeriesCard(
                            series = DownloadReferenceSeries(title, downloadGroupArtwork(episodes, savedRecords),
                                "${episodes.size} eps", if (bytes > 0) Formatter.formatShortFileSize(context, bytes) else "",
                                if (progress.running) "↓ ${episodes.count { !it.complete && !it.failed && it.transfer != TransferState.PAUSED }}" else null,
                                continued?.let { "Last watched · EP ${it.episodeNumber}" }
                                    ?: (progress.label + progress.percent?.let { " ${it.toInt()}%" }.orEmpty())),
                            images = images, modifier = Modifier.weight(1f), selectionActive = model.selectionMode, selected = selected > 0,
                            onSelect = { model.selectDownloads(episodes.map { it.id }.toSet()) },
                            onRemoveSeries = { deleteGroup = episodes },
                            onOpen = {
                                if (model.selectionMode) {
                                    val ids = episodes.map { it.id }.toSet()
                                    model.selectDownloads(if (selected == episodes.size) model.selectedDownloadIds - ids else model.selectedDownloadIds + ids)
                                } else openGroup = groupId
                            })
                    }
                    repeat(posterColumns - groups.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        snapshot.groups.filter { it.first == openGroup }.forEach { (_, episodes) ->
            items(episodes, key = { it.id }, contentType = { "download-episode-row" }) { row ->
                val parts = row.title.split(" · ")
                var menu by remember { mutableStateOf(false) }
                val pendingRow = siteDownload.isPendingRow(row.id)
                val historyKey = savedRecords[row.id]?.identity?.locator?.takeIf(String::isNotBlank) ?: row.savedUri
                val resumePosition = if (historyKey.isNotBlank()) playbackHistory.position(historyKey) else 0L
                val watched = historyKey.isNotBlank() && playbackHistory.watched(historyKey)
                val resumeLabel = resumePosition.takeIf { it > 0L }?.let {
                    val seconds = it / 1000
                    "Resume at ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                }
                val showProgress = !pendingRow && !row.failed && (row.transfer == TransferState.DOWNLOADING || row.saving)
                val control = when {
                    DownloadAction.RETRY in row.actions -> AppIcon.Refresh
                    DownloadAction.RESUME in row.actions -> AppIcon.Play
                    DownloadAction.PAUSE in row.actions -> AppIcon.Pause
                    else -> null
                }
                DownloadReferenceEpisodeRow(
                    episode = DownloadReferenceEpisode(
                        title = (if (row.showId.startsWith("sites:") && row.episodeNumber == "1" && !row.title.contains(" · Episode "))
                            row.showTitle.ifBlank { parts.first() } else "Episode ${row.episodeNumber.ifBlank { parts.getOrNull(1)?.removePrefix("Episode ").orEmpty() }}") +
                            row.audioCode?.takeIf { it.isNotBlank() && it != "und" }?.let { " · ${audioTag(it)}" }.orEmpty(),
                        subtitle = (row.quality ?: parts.getOrNull(2).orEmpty()).takeUnless { it in setOf("Original", "Best") }.orEmpty(), artworkUrl = row.thumbnail,
                        status = listOfNotNull(if (watched) "Watched" else resumeLabel,
                            savedFileLocationStatus(row) ?: friendlyDownloadStatus(row.status),
                            row.bytes.takeIf { it > 0 }?.let { Formatter.formatShortFileSize(context, it) + if (row.totalBytes > 0 && !row.complete) " / ${Formatter.formatShortFileSize(context, row.totalBytes)}" else "" },
                            row.percent.takeIf { it >= 0 && !row.complete }?.let { "${it.toInt()}%" }).joinToString(" · "),
                        playable = row.complete, failed = row.failed, watched = watched,
                        watchProgress = if (!watched && historyKey.isNotBlank()) playbackHistory.fraction(historyKey) else 0f,
                        progress = if (showProgress && row.percent >= 0) row.percent / 100f else null,
                        indeterminate = showProgress && row.percent < 0),
                    images = images, selectionActive = model.selectionMode, selected = row.id in model.selectedDownloadIds,
                    onToggleSelected = { if (model.selectionMode) model.toggleDownload(row.id) else model.enterSelection(row.id) },
                    onPlay = { onPlay(row) }, controlIcon = control,
                    controlLabel = when (control) { AppIcon.Refresh -> "Retry this download"; AppIcon.Play -> "Resume download"; AppIcon.Pause -> "Pause download"; else -> null },
                    onControl = {
                        if (pendingRow) { if (DownloadAction.RESUME in row.actions) siteDownload.resumePending(row.id) else siteDownload.pausePending(row.id) }
                        else when { DownloadAction.RETRY in row.actions -> model.retryDownload(row.id)
                            DownloadAction.RESUME in row.actions -> model.resumeDownload(row.id)
                            else -> model.pauseDownload(row.id) }
                    }, actions = {
                            Box {
                                IconButton(onClick = { menu = true }) { AppSymbol(AppIcon.More, description = "Actions for ${row.title}") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    if (row.complete && historyKey.isNotBlank()) DropdownMenuItem(
                                        text = { Text(if (watched) "Mark as unwatched" else "Mark as watched") },
                                        onClick = { menu = false; playbackHistory.setWatched(historyKey, !watched) })
                                    DropdownMenuItem(text = { Text("Select episodes") }, onClick = { menu = false; model.enterSelection(row.id) })
                                    DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_details)) }, onClick = { menu = false; detailRow = row })
                                    if (row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_relink_video)) }, onClick = { menu = false; relinkId = row.id; relinkPicker.launch(arrayOf("video/*")) })
                                    if (row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_remove_record_keep_file)) }, onClick = { menu = false; forgetId = row.id })
                                    if (!row.complete && !siteDownload.isPendingRow(row.id)) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_move_to_front)) }, onClick = { menu = false; model.prioritize(row.id) })
                                    if (row.complete) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_play)) }, onClick = { menu = false; onPlay(row) })
                                    if (DownloadAction.PAUSE in row.actions || DownloadAction.RESUME in row.actions) {
                                        val paused = DownloadAction.RESUME in row.actions
                                        DropdownMenuItem(text = { Text(if (siteDownload.isPendingRow(row.id)) { if (paused) "Resume link preparation" else "Pause link preparation" } else if (paused) "Resume" else "Pause") }, onClick = {
                                            menu = false
                                            if (siteDownload.isPendingRow(row.id)) { if (paused) siteDownload.resumePending(row.id) else siteDownload.pausePending(row.id) }
                                            else if (paused) model.resumeDownload(row.id) else model.pauseDownload(row.id)
                                        })
                                    }
                                    if (DownloadAction.VERIFY in row.actions) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_verify)) }, onClick = { menu = false; model.verifyDownload(row.id) })
                                    if (DownloadAction.RETRY in row.actions) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry)) }, onClick = { menu = false; model.retryDownload(row.id) })
                                    if (row.failure in setOf(FailureReason.FOLDER_ACCESS, FailureReason.STORAGE_FULL)) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_change_folder)) }, onClick = { menu = false; model.showSettings = true })
                                    if (DownloadAction.RETRY_SAVE in row.actions) DropdownMenuItem(text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry_save)) }, onClick = { menu = false; model.retryExport(row.id) })
                                    DropdownMenuItem(
                                        text = { Text(
                                            if (row.complete) "Delete saved video" else "Cancel download",
                                            color = MaterialTheme.colorScheme.error
                                        ) },
                                        onClick = { menu = false; onRemove(row.id) }
                                    )
                                }
                            }
                    })
            }
        }
        if (openGroup != null && !model.selectionMode && openRows.isNotEmpty()) item(key = "remove-series", contentType = "remove-series-action") {
            TextButton(onClick = { deleteGroup = openRows }, modifier = Modifier.padding(top = 8.dp)) {
                AppSymbol(AppIcon.Delete)
                Text("Remove all episodes", Modifier.padding(start = 6.dp))
            }
        }
    }
    }
}

@Composable
private fun DownloadFilterPill(label: String, choices: List<String>, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelLarge, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                AppSymbol(AppIcon.Expand, Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(option) }) }
        }
    }
}

@Composable
internal fun SettingsScreen(model: DownloaderViewModel, onAbout: () -> Unit, siteDownload: SiteDownloadViewModel) {
    var showSiteCookies by remember { mutableStateOf(false) }
    if (showSiteCookies) SiteDownloadCookiesDialog(siteDownload) { showSiteCookies = false }
    var showClear by remember { mutableStateOf(false) }
    var editFolder by remember { mutableStateOf(false) }
    var folder by remember(model.outputFolder) { mutableStateOf(model.outputFolder) }
    var invalid by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let { model.chooseOutputTree(it) } }
    val busy = model.resolving || model.searching || model.loadingDetails || model.loadingSources || model.clearingSession
    var showHealth by remember { mutableStateOf(false) }
    if (showHealth) AlertDialog(onDismissRequest = { showHealth = false }, title = { Text("Source health") }, text = { Text(model.sourceHealthHistory() + "\n\nBrowser verification does not guarantee HTTP acceptance.", Modifier.verticalScroll(rememberScrollState())) }, confirmButton = { TextButton(onClick = { showHealth = false; model.verifyManually() }) { Text("Verify") } }, dismissButton = { TextButton(onClick = { showHealth = false }) { Text("Close") } })
    var showRecovery by remember { mutableStateOf(false) }
    var diagnosticPreview by remember { mutableStateOf<String?>(null) }
    var exportingDiagnostic by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val catalogSourcePreferences = remember(context) { CatalogSourcePreferences.also { it.attach(context) } }
    val catalogSourceOrder by catalogSourcePreferences.state.collectAsState()
    var editEnabledSources by remember { mutableStateOf(false) }
    var editSourceOrder by remember { mutableStateOf(false) }
    if (editEnabledSources) AlertDialog(
        onDismissRequest = { editEnabledSources = false },
        title = { Text("Sources shown") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Text("Choose which sources appear on Home and Browse. Keep at least one turned on.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                catalogSourceOrder.order.forEach { source ->
                    val checked = source in catalogSourceOrder.enabled
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(source.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Checkbox(checked = checked,
                            enabled = !checked || catalogSourceOrder.enabled.size > 1,
                            onCheckedChange = { catalogSourcePreferences.setEnabled(source, it) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { editEnabledSources = false }) { Text("Done") } },
    )
    if (editSourceOrder) AlertDialog(
        onDismissRequest = { editSourceOrder = false },
        title = { Text("Source order") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                Text("Move sources to change their order on Home and Browse.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp))
                catalogSourceOrder.order.forEachIndexed { index, source ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(source.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { catalogSourcePreferences.move(source, -1) }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${source.label} up")
                        }
                        IconButton(onClick = { catalogSourcePreferences.move(source, 1) }, enabled = index < catalogSourceOrder.order.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${source.label} down")
                        }
                    }
                    if (index < catalogSourceOrder.order.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = { editSourceOrder = false }) { Text("Done") } },
    )
    val scope = rememberCoroutineScope()
    val diagnosticPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val contents = exportingDiagnostic
        exportingDiagnostic = null
        if (uri != null && contents != null) scope.launch {
            try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(contents.toByteArray()) } ?: error("Cannot write report") } }
            catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; android.widget.Toast.makeText(context, "Could not export diagnostics", android.widget.Toast.LENGTH_LONG).show() }
        }
    }
    if (showRecovery) AlertDialog(onDismissRequest = { showRecovery = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_storage_and_recovery)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val report = model.storageReport
            Text(if (report == null) "Checking storage…" else "App storage available: ${Formatter.formatShortFileSize(context, report.available)}\nTemporary media: ${Formatter.formatShortFileSize(context, report.retained)}\nFailed saves: ${report.failed}\nInaccessible saved files: ${report.inaccessible}")
            Text(androidx.compose.ui.res.stringResource(R.string.ui_saving_hls_needs_space_for_cached_segments_an_assembled_video_and))
            TextButton(onClick = model::retryFailedSaves) { Text(androidx.compose.ui.res.stringResource(R.string.ui_retry_failed_saves)) }
            TextButton(onClick = { showRecovery = false; model.showSettings = false; model.showDownloads = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_repair_relink_or_delete_in_downloads)) }
            TextButton(onClick = model::inspectLibrary) { Text(androidx.compose.ui.res.stringResource(R.string.ui_refresh_storage_check)) }
        }
    }, confirmButton = { TextButton(onClick = { showRecovery = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_done)) } })
    diagnosticPreview?.let { preview -> AlertDialog(onDismissRequest = { diagnosticPreview = null }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_preview_diagnostics)) },
        text = { Text(preview, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { exportingDiagnostic = preview; diagnosticPreview = null; diagnosticPicker.launch("animepahe-diagnostics.txt") }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_save_report)) } },
        dismissButton = { TextButton(onClick = { diagnosticPreview = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } }) }
    var choice by remember { mutableStateOf<String?>(null) }
    var section by rememberSaveable { mutableStateOf<String?>(null) }
    val themeBackground by SettingsStore.themeBackground.collectAsState()
    val themeAccent by SettingsStore.themeAccent.collectAsState()
    val themeStyle by SettingsStore.themeStyle.collectAsState()
    val themeContrast by SettingsStore.themeContrast.collectAsState()
    androidx.activity.compose.BackHandler(enabled = section != null) { section = null }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (section != null) IconButton(onClick = { section = null }) { AppSymbol(AppIcon.Back, description = "Back to settings") }
            Text(settingsSectionTiles.firstOrNull { it.id == section }?.title ?: "Settings",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 46.sp), fontWeight = FontWeight.Black,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if (section == null) ReferenceSettingsIndex { if (it == "about") onAbout() else section = it }
        else LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (section) {
                "downloads" -> {
                    item(contentType = "setting-switch") {
                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppSymbol(AppIcon.Download, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) { Text("Download on Wi-Fi only", style = MaterialTheme.typography.titleSmall); Text("Pause downloads on metered networks", style = MaterialTheme.typography.bodySmall) }
                            Switch(checked = model.unmeteredOnly, onCheckedChange = model::setUnmetered)
                        }
                    }
                    item(contentType = "settings-row") { SettingsRow(AppIcon.Download, "Download speed", "${model.simultaneousDownloads} at a time") { choice = "speed" } }
                    item(contentType = "settings-row") { SettingsRow(AppIcon.Folder, "Download storage", outputLabel(model)) { choice = "storage" } }
                    item(contentType = "settings-row") { SettingsRow(AppIcon.Folder, "Storage and recovery", "Check free space and missing files") { model.inspectLibrary(); showRecovery = true } }
                }
                "player" -> {
                    item(contentType = "playback-setting") { PlaybackPreference("play-on-open", "Play when an episode opens", "Start playing automatically") }
                    item(contentType = "playback-setting") { PlaybackPreference("auto-next", "Play the next episode", "Continue automatically when an episode finishes") }
                    item(contentType = "settings-description") { Text("Downloaded episodes play first. Your place is shared between online and downloaded playback.", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                "notifications" -> { item(contentType = "notification-settings") { NotificationPreferences() } }
                "subtitles" -> { item(contentType = "subtitle-settings") { ProviderSubtitleSettings() } }
                "servers" -> {
                    item(contentType = "source-setting") { CatalogSourceSettingRow("Enabled sources", "Sources available on Home and Browse", "${catalogSourceOrder.enabled.size} of ${catalogSourceOrder.order.size}") { editEnabledSources = true } }
                    item(contentType = "source-setting") {
                        val enabledInOrder = catalogSourceOrder.order.filter { it in catalogSourceOrder.enabled }
                        val summary = enabledInOrder.take(2).joinToString(" · ") { it.label } +
                            if (enabledInOrder.size > 2) " +${enabledInOrder.size - 2}" else ""
                        CatalogSourceSettingRow("Source priority", "Order used for the source chips on Home and Browse", summary) { editSourceOrder = true }
                    }
                    item(contentType = "source-setting") { CatalogSourceSettingRow("Browser cookies", "Manage saved verification for video sources", "") { showSiteCookies = true } }
                }
                "accounts" -> { item(contentType = "settings-heading") { Text("Saved on this device", style = MaterialTheme.typography.titleMedium) }; item(contentType = "settings-description") { Text("Your saved shows and viewing progress stay on this device.", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                "appearance" -> {
                    item(contentType = "appearance-settings") {
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Choose a color theme for the app. It updates immediately.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ThemePreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = preset.matches(themeBackground, themeAccent, themeStyle, themeContrast),
                                        onClick = { SettingsStore.applyThemePreset(preset) },
                                        label = { Text(preset.label) },
                                    )
                                }
                            }
                            OutlinedButton(onClick = SettingsStore::resetTheme) { Text("Reset theme") }
                        }
                    }
                }
                "content" -> { item(contentType = "settings-heading") { Text("Short dramas", style = MaterialTheme.typography.titleMedium) }; item(contentType = "settings-description") { Text("Choose a short drama source from Home or Browse to explore its catalog.", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                "data" -> {
                    item(contentType = "settings-description") { Text("Temporary storage: ${Formatter.formatShortFileSize(context, model.temporaryBytes)}", Modifier.padding(vertical = 12.dp)) }
                    item(contentType = "settings-row") { SettingsRow(AppIcon.Info, "Export diagnostics", "Preview a report without titles, paths, or cookies") { diagnosticPreview = diagnostics(context) } }
                    item(contentType = "settings-row") { SettingsRow(AppIcon.Folder, "Storage and recovery", "Check free space and missing files") { model.inspectLibrary(); showRecovery = true } }
                }
                "app" -> { item(contentType = "settings-row") { SettingsRow(AppIcon.Info, "About", "Version and what’s new", onAbout) } }
            }
            model.settingsMessage?.let { message -> item(contentType = "settings-message") { Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) } }
        }
    }
    choice?.let { selected ->
        AlertDialog(onDismissRequest = { choice = null }, title = { Text(when (selected) {
            "storage" -> "Download storage"
            "audio" -> "Default audio"
            "speed" -> "Download speed"
            else -> "Browser session"
        }) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (selected) {
                    "storage" -> {
                        DestinationSummary(outputLabel(model))
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_new_videos_are_grouped_in_show_folders), style = MaterialTheme.typography.bodySmall)
                        FilledTonalButton(onClick = { choice = null; picker.launch(model.outputTree.takeIf { it.isNotBlank() }?.let(Uri::parse)) }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_choose_folder)) }
                        TextButton(onClick = { choice = null; editFolder = true; invalid = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_use_downloads)) }
                    }
                    "audio" -> {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_if_unavailable_uses_the_source_default_audio_can_also_be_changed_), style = MaterialTheme.typography.bodySmall)
                        listOf("Original", "jpn", "eng").forEach { code ->
                            TextButton(onClick = { model.updateDefaultAudio(code); choice = null }, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(selected = model.defaultAudio == code, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (code == "Original") "Source default" else audioLabel(code), Modifier.weight(1f))
                            }
                        }
                    }
                    "speed" -> {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_episodes_at_a_time), style = MaterialTheme.typography.titleSmall)
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_other_episodes_wait_their_turn_direct_file_downloads_are_managed_), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 2, 3, 4, 6).forEach { count ->
                                FilterChip(selected = model.simultaneousDownloads == count, onClick = { model.updateSimultaneousDownloads(count) }, label = { Text("$count") })
                            }
                        }
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_download_connections), style = MaterialTheme.typography.titleSmall)
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_more_connections_may_speed_up_downloads_but_use_more_network_reso), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 4, 8, 12).forEach { count ->
                                FilterChip(selected = model.connections == count, onClick = { model.setDownloadConnections(count) }, label = { Text("$count") })
                            }
                        }
                    }
                    else -> {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_saved_cookies_are_shared_with_searches_and_downloads))
                        FilledTonalButton(onClick = { choice = null; model.verifyManually() }, enabled = !busy) { Text(androidx.compose.ui.res.stringResource(R.string.ui_open_verification)) }
                        OutlinedButton(onClick = { choice = null; showClear = true }, enabled = !busy) { Text(if (model.clearingSession) "Clearing…" else "Clear session data") }
                        if (busy) Text(androidx.compose.ui.res.stringResource(R.string.ui_available_when_the_current_request_finishes), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { choice = null }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_done)) } })
    }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, icon = { AppSymbol(AppIcon.Shield) },
        title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_clear_your_session)) }, text = { Text(androidx.compose.ui.res.stringResource(R.string.ui_you_may_need_to_verify_again_this_removes_cookies_website_data_an)) },
        confirmButton = { TextButton(onClick = { showClear = false; model.clearSessionData() }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_clear_session)) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } })
    if (editFolder) AlertDialog(onDismissRequest = { editFolder = false }, title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_save_inside_downloads)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(androidx.compose.ui.res.stringResource(R.string.ui_choose_a_subfolder_or_leave_empty_to_use_downloads_itself))
            OutlinedTextField(value = folder, onValueChange = { folder = it; invalid = false }, singleLine = true,
                label = { Text(androidx.compose.ui.res.stringResource(R.string.ui_subfolder)) }, prefix = { Text("Downloads/") }, isError = invalid,
                supportingText = { if (invalid) Text(androidx.compose.ui.res.stringResource(R.string.ui_use_folder_names_without_or_special_characters)) })
        } }, confirmButton = { TextButton(onClick = { if (model.saveOutputFolder(folder)) editFolder = false else invalid = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_save)) } },
        dismissButton = { TextButton(onClick = { editFolder = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_cancel)) } })
}

@Composable
private fun DownloaderSettingsSection(title: String) {
    Text(title, Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun AboutScreen() {
    val context = LocalContext.current
    val info = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(contentType = "about-version") {
            Text(androidx.compose.ui.res.stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text("Version ${info.versionName ?: "Unknown"} · Build ${info.longVersionCode}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(contentType = "release-notes-heading") {
            Text(androidx.compose.ui.res.stringResource(R.string.ui_what_s_new), style = MaterialTheme.typography.titleLarge)
            Text("Version ${info.versionName ?: "Unknown"}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        items(listOf(
            "Faster video saving and smoother progress updates.",
            "Custom video links and series downloads.",
            "Clearer download statuses and a simpler downloads list.",
            "Anilili-inspired AMOLED styling and external playback."
        ), key = { it }, contentType = { "release-note" }) { change ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("•", color = MaterialTheme.colorScheme.primary)
                Text(change, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
