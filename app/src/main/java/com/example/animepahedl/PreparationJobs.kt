package com.example.animepahedl

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.UUID
import java.util.concurrent.TimeUnit

internal data class PreparationStatus(val pending: Boolean = false, val paused: Boolean = false,
    val label: String = "", val verificationUrl: String? = null, val needsFolderAccess: Boolean = false,
    val totalSelected: Int = 0, val running: Boolean = false, val activeRowId: String? = null,
    val items: Map<String, PreparationStatus> = emptyMap()) {
    val automatic: Boolean get() = if (items.isNotEmpty()) items.values.any { it.automatic } else pending && !paused && verificationUrl == null && !needsFolderAccess
}

/** Each episode owns a durable journal and unique WorkManager request. */
internal object PreparationJobs {
    const val ANIME = "episodes"
    const val SITES = "sites"
    private val gson = Gson()
    private val locks = java.util.concurrent.ConcurrentHashMap<String, Mutex>()
    private fun name(slot: String) = "prepare-links-$slot"
    private fun lock(slot: String) = locks.getOrPut(slot) { Mutex() }
    private const val ITEM_PREFIX = "site-item:"
    private const val ITEM_TAG = "site-preparation-item"
    private val kinds = listOf("preparation", "preparation-generation", "preparation-paused", "preparation-verification", "preparation-folder", "preparation-total", "preparation-failures", "preparation-error", "preparation-order")
    fun itemSlots(): List<String> = DownloadStore.operations("preparation").keys.filter { it.startsWith(ITEM_PREFIX) }.sortedWith(compareBy<String> { DownloadStore.operations("preparation-order")[it]?.toLongOrNull() ?: Long.MAX_VALUE }.thenBy { it })
    fun itemSlot(id: String) = ITEM_PREFIX + id
    fun payload(slot: String): String? {
        if (slot != SITES) return DownloadStore.operations("preparation")[slot]
        DownloadStore.operations("preparation")[SITES]?.let { return it }
        val records = itemSlots().mapNotNull { DownloadStore.operations("preparation")[it] }
            .map { gson.fromJson(it, SiteDownloadPreparation::class.java) }
        return records.firstOrNull()?.withBatches(records.flatMap { it.batches() })?.let(gson::toJson)
    }
    /** Commit children before removing the legacy journal: restart can safely repeat this migration. */
    private suspend fun migrate(context: Context) = lock(SITES).withLock {
        val raw = DownloadStore.operations("preparation")[SITES] ?: return@withLock
        val record = gson.fromJson(raw, SiteDownloadPreparation::class.java)
        val entries = splitPreparation(record)
        for ((index, entry) in entries.entries.withIndex()) {
            val (id, item) = entry
            val key = itemSlot(id)
            DownloadStore.operation(key, "preparation", gson.toJson(item))
            DownloadStore.operation(key, "preparation-generation", UUID.randomUUID().toString())
            // Explicit batch pause applies to all items; a failure/verification only applies to the old head.
            for (kind in kinds.drop(2)) {
                val value = DownloadStore.operations(kind)[SITES]
                if (inheritLegacyPreparationState(kind, index == 0, DownloadStore.operations("preparation-error")[SITES] != null))
                    DownloadStore.operation(key, kind, value)
            }
            DownloadStore.operation(key, "preparation-order", index.toString())
        }
        DownloadStore.flush()
        for (kind in kinds) DownloadStore.operation(SITES, kind, null)
        DownloadStore.flush()
        withContext(Dispatchers.IO) { WorkManager.getInstance(context).cancelUniqueWork(name(SITES)).result.get() }
    }
    private fun generation(slot: String) = DownloadStore.operations("preparation-generation")[slot]
    fun active(slot: String, token: String): Boolean = generation(slot) == token && payload(slot) != null &&
        slot !in DownloadStore.operations("preparation-paused") && slot !in DownloadStore.operations("preparation-verification") && slot !in DownloadStore.operations("preparation-folder")

