package com.example.animepahedl

import androidx.core.content.edit
import android.app.Notification
import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.core.app.NotificationCompat
import android.text.format.Formatter
import java.io.File
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object OfflineDownloads {
    private var instance: DownloadManager? = null
    private var segmentExecutor: ThreadPoolExecutor? = null

    /** Progress and state changes from Media3, consumed by the UI instead of a polling loop. */
    val updates = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    @Synchronized
    fun setConnections(count: Int) {
        val limit = count.coerceIn(2, 12)
        segmentExecutor?.let { pool ->
            if (limit > pool.maximumPoolSize) { pool.maximumPoolSize = limit; pool.corePoolSize = limit }
            else { pool.corePoolSize = limit; pool.maximumPoolSize = limit }
        }
    }
    @Synchronized
    fun setSimultaneousDownloads(count: Int) {
        instance?.maxParallelDownloads = count.coerceIn(1, 6)
    }
    lateinit var cache: SimpleCache
        private set

    /** Called on IO before UI startup; services retain a synchronized fallback. */
    @Synchronized
    internal fun prepareCache(context: Context) {
        if (::cache.isInitialized) return
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        val legacy = File(app.filesDir, "offline-media")
        // Retain pre-update downloads until they have been exported successfully.
        val directory = if (legacy.exists() && legacy.walkTopDown().any { it.extension == "exo" }) legacy
            else File(app.getExternalFilesDir(null) ?: app.filesDir, "hls-temp")
        cache = SimpleCache(directory, NoOpCacheEvictor(), database)
    }

    internal fun retryPending(context: Context, manager: DownloadManager) {
        if (manager.downloadsPaused) return
        val retries = context.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
        val requests = mutableListOf<androidx.media3.exoplayer.offline.DownloadRequest>()
        manager.downloadIndex.getDownloads(Download.STATE_FAILED).use { cursor ->
            while (cursor.moveToNext()) {
                val download = cursor.download
                if (!Exports.deleting(download.request.id) && retries.getBoolean(download.request.id, false) && download.stopReason == Download.STOP_REASON_NONE)
                    requests += download.request
            }
        }
        requests.forEach { manager.addDownload(it) }
    }

    @Synchronized
    fun manager(context: Context): DownloadManager {
        instance?.let { return it }
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        if (!::cache.isInitialized) prepareCache(app)
        val transferListener = object : androidx.media3.datasource.TransferListener {
            private val last = java.util.concurrent.atomic.AtomicLong(0)
            override fun onTransferInitializing(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) = Unit
            override fun onTransferStart(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) = Unit
            override fun onBytesTransferred(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
                if (isNetwork) DownloadSpeed.add(bytesTransferred)
                val now = android.os.SystemClock.elapsedRealtime()
                val previous = last.get()
                if (now - previous >= 750 && last.compareAndSet(previous, now)) updates.tryEmit(Unit)
            }
            override fun onTransferEnd(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) = Unit
        }
        val connections = AppSettings(app).connections
        val executor = ThreadPoolExecutor(connections, connections, 30, TimeUnit.SECONDS, java.util.concurrent.ArrayBlockingQueue<Runnable>(48), ThreadPoolExecutor.CallerRunsPolicy())
            .apply { allowCoreThreadTimeOut(true) }
        segmentExecutor = executor
        val downloaderFactory = androidx.media3.exoplayer.offline.DownloaderFactory { request ->
            // Media3 invokes this on its download worker, including after process recovery.
            // Database loading must finish before choosing this download's HTTP context.
            kotlinx.coroutines.runBlocking { DownloadStore.awaitReady() }
            if (!hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null) throw OutputFolderAccessRequired()
            val record = requireNotNull(Exports.all(app)[request.id]) { "This download record is missing. Retry to resolve this episode again." }
            require(record.identity?.isSiteDownload == true) { "This download record is not supported by this edition." }
            val media = requireNotNull(record.media) { "Video source context is missing. Retry to resolve this episode again." }
            val upstream = OkHttpDataSource.Factory(MediaRequests.client(media, SiteDownloadCookieStore(app)::headerFor)).setTransferListener(transferListener)
                .setDefaultRequestProperties(mediaRequestHeaders(media))
            val factory = androidx.media3.datasource.cache.CacheDataSource.Factory().setCache(cache)
                .setUpstreamDataSourceFactory(upstream)
            androidx.media3.exoplayer.offline.DefaultDownloaderFactory(factory, executor).createDownloader(request)
        }
        return DownloadManager(app, androidx.media3.exoplayer.offline.DefaultDownloadIndex(database), downloaderFactory).also {
            val retries = app.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
            it.addListener(object : DownloadManager.Listener {
                override fun onDownloadsPausedChanged(manager: DownloadManager, downloadsPaused: Boolean) {
                    if (!downloadsPaused && (!hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null)) manager.pauseDownloads()
                }
                override fun onInitialized(manager: DownloadManager) {
                    retryPending(app, manager)
                    updates.tryEmit(Unit)
                }
                override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                    val challenge = finalException?.let { error ->
                        generateSequence<Throwable>(error) { it.cause }.filterIsInstance<VerificationRequired>().firstOrNull()
                    }
                    if (challenge != null) {
                        val event = MediaVerification(download.request.id, challenge.url, challenge.browserUrl, System.currentTimeMillis())
                        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { putString(event.id, Gson().toJson(event)) }
                        DebugTrace.event("media_challenge", "url" to DebugTrace.safeUrl(challenge.url))
                    }
                    if (finalException != null) DebugTrace.event("media_failed", "type" to finalException.javaClass.simpleName,
                        "url" to DebugTrace.safeUrl(download.request.uri.toString()))
                    // Connectivity requirements handle a disconnected device. Retry transient I/O failures
                    // too (for example a router with no internet), retaining every cached segment.
                    val causes = finalException?.let { generateSequence<Throwable>(it) { error -> error.cause }.toList() }.orEmpty()
                    val http = causes.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                    if (download.state == Download.STATE_FAILED && challenge == null &&
                        causes.any { it is java.io.IOException } && (http == null || http.responseCode == 408 || http.responseCode == 429 || http.responseCode >= 500)) {
                        retries.edit { putBoolean(download.request.id, true) }
                        DownloadRetryService.schedule(app)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val current = manager.downloadIndex.getDownload(download.request.id)
                            if (current?.state == Download.STATE_FAILED && current.stopReason == Download.STOP_REASON_NONE && !manager.downloadsPaused)
                                manager.addDownload(current.request)
                        }, 30_000)
                    }
                    if (download.state == Download.STATE_COMPLETED || download.state == Download.STATE_REMOVING ||
                        (download.state == Download.STATE_FAILED && (challenge != null || (http != null && http.responseCode in 400..499 && http.responseCode != 408 && http.responseCode != 429))))
                        retries.edit { remove(download.request.id) }
                    if (download.state == Download.STATE_COMPLETED && !Exports.deleting(download.request.id)) {
                        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(download.request.id) }
                        if (!Exports.all(app).containsKey(download.request.id))
                            Exports.prepare(app, download.request.id, String(download.request.data))
                        Exports.start(app)
                    }
                    updates.tryEmit(Unit)
                }
            })
            it.setRequirements(androidx.media3.exoplayer.scheduler.Requirements(if (AppSettings(app).unmeteredOnly) androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED else androidx.media3.exoplayer.scheduler.Requirements.NETWORK))
            it.minRetryCount = 10
            it.maxParallelDownloads = AppSettings(app).simultaneousDownloads
            if (hasOutputFolderAccess(app) && DownloadStore.operations("folder-access")["transfers"] == null) it.resumeDownloads() else {
                it.pauseDownloads()
                DownloadStore.operation("transfers", "folder-access", "1")
            }
            instance = it
        }
    }
}

