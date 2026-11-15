package com.example.animepahedl.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

/** A hold owns a temporary speed change until release, cancellation, or leaving the player. */
internal fun Modifier.dramaPlayerGestures(
    enabled: Boolean,
    onTap: () -> Unit,
    onHold: () -> (() -> Unit),
): Modifier = composed {
    val latestTap by rememberUpdatedState(onTap)
    val latestHold by rememberUpdatedState(onHold)
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        var restore: (() -> Unit)? = null
        detectTapGestures(
            onTap = { latestTap() },
            onLongPress = { restore = latestHold() },
            onPress = {
                try {
                    awaitRelease()
                } finally {
                    val finish = restore
                    restore = null
                    finish?.invoke()
                }
            },
        )
    }
}
