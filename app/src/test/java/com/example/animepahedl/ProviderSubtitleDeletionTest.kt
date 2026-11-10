package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class ProviderSubtitleDeletionTest {
    @Test fun onlyUnreferencedVttFilesInsideSubtitleDirectoryAreEligibleForDeletion() {
        val root = java.nio.file.Files.createTempDirectory("subtitle-delete-").toFile()
        try {
            val subtitles = File(root, "provider-subtitles").apply { mkdir() }
            val referenced = File(subtitles, "shared.vtt").apply { writeText("WEBVTT") }
            val candidate = File(subtitles, "orphan.vtt").apply { writeText("WEBVTT") }
            val unrelated = File(root, "outside.vtt").apply { writeText("WEBVTT") }

            assertNull(unreferencedProviderSubtitleFile(referenced.toURI().toString(), subtitles, setOf(referenced.toURI().toString())))
            assertEquals(candidate.canonicalFile,
                unreferencedProviderSubtitleFile(candidate.toURI().toString(), subtitles, emptySet()))
            assertNull(unreferencedProviderSubtitleFile(unrelated.toURI().toString(), subtitles, emptySet()))
            assertNull(unreferencedProviderSubtitleFile(File(subtitles, "captions.srt").toURI().toString(), subtitles, emptySet()))
            assertNull(unreferencedProviderSubtitleFile("https://cdn.example/captions.vtt", subtitles, emptySet()))
        } finally {
            root.deleteRecursively()
        }
    }
}
