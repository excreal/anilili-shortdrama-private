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
    val totalSelected: Int = 0) {
    val automatic get() = pending && !paused && verificationUrl == null && !needsFolderAccess
}

/** The journal holds the batch; WorkManager owns execution, network waits, and retry timing. */
internal object PreparationJobs {
    const val ANIME = "episodes"
    const val SITES = "sites"
    private val gson = Gson()
    private val locks = java.util.concurrent.ConcurrentHashMap<String, Mutex>()
    private fun name(slot: String) = "prepare-links-$slot"
    private fun lock(slot: String) = locks.getOrPut(slot) { Mutex() }
    fun payload(slot: String): String? = DownloadStore.operations("preparation")[slot]
    private fun generation(slot: String) = DownloadStore.operations("preparation-generation")[slot]
    fun active(slot: String, token: String): Boolean = generation(slot) == token && payload(slot) != null &&
        slot !in DownloadStore.operations("preparation-paused") && slot !in DownloadStore.operations("preparation-verification") && slot !in DownloadStore.operations("preparation-folder")

    fun observe(context: Context, slot: String): Flow<PreparationStatus> = flow {
        emitAll(combine(
            DownloadStore.revisions, WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(name(slot))
        ) { _, jobs ->
        val pending = payload(slot) != null
        val paused = slot in DownloadStore.operations("preparation-paused")
        val verification = DownloadStore.operations("preparation-verification")[slot]
        val needsFolder = slot in DownloadStore.operations("preparation-folder")
        val job = jobs.lastOrNull { !it.state.isFinished }
        PreparationStatus(pending, paused, when {
            !pending -> ""
            needsFolder -> "Choose downloads folder to check existing videos"
            paused -> "Link preparation paused"
            verification != null -> "Verify session to continue"
            job?.state == WorkInfo.State.RUNNING -> job.progress.getString("label") ?: "Preparing links in background"
            job != null && job.runAttemptCount > 0 -> "Retrying remaining links automatically"
            else -> "Preparing links · waiting for network or scheduler"
        }, verification, needsFolder, DownloadStore.operations("preparation-total")[slot]?.toIntOrNull() ?: 0)
        })
    }.retryWhen { failure, _ ->
        if (failure !is Exception) false else {
            emit(PreparationStatus(payload(slot) != null, paused = true, label = "Waiting for preparation storage"))
            delay(30_000)
            true
        }
    }

