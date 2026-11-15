package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class SiteDownloadCookieTest {
    @Test fun browserVerificationReplacesImportedSession() {
        assertEquals("cf_clearance=fresh; account=imported; session=current=token",
            mergeSiteDownloadCookies("cf_clearance=stale; account=imported", "cf_clearance=fresh; session=current=token"))
        assertEquals("account=imported", mergeSiteDownloadCookies("account=imported", ""))
        assertEquals("session=", mergeSiteDownloadCookies("session=stale", "session="))
    }

    @Test fun rejectsUnscopedRawCookieHeaders() {
        for (input in listOf("Cookie: session=secret", "session=secret")) {
            assertFalse(validSiteDownloadCookieInput(input))
            assertEquals("", siteDownloadCookieHeader(input, "https://example.com/"))
        }
    }
    @Test fun netscapeHonorsDomainPathSecureExpiryAndHttpOnly() {
        val input = "# Netscape HTTP Cookie File\n#HttpOnly_.example.com\tTRUE\t/private\tTRUE\t200\tauth\tsecret\nexample.com\tFALSE\t/\tFALSE\t0\troot\tvalue"
        assertEquals("auth=secret; root=value", siteDownloadCookieHeader(input, "https://example.com/private/video", 100))
        assertEquals("auth=secret", siteDownloadCookieHeader(input, "https://cdn.example.com/private/video", 100))
        assertEquals("root=value", siteDownloadCookieHeader(input, "http://example.com/private/video", 100))
        assertEquals("root=value", siteDownloadCookieHeader(input, "https://example.com/private-other", 100))
        assertEquals("root=value", siteDownloadCookieHeader(input, "https://example.com/private/video", 201))
        assertEquals("", siteDownloadCookieHeader(input, "https://example.com.evil.test/private/video", 100))
    }
    @Test fun windowsCookieFilesPreserveTrailingEmptyValues() {
        val input = "# Netscape HTTP Cookie File\r\nexample.com\tFALSE\t/\tTRUE\t0\tsession\tvalue\r\nexample.com\tFALSE\t/\tTRUE\t0\tempty\t\r\n"
        val normalized = normalizeSiteDownloadCookieFile(input)
        assertTrue(normalized.endsWith("empty\t"))
        assertFalse(normalized.contains('\r'))
        assertTrue(validSiteDownloadCookieInput(input))
        assertEquals("session=value; empty=", siteDownloadCookieHeader(input, "https://example.com/"))
        assertEquals("session=value; empty=", siteDownloadCookieHeader(normalized, "https://example.com/"))
    }
    @Test fun rejectsHeaderInjectionWithoutEchoingContents() {
        assertFalse(validSiteDownloadCookieInput("session=secret\r\nX-Injected: yes"))
        assertFalse(validSiteDownloadCookieInput("not a cookie"))
        assertFalse(validSiteDownloadCookieInput("# Netscape HTTP Cookie File\n"))
        assertFalse(validSiteDownloadCookieInput("example.com\tFALSE\t/\tTRUE\t0\tsession\tsecret\rX"))
        assertEquals("", siteDownloadCookieHeader("session=secret\r\nX: yes", "https://example.com/"))
    }
}
