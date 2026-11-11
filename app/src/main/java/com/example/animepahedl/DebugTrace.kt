package com.example.animepahedl

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Local, exportable request and playback diagnostics. Values are stored exactly as supplied. */
internal object DebugTrace {
    private const val MAX_LOG_BYTES = 5L * 1024 * 1024
    @Volatile private var file: File? = null
    private val previousFile get() = file?.let { File(it.parentFile, "${it.name}.1") }
    private val writer = Executors.newSingleThreadExecutor()
    private val fileLock = Any()
    private val gson = Gson()

    fun initialize(context: Context) {
        val destination = File(context.filesDir, "diagnostics/anilili-diagnostics.jsonl")
        destination.parentFile?.mkdirs()
        file = destination
        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0)
        }
        event("app_started", "version_name" to packageInfo.versionName, "version_code" to packageInfo.longVersionCode,
            "android_release" to Build.VERSION.RELEASE, "android_sdk" to Build.VERSION.SDK_INT,
            "device" to "${Build.MANUFACTURER} ${Build.MODEL}", "abis" to Build.SUPPORTED_ABIS.toList())
    }

    /** Kept for existing call sites. Diagnostic values are intentionally not sanitized. */
    fun safeUrl(url: String): String = url

    @Synchronized
    fun event(name: String, vararg values: Pair<String, Any?>) {
        val destination = file ?: return
        val line = gson.toJson(mapOf("time" to System.currentTimeMillis(), "event" to name) + values.toMap())
        Log.d("AnililiShortDramaHTTP", line)
        writer.execute {
            synchronized(fileLock) {
                runCatching {
                    destination.parentFile?.mkdirs()
                    if (destination.length() + line.toByteArray(Charsets.UTF_8).size > MAX_LOG_BYTES) {
                        val previous = previousFile
                        if (previous != null) {
                            previous.delete()
                            destination.renameTo(previous)
                        }
                    }
                    destination.appendText("$line\n", Charsets.UTF_8)
                }.onFailure { Log.e("AnililiShortDramaHTTP", "Could not append diagnostics", it) }
            }
        }
    }

    /** Wait for queued events, then return the previous and current log in chronological order. */
    fun readLog(): String {
        val destination = file ?: error("Diagnostics have not been initialized")
        return writer.submit<String> {
            synchronized(fileLock) {
                buildString {
                    previousFile?.takeIf(File::exists)?.let { append(it.readText(Charsets.UTF_8)) }
                    destination.takeIf(File::exists)?.let { append(it.readText(Charsets.UTF_8)) }
                }
            }
        }.get(15, TimeUnit.SECONDS)
    }
}
