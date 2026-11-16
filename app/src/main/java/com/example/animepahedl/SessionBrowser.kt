package com.example.animepahedl

import androidx.core.net.toUri
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
@SuppressLint("SetJavaScriptEnabled")
internal fun SessionBrowser(url: String, onClose: (Boolean) -> Unit, onResetNarto: (() -> Unit)? = null) {
    val context = LocalContext.current
    val close by rememberUpdatedState(onClose)
    var location by remember(url) { mutableStateOf(url) }
    var reason by remember(url) { mutableStateOf("Loading verification page…") }
    var loading by remember(url) { mutableStateOf(true) }
    var pageError by remember(url) { mutableStateOf<String?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var consecutiveChecks by remember(url) { mutableIntStateOf(0) }
    val browser = remember(url) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.userAgentString = AnimeNetwork.userAgent
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.scheme != "https") return true
                    val address = request.url.toString()
                    val page = verificationPage(address)
                    if (page != address) { view.loadUrl(page); return true }
                    return false
                }
                override fun onPageStarted(view: WebView, address: String, favicon: Bitmap?) {
                    location = address
                    loading = true
                    pageError = null
                    consecutiveChecks = 0
                    DebugTrace.event("browser_load", "url" to DebugTrace.safeUrl(address))
                }
                override fun onPageFinished(view: WebView, address: String) {
                    location = address
                    loading = false
                    CookieManager.getInstance().flush()
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) {
                        pageError = "Destination returned HTTP ${response.statusCode}"
                        DebugTrace.event("browser_http_error", "url" to DebugTrace.safeUrl(request.url.toString()), "status" to response.statusCode)
                    }
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        pageError = error.description.toString()
                        loading = false
                        DebugTrace.event("browser_load_error", "url" to DebugTrace.safeUrl(request.url.toString()), "code" to error.errorCode)
                    }
                }
            }
            loadUrl(verificationPage(url))
        }
    }
    LaunchedEffect(browser) {
        while (true) {
            delay(1000)
            val state = suspendCancellableCoroutine<String> { continuation ->
                browser.evaluateJavascript(SessionChecks.browserStateScript) { value ->
                    if (continuation.isActive) continuation.resume(value ?: "null")
                }
            }
            val result = runCatching {
                val value = JsonParser.parseString(state)
                (if (value.isJsonPrimitive && value.asJsonPrimitive.isString) JsonParser.parseString(value.asString) else value).asJsonObject
            }.getOrNull()
            val ready = result?.get("ready")?.asBoolean == true && !loading && pageError == null &&
                verificationOriginMatches(url, location)
            reason = pageError ?: result?.get("reason")?.asString ?: "Waiting for page state…"
            consecutiveChecks = if (ready) consecutiveChecks + 1 else 0
            DebugTrace.event("browser_check", "url" to DebugTrace.safeUrl(location), "ready" to ready, "consecutive" to consecutiveChecks)
            if (consecutiveChecks >= 2) {
                CookieManager.getInstance().flush()
                DebugTrace.event("browser_complete", "url" to DebugTrace.safeUrl(location))
                close(true)
                break
            }
        }
    }
    DisposableEffect(browser) { onDispose { browser.stopLoading(); browser.destroy() } }
    Dialog(onDismissRequest = { close(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints(Modifier.safeDrawingPadding()) {
            val headerHeight = maxHeight * 0.45f
            Column {
            Column(Modifier.heightIn(max = headerHeight).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { close(false) }) { AppSymbol(AppIcon.Close, description = "Close verification") }
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_session_verification), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text((location).toUri().host ?: "Secure browser", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { browser.reload() }) { AppSymbol(AppIcon.Refresh, description = "Reload page") }
                }
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (pageError != null) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else AppSymbol(AppIcon.Shield, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (pageError != null) "Page needs attention" else if (consecutiveChecks > 0) "Page ready" else "Complete the check below",
                                    style = MaterialTheme.typography.titleSmall)
                                Text(reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(androidx.compose.ui.res.stringResource(R.string.ui_once_the_page_is_ready_your_session_is_saved_and_the_app_retries_),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(androidx.compose.ui.res.stringResource(R.string.ui_secure_browser), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { showHelp = true }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_need_help)) }
                }
                if (onResetNarto != null) TextButton(onClick = { close(false); onResetNarto() }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Clear Narto cookies and retry")
                }
            }
                Surface(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    shape = RoundedCornerShape(32.dp), tonalElevation = 2.dp) {
                    Column {
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        AndroidView(factory = { browser }, modifier = Modifier.fillMaxWidth().weight(1f))
                    }
                }
                Text(androidx.compose.ui.res.stringResource(R.string.ui_cookies_stay_on_your_device_close_at_any_time_to_return_to_the_ap),
                    Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, icon = { AppSymbol(AppIcon.Shield) },
        title = { Text(androidx.compose.ui.res.stringResource(R.string.ui_verifying_your_session)) },
        text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = "1. Wait for the website to load.\n\n2. Complete any verification prompt yourself.\n\n3. When the destination loads, the app returns automatically and retries once.\n\nIf verification keeps failing, reload this page or clear session data in Settings and try again. Some sites may accept the browser but still reject app requests.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text(androidx.compose.ui.res.stringResource(R.string.ui_got_it)) } })
}
