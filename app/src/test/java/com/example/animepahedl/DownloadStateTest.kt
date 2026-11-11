package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadStateTest {
    private fun saved(failure: FailureReason?) = DownloadRow(
        id = "saved",
        title = "Saved video",
        transfer = TransferState.COMPLETE,
        percent = 100f,
        availability = Availability.SAVED,
        failure = failure,
    )

    @Test fun savedLocationStatusSurfacesDeletionAndFolderFailures() {
        assertEquals("Delete failed", savedFileLocationStatus(saved(FailureReason.DELETION)))
        assertEquals("Folder access needed", savedFileLocationStatus(saved(FailureReason.FOLDER_ACCESS)))
        assertEquals("In Downloads folder", savedFileLocationStatus(saved(null)))
        assertNull(savedFileLocationStatus(DownloadRow("active", "Active video", TransferState.DOWNLOADING, 25f)))
    }
}
