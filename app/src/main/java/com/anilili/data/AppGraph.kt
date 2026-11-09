package com.anilili.data

import android.content.Context
import android.app.ActivityManager
import com.anilili.data.remote.OpenSubtitlesClient
import com.anilili.data.auth.OpenSubtitlesAuthManager
import com.anilili.data.settings.SettingsStore
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

/** Host bindings only: the copied player and its state/settings remain in their original packages. */
internal object AppGraph {
    var isTv = false; private set
    var isLowRamDevice = false; private set
    var memoryClassMb = 128; private set
    lateinit var httpClient: okhttp3.OkHttpClient; private set
    lateinit var openSubtitles: OpenSubtitlesClient; private set
    private val settingsReady = CompletableDeferred<Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    suspend fun awaitReady() = settingsReady.await()
    private var initialized = false
    @Synchronized fun init(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        val manager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        isLowRamDevice = manager.isLowRamDevice; memoryClassMb = manager.memoryClass
        isTv = com.anilili.data.device.isTelevision(app)
        httpClient = okhttp3.OkHttpClient.Builder().connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(40, java.util.concurrent.TimeUnit.SECONDS).build()
        SettingsStore.init(app)
        configureOpenSubtitles(app)
        scope.launch {
            SettingsStore.awaitLoaded()
            val prefs = app.getSharedPreferences("app-settings", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("reference-player-preferences-migrated", false)) {
                if (prefs.contains("play-on-open")) SettingsStore.setPlayOnOpen(prefs.getBoolean("play-on-open", true))
                if (prefs.contains("auto-next")) SettingsStore.setAutoplay(prefs.getBoolean("auto-next", true))
                prefs.edit().putBoolean("reference-player-preferences-migrated", true).apply()
            }
            settingsReady.complete(Unit)
            combine(SettingsStore.playOnOpen, SettingsStore.autoplay) { play, next -> play to next }.collect { (play, next) ->
                prefs.edit().putBoolean("play-on-open", play).putBoolean("auto-next", next).apply()
            }
        }
    }
    fun configureOpenSubtitles(context: Context) {
        val key = context.getSharedPreferences("app-settings", Context.MODE_PRIVATE).getString("opensubtitles-api-key", "").orEmpty()
        openSubtitles = OpenSubtitlesClient(httpClient, Json { ignoreUnknownKeys = true }, key, "Anilili Short Dramas/1.1")
        OpenSubtitlesAuthManager.init(context, openSubtitles)
    }
    fun refreshIsTv(value: Boolean) { isTv = value }
}
