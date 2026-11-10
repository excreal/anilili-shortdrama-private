package com.example.animepahedl

import androidx.core.content.edit
import android.content.Context

/** DownloadManager destinations must stay inside the public Downloads directory. */
internal fun normalizeOutputFolder(input: String): String? {
    val path = input.trim().trim('/')
    if (path.isEmpty()) return ""
    val segments = path.split('/')
    if (segments.any { it.isBlank() || it == "." || it == ".." || it.length > 80 ||
        it.any { char -> char.code < 32 || char in "\\:*?\"<>|" } }) return null
    return path.takeIf { it.length <= 180 }
}

internal class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var unmeteredOnly: Boolean
        get() = preferences.getBoolean("unmetered-only", false)
        set(value) { preferences.edit { putBoolean("unmetered-only", value) } }
    var defaultAudio: String
        get() = preferences.getString("default-audio", "Original").orEmpty()
        set(value) { preferences.edit { putString("default-audio", value) } }
    var connections: Int
        get() = preferences.getInt("hls-connections", 8).coerceIn(2, 12)
        set(value) { preferences.edit { putInt("hls-connections", value.coerceIn(2, 12)) } }
    var simultaneousDownloads: Int
        get() = preferences.getInt("simultaneous-downloads", 2).coerceIn(1, 6)
        set(value) { preferences.edit { putInt("simultaneous-downloads", value.coerceIn(1, 6)) } }
    var outputTree: String
        get() = preferences.getString("output-tree", "").orEmpty()
        set(value) { preferences.edit { putString("output-tree", value) } }
    var outputFolder: String
        get() = preferences.getString("output-folder", "ShortDrama").orEmpty()
        set(value) { preferences.edit { putString("output-folder", value) } }
}