    fun observe(context: Context, slot: String): Flow<PreparationStatus> = flow {
        DownloadStore.awaitReady()
        recover(context)
        emitAll(combine(DownloadStore.revisions,
            WorkManager.getInstance(context).getWorkInfosByTagFlow(ITEM_TAG)) { _, jobs ->
            val statuses = itemSlots().associate { key ->
                val job = jobs.firstOrNull { key in it.tags && generation(key) in it.tags && !it.state.isFinished }
                val paused = key in DownloadStore.operations("preparation-paused")
                val verification = DownloadStore.operations("preparation-verification")[key]
                val folder = key in DownloadStore.operations("preparation-folder")
                val running = job?.state == WorkInfo.State.RUNNING && job.progress.getString("rowId") != null && !paused && verification == null && !folder
                val label = when {
                    folder -> "Choose downloads folder"
                    paused -> DownloadStore.operations("preparation-error")[key] ?: "Preparation paused"
                    verification != null -> "Session verification needed"
                    running -> "Preparing download link"
                    else -> DownloadStore.operations("preparation-error")[key] ?: "Waiting to prepare link"
                }
                key.removePrefix(ITEM_PREFIX) to PreparationStatus(true, paused, label, verification, folder, 1, running,
                    job?.progress?.getString("rowId"))
            }
            aggregatePreparationStatus(statuses)
        })
    }.retryWhen { failure, _ ->
        if (failure !is Exception || failure is CancellationException) false else {
            emit(PreparationStatus(payload(slot) != null, paused = true, label = "Waiting for preparation storage"))
            delay(30_000)
            true
        }
    }

