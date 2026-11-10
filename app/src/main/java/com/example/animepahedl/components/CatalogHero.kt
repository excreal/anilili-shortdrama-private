package com.example.animepahedl.components

import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.animepahedl.CatalogItem
import kotlinx.coroutines.delay

/** Anilili's phone HeroPager/HeroCard: full-bleed cover, canvas fade and centered title. */
@Composable
internal fun CatalogHero(items: List<CatalogItem>, images: ImageLoader, onOpen: (String) -> Unit, backdrop: dev.chrisbanes.haze.HazeState? = null) {
    val featured = items.distinctBy { it.url }.take(6)
    if (featured.isEmpty()) return
    val pager = rememberPagerState(pageCount = { featured.size })
    val dragged by pager.interactionSource.collectIsDraggedAsState()
    val canvas = MaterialTheme.colorScheme.background
    LaunchedEffect(featured.map { it.url }, pager.settledPage, dragged) {
        if (featured.size > 1 && !dragged) {
            delay(7_000)
            pager.animateScrollToPage((pager.settledPage + 1) % featured.size)
        }
    }
    Box(Modifier.fillMaxWidth().height(490.dp).then(if (backdrop != null) Modifier.hazeSource(backdrop) else Modifier)) {
        HorizontalPager(pager, modifier = Modifier.fillMaxSize(), key = { featured[it].url }) { index ->
            val item = featured[index]
            val context = LocalContext.current
            val artwork = remember(context, item.poster, item.url) { catalogArtworkRequest(context, item) }
            Box(Modifier.fillMaxSize().clickable(onClickLabel = "Open ${item.title}") { onOpen(item.url) }) {
                AsyncImage(model = artwork, imageLoader = images, contentDescription = null,
                    contentScale = ContentScale.Crop, alignment = Alignment.Center, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = .30f), .42f to Color.Black.copy(alpha = .12f),
                    .72f to Color.Black.copy(alpha = .45f), 1f to canvas,
                )))
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SHORT DRAMA", style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = .85f))
                    Text(item.title, Modifier.fillMaxWidth(.9f).padding(top = 10.dp),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 30.sp, shadow = androidx.compose.ui.graphics.Shadow(Color.Black, androidx.compose.ui.geometry.Offset(0f, 2f), 10f)),
                        textAlign = TextAlign.Center, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(featured.size) { index ->
                Box(Modifier.width(if (index == pager.settledPage) 18.dp else 5.dp).height(5.dp).clip(CircleShape)
                    .background(if (index == pager.settledPage) Color.White else Color.White.copy(alpha = .35f)))
            }
        }
    }
}