    suspend fun submit(context: Context, slot: String, record: Any) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        lock(slot).withLock {
            require(payload(slot) == null) { "A batch is already being prepared." }
            DownloadStore.operation(slot, "preparation", gson.toJson(record))
            if (record is SiteDownloadPreparation) DownloadStore.operation(slot, "preparation-total", record.batches().sumOf { it.episodes.size }.toString())
            DownloadStore.operation(slot, "preparation-generation", UUID.randomUUID().toString())
            DownloadStore.operation(slot, "preparation-paused", null)
            DownloadStore.operation(slot, "preparation-verification", null)
            DownloadStore.operation(slot, "preparation-folder", null)
            DownloadStore.flush()
            enqueue(context, slot, ExistingWorkPolicy.REPLACE)
        }
    }
    /** Add a title to the persistent queue and restart the worker with a fresh journal token. */
    suspend fun appendSites(context: Context, record: SiteDownloadPreparation) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        lock(SITES).withLock {
            val current = payload(SITES)?.let { gson.fromJson(it, SiteDownloadPreparation::class.java) }
            val merged = current?.let { appendSitePreparation(it, record) } ?: record
            DownloadStore.operation(SITES, "preparation", gson.toJson(merged))
            DownloadStore.operation(SITES, "preparation-total", merged.batches().sumOf { it.episodes.size }.toString())
            DownloadStore.operation(SITES, "preparation-generation", UUID.randomUUID().toString())
            if (current == null) {
                DownloadStore.operation(SITES, "preparation-paused", null)
                DownloadStore.operation(SITES, "preparation-verification", null)
                DownloadStore.operation(SITES, "preparation-folder", null)
            }
            DownloadStore.flush()
            if (SITES !in DownloadStore.operations("preparation-paused") &&
                SITES !in DownloadStore.operations("preparation-verification") &&
                SITES !in DownloadStore.operations("preparation-folder")) enqueue(context, SITES, ExistingWorkPolicy.REPLACE)
        }
    }
    suspend fun resume(context: Context, slot: String) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        lock(slot).withLock {
            if (payload(slot) == null) return@withLock
            // A new token prevents a cancelled predecessor from changing this journal.
            DownloadStore.operation(slot, "preparation-generation", UUID.randomUUID().toString())
            DownloadStore.operation(slot, "preparation-paused", null)
            DownloadStore.operation(slot, "preparation-verification", null)
            DownloadStore.operation(slot, "preparation-folder", null)
            DownloadStore.flush()
            enqueue(context, slot, ExistingWorkPolicy.REPLACE)
        }
    }
    suspend fun pause(context: Context, slot: String) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        lock(slot).withLock {
            if (payload(slot) != null) DownloadStore.operation(slot, "preparation-paused", "1")
            DownloadStore.flush()
            withContext(Dispatchers.IO) { WorkManager.getInstance(context).cancelUniqueWork(name(slot)).result.get() }
        }
    }
    suspend fun discard(context: Context, slot: String) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        lock(slot).withLock {
            for (kind in listOf("preparation", "preparation-generation", "preparation-paused", "preparation-verification", "preparation-folder", "preparation-total"))
                DownloadStore.operation(slot, kind, null)
            DownloadStore.flush()
            withContext(Dispatchers.IO) { WorkManager.getInstance(context).cancelUniqueWork(name(slot)).result.get() }
        }
    }
    suspend fun removeSiteEpisodes(context: Context, ids: Set<String>) = withContext(NonCancellable) {
        DownloadStore.awaitReady()
        lock(SITES).withLock {
            val raw = payload(SITES) ?: return@withLock
            val record = gson.fromJson(raw, SiteDownloadPreparation::class.java)
            val before = record.batches().sumOf { it.episodes.size }
            val batches = record.batches().mapNotNull { batch ->
                val episodes = batch.episodes.filterNot { pendingEpisodeRowId(batch.series, it) in ids }
                batch.copy(episodes = episodes).takeIf { episodes.isNotEmpty() }
            }
            if (batches.sumOf { it.episodes.size } == before) return@withLock
            val paused = SITES in DownloadStore.operations("preparation-paused")
            DownloadStore.operation(SITES, "preparation-generation", UUID.randomUUID().toString())
            val total = batches.sumOf { it.episodes.size }
            DownloadStore.operation(SITES, "preparation-total", total.toString())
            DownloadStore.operation(SITES, "preparation", record.withBatches(batches)?.let(gson::toJson))
            DownloadStore.flush()
            withContext(Dispatchers.IO) { WorkManager.getInstance(context).cancelUniqueWork(name(SITES)).result.get() }
            if (batches.isNotEmpty() && !paused && SITES !in DownloadStore.operations("preparation-verification") &&
                SITES !in DownloadStore.operations("preparation-folder")) enqueue(context, SITES, ExistingWorkPolicy.REPLACE)
        }
    }
    suspend fun save(slot: String, token: String, record: Any?): Boolean = withContext(NonCancellable) {
        lock(slot).withLock {
            if (generation(slot) != token) return@withLock false
            DownloadStore.operation(slot, "preparation", record?.let(gson::toJson))
            if (slot == SITES) {
                val queued = record as? SiteDownloadPreparation
                DownloadStore.operation(slot, "preparation-total", queued?.batches()?.sumOf { it.episodes.size }?.toString())
            }
            DownloadStore.flush()
            true
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
    suspend fun needsFolder(slot: String, token: String) = withContext(NonCancellable) {
        lock(slot).withLock {
            if (generation(slot) == token) {
                DownloadStore.operation(slot, "preparation-folder", "1")
                DownloadStore.flush()
            }
        }
    }
    suspend fun folderAccessRestored(context: Context) {
        for (slot in listOf(SITES)) if (slot in DownloadStore.operations("preparation-folder") &&
            slot !in DownloadStore.operations("preparation-paused")) resume(context, slot)
    }
    suspend fun recover(context: Context) {
        DownloadStore.awaitReady()
        for (slot in listOf(SITES)) lock(slot).withLock {
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
            .setConstraints(Constraints.Builder().setRequiredNetworkType(
                if (AppSettings(context).unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        withContext(Dispatchers.IO) { WorkManager.getInstance(context).enqueueUniqueWork(name(slot), policy, request).result.get() }
    }
}

/** Both AnimePahe and site batches survive leaving the screen and process restarts. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PreparationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val slot = inputData.getString("slot").orEmpty()
    private val token = inputData.getString("generation").orEmpty()
    private val gson = Gson()
    private val notificationId get() = if (slot == PreparationJobs.ANIME) 40 else 41

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
    private suspend fun progress(label: String) {
        setProgress(workDataOf("label" to label))
        setForeground(foreground(label))
    }
    override suspend fun doWork(): Result {
        if (slot != PreparationJobs.SITES || token.isBlank()) return Result.failure()
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
        } catch (challenge: VerificationRequired) {
            PreparationJobs.needsVerification(slot, token, challenge.browserUrl)
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
    private val outputTree by lazy { AppSettings(applicationContext).outputTree }
    private val existingFiles by lazy { ExistingOutputIndex(applicationContext, outputTree) }
    private suspend fun reuseExisting(identity: DownloadIdentity): Boolean {
        for ((id, record) in Exports.all(applicationContext)) if (record.identity?.key == identity.key && record.uri.isEmpty())
            clearPendingVideo(applicationContext, id)
        val location = downloadLocation(identity.anime.title, identity.episode.number.takeUnless { identity.isSingleVideo },
            identity.quality, "mp4", identity.audio)
        val found = withContext(Dispatchers.IO) { existingFiles.find(location) } ?: return false
        adoptExistingVideo(applicationContext, identity, found, outputTree)
        return true
    }
    private suspend fun enqueue(identity: DownloadIdentity, media: ResolvedMedia) {
        val context = applicationContext
        if (reuseExisting(identity)) return
        val exists = Exports.all(context).any { (id, record) -> !Exports.deleting(id) && (record.identity?.key == identity.key || id == identity.key) }
        if (exists) return
        currentCoroutineContext().ensureActive()
        if (!PreparationJobs.active(slot, token)) throw CancellationException("Preparation paused or replaced")
        AndroidDownloadRepository(context, AnimeRepository()).enqueue(identity, media)
    }
    private suspend fun attempt(action: suspend () -> Unit): Boolean {
        repeat(2) { retry ->
            currentCoroutineContext().ensureActive()
            if (!PreparationJobs.active(slot, token)) throw CancellationException("Preparation paused or replaced")
            try {
                withTimeout(120_000) { action() }
                return true
            } catch (_: TimeoutCancellationException) { /* Retry a timed-out source, keeping the journal. */
            } catch (folder: OutputFolderAccessRequired) { throw folder
            } catch (challenge: VerificationRequired) { throw challenge
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { /* Retry below, then let WorkManager back off. */ }
            if (retry == 0) delay(3_000)
        }
        return false
    }
    private suspend fun prepareSites(record: SiteDownloadPreparation): Result {
        val remaining = record.batches().toMutableList()
        val cookies = SiteDownloadCookieStore(applicationContext)
        val repository = SiteDownloadRepository(cookieHeader = cookies::headerFor)
        val start = android.os.SystemClock.elapsedRealtime()
        var done = 0
        val total = remaining.sumOf { it.episodes.size }
        while (remaining.isNotEmpty()) {
            if (android.os.SystemClock.elapsedRealtime() - start > 7 * 60_000) break
            val batch = remaining.first()
            val episode = batch.episodes.first()
            progress(if (batch.series.isSingleVideo) "Preparing video" else "Preparing ${done + 1}/$total · ${batch.series.title} · Episode ${episode.displayNumber}")
            if (attempt {
                if (batch.quality == "Best" || !reuseExisting(DownloadIdentity.siteDownload(batch.series, episode, batch.quality, "und"))) {
                    val media = cookies.applyTo(repository.resolve(episode.url, batch.quality, cookies.headerFor(episode.url)))
                    enqueue(DownloadIdentity.siteDownload(batch.series, episode, media.quality, media.audio), media)
                }
            }) {
                val next = batch.copy(episodes = batch.episodes.drop(1))
                if (next.episodes.isEmpty()) remaining.removeAt(0) else remaining[0] = next
                done++
                if (!PreparationJobs.save(slot, token, record.withBatches(remaining))) return Result.success()
            }
        }
        return if (remaining.isEmpty()) Result.success() else Result.retry()
    }
}
