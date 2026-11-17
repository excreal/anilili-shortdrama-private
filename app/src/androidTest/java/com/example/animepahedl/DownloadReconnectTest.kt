package com.example.animepahedl

import android.content.Context
import android.os.PowerManager
import androidx.core.content.edit
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.offline.*
import androidx.media3.exoplayer.scheduler.Requirements
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloadReconnectTest {
    @Test fun unmetRequirementsHoldRetryAndReconnectResumesWithoutOverridingPause() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        DownloadStore.awaitReady()
        // Use an unmet device-idle requirement as a deterministic stand-in for lost connectivity.
        // This exercises real DownloadManager callbacks without switching the device's radios.
        assumeFalse(app.getSystemService(PowerManager::class.java).isDeviceIdleMode)
        val id = "reconnect-${System.nanoTime()}"
        val database = StandaloneDatabaseProvider(app)
        val index = DefaultDownloadIndex(database, id.replace("-", "_"))
        val retries = app.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
        val failed = CountDownLatch(1)
        val restarted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        val changes = AtomicInteger()
        lateinit var manager: DownloadManager
        instrumentation.runOnMainSync {
            manager = DownloadManager(app, index, DownloaderFactory {
                object : Downloader {
                    override fun download(listener: Downloader.ProgressListener?) {
                        if (calls.incrementAndGet() == 1) throw IOException("Disconnected fixture")
                        restarted.countDown()
                        release.await(15, TimeUnit.SECONDS)
                    }
                    override fun cancel() { release.countDown() }
                    override fun remove() = Unit
                }
            })
            manager.minRetryCount = 0
            manager.setRequirements(Requirements(0))
            manager.addListener(DownloadRequirementsListener(
                onReady = { OfflineDownloads.retryPending(app, it) },
                onChanged = { changes.incrementAndGet() },
            ))
            manager.addListener(object : DownloadManager.Listener {
                override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                    if (download.state == Download.STATE_FAILED) {
                        retries.edit { putBoolean(id, true) }
                        manager.setRequirements(Requirements(Requirements.DEVICE_IDLE))
                        failed.countDown()
                    }
                }
            })
            manager.resumeDownloads()
            manager.addDownload(DownloadRequest.Builder(id, android.net.Uri.parse("https://fixture.invalid/video.mp4")).build())
        }
        try {
            assertTrue("Fixture should fail once", failed.await(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                assertNotEquals(0, manager.notMetRequirements)
                OfflineDownloads.retryPending(app, manager)
                assertEquals(1, calls.get())
                manager.pauseDownloads()
                manager.setRequirements(Requirements(0))
                assertTrue(manager.downloadsPaused)
                assertEquals("Reconnect must not override manual pause", 1, calls.get())
                manager.resumeDownloads()
            }
            assertTrue("Pending retry must restart on readiness/resume", restarted.await(10, TimeUnit.SECONDS))
            assertEquals(2, calls.get())
            assertTrue("Requirement-only changes must refresh status", changes.get() >= 3)
        } finally {
            release.countDown()
            instrumentation.runOnMainSync { manager.release() }
            index.removeDownload(id)
            retries.edit { remove(id); remove("attempts:$id") }
            database.close()
        }
    }

    @Test fun schedulerRestartIntentResolvesToDownloadService() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = android.content.Intent("androidx.media3.exoplayer.downloadService.action.RESTART")
            .setPackage(app.packageName).addCategory(android.content.Intent.CATEGORY_DEFAULT)
        val service = app.packageManager.resolveService(intent, 0)?.serviceInfo
        assertEquals(OfflineDownloadService::class.java.name, service?.name)
        assertFalse(service!!.exported)
    }
}
