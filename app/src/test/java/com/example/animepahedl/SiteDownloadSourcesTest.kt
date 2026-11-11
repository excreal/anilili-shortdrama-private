package com.example.animepahedl

import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SiteDownloadSourcesTest {
    private fun json(value: String) = JsonParser.parseString(value).asJsonObject
    private fun encrypted(url: String): String {
        val nonce = ByteArray(12) { 7 }
        val key = Base64.getDecoder().decode("QC6Ir2trghxRAyyyWZEOEFR4GgLhnfQ4A19I3QBlQkc=")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return Base64.getEncoder().encodeToString(nonce + cipher.doFinal(url.toByteArray()))
    }
    @Test fun identifiersRequireSupportedLinksAndExcludeTikTok() {
        assertEquals(SiteDownloadProvider.KISS18, siteDownloadProvider(siteDownloadInput("https://kiss18.vip/watch/story/4")))
        assertEquals("story" to 4, dramaRoute(siteDownloadInput("https://kiss18.vip/watch/story/4"), SiteDownloadProvider.KISS18))
        assertTrue(runCatching { siteDownloadInput("123") }.isFailure)
        assertTrue(runCatching { siteDownloadProvider(siteDownloadInput("https://www.tiktok.com/shortdrama/episode/123/1")) }.isFailure)
        assertTrue(runCatching { siteDownloadProvider(siteDownloadInput("https://kiss18.vip.evil.test/watch/story/1")) }.isFailure)
        assertTrue(runCatching { siteDownloadInput("file:///etc/passwd") }.isFailure)
        assertTrue(runCatching { siteDownloadInput("https://user:secret@kiss18.vip/") }.isFailure)
    }
    @Test fun dramaSeriesUsesKnownEpisodesAndRejectsForeignLinks() {
        val body = """<meta property="og:title" content="A Show | Dramatip"><a href="episode-2">2</a><a href="https://evil.test/series/a/episode-3">3</a><script>{"epCount":99,"serial_number":1}</script>"""
        val series = parseDramaSeries(body, "https://dramatip.net/series/a".toHttpUrl(), SiteDownloadProvider.DRAMATIP, "a")
        assertEquals("A Show", series.title)
        assertEquals(listOf(1, 2), series.episodes.map { it.number })
        assertEquals("https://dramatip.net/series/a/episode-2", series.episodes[1].url)
    }
    @Test fun encryptedSourcesAuthenticateAndStayOnCurrentEpisode() {
        val current = encrypted("https://cdn.test/current.m3u8")
        val next = encrypted("https://cdn.test/next.m3u8")
        val body = """{"sourceFirst":{"chain":[{"type":"hls","enc":"$current"}]},"nextSourceFirst":{"chain":[{"type":"hls","enc":"$next"}]}}"""
        assertEquals("https://cdn.test/current.m3u8", parseDramatipVariants(body).single().url)
        assertTrue(runCatching { parseDramatipVariants("""{"nextSourceFirst":{"chain":[{"enc":"$next"}]}}""") }.isFailure)
        val corrupted = Base64.getDecoder().decode(current).also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertTrue(runCatching { decryptDramaSource(Base64.getEncoder().encodeToString(corrupted)) }.isFailure)
    }
    @Test fun flightDeferredSourcesAndProviderGateways() {
        val raw = "https://video.shorttv.live/hls/a.m3u8?token=a%26b"
        val encrypted = encrypted(raw)
        val page = """{"sourceFirst":{"chain":[{"type":"dramawave","enc":"${'$'}46"}]}}""" + "\\n46:T${encrypted.length.toString(16)},$encrypted"
        val variant = parseDramatipVariants(page).single()
        assertEquals("https://cdn.dramadizilerim.xyz/https/video.shorttv.live/hls/a.m3u8?token=a%26b", variant.url)
        assertEquals(raw, variant.backupUrls.single().toHttpUrl().queryParameter("url"))
        assertEquals("https://dramatip.net", variant.headers["Origin"])
        assertNull(dramaProxyBackup("https://cdn.dramadizilerim.xyz.evil.test/https/x.test/a"))
    }
    @Test fun encryptedQualityProfilesAndExtensionlessHls() {
        val p720 = encrypted("https://cdn.test/720.mp4")
        val p1080 = encrypted("https://cdn.test/1080.mp4")
        val variants = parseDramatipVariants("""{"sourceFirst":{"chain":[{"type":"mp4","encByQuality":{"720":"$p720","1080":"$p1080"}}]}}""")
        assertEquals("1080p", chooseDramaVariant(variants, "Best").quality)
        assertEquals("720p", chooseDramaVariant(variants, "900p").quality)
        assertEquals("720p", chooseDramaVariant(variants, "100p").quality)
        val goodshort = encrypted("https://goodshort.example/hls/123?bookId=456")
        assertEquals("application/x-mpegURL", parseDramatipVariants("""{"sourceFirst":{"chain":[{"type":"goodshort","enc":"$goodshort"}]}}""").single().mimeType)
    }
    @Test fun kissSeriesAndSchemaMatchRequestedEpisode() {
        val base = "https://kiss18.vip/title/story".toHttpUrl()
        val html = """<title>A Story — Watch Episodes | Kiss18</title><meta property="og:image" content="/poster.jpg"><a href="/watch/story/3">3</a><a href="/watch/story/1">1</a><a href="/watch/other/2">2</a><a href="https://evil.test/watch/story/4">4</a>"""
        val series = parseDramaSeries(html, base, SiteDownloadProvider.KISS18, "story")
        assertEquals("A Story", series.title)
        assertEquals(listOf(1, 3), series.episodes.map { it.number })
        assertEquals("https://kiss18.vip/poster.jpg", series.poster)
        val schema = """<script type="application/ld+json">{"@graph":[{"@type":"VideoObject","url":"/watch/story/2","contentUrl":"https://cdn.test/2.m3u8"}]}</script>"""
        assertNull(parseKissPublicSource(schema, "https://kiss18.vip/watch/story/1".toHttpUrl()))
        assertEquals("https://cdn.test/2.m3u8", parseKissPublicSource(schema, "https://kiss18.vip/watch/story/2".toHttpUrl()))
    }
    @Test fun kissCatalogueAvailabilityAndPlaybackIdentity() {
        val catalogue = """export const snapshot = {"items":[{"public_slug":"story","public_slug_aliases":["alias"],"episodes":[{"id":"e1","ordinal":1,"playable":false},{"id":"e2","ordinal":2,"playable":true}]}]};"""
        val episode = kissCatalogueEpisode(catalogue, "alias", 2)
        assertTrue(runCatching { kissCatalogueEpisode(catalogue, "story", 1) }.isFailure)
        val entry = json("""{"episodeId":"e2","candidateId":"a&b","format":"hls","gateway":true}""")
        val base = "https://kiss18.vip/watch/story/2".toHttpUrl()
        assertEquals("a&b", kissPlaybackVariant(entry, episode, "a&b", base).url.toHttpUrl().queryParameter("candidate"))
        assertTrue(runCatching { kissPlaybackVariant(entry, episode, "wrong", base) }.isFailure)
        entry.addProperty("episodeId", "e1")
        assertTrue(runCatching { kissPlaybackVariant(entry, episode, "a&b", base) }.isFailure)
    }
    @Test fun nartoUsesExactOrdinalAndRetainsFileAlternative() {
        val html = """const episodeItemsRaw = [{"route_episode_number":1,"play_url":"https://cdn.test/1.m3u8","browser_prefetch_mode":"hls","direct_play_url":"https://cdn.test/1.mp4"},{"route_episode_number":2,"direct_play_url":"/videos/2.mp4"}];"""
        val base = "https://narto-drama.com/detail/watch/story/2".toHttpUrl()
        assertEquals("https://narto-drama.com/videos/2.mp4", parseNartoVariants(html, base, 2).single().url)
        assertEquals(2, parseNartoVariants(html, base, 1).size)
        assertEquals("application/x-mpegURL", parseNartoVariants(html, base, 1).first().mimeType)
        assertEquals("video/mp4", chooseNartoVariant(parseNartoVariants(html, base, 1), "Best").mimeType)
        assertTrue(runCatching { parseNartoVariants(html, base, 3) }.isFailure)
    }
    @Test fun nartoAcceptsOpaqueSignedStreamWithAdvertisedHlsMode() {
        val body = "const episodeItemsRaw = " + javaClass.getResource("/narto/episode-actual-shape.json")!!.readText().trim() + ";"
        val base = "https://narto-drama.com/detail/watch/story/1".toHttpUrl()
        val media = parseNartoVariants(body, base, 1).single()
        assertEquals("https://stream.narto-drama.com/e/m/SANITIZED_SIGNED_STREAM?mh=1", media.url)
        assertEquals("application/x-mpegURL", media.mimeType)
    }
    @Test fun nartoRecognizesExtensionlessViglooPlayUrlAsHlsWhenPrefetchIsDisabled() {
        val body = """const episodeItemsRaw = [{"route_episode_number":1,"direct_play_url":"https://stream-e1.narto-drama.com/e/m/DIRECT","play_url":"https://vigloo-stream.narto-drama.com/e/m/PLAY","schema_content_url":"https://vigloo-stream.narto-drama.com/e/m/SCHEMA","browser_prefetch_mode":"none"}];"""
        val variants = parseNartoVariants(body, "https://edge.narto-drama.com/detail/watch/story/1".toHttpUrl(), 1)
        val media = variants.first()
        assertEquals("https://vigloo-stream.narto-drama.com/e/m/PLAY", media.url)
        assertEquals("application/x-mpegURL", media.mimeType)
        assertEquals(3, variants.size)
        assertEquals("application/x-mpegURL", variants[1].mimeType)
        assertEquals("video/mp4", variants[2].mimeType)
    }
    @Test fun nartoFixtureExposesBothPlayableUrlsAndMarksOpaqueStreamAsHls() {
        val episodes = javaClass.getResource("/narto/episode-actual-shape.json")!!.readText().trim()
        val base = "https://narto-drama.com/detail/watch/story/1".toHttpUrl()
        val parsed = parseNartoVariants("const episodeItemsRaw = $episodes;", base, 1)
        assertEquals(1, parsed.size)
        assertTrue(parsed.single().url.startsWith("https://stream.narto-drama.com/"))
        assertEquals("application/x-mpegURL", parsed.single().mimeType)
    }
    @Test fun nartoKeepsHlsTypeWhenDirectAndBrowserFieldsShareUrl() {
        val body = """const episodeItemsRaw = [{"route_episode_number":1,"direct_play_url":"https://stream.narto-drama.com/opaque","direct_play_is_hls":false,"play_url":"https://stream.narto-drama.com/opaque","browser_prefetch_mode":"hls"}];"""
        val parsed = parseNartoVariants(body, "https://narto-drama.com/detail/watch/story/1".toHttpUrl(), 1)
        assertEquals(1, parsed.size)
        assertEquals("application/x-mpegURL", parsed.single().mimeType)
    }
    @Test fun nartoDetectsFlashSaleVideoWhenPrefetchModeIsNone() {
        val body = "const episodeItemsRaw = " + javaClass.getResource("/narto/flash-sale-episode-actual-shape.json")!!.readText().trim() + ";"
        val base = "https://edge.narto-drama.com/detail/watch/1-flash-sale/1?lang=en-US".toHttpUrl()
        val media = parseNartoVariants(body, base, 1).single()
        assertEquals("https://melolo2.narto-drama.com/SANITIZED_OPAQUE_VIDEO_TOKEN", media.url)
        assertEquals("video/mp4", media.mimeType)
    }
    @Test fun linkRefreshIsBoundedPerEpisodeAndDoesNotRetryDirectUrls() {
        val guard = MediaLinkRefreshGuard()
        assertTrue(guard.shouldRefresh(SiteDownloadProvider.NARTO, "episode-1"))
        assertFalse(guard.shouldRefresh(SiteDownloadProvider.NARTO, "episode-1"))
        assertTrue(guard.shouldRefresh(SiteDownloadProvider.NARTO, "episode-2"))
        assertFalse(guard.shouldRefresh(SiteDownloadProvider.DIRECT, "file-1"))
    }

}