    suspend fun submit(context: Context, slot: String, record: Any) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        if (slot == SITES && record is SiteDownloadPreparation) { appendSites(context, record); return@withContext }
        lock(slot).withLock {
            require(payload(slot) == null) { "A batch is already being prepared." }
            DownloadStore.operation(slot, "preparation", gson.toJson(record))
            if (record is SiteDownloadPreparation) DownloadStore.operation(slot, "preparation-total", record.batches().sumOf { it.episodes.size }.toString())
            DownloadStore.operation(slot, "preparation-generation", UUID.randomUUID().toString())
            DownloadStore.operation(slot, "preparation-failures", null)
            DownloadStore.operation(slot, "preparation-error", null)
            DownloadStore.operation(slot, "preparation-paused", null)
            DownloadStore.operation(slot, "preparation-verification", null)
            DownloadStore.operation(slot, "preparation-folder", null)
            DownloadStore.flush()
            enqueue(context, slot, ExistingWorkPolicy.REPLACE)
        }
    }
    /** Bulk selection enqueues independent durable items without replacing active workers. */
    suspend fun appendSites(context: Context, record: SiteDownloadPreparation) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        migrate(context)
        for ((id, item) in splitPreparation(record)) {
            val key = itemSlot(id)
            lock(key).withLock {
                if (payload(key) == null) {
                    for (kind in kinds) DownloadStore.operation(key, kind, null)
                    val order = itemSlots().maxOfOrNull { DownloadStore.operations("preparation-order")[it]?.toLongOrNull() ?: 0L } ?: 0L
                    DownloadStore.operation(key, "preparation-order", (order + 1).toString())
                    DownloadStore.operation(key, "preparation", gson.toJson(item))
                    DownloadStore.operation(key, "preparation-generation", UUID.randomUUID().toString())
                    DownloadStore.flush()
                }
                if (active(key, generation(key).orEmpty())) enqueue(context, key, ExistingWorkPolicy.KEEP)
            }
        }
    }
    suspend fun resume(context: Context, slot: String): Unit = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        if (slot == SITES) {
            migrate(context)
            for (key in itemSlots()) if (!active(key, generation(key).orEmpty())) resume(context, key)
            recover(context)
            return@withContext
        }
        lock(slot).withLock {
            if (payload(slot) == null) return@withLock
            // A new token prevents a cancelled predecessor from changing this journal.
            DownloadStore.operation(slot, "preparation-generation", UUID.randomUUID().toString())
            DownloadStore.operation(slot, "preparation-failures", null)
            DownloadStore.operation(slot, "preparation-error", null)
            DownloadStore.operation(slot, "preparation-paused", null)
            DownloadStore.operation(slot, "preparation-verification", null)
            DownloadStore.operation(slot, "preparation-folder", null)
            DownloadStore.flush()
            enqueue(context, slot, ExistingWorkPolicy.REPLACE)
        }
    }
    suspend fun pause(context: Context, slot: String): Unit = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        if (slot == SITES) { migrate(context); for (key in itemSlots()) pause(context, key); return@withContext }
        lock(slot).withLock {
            if (payload(slot) != null) {
                DownloadStore.operation(slot, "preparation-paused", "1")
                DownloadStore.operation(slot, "preparation-error", null)
            }
            DownloadStore.flush()
            withContext(Dispatchers.IO) { WorkManager.getInstance(context).cancelUniqueWork(name(slot)).result.get() }
        }
    }
    suspend fun discard(context: Context, slot: String): Unit = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        if (slot == SITES) { migrate(context); for (key in itemSlots()) discard(context, key); return@withContext }
        lock(slot).withLock {
            for (kind in kinds)
                DownloadStore.operation(slot, kind, null)
            DownloadStore.flush()
            withContext(Dispatchers.IO) { WorkManager.getInstance(context).cancelUniqueWork(name(slot)).result.get() }
        }
    }
    suspend fun removeSiteEpisodes(context: Context, ids: Set<String>) {
        DownloadStore.awaitReady()
        migrate(context)
        for (id in ids) discard(context, itemSlot(id))
    }
    suspend fun save(slot: String, token: String, record: Any?): Boolean = withContext(NonCancellable) {
        lock(slot).withLock {
            if (generation(slot) != token) return@withLock false
            DownloadStore.operation(slot, "preparation-failures", null)
            DownloadStore.operation(slot, "preparation-error", null)
            DownloadStore.operation(slot, "preparation", record?.let(gson::toJson))
            if (record == null) for (kind in kinds) DownloadStore.operation(slot, kind, null)
            DownloadStore.flush()
            true
        }
    }
    /** Persist the retry budget so process restarts cannot create an endless retry loop. */
    suspend fun failed(slot: String, token: String): Boolean = withContext(NonCancellable) {
        lock(slot).withLock {
            if (!active(slot, token)) return@withLock false
            val failures = (DownloadStore.operations("preparation-failures")[slot]?.toIntOrNull() ?: 0) + 1
            val retry = failures < 3
            DownloadStore.operation(slot, "preparation-failures", failures.toString())
            DownloadStore.operation(slot, "preparation-error", if (retry)
                "Link preparation failed · retry $failures/2 scheduled"
                else "Could not prepare this link after 3 attempts. Resume to retry.")
            if (!retry) DownloadStore.operation(slot, "preparation-paused", "1")
            DownloadStore.flush()
            retry
        }
    }
    suspend fun needsVerification(slot: String, token: String, url: String) = withContext(NonCancellable) {
        lock(slot).withLock {
            if (generation(slot) == token) {
                DownloadStore.operation(slot, "preparation-verification", url)
                DownloadStore.flush()
            }
        }
    }
    suspend fun blockedByAdGate(slot: String, token: String) = withContext(NonCancellable) {
        lock(slot).withLock {
            if (!active(slot, token)) return@withLock
            DownloadStore.operation(slot, "preparation-verification", null)
            DownloadStore.operation(slot, "preparation-paused", "1")
            DownloadStore.operation(slot, "preparation-error", ProviderAdGate().message)
            DownloadStore.flush()
        }
    }
    suspend fun needsFolder(slot: String, token: String) = withContext(NonCancellable) {
        lock(slot).withLock {
            if (generation(slot) == token) {
                DownloadStore.operation(slot, "preparation-folder", "1")
                DownloadStore.flush()
            }
        }
    }
    suspend fun folderAccessRestored(context: Context) {
        for (slot in itemSlots()) if (slot in DownloadStore.operations("preparation-folder") &&
            slot !in DownloadStore.operations("preparation-paused")) resume(context, slot)
    }
    suspend fun recover(context: Context) {
        DownloadStore.awaitReady()
        migrate(context)
        for (slot in itemSlots()) lock(slot).withLock {
            if (payload(slot) != null && slot !in DownloadStore.operations("preparation-paused") &&
                slot !in DownloadStore.operations("preparation-verification") && slot !in DownloadStore.operations("preparation-folder")) {
                if (generation(slot) == null) {
                    DownloadStore.operation(slot, "preparation-generation", UUID.randomUUID().toString())
                    DownloadStore.flush()
                }
                enqueue(context, slot, ExistingWorkPolicy.KEEP)
            }
        }
    }
    private suspend fun enqueue(context: Context, slot: String, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<PreparationWorker>()
            .setInputData(workDataOf("slot" to slot, "generation" to generation(slot)))
            .addTag(ITEM_TAG).addTag(slot).addTag(generation(slot).orEmpty())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(
                if (AppSettings(context).unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        withContext(Dispatchers.IO) { WorkManager.getInstance(context).enqueueUniqueWork(name(slot), policy, request).result.get() }
    }
}

/** Each episode survives leaving the screen and process restarts independently. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PreparationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val slot = inputData.getString("slot").orEmpty()
    private val token = inputData.getString("generation").orEmpty()
    private val gson = Gson()
    private val notificationId get() = 1000 + (slot.hashCode() and 0x3fffffff)
    companion object { private val permits = kotlinx.coroutines.sync.Semaphore(2) }

    override suspend fun getForegroundInfo(): ForegroundInfo = foreground("Preparing download links")
    private fun foreground(label: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("preparation", "Preparing download links", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(applicationContext, "preparation")
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Preparing download links")
            .setContentText(label).setContentIntent(downloadsPendingIntent(applicationContext))
            .setOnlyAlertOnce(true).setOngoing(true).build()
        return ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
    private suspend fun progress(label: String, rowId: String) {
        setProgress(workDataOf("label" to label, "rowId" to rowId))
        setForeground(foreground(label))
    }
    override suspend fun doWork(): Result {
        if (slot == PreparationJobs.SITES) { withContext(NonCancellable) { PreparationJobs.recover(applicationContext) }; return Result.success() }
        permits.acquire()
        try { return prepareItem() } finally { permits.release() }
    }
    private suspend fun prepareItem(): Result {
        if (!slot.startsWith("site-item:") || token.isBlank()) return Result.failure()
        return try {
            DownloadStore.awaitReady()
            if (!PreparationJobs.active(slot, token)) return Result.success()
            if (!hasOutputFolderAccess(applicationContext)) throw OutputFolderAccessRequired()
            setForeground(getForegroundInfo())
            withContext(Dispatchers.IO) {
                OfflineDownloads.prepareCache(applicationContext)
                reconcileDirectEnqueues(applicationContext)
            }
            val raw = PreparationJobs.payload(slot) ?: return Result.success()
            prepareSites(gson.fromJson(raw, SiteDownloadPreparation::class.java))
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: OutputFolderAccessRequired) {
            PreparationJobs.needsFolder(slot, token)
            Result.success()
        } catch (_: ProviderAdGate) {
            PreparationJobs.blockedByAdGate(slot, token)
            Result.success()
        } catch (challenge: VerificationRequired) {
            PreparationJobs.needsVerification(slot, token, challenge.browserUrl)
            Result.success()
        } catch (_: Exception) { failedAttempt() }
    }
    private val outputTree by lazy { AppSettings(applicationContext).outputTree }
    private val existingFiles by lazy { ExistingOutputIndex(applicationContext, outputTree) }
    private suspend fun reuseExisting(identity: DownloadIdentity): Boolean {
        val location = downloadLocation(identity.anime.title, identity.episode.number.takeUnless { identity.isSingleVideo },
            identity.quality, "mp4", identity.audio)
        val found = withContext(Dispatchers.IO) { existingFiles.find(location) } ?: return false
        for ((id, record) in Exports.all(applicationContext)) if (record.identity?.key == identity.key && record.uri.isEmpty())
            clearPendingVideo(applicationContext, id)
        adoptExistingVideo(applicationContext, identity, found, outputTree)
        return true
    }
    private suspend fun enqueue(identity: DownloadIdentity, media: ResolvedMedia) {
        val context = applicationContext
        if (reuseExisting(identity)) return
        currentCoroutineContext().ensureActive()
        if (!PreparationJobs.active(slot, token)) throw CancellationException("Preparation paused or replaced")
        AndroidDownloadRepository(context, AnimeRepository()).enqueue(identity, media)
    }
    private suspend fun failedAttempt(): Result =
        if (PreparationJobs.failed(slot, token)) Result.retry() else Result.success()

    private suspend fun attempt(action: suspend () -> Unit): Boolean {
        currentCoroutineContext().ensureActive()
        if (!PreparationJobs.active(slot, token)) throw CancellationException("Preparation paused or replaced")
        return try {
            withTimeout(120_000) { action() }
            true
        } catch (_: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            false
        } catch (gate: ProviderAdGate) { throw gate
        } catch (folder: OutputFolderAccessRequired) { throw folder
        } catch (challenge: VerificationRequired) { throw challenge
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { false }
    }
    private suspend fun prepareSites(record: SiteDownloadPreparation): Result {
        val batch = record.batches().single()
        val episode = batch.episodes.single()
        val cookies = SiteDownloadCookieStore(applicationContext)
        val repository = appSiteDownloadRepository(cookieHeader = cookies::headerFor)
        progress("Preparing ${batch.series.title} · Episode ${episode.displayNumber}", pendingEpisodeRowId(batch.series, episode, batch.quality))
        if (!attempt {
            if (batch.quality == "Best" || !reuseExisting(DownloadIdentity.siteDownload(batch.series, episode, batch.quality, "und"))) {
                val media = cookies.applyTo(repository.resolve(episode.url, batch.quality, cookies.headerFor(episode.url)))
                enqueue(DownloadIdentity.siteDownload(batch.series, episode, media.quality, media.audio), media)
            }
        }) return failedAttempt()
        PreparationJobs.save(slot, token, null)
        return Result.success()
    }
}
