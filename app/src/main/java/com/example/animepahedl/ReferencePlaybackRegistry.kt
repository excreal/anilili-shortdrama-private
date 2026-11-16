package com.example.animepahedl

import android.content.Context
import android.content.SharedPreferences

/** Stable local series IDs bridge the source player's media-session/external-return contracts. */
internal object ReferencePlaybackRegistry {
    private lateinit var prefs: SharedPreferences
    private lateinit var history: PlaybackHistory
    fun init(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("reference-player-series", Context.MODE_PRIVATE)
        history = PlaybackHistory(appContext)
    }
    private data class Binding(val series: Int, val episode: String)
    private val writes = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val completed = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val bindings = linkedMapOf<String, Binding>()
    private fun hash(value: String) = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    val inPictureInPicture = kotlinx.coroutines.flow.MutableStateFlow(false)
    val ready = kotlinx.coroutines.flow.MutableStateFlow(false)
    val pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun bind(id: Int, episode: SiteDownloadEpisode, url: String) {
        synchronized(bindings) { bindings[hash(url)] = Binding(id, episode.id); while (bindings.size > 64) bindings.remove(bindings.keys.first()) }
    }
    fun progressForStream(url: String, position: Long, duration: Long, ended: Boolean = false, force: Boolean = false) {
        val binding = synchronized(bindings) { bindings[hash(url)] } ?: return
        val series = series(binding.series) ?: return
        val episode = series.episodes.firstOrNull { it.id == binding.episode } ?: return
        if (position <= 0L && duration <= 0L && !ended) return
        val identity = "${binding.series}:${binding.episode}"
        if (ended) completed.add(identity)
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && !ended && now - (writes[identity] ?: Long.MIN_VALUE / 2) < 5_000L) return
        writes[identity] = now
        history.save("series:${series.provider}:${series.id}", episode.url, position, identity in completed, duration)
        if (duration > 0L && position.toDouble() / duration >= com.anilili.data.settings.SettingsStore.watchedThresholdPercent.value / 100.0) history.markWatched(episode.url)
    }
    fun beginEpisode(id: Int, episode: SiteDownloadEpisode) { completed.remove("$id:${episode.id}") }
    fun capture() {
        val snapshot = com.anilili.playback.PlaybackService.hostSnapshot() ?: return
        progressForStream(snapshot.url, snapshot.position, snapshot.duration, snapshot.ended, force = true)
    }
    fun isOffline(id: Int) = prefs.getBoolean("offline:$id", false)
    fun register(series: SiteDownloadSeries, offline: Boolean = false): Int {
        val key = "series:${series.provider}:${series.id}"
        val existing = prefs.getInt(key, 0)
        val id = existing.takeIf { it > 0 } ?: (prefs.getInt("next-id", 1)).also {
            prefs.edit().putInt("next-id", it + 1).putInt(key, it).apply()
        }
        prefs.edit().putString("metadata:$id", com.google.gson.Gson().toJson(series)).putBoolean("offline:$id", offline).apply()
        return id
    }
    fun series(id: Int): SiteDownloadSeries? = runCatching {
        prefs.getString("metadata:$id", null)?.let { com.google.gson.Gson().fromJson(it, SiteDownloadSeries::class.java) }
    }.getOrNull()
    fun progress(id: Int, number: Double, position: Long, duration: Long, completed: Boolean) {
        val series = series(id) ?: return
        val episode = series.episodes.firstOrNull { it.displayNumber.toDoubleOrNull() == number } ?: return
        history.save("series:${series.provider}:${series.id}", episode.url, position, completed, duration)
    }
}
