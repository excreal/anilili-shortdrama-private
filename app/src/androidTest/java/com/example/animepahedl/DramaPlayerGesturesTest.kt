package com.example.animepahedl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.example.animepahedl.components.dramaPlayerGestures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DramaPlayerGesturesTest {
    @get:Rule val compose = createComposeRule()

    private var speed = 1.25f
    private var taps = 0
    private val changes = mutableListOf<Float>()
    private val enabled = mutableStateOf(true)

    private fun showGestureSurface() {
        compose.setContent {
            Box(Modifier.size(200.dp).testTag("video").dramaPlayerGestures(
                enabled = enabled.value,
                onTap = { taps++ },
                onHold = {
                    val previous = speed
                    speed = 2f
                    changes += speed
                    val restore: () -> Unit = { speed = previous; changes += speed }
                    restore
                },
            ))
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun manualPagingSwitchesMediaAndKeepsOnePlayerView() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val entries = (0..2).map { com.example.animepahedl.components.DramaPlayerEpisode("swipe-$it", "swipe-history-$it", "Episode ${it + 1}") }
        compose.setContent {
            com.example.animepahedl.components.DramaPlayerSurface("Swipe regression", "swipe-regression", entries,
                entries[0].id, onClose = {}, resolve = { index ->
                    // Missing local media is deliberate: the timeline must still switch to the
                    // requested item, and a load error must not restore the preceding episode.
                    androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(
                        androidx.media3.datasource.DefaultDataSource.Factory(context)
                    ).createMediaSource(androidx.media3.common.MediaItem.Builder()
                        .setMediaId(entries[index].id).setUri("file:///missing-swipe-$index.mp4").build())
                })
        }
        var original: androidx.media3.ui.PlayerView? = null
        fun assertSelected(index: Int) {
            compose.waitForIdle()
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(androidx.media3.ui.PlayerView::class.java))
                .check { view, error ->
                    if (error != null) throw error
                    val output = view as androidx.media3.ui.PlayerView
                    if (original == null) original = output else org.junit.Assert.assertSame(original, output)
                    assertEquals(entries[index].id, output.player?.currentMediaItem?.mediaId)
                }
        }
        assertSelected(0)
        compose.onNodeWithTag("drama-pager").performTouchInput { swipeUp(durationMillis = 300) }
        assertSelected(1)
        compose.onNodeWithTag("drama-pager").performTouchInput { swipeUp(durationMillis = 300) }
        assertSelected(2)
    }

    @Test fun tapDoesNotChangeSpeed() {
        showGestureSurface()
        compose.onNodeWithTag("video").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(1, taps)
            assertEquals(emptyList<Float>(), changes)
            assertEquals(1.25f, speed)
        }
    }

    @Test fun holdRestoresSelectedSpeedWithoutTogglingPlayback() {
        showGestureSurface()
        compose.onNodeWithTag("video").performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(listOf(2f, 1.25f), changes)
            assertEquals(0, taps)
            assertEquals(1.25f, speed)
        }
    }

    @Test fun swipeBeforeHoldDoesNotChangeSpeedOrTogglePlayback() {
        showGestureSurface()
        compose.onNodeWithTag("video").performTouchInput { swipeUp(durationMillis = 200) }
        compose.runOnIdle {
            assertEquals(emptyList<Float>(), changes)
            assertEquals(0, taps)
        }
    }

    @Test fun cancellingHoldRestoresSelectedSpeed() {
        showGestureSurface()
        compose.onNodeWithTag("video").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            moveTo(center)
            cancel()
        }
        compose.runOnIdle {
            assertEquals(listOf(2f, 1.25f), changes)
            assertEquals(0, taps)
        }
    }

    @Test fun leavingForegroundDuringHoldRestoresSelectedSpeed() {
        showGestureSurface()
        compose.onNodeWithTag("video").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            moveTo(center)
        }
        compose.runOnIdle {
            assertEquals(2f, speed)
            enabled.value = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(2f, 1.25f), changes)
            assertEquals(0, taps)
        }
        compose.onNodeWithTag("video").performTouchInput { cancel() }
    }
}
