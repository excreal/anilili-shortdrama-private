package com.example.animepahedl

import android.content.Context
import java.security.MessageDigest

/** Local/remote playback share locator-based keys; signed playback URLs are never persisted. */
internal class PlaybackHistory(context: Context, preferenceName: String = "playback-history") {
    private val preferences = context.applicationContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
    fun position(key: String): Long = preferences.getLong("p:${hash(key)}", 0L).coerceAtLeast(0L)
    fun duration(key: String): Long = preferences.getLong("d:${hash(key)}", 0L).coerceAtLeast(0L)
    fun markWatched(key: String) {
        val episodeHash = hash(key)
        preferences.edit().putBoolean("w:$episodeHash", true).putLong("p:$episodeHash", 0L).apply()
    }
    fun lastEpisode(series: String): String? = preferences.getString("s:${hash(series)}", null)
    fun watched(key: String): Boolean = preferences.getBoolean("w:${hash(key)}", false)
    fun setWatched(key: String, watched: Boolean) {
        val editor = preferences.edit().putBoolean("w:${hash(key)}", watched)
        if (watched) editor.putLong("p:${hash(key)}", 0L)
        editor.apply()
    }
    fun removeFromContinueWatching(series: SiteDownloadSeries) {
        val key = "series:${series.provider.name}:${series.id}"
        preferences.edit().remove("meta:${hash(key)}").apply()
    }
    fun markCompleted(series: SiteDownloadSeries) {
        val episodes = (metadata(series)?.episodes.orEmpty() + series.episodes).distinctBy { it.url }
        val editor = preferences.edit()
        episodes.forEach {
            val episodeHash = hash(it.url)
            editor.putBoolean("w:$episodeHash", true).putLong("p:$episodeHash", 0L)
        }
        editor.apply()
    }
    fun fraction(key: String): Float {
        if (watched(key)) return 1f
        val duration = preferences.getLong("d:${hash(key)}", 0L)
        return if (duration > 0L) (position(key).toFloat() / duration).coerceIn(0f, 1f) else 0f
    }
    fun canResume(series: SiteDownloadSeries): Boolean {
        val last = lastEpisode("series:${series.provider.name}:${series.id}") ?: return false
        return preferences.getLong("p:$last", 0L) > 0L
    }
    fun save(series: String, key: String, position: Long, completed: Boolean = false, duration: Long = 0L) {
        val episodeHash = hash(key)
        val editor = preferences.edit()
        if (duration > 0L) editor.putLong("d:$episodeHash", duration)
        if (completed) editor.putBoolean("w:$episodeHash", true) else editor.remove("w:$episodeHash")
        editor.putLong("t:${hash(series)}", System.currentTimeMillis())
            .putLong("p:$episodeHash", if (completed) 0L else position.coerceAtLeast(0L))
            .putString("s:${hash(series)}", episodeHash).apply()
    }
    fun metadata(series: SiteDownloadSeries): SiteDownloadSeries? = runCatching {
        val key = "series:${series.provider.name}:${series.id}"
        preferences.getString("meta:${hash(key)}", null)?.let {
            com.google.gson.Gson().fromJson(it, SiteDownloadSeries::class.java)
        }
    }.getOrNull()
    fun registerLocal(series: SiteDownloadSeries) {
        val previous = metadata(series)
        register(if (previous == null) series else previous.copy(episodes =
            (previous.episodes + series.episodes).distinctBy { it.id }.sortedBy { it.displayNumber.toDoubleOrNull() ?: it.number.toDouble() }))
    }
    fun register(series: SiteDownloadSeries) {
        val key = "series:${series.provider.name}:${series.id}"
        preferences.edit().putString("meta:${hash(key)}", com.google.gson.Gson().toJson(series)).apply()
    }
    fun recent(): List<SiteDownloadSeries> = preferences.all.entries.filter { it.key.startsWith("meta:") }
        .mapNotNull { entry ->
            val time = preferences.getLong("t:" + entry.key.removePrefix("meta:"), 0)
            val last = preferences.getString("s:" + entry.key.removePrefix("meta:"), null)
            if (time == 0L || last == null || preferences.getLong("p:$last", 0L) <= 0L || preferences.getBoolean("w:$last", false)) null else runCatching { time to com.google.gson.Gson().fromJson(entry.value as String, SiteDownloadSeries::class.java) }.getOrNull()
        }.sortedByDescending { it.first }.take(12).map { it.second }
    fun matches(savedHash: String?, key: String): Boolean = savedHash != null && savedHash == hash(key)
}
