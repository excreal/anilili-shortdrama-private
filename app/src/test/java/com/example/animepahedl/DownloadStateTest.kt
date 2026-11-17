package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadStateTest {
    @Test fun networkWaitDistinguishesDisconnectedFromUnmeteredPolicy() {
        val network = androidx.media3.exoplayer.scheduler.Requirements.NETWORK
        val unmetered = androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED
        assertEquals("Waiting for internet", downloadRequirementsMessage(network or unmetered))
        assertEquals("Waiting for Wi-Fi or an unmetered network", downloadRequirementsMessage(unmetered))
        assertNull(downloadRequirementsMessage(0))
    }

    private fun saved(failure: FailureReason?) = DownloadRow(
        id = "saved",
        title = "Saved video",
        transfer = TransferState.COMPLETE,
        percent = 100f,
        availability = Availability.SAVED,
        failure = failure,
    )

    @Test fun savedStatusSurfacesDeletionAndFolderFailuresWithoutAssumingLocation() {
        assertEquals("Delete failed", savedFileStatus(saved(FailureReason.DELETION)))
        assertEquals("Folder access needed", savedFileStatus(saved(FailureReason.FOLDER_ACCESS)))
        assertEquals("Saved offline", savedFileStatus(saved(null)))
        assertNull(savedFileStatus(DownloadRow("active", "Active video", TransferState.DOWNLOADING, 25f)))
    }
}
