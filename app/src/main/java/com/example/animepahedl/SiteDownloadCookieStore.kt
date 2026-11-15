package com.example.animepahedl

import android.content.Context
import androidx.core.content.edit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Imported cookies stay in private preferences; only matching domains receive them. */
internal class SiteDownloadCookieStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("sites-cookies", Context.MODE_PRIVATE)
    val hasCookies get() = preferences.getString("input", "").orEmpty().let { it.isNotBlank() && validSiteDownloadCookieInput(it) }
    fun headerFor(url: String): String {
        val browser = android.webkit.CookieManager.getInstance().getCookie(url).orEmpty()
        val imported = siteDownloadCookieHeader(preferences.getString("input", "").orEmpty(), url)
        return mergeSiteDownloadCookies(imported, browser)
    }
    fun applyTo(media: ResolvedMedia): ResolvedMedia {
        val cookie = headerFor(media.url)
        if (cookie.isBlank()) return media
        return media.copy(headers = media.headers.orEmpty().filterKeys { !it.equals("Cookie", true) } + ("Cookie" to cookie))
    }
    fun save(input: String) {
        require(input.toByteArray(Charsets.UTF_8).size <= 262_144) { "Cookie import is too large." }
        val normalized = normalizeSiteDownloadCookieFile(input)
        require(normalized.isBlank() || validSiteDownloadCookieInput(normalized)) { "Import a Netscape cookie file with domain information." }
        preferences.edit { if (normalized.isBlank()) remove("input") else putString("input", normalized) }
    }
    fun clear() = preferences.edit { remove("input") }
}

/** Browser verification can refresh a session after import; never replace it with the saved snapshot. */
internal fun mergeSiteDownloadCookies(imported: String, browser: String): String {
    val merged = linkedMapOf<String, String>()
    for (header in listOf(imported, browser)) header.split(';').forEach { part ->
        val name = part.substringBefore('=').trim()
        if (name.isNotEmpty() && '=' in part) merged[name] = part.substringAfter('=').trim()
    }
    return merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
}

/** Preserve tab-delimited empty values while accepting standard Windows line endings. */
internal fun normalizeSiteDownloadCookieFile(input: String): String = input.replace("\r\n", "\n").trim('\n')

internal fun validSiteDownloadCookieInput(input: String): Boolean {
    val normalized = normalizeSiteDownloadCookieFile(input)
    if ('\r' in normalized) return false
    val rows = normalized.split('\n').filter { it.isNotBlank() && (!it.startsWith('#') || it.startsWith("#HttpOnly_")) }
    if (rows.isEmpty()) return false
    return rows.all { line ->
        val fields = line.removePrefix("#HttpOnly_").split('\t')
        fields.size == 7 && fields[0].isNotBlank() && fields[5].isNotBlank() &&
            fields[4].toLongOrNull() != null && fields[1].uppercase() in setOf("TRUE", "FALSE") &&
            fields[3].uppercase() in setOf("TRUE", "FALSE") && validCookiePair(fields[5], fields[6])
    }
}

internal fun siteDownloadCookieHeader(input: String, url: String, nowSeconds: Long = System.currentTimeMillis() / 1000): String {
    val target = url.toHttpUrlOrNull() ?: return ""
    val normalized = normalizeSiteDownloadCookieFile(input)
    // A raw header has no domain metadata and must never be sent to an arbitrary website.
    if (!validSiteDownloadCookieInput(normalized)) return ""
    return normalized.split('\n').filter { it.isNotBlank() && (!it.startsWith('#') || it.startsWith("#HttpOnly_")) }
        .mapNotNull { line ->
            val fields = line.removePrefix("#HttpOnly_").split('\t')
            val domain = fields[0].removePrefix(".").lowercase()
            val domainMatches = target.host == domain || (fields[1].equals("TRUE", true) && target.host.endsWith(".$domain"))
            val cookiePath = fields[2].ifBlank { "/" }
            val pathMatches = target.encodedPath == cookiePath || (target.encodedPath.startsWith(cookiePath) &&
                (cookiePath.endsWith('/') || target.encodedPath.drop(cookiePath.length).startsWith('/')))
            val expiry = fields[4].toLongOrNull() ?: return@mapNotNull null
            if (!domainMatches || !pathMatches || (fields[3].equals("TRUE", true) && !target.isHttps) || (expiry > 0 && expiry <= nowSeconds)) null
            else "${fields[5]}=${fields[6]}"
        }.joinToString("; ")
}

private fun validCookiePair(name: String, value: String): Boolean =
    name.matches(Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) && value.none { it.code < 0x20 || it.code >= 0x7f || it == ';' }
