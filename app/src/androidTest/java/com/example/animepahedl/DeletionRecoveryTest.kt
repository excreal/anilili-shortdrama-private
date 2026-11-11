package com.example.animepahedl

import android.content.*
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DeletionRecoveryTest {
    @Test fun missingStagingMediaOffersFreshSourceRetryInsteadOfRetrySave() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val id = "direct:${System.nanoTime()}"
        try {
            Exports.put(app, id, ExportRecord("Missing source fixture", "", "", error = "Saving failed"))
            DownloadStore.flush()
            val repository = AndroidDownloadRepository(app, AnimeRepository())
            withContext(Dispatchers.Main) { repository.refresh() }
            val row = repository.rows.value.first { it.id == id }
            assertTrue(DownloadAction.RETRY in row.actions)
            assertFalse(DownloadAction.RETRY_SAVE in row.actions)
        } finally { Exports.remove(app, id); DownloadStore.flush() }
    }

    @Test fun batchContinuesAfterProviderRefusalAndKeepsFailedMetadata() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        DownloadStore.awaitReady()
        val ids = listOf("delete", "missing", "refuse", "denied").associateWith { "direct:${System.nanoTime()}" }
        ids.forEach { (kind, id) -> Exports.put(app, id, ExportRecord("Deletion fixture", "", "", uri = "content://com.example.animepahedl.test.deletion/$kind-${System.nanoTime()}")) }
        DownloadStore.flush()
        val repository = AndroidDownloadRepository(app, AnimeRepository())
        try {
            val results = withContext(Dispatchers.Main) { repository.deleteBatch(ids.values.toSet()) }
            assertEquals(2, results.count { it.succeeded })
            assertFalse(Exports.all(app).containsKey(ids.getValue("delete")))
            assertFalse(Exports.all(app).containsKey(ids.getValue("missing")))
            listOf("refuse", "denied").forEach { kind ->
                assertTrue(Exports.all(app).containsKey(ids.getValue(kind)))
                assertTrue(Exports.deleting(ids.getValue(kind)))
            }
        } finally { ids.values.forEach { Exports.remove(app, it); DownloadStore.operation(it, "delete", null) }; DownloadStore.flush() }
    }
    @Test fun deletingAlreadyMissingSavedMediaClearsItsDownloadRow() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val id = "already-missing:${System.nanoTime()}"
        Exports.put(app, id, ExportRecord("Missing saved fixture", "", "", uri = "content://com.example.animepahedl.test.deletion/missing-${System.nanoTime()}"))
        DownloadStore.flush()
        try {
            val repository = AndroidDownloadRepository(app, AnimeRepository())
            val result = withContext(Dispatchers.Main) { repository.delete(id) }
            assertTrue(result.message.orEmpty(), result.succeeded)
            assertFalse(Exports.all(app).containsKey(id))
        } finally { Exports.remove(app, id); DownloadStore.operation(id, "delete", null); DownloadStore.flush() }
    }
    @Test fun externallyDeletedFileWithNullCursorIsReconciledAndCanBeDeleted() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val id = "removed-outside-app:${System.nanoTime()}"
        val record = ExportRecord("Externally removed video", "", "",
            uri = "content://com.example.animepahedl.test.deletion/nullcursor-${System.nanoTime()}")
        Exports.put(app, id, record)
        DownloadStore.flush()
        try {
            assertEquals(setOf(id), missingSavedFiles(app, mapOf(id to record)).keys)
            val outcome = AndroidDownloadRepository(app, AnimeRepository()).delete(id)
            assertTrue(outcome.message.orEmpty(), outcome.succeeded)
            assertFalse(Exports.all(app).containsKey(id))
        } finally {
            Exports.remove(app, id)
            DownloadStore.operation(id, "delete", null)
            DownloadStore.flush()
        }
    }

    @Test fun inaccessibleProviderDoesNotCauseAutomaticRemoval() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val id = "provider-inaccessible:${System.nanoTime()}"
        val record = ExportRecord("Permission failure fixture", "", "",
            uri = "content://com.example.animepahedl.test.deletion/unavailable-${System.nanoTime()}")
        assertTrue(missingSavedFiles(app, mapOf(id to record)).isEmpty())
    }

    @Test fun forgettingSavedRecordClearsFailedDeleteMarkerAndAllowsRequeue() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val id = "forget-after-delete-failure:${System.nanoTime()}"
        Exports.put(app, id, ExportRecord("Saved fixture", "", "", uri = "content://saved/fixture"))
        DownloadStore.operation(id, "delete", "Storage provider refused deletion")
        DownloadStore.operation(id, "delete-keep-file", "1")
        DownloadStore.flush()
        try {
            forgetSavedDownloadRecord(app, id)
            assertFalse(Exports.all(app).containsKey(id))
            assertFalse(Exports.deleting(id))
            assertFalse(DownloadStore.operations("delete-keep-file").containsKey(id))

            Exports.prepare(app, id, "Requeued fixture")
            DownloadStore.flush()
            assertTrue(Exports.all(app).containsKey(id))
            assertTrue(Exports.all(app)[id]?.uri.isNullOrBlank())
        } finally {
            Exports.remove(app, id)
            DownloadStore.operation(id, "delete", null)
            DownloadStore.operation(id, "delete-keep-file", null)
            DownloadStore.flush()
        }
    }

    @Test fun legacyImportIsRepeatableAndNeverOverwritesSavedDestinations() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        withContext(Dispatchers.IO) {
            val db = androidx.room.Room.inMemoryDatabaseBuilder(context, DownloadDatabase::class.java).build()
            try {
                val dao = db.records()
                val record = ExportRecord("Saved", "tree", "folder", "content://saved/video", bytes = 123)
                val json = com.google.gson.Gson().toJson(record)
                repeat(2) { dao.importLegacy(StoredDownload("existing-id", json)) }
                dao.importLegacy(StoredDownload("existing-id", "{}"))
                assertEquals(1, dao.downloads().size)
                assertEquals(json, dao.downloads().single().metadata)
            } finally { db.close() }
        }
    }
}
