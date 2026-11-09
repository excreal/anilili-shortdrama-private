package com.example.animepahedl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.example.animepahedl.components.*
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogScreen(
    model: CatalogViewModel, images: ImageLoader, browseMode: Boolean = false,
    onExpand: () -> Unit = {}, onOpen: (String) -> Unit, onLink: () -> Unit,
    continueWatching: (@Composable () -> Unit)? = null,
    backdrop: dev.chrisbanes.haze.HazeState? = null,
    onDownloads: () -> Unit = {},
) {
    var filters by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf(true) }
    val heroBackdrop = remember { dev.chrisbanes.haze.HazeState() }
    val grid = rememberLazyGridState()
    val phoneCardWidth = ((LocalConfiguration.current.screenWidthDp - 62) / 3.35f).coerceIn(108f, 150f).dp
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(model.source) { mutableStateOf(emptyList<String>()) }
    // Keep the short-drama catalog at the original dense poster size on phones.
    val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 4 else 3
    val searchSourceName = if (model.source == CatalogSource.KISSKH) "KissKH" else model.source.label
    LaunchedEffect(grid, model.items.size, model.loading, browseMode) {
        snapshotFlow { browseMode && model.items.isNotEmpty() && !model.loading && model.nextUrl != null &&
            (grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >= grid.layoutInfo.totalItemsCount - 5 }
            .distinctUntilChanged().collect { if (it) model.more() }
    }
    LazyVerticalGrid(state = grid, columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(bottom = 108.dp), horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(if (browseMode) 12.dp else 0.dp)) {
        if (!browseMode) item(span = { GridItemSpan(maxLineSpan) }, contentType = "home-hero") {
            Box(Modifier.fillMaxWidth()) {
                if (model.items.isNotEmpty()) CatalogHero(model.items, images, onOpen, backdrop = heroBackdrop)
                else Spacer(Modifier.height(100.dp))
                ReferenceHomeChrome(model, heroBackdrop, onLink, onDownloads)
            }
        }
        if (browseMode) item(span = { GridItemSpan(maxLineSpan) }, contentType = "browse-controls") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Browse", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp), fontWeight = FontWeight.Black)
                    TextButton(onClick = model::reset) { Text("Reset") }
                    IconButton(onClick = { categories = !categories }) {
                        AppSymbol(AppIcon.Expand, description = if (categories) "Hide source shortcuts" else "Show source shortcuts")
                    }
                }
                Text("Find your next story", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(model.query, model::edit,
                        Modifier.weight(1f).semantics { contentDescription = "Search $searchSourceName" }, singleLine = true,
                        placeholder = { Text("Search $searchSourceName…", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        shape = MaterialTheme.shapes.medium,
                        leadingIcon = { AppSymbol(AppIcon.Browse) },
                        trailingIcon = { if (model.query.isNotBlank()) IconButton(onClick = { model.edit("") }) { AppSymbol(AppIcon.Close, description = "Clear search") } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { model.refresh() }))
                    FilledIconButton(onClick = { filters = true }, modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                        AppSymbol(AppIcon.Sort, description = "Select source")
                    }
                }
            }
        }
        if (browseMode && categories) item(span = { GridItemSpan(maxLineSpan) }, contentType = "source-shortcuts") {
            Column(Modifier.fillMaxWidth()) {
            Text("Sources", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(model.availableSources, key = { it.name }) { source ->
                    OutlinedButton(onClick = { model.select(source) }, modifier = Modifier.semantics { selected = model.source == source }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp), shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, containerColor = if (model.source == source) Color.White.copy(alpha = .12f) else Color.Transparent)) { Text(source.label) }
                }
            }
            HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (!browseMode && continueWatching != null) item(span = { GridItemSpan(maxLineSpan) }, contentType = "continue-watching") { continueWatching() }
        if (!browseMode && model.items.isNotEmpty()) catalogHomeSections(model.items, images, columns, phoneCardWidth, expanded,
            toggle = { name -> expanded = if (name in expanded) expanded - name else expanded + name },
            onOpen = onOpen, onMore = model::more, loading = model.loading, hasMore = model.nextUrl != null)
        if (browseMode) item(span = { GridItemSpan(maxLineSpan) }, contentType = "results-heading") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(model.filterLabel ?: if (model.query.isNotBlank()) "Results for “${model.query}”" else "Discover ${model.source.label}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${model.items.size}${if (model.nextUrl != null) "+" else ""} results", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (browseMode) items(model.items, key = { it.url }, contentType = { "catalog-poster" }) { entry ->
            CatalogPosterCard(entry, images, { onOpen(entry.url) }, Modifier.padding(horizontal = 5.dp))
        }
        if (model.loading) item(span = { GridItemSpan(maxLineSpan) }, contentType = "loading-indicator") {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { WaterFillLogoIndicator(size = 64.dp) }
        }
        model.error?.let { message -> item(span = { GridItemSpan(maxLineSpan) }, contentType = "catalog-error") {
            Column(Modifier.padding(16.dp)) { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = model::refresh) { Text("Retry") } }
        } }
        if (!model.loading && model.error == null && model.items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }, contentType = "catalog-empty") {
            Text("No titles found.", Modifier.padding(16.dp))
        }
    }
    if (filters) ModalBottomSheet(onDismissRequest = { filters = false }) {
        Text("Source", Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
        model.availableSources.forEach { source ->
            TextButton(onClick = { model.select(source); filters = false }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                RadioButton(selected = model.source == source, onClick = null)
                Spacer(Modifier.width(12.dp)); Text(source.label, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}
