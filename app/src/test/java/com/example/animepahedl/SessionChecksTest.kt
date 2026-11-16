package com.example.animepahedl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionChecksTest {
    @Test fun identifiesAdGateWithoutCallingItACaptcha() {
        val html = "<h1>Ad before watching</h1><p>6 of 6 ads required to unlock the stream</p>"
        assertTrue(SessionChecks.isAdGate(html))
        assertFalse(SessionChecks.isChallenge(html))
        assertFalse(SessionChecks.isAdGate("<script>const label = 'Ad before watching';</script><h1>Episode 26</h1>"))
    }
    @Test fun detectsDownloadPageCaptchaWithoutAnErrorStatus() {
        assertTrue(SessionChecks.isChallenge("<title>Download</title><div class='cf-turnstile'></div>"))
        assertTrue(SessionChecks.isChallenge("<title>Security verification</title>"))
        assertTrue(SessionChecks.isChallenge("<title>Loading</title>", "Challenge"))
    }

    @Test fun doesNotTreatAnOrdinaryErrorOrBackgroundScriptAsCaptcha() {
        assertFalse(SessionChecks.isChallenge("<title>Forbidden</title>"))
        assertFalse(SessionChecks.isChallenge("<title>file.mp4 :: Kwik</title><script src='/cdn-cgi/challenge-platform/scripts/jsd/main.js'></script>"))
        assertFalse(SessionChecks.isChallenge("{\"error\":{\"code\":\"decode_error\",\"message\":\"payload.stdin missing\"}}"))
    }

    @Test fun ignoresHiddenAndSolvedWidgetsLikeRust() {
        assertFalse(SessionChecks.isChallenge("<div hidden><div class='h-captcha'></div></div>"))
        assertFalse(SessionChecks.isChallenge("<div style='display: none'><div class='cf-turnstile'></div></div>"))
        assertFalse(SessionChecks.isChallenge("<div class='cf-turnstile'></div><input name='cf-turnstile-response' value='solved'>"))
    }
}
