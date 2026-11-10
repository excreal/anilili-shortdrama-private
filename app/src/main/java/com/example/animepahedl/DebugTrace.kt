package com.example.animepahedl

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

/** Debug-only metadata. Never writes cookie values, request bodies, or signed URL queries. */
internal object DebugTrace {
    private var file: File? = null
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor()
    fun initialize(context: Context) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            file = File(context.getExternalFilesDir(null) ?: context.filesDir, "captures/http-diagnostics.jsonl")
                .also { it.parentFile?.mkdirs() }
        }
    }

    fun safeUrl(url: String): String = url.toHttpUrlOrNull()?.newBuilder()?.username("")?.password("")?.encodedPath("/")?.query(null)?.fragment(null)?.build()?.toString() ?: "invalid-url"

    @Synchronized
    fun event(name: String, vararg values: Pair<String, Any?>) {
        val destination = file ?: return
        val line = Gson().toJson(mapOf("time" to System.currentTimeMillis(), "event" to name) + values.associate { (key, value) -> key to if (key in setOf("reason", "summary", "message")) "redacted" else value })
        Log.d("AnililiShortDramaHTTP", line)
        writer.execute {
            runCatching {
                if (destination.length() > 1_000_000) destination.writeText("")
                destination.appendText("$line\n")
            }
        }
    }
}