internal data class MediaVerification(val id: String, val url: String, val browserUrl: String, val time: Long)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OfflineDownloadService : DownloadService(1, 1000L, "downloads", R.string.download_channel, 0) {
    private val wakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "$packageName:offline-downloads")
            .apply { setReferenceCounted(false) }
    }

    override fun onCreate() {
        super.onCreate()
        // A foreground service alone does not keep the CPU awake on aggressive OEM builds.
        if (!wakeLock.isHeld) wakeLock.acquire(6 * 60 * 60_000L)
    }

    override fun onDestroy() {
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun getDownloadManager(): DownloadManager = OfflineDownloads.manager(this)
    override fun getScheduler(): Scheduler = androidx.media3.exoplayer.scheduler.PlatformScheduler(this, 10)
    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15+ limits data-sync foreground-service runtime.
        DownloadStore.operation("transfer", "service-timeout", "Android stopped background downloads at its time limit. Reopen the app and resume paused downloads.")
        OfflineDownloads.manager(this).pauseDownloads()
        stopSelf()
    }
    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val active = downloads.filter { it.state == Download.STATE_DOWNLOADING }
        val known = active.map { it.percentDownloaded }.filter { it >= 0f }
        val percent = if (active.isNotEmpty() && known.size == active.size) known.average().toInt().coerceIn(0, 100) else null
        val rate = if (active.isEmpty()) { DownloadSpeed.reset(); 0L } else DownloadSpeed.sample()
        val title = if (active.size == 1) String(active[0].request.data).ifBlank { "Downloading episode" }
            else if (active.size > 1) "Downloading ${active.size} episodes" else "Downloads"
        val text = when {
            notMetRequirements != 0 -> "Waiting for internet"
            active.isEmpty() -> "Waiting to download"
            else -> listOfNotNull(percent?.let { if (active.size > 1) "Average $it%" else "$it%" }, rate.takeIf { it > 0 }?.let { "${Formatter.formatShortFileSize(this, it)}/s" }).joinToString(" · ").ifEmpty { "Downloading" }
        }
        return NotificationCompat.Builder(this, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title).setContentText(text)
            .setContentIntent(downloadsPendingIntent(this)).setOnlyAlertOnce(true).setOngoing(true)
            .addAction(0, if (OfflineDownloads.manager(this).downloadsPaused) "Resume HLS" else "Pause HLS",
                android.app.PendingIntent.getForegroundService(this, 31,
                    if (OfflineDownloads.manager(this).downloadsPaused) DownloadService.buildResumeDownloadsIntent(this, OfflineDownloadService::class.java, true)
                    else DownloadService.buildPauseDownloadsIntent(this, OfflineDownloadService::class.java, true),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .setProgress(100, percent ?: 0, percent == null).build()
    }

}

/** A persisted job survives process death between a transient failure and its delayed retry. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloadRetryService : android.app.job.JobService() {
    override fun onStartJob(params: android.app.job.JobParameters): Boolean {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val result = runCatching {
                DownloadService.startForeground(this, OfflineDownloadService::class.java)
                val manager = OfflineDownloads.manager(this)
                if (manager.isInitialized) OfflineDownloads.retryPending(this, manager)
            }
            result.onFailure { DebugTrace.event("download_retry_deferred", "reason" to it.message) }
            jobFinished(params, result.isFailure)
        }
        return true
    }
    override fun onStopJob(params: android.app.job.JobParameters) = true

    companion object {
        fun schedule(context: Context) {
            val job = android.app.job.JobInfo.Builder(11, android.content.ComponentName(context, DownloadRetryService::class.java))
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(30_000).setPersisted(true)
                .setBackoffCriteria(30_000, android.app.job.JobInfo.BACKOFF_POLICY_EXPONENTIAL).build()
            context.getSystemService(android.app.job.JobScheduler::class.java).schedule(job)
        }
    }
}
