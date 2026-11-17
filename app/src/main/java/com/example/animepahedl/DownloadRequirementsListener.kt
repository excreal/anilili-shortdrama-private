package com.example.animepahedl

import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.scheduler.Requirements

/** Network changes can leave an item QUEUED, so download-state callbacks alone are insufficient. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class DownloadRequirementsListener(
    private val onReady: (DownloadManager) -> Unit,
    private val onChanged: () -> Unit,
) : DownloadManager.Listener {
    override fun onRequirementsStateChanged(manager: DownloadManager, requirements: Requirements, notMetRequirements: Int) {
        // Media3 resumes queued transfers itself. The owner may also have persisted failed retries.
        if (notMetRequirements == 0 && !manager.downloadsPaused) onReady(manager)
        onChanged()
    }

    override fun onWaitingForRequirementsChanged(manager: DownloadManager, waitingForRequirements: Boolean) {
        onChanged()
    }

    override fun onDownloadsPausedChanged(manager: DownloadManager, downloadsPaused: Boolean) {
        if (!downloadsPaused && !manager.downloadsPaused && manager.notMetRequirements == 0) onReady(manager)
        onChanged()
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun downloadRequirementsMessage(notMetRequirements: Int): String? = when {
    notMetRequirements and Requirements.NETWORK != 0 -> "Waiting for internet"
    notMetRequirements and Requirements.NETWORK_UNMETERED != 0 -> "Waiting for Wi-Fi or an unmetered network"
    else -> null
}
