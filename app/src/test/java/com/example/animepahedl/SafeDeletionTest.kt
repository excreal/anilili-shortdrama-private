package com.example.animepahedl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeDeletionTest {
    @Test fun matchesOnlySameBasenameSubtitleSidecarsIgnoringCase() {
        assertTrue(isOutputSubtitleSidecar("The Show - Episode 1.srt", "The Show - Episode 1.mp4"))
        assertTrue(isOutputSubtitleSidecar("the show - episode 1.VTT", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 2.srt", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 1.srt.bak", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 1.jpg", "The Show - Episode 1.mp4"))
    }
}
