package com.example.animepahedl.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.animepahedl.AppIcon
import com.example.animepahedl.AppSymbol
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.animepahedl.CatalogItem
import com.example.animepahedl.ArtworkRecovery
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** Anilili's dense poster presentation, adapted to remote titles without anime metadata. */
@Composable
internal fun CatalogPosterCard(
    item: CatalogItem,
    images: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    metadata: String? = null,
) {
    val context = LocalContext.current
    var poster by remember(item.poster) { mutableStateOf(ArtworkRecovery.cached(context, item.url, item.poster)) }
    var attempted by remember(item.poster) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val request = remember(context, poster, item.url) { catalogArtworkRequest(context, item.copy(poster = poster)) }
    var failed by remember(item.poster) { mutableStateOf(item.poster.isBlank()) }
    val titleStyle = MaterialTheme.typography.labelLarge
    val titleBlockHeight = with(LocalDensity.current) { titleStyle.lineHeight.toDp() * 2 + 4.dp }
    Column(modifier.clickable(role = Role.Button, onClickLabel = "Open details", onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(if (item.wideArtwork) 16f / 9f else 2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (failed) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppSymbol(AppIcon.Play) }
            AsyncImage(
                model = request,
                onError = {
                    failed = true
                    if (!attempted) { attempted = true; scope.launch {
                        try { ArtworkRecovery.poster(item.url, poster)?.let { ArtworkRecovery.retain(context, item.url, item.poster, it); poster = it; failed = false } }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* Keep a stable error placeholder. */ }
                    } }
                },
                onSuccess = { failed = false },
                imageLoader = images,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = item.title,
            style = titleStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 5.dp).heightIn(min = titleBlockHeight),
        )
        if (!metadata.isNullOrBlank()) Text(
            text = metadata,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun catalogArtworkRequest(context: android.content.Context, item: CatalogItem): ImageRequest =
    ImageRequest.Builder(context).data(item.poster)
            .memoryCacheKey("poster-v3:" + item.poster).diskCacheKey("poster-v3:" + item.poster)
            .placeholderMemoryCacheKey("poster-v3:" + item.poster).crossfade(150)
            .apply {
                val detail = item.url.toHttpUrlOrNull()
                val poster = item.poster.toHttpUrlOrNull()
                // Dramatip explicitly marks CDN artwork no-referrer. Other sources receive
                // only their own origin, never the old AnimePahe referral or private URL query.
                if (detail != null && poster != null && detail.host != "dramatip.net" &&
                    poster.host == detail.host) {
                    addHeader("Referer", detail.newBuilder().encodedPath("/").query(null).fragment(null).build().toString())
                }
            }.build()
