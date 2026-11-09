@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.animepahedl

import com.example.animepahedl.components.*
import androidx.compose.animation.animateContentSize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SiteDownloadScreen(
    model: SiteDownloadViewModel,
    downloads: DownloaderViewModel,
    images: ImageLoader,
    onEnqueue: () -> Boolean,
    onWatch: () -> Unit = {},
    onWatchEpisode: ((String) -> Unit)? = null,
    onBack: () -> Unit = {},
    saved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onExploreMetadata: (String, SiteShowTerm) -> Unit = { _, _ -> },
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var historyRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("playback-history", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> historyRevision++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val history = remember(context, historyRevision) { PlaybackHistory(context) }
    var cookiesOpen by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var episodesTab by remember(model.series?.id) { mutableStateOf(false) }
    var overviewExpanded by remember(model.series?.id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 108.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if ((!model.inspecting && model.series == null)) item(contentType = "link-entry") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Open a video link", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(model.input, model::updateInput, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    label = { Text("Video or series URL") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { keyboard?.hide(); model.inspect() }))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { keyboard?.hide(); model.inspect() }, enabled = model.input.isNotBlank() && !model.inspecting) { Text("Open") }
                    TextButton(onClick = { cookiesOpen = true }) { Text("Browser cookies") }
                    if (model.inspecting) TextButton(onClick = model::cancelInspect) { Text("Cancel") }
                }
            }
        }
        if (model.inspecting) item(contentType = "loading") { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { WaterFillLogoIndicator(size = 72.dp) } }
        model.notice?.let { notice -> item(contentType = "notice") {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(notice, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = model::dismissNotice) { AppSymbol(AppIcon.Close, description = "Dismiss notice") }
            }
        } }
        model.error?.let { message -> item(contentType = "error") {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) { Text(message); TextButton(onClick = model::dismissError) { Text("Dismiss") } }
            }
        } }
        model.series?.let { series ->
            item(contentType = "detail-hero") { RemoteDetailHero(series, images) }
            item(contentType = "primary-actions") {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { confirm = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                        enabled = model.ready && !model.preparing && !model.discarding && model.pending == null) {
                        AppSymbol(AppIcon.Download, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Download")
                    }
                    Button(onClick = onWatch, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) {
                        AppSymbol(AppIcon.Play, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text(if (history.canResume(series)) "Resume" else "Play")
                    }
                }
            }
            item(contentType = "playback-summary") {
                val summary = rememberSeriesPlaybackSummary(series)
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    summary.resumeLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(summary.downloadLabel + if (summary.watched > 0) " · ${summary.watchLabel}" else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            series.metadata?.let { metadata -> item(contentType = "metadata-highlights") { SiteMetadataHighlights(series.provider, metadata, onExploreMetadata) } }
            if (!series.isSingleVideo) stickyHeader(contentType = "episode-tabs") {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(false to "Overview", true to "Episodes").filterNot { it.first && series.isSingleVideo }.forEach { (value, label) ->
                            val active = episodesTab == value
                            Text(label, style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(18.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .selectable(selected = active, role = Role.Tab, onClick = { episodesTab = value })
                                    .padding(horizontal = 18.dp, vertical = 14.dp))
                        }
                    }
                }
            }
            if (!episodesTab) {
                val overview = series.overviewText()
                if (overview.isNotBlank()) item {
                    var overviewOverflows by remember(series.id, overview) { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize()) {
                        Text("Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(overview, Modifier.padding(top = 7.dp), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (overviewExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis, onTextLayout = { overviewOverflows = it.hasVisualOverflow })
                        if (overviewOverflows || overviewExpanded) {
                            TextButton(onClick = { overviewExpanded = !overviewExpanded },
                                modifier = Modifier.padding(top = 2.dp), contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                                Text(if (overviewExpanded) "Show less" else "More")
                            }
                        }
                    }
                }

            } else {
                item(contentType = "episode-heading") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${series.episodes.size} ${if (series.episodes.size == 1) "episode" else "episodes"}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { confirm = true }, enabled = model.ready && !model.preparing && model.pending == null) { Text("Download episodes") }
                    }
                }
                items(series.episodes, key = { it.id }, contentType = { "episode-row" }) { episode ->
                    val number = episode.displayNumber.filter(Char::isDigit)
                    val metadata = series.metadata?.episodes?.firstOrNull { it.number.filter(Char::isDigit) == number }
                    Surface(onClick = { onWatchEpisode?.invoke(episode.id) ?: onWatch() },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(episode.displayNumber.padStart(2, '0'), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(Modifier.weight(1f)) {
                                Text("Episode ${episode.displayNumber}", style = MaterialTheme.typography.titleSmall)
                                if (episode.title.isNotBlank() && !episode.title.equals("Episode ${episode.displayNumber}", true)) Text(episode.title,
                                    style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                metadata?.description?.takeIf(String::isNotBlank)?.let { Text(it,
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                metadata?.let { detail ->
                                    val facts = listOfNotNull(detail.releaseDate, detail.durationSeconds?.let(::metadataDurationLabel),
                                        detail.subtitles.takeIf { it.isNotEmpty() }?.let { "${it.size} subtitle tracks" }).joinToString(" · ")
                                    if (facts.isNotBlank()) Text(facts, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            AppSymbol(AppIcon.Play, description = "Play episode ${episode.displayNumber}")
                        }
                    }
                }
            }
            if (model.pending != null) item(contentType = "pending-download-message") { Text("Preparing downloads in the background. Manage them in Downloads.",
                Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (model.series != null) Row(
        Modifier.fillMaxWidth().chromeFade(overHero = true).statusBarsPadding().height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { AppSymbol(AppIcon.Back, Modifier.offset(x = (-8).dp), description = "Back") }
        Text("Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleSave) {
            AppSymbol(if (saved) AppIcon.BookmarkFilled else AppIcon.Bookmark, Modifier.offset(x = 8.dp),
                description = if (saved) "Unsave show" else "Save show")
        }
    }
    }
    if (cookiesOpen) SiteDownloadCookiesDialog(model, onClose = { cookiesOpen = false })
    if (confirm) model.series?.let { series ->
        RemoteDownloadSheet(series, model, downloads, images, onDismiss = { confirm = false }, onConfirm = {
            confirm = false
            if (onEnqueue()) model.downloadSelected()
        })
    }
}

@Composable
private fun RemoteDetailHero(series: SiteDownloadSeries, images: ImageLoader) {
    val canvas = MaterialTheme.colorScheme.background
    val backdrop = series.metadata?.images?.firstOrNull { it.role in setOf("banner", "backdrop", "featured_artwork") }?.url ?: series.poster
    val poster = series.posterThumbnail()
    val bannerArtwork = series.provider == SiteDownloadProvider.KISSKH
    Box(Modifier.fillMaxWidth().height(286.dp)) {
        AsyncImage(model = backdrop, imageLoader = images, contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(190.dp).background(Brush.verticalGradient(listOf(Color.Transparent, canvas))))
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
            Thumbnail(poster, series.title, images,
                Modifier.width(if (bannerArtwork) 120.dp else 108.dp)
                    .aspectRatio(if (bannerArtwork) 16f / 9f else 2f / 3f).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().heightIn(min = 60.dp), contentAlignment = Alignment.BottomStart) {
                    Text(series.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Text(series.provider.contentLabel, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = .16f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp))
                Text(if (series.isSingleVideo) "Full video" else "${series.episodes.size} episodes", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SiteMetadataHighlights(provider: SiteDownloadProvider, metadata: SiteShowMetadata, onExplore: (String, SiteShowTerm) -> Unit) {
    val rating = metadata.ratings.firstOrNull { it.value != null }
    val people = metadata.people.take(6).groupBy { it.role?.replaceFirstChar(Char::uppercase) ?: "People" }
    val facts = listOfNotNull(metadata.year?.toString(), metadata.status, metadata.countries.firstOrNull(),
        metadata.languages.firstOrNull(), metadata.studios.firstOrNull(),
        metadata.episodeCount?.takeUnless { provider in setOf(SiteDownloadProvider.EROTICMV, SiteDownloadProvider.DIRECT) }
            ?.let { "$it ${if (it == 1) "episode" else "episodes"}" },
        metadata.durationSeconds?.let(::metadataDurationLabel)).distinct()
    if (rating == null && facts.isEmpty() && metadata.genres.isEmpty() && metadata.categories.isEmpty() && metadata.tags.isEmpty() &&
        metadata.genreTerms.isEmpty() && metadata.categoryTerms.isEmpty() && metadata.tagTerms.isEmpty() && people.isEmpty() && metadata.alternativeTitles.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rating?.let {
            val value = java.text.DecimalFormat("#.#").format(it.value)
            val scale = it.scale?.let { scoreScale -> "/${java.text.DecimalFormat("#.#").format(scoreScale)}" }.orEmpty()
            val votes = it.votes?.takeIf { count -> count > 0 }?.let { count -> " · ${java.text.NumberFormat.getIntegerInstance().format(count)} votes" }.orEmpty()
            Text("★ $value$scale$votes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        MetadataTermChips("Genres", "genre", metadata.genreTerms, metadata.genres, provider, onExplore)
        MetadataTermChips("Categories", "category", metadata.categoryTerms, metadata.categories, provider, onExplore)
        MetadataTermChips("Tags", "tag", metadata.tagTerms, metadata.tags, provider, onExplore)
        metadata.alternativeTitles.take(3).takeIf { it.isNotEmpty() }?.let { Text("Also known as ${it.joinToString(" · ")}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        people.forEach { (role, names) -> Text("$role: ${names.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun MetadataTermChips(
    label: String,
    kind: String,
    terms: List<SiteShowTerm>,
    legacyNames: List<String>,
    provider: SiteDownloadProvider,
    onExplore: (String, SiteShowTerm) -> Unit,
) {
    val linked = terms.distinctBy { it.url ?: it.id ?: it.name }
    val unique = (linked + legacyNames.filterNot { old -> linked.any { it.name.equals(old, true) } }.map { SiteShowTerm(it) }).take(16)
    if (unique.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(unique, key = { it.url ?: it.id ?: it.name }, contentType = { "metadata-term" }) { term ->
                val filterable = when (provider) {
                    SiteDownloadProvider.KISS18 -> kind in setOf("tag", "category", "genre")
                    SiteDownloadProvider.DRAMATIP, SiteDownloadProvider.NARTO, SiteDownloadProvider.EROTICMV -> term.url != null
                    else -> false
                }
                AssistChip(onClick = { if (filterable) onExplore(kind, term) }, enabled = filterable,
                    label = { Text(term.name, maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
        }
    }
}

private fun metadataDurationLabel(seconds: Long): String = when {
    seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    seconds >= 60 -> "${seconds / 60} min"
    else -> "$seconds sec"
}

@Composable
private fun RemoteDownloadSheet(series: SiteDownloadSeries, model: SiteDownloadViewModel, downloads: DownloaderViewModel,
    images: ImageLoader, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val selected = remember(model.selectedEpisodes) { model.selectedEpisodes.map { it.id }.toSet() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Thumbnail(series.posterThumbnail(), series.title, images, Modifier.size(56.dp, 76.dp), cornerRadius = 6.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (series.isSingleVideo) "DOWNLOAD VIDEO" else "DOWNLOAD EPISODES", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp), fontWeight = FontWeight.W800, color = MaterialTheme.colorScheme.primary)
                    Text(series.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W800, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (series.isSingleVideo) series.provider.contentLabel else "${series.episodes.size} episodes available", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { AppSymbol(AppIcon.Close, description = "Close download sheet") }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { DownloadSectionLabel("Quality") }
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (listOf("Best") + series.qualities).distinct().forEach { quality ->
                            val active = model.quality == quality
                            Box(Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else Color.Transparent)
                                .selectable(selected = active, enabled = !model.preparing, role = Role.Tab, onClick = { model.chooseQuality(quality) }),
                                contentAlignment = Alignment.Center) {
                                Text(quality, style = MaterialTheme.typography.bodyMedium, fontWeight = if (active) FontWeight.W700 else FontWeight.W600,
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (!series.isSingleVideo) {
                    item { DownloadSectionLabel("Choose episodes") }
                    item {
                        OutlinedTextField(model.selection, model::updateSelection, modifier = Modifier.fillMaxWidth(),
                            label = { Text("Episode ranges") }, placeholder = { Text("1-5, 9, 12-20") }, singleLine = true,
                            shape = RoundedCornerShape(10.dp), isError = model.selectionError != null,
                            supportingText = { Text(model.selectionError ?: "Blank selects all. Combine ranges with commas.") })
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${selected.size} selected", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            TextButton(onClick = { model.updateSelection("") }) { Text("All") }
                            TextButton(onClick = { model.updateSelection("none") }) { Text("Clear") }
                        }
                    }
                    items(series.episodes, key = { it.id }, contentType = { "download-episode-choice" }) { episode ->
                        val checked = episode.id in selected
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (checked) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else MaterialTheme.colorScheme.surfaceContainerLow)
                            .selectable(checked, role = Role.Checkbox, onClick = { model.toggleEpisode(episode) })
                            .heightIn(min = 46.dp).padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text("Episode ${episode.displayNumber}", style = MaterialTheme.typography.bodyMedium, fontWeight = if (checked) FontWeight.W700 else FontWeight.W600)
                                if (episode.title.isNotBlank() && !episode.title.equals("Episode ${episode.displayNumber}", true)) Text(episode.title,
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(1.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center) {
                                if (checked) CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                                    AppSymbol(AppIcon.Check, Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                DestinationSummary(outputLabel(downloads))
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(8.dp),
                    enabled = model.ready && model.selectionError == null && selected.isNotEmpty() && !downloads.checkingExistingFiles &&
                        model.pending == null && !model.preparing && !model.discarding) {
                    Text((if (series.isSingleVideo) "Download video" else "Download ${selected.size} episode${if (selected.size == 1) "" else "s"}").uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp), fontWeight = FontWeight.W800)
                }
            }
        }
    }
}

@Composable
private fun DownloadSectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
        fontWeight = FontWeight.W800, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
internal fun SiteDownloadCookiesDialog(model: SiteDownloadViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readNBytes(262_145)
                        require(bytes.size <= 262_144)
                        bytes.toString(Charsets.UTF_8)
                    } ?: error("Cannot read cookie file")
                }
                if (model.saveCookies(text)) onClose() else error = "Invalid cookie format. Use a Netscape cookie file."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { error = "Could not import this file. Use a text cookie file no larger than 256 KiB." }
            finally { importing = false }
        }
    }
    AlertDialog(onDismissRequest = { if (!importing) onClose() }, title = { Text("Browser cookies") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import a Netscape cookie file with domain information, or paste its contents. Cookies stay in app-private storage.")
            OutlinedTextField(input, { input = it; error = null }, label = { Text("Netscape cookie text") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), enabled = !importing)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = { importer.launch(arrayOf("text/*", "application/octet-stream")) }, enabled = !importing) { Text("Import cookie file") }
            if (model.hasCookies) TextButton(onClick = { model.clearCookies(); onClose() }, enabled = !importing) { Text("Remove saved cookies") }
        } },
        confirmButton = { TextButton(onClick = {
            if (model.saveCookies(input)) onClose() else error = "Invalid cookie format. Use a Netscape cookie file."
        }, enabled = !importing && input.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onClose, enabled = !importing) { Text("Cancel") } })
}
