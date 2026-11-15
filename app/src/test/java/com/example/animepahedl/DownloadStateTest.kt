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

    @Test fun savedStatusSurfacesDeletionAndFolderFailuresWithoutAssumingLocation() {
        assertEquals("Delete failed", savedFileStatus(saved(FailureReason.DELETION)))
        assertEquals("Folder access needed", savedFileStatus(saved(FailureReason.FOLDER_ACCESS)))
        assertEquals("Saved offline", savedFileStatus(saved(null)))
        assertNull(savedFileStatus(DownloadRow("active", "Active video", TransferState.DOWNLOADING, 25f)))
    }
}
