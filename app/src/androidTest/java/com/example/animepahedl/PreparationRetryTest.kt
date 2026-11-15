package com.example.animepahedl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class PreparationRetryTest {
    @Test fun failedLinkStopsAfterThreeAttemptsAndSuccessfulProgressResetsBudget() = runBlocking {
        DownloadStore.awaitReady()
        val slot = "test-preparation-${UUID.randomUUID()}"
        val token = UUID.randomUUID().toString()
        try {
            DownloadStore.operation(slot, "preparation", "{}")
            DownloadStore.operation(slot, "preparation-generation", token)
            DownloadStore.flush()
            assertTrue(PreparationJobs.failed(slot, token))
            assertTrue(PreparationJobs.save(slot, token, emptyMap<String, String>()))
            assertFalse(slot in DownloadStore.operations("preparation-failures"))
            assertTrue(PreparationJobs.failed(slot, token))
            assertTrue(PreparationJobs.failed(slot, token))
            assertFalse(PreparationJobs.failed(slot, token))
            assertFalse(PreparationJobs.active(slot, token))
            assertNotNull(PreparationJobs.payload(slot))
            assertTrue(DownloadStore.operations("preparation-error")[slot]!!.contains("3 attempts"))
            assertFalse(PreparationJobs.failed(slot, "replaced-worker"))
            assertEquals("3", DownloadStore.operations("preparation-failures")[slot])
        } finally {
            for (kind in listOf("preparation", "preparation-generation", "preparation-paused", "preparation-failures", "preparation-error")) {
                DownloadStore.operation(slot, kind, null)
            }
            DownloadStore.flush()
        }
    }
}
