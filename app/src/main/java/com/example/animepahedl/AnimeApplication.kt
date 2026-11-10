package com.example.animepahedl

import androidx.core.content.edit
import android.app.Application
import android.webkit.WebSettings
import kotlinx.coroutines.*

class AnimeApplication : Application(), coil.ImageLoaderFactory {
    override fun newImageLoader(): coil.ImageLoader {
        // Image requests must never inherit AnimePahe cookies or its default Referer.
        val cookies = SiteDownloadCookieStore(this)
        val dispatcher = okhttp3.Dispatcher().apply { maxRequests = 6; maxRequestsPerHost = 3 }
        val client = okhttp3.OkHttpClient.Builder().dispatcher(dispatcher)
            .cookieJar(okhttp3.CookieJar.NO_COOKIES)
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val fallback = dramatipPublicPosterUrl(request.url)
                val chosen = if (fallback != null) request.newBuilder().url(fallback).build() else request
                chain.proceed(chosen)
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder().removeHeader("Cookie")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36")
                // Scope imported sessions to the image URL's cookie domain, including redirect hops.
                cookies.headerFor(request.url.toString()).takeIf(String::isNotBlank)?.let { builder.header("Cookie", it) }
                val host = request.url.host
                if (host in setOf("kiss18.vip", "www.kiss18.vip", "narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com"))
                    builder.header("Referer", request.url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString())
                val response = chain.proceed(builder.build())
                // Do not let CDN error pages enter Coil's persistent success cache, even when
                // an origin incorrectly serves its HTML error with status 200.
                val mime = response.header("Content-Type").orEmpty().substringBefore(';').trim()
                if (response.isSuccessful && (mime.equals("text/html", true) || mime.equals("application/json", true))) {
                    response.close()
                    throw java.io.IOException("The image host returned an error page instead of artwork.")
                }
                response
            }.build()
        val lowRam = (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
        val free = runCatching { android.os.StatFs(cacheDir.absolutePath).availableBytes }.getOrDefault(640L * 1024 * 1024)
        val diskBytes = (free / 20).coerceIn(32L * 1024 * 1024, 256L * 1024 * 1024)
        val imageDispatcher = okhttp3.Dispatcher().apply { maxRequests = 12; maxRequestsPerHost = 5 }
        val imageClient = client.newBuilder().dispatcher(imageDispatcher).retryOnConnectionFailure(true)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(35, java.util.concurrent.TimeUnit.SECONDS).build()
        return coil.ImageLoader.Builder(this)
            .okHttpClient(imageClient)
            .memoryCache { coil.memory.MemoryCache.Builder(this).maxSizePercent(if (lowRam) .08 else .12).build() }
            .diskCache { coil.disk.DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(diskBytes).build() }
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        ProviderSubtitles.init(this)
        ReferencePlaybackRegistry.init(this)
        com.anilili.data.AppGraph.init(this)
        DownloadStore.initialize(this)
        AnnouncementManager.init(this)
        AnnouncementScheduler.schedule(this)
        DebugTrace.initialize(this)
        // Keep HTTP and WebView on the same UA across launches, including service restarts.
        val preferences = getSharedPreferences("browser-session", MODE_PRIVATE)
        AnimeNetwork.userAgent = preferences.getString("user-agent", null)
            ?: runCatching { WebSettings.getDefaultUserAgent(this) }.getOrDefault(AnimeNetwork.userAgent).also {
                preferences.edit { putString("user-agent", it) }
            }
        recoveryScope.launch {
            try {
                DownloadStore.awaitReady()
                ProviderSubtitles.markDownloadsReady()
                ProviderSubtitles.trim()
                PreparationJobs.discard(this@AnimeApplication, PreparationJobs.ANIME)
                if (hasOutputFolderAccess(this@AnimeApplication) && DownloadStore.operations("folder-access")["transfers"] != null)
                    reconcileExistingVideos(this@AnimeApplication)
                PreparationJobs.recover(this@AnimeApplication)
            }
            catch (_: Exception) { DebugTrace.event("preparation_recovery_deferred") }
        }
    }
}
