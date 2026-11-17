package com.example.animepahedl

import org.jsoup.Jsoup
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Mirrors src/http.rs: a normal Cloudflare jsd script is not a CAPTCHA. */
internal object SessionChecks {
    fun isAdGate(body: String): Boolean = Regex("ad before watching|ads required to unlock the stream", RegexOption.IGNORE_CASE)
        .containsMatchIn(Jsoup.parse(body).body().text())
    private val titles = listOf("just a moment", "attention required", "verify you are human", "security verification")
    private const val TOKENS = "[name=cf-turnstile-response], [name=g-recaptcha-response], [name=h-captcha-response]"
    private const val WIDGETS = ".cf-turnstile, .g-recaptcha, .h-captcha, #aws-waf-captcha-container, #challenge-form, iframe[src*=challenges.cloudflare.com], iframe[src*=hcaptcha.com], iframe[src*=google.com/recaptcha/]"

    fun isChallenge(body: String, mitigated: String? = null, includeWidgets: Boolean = true): Boolean {
        if (mitigated.equals("challenge", true)) return true
        val document = Jsoup.parse(body)
        if (titles.any { document.title().contains(it, true) }) return true
        if (!includeWidgets) return false
        val solved = document.select(TOKENS).any { it.`val`().isNotBlank() || it.text().isNotBlank() }
        return !solved && document.select(WIDGETS).any { element ->
            (listOf(element) + element.parents()).none { ancestor ->
                val style = ancestor.attr("style").replace(Regex("\\s"), "").lowercase()
                ancestor.hasAttr("hidden") || "display:none" in style || "visibility:hidden" in style
            }
        }
    }

    // The same readiness rules as the Rust page_ready.js; no challenge-solving code.
    val browserStateScript = """
        (() => {
          const state = (ready, reason) => JSON.stringify({ready, reason, url:location.href});
          if (location.protocol !== 'https:') return state(false, 'Waiting for HTTPS');
          if (/ad before watching|ads required to unlock the stream/i.test(document.body?.innerText || ''))
            return state(false, 'This is a provider ad gate, not session verification. Close this page or clear Narto cookies and retry.');
          if (/just a moment|attention required|verify you are human|security verification/i.test(document.title))
            return state(false, 'Waiting for verification and destination navigation');
          const visible = e => e.getClientRects().length > 0 && getComputedStyle(e).visibility !== 'hidden';
          if ([...document.querySelectorAll('#challenge-running,#challenge-stage,#cf-challenge-running,#challenge-form')].some(visible))
            return state(false, 'Waiting for the challenge to finish');
          const widgets = document.querySelectorAll('.cf-turnstile,.g-recaptcha,.h-captcha,#aws-waf-captcha-container,iframe[src*="challenges.cloudflare.com"],iframe[src*="hcaptcha.com"],iframe[src*="google.com/recaptcha/"]');
          const solved = [...document.querySelectorAll('[name="cf-turnstile-response"],[name="g-recaptcha-response"],[name="h-captcha-response"]')].some(e => e.value.trim());
          if ([...widgets].some(visible) && !solved) return state(false, 'Please complete the CAPTCHA manually');
          if (document.readyState !== 'complete') return state(false, 'Loading destination');
          return state(true, 'Destination loaded without a detected challenge');
        })()
    """.trimIndent()
}

/** Default HTTPS ports normalize through HttpUrl; a different port is a different origin. */
internal fun verificationOriginMatches(expected: String, actual: String): Boolean {
    val first = expected.toHttpUrlOrNull() ?: return false
    val second = actual.toHttpUrlOrNull() ?: return false
    return first.scheme == "https" && second.scheme == first.scheme && first.host == second.host && first.port == second.port
}
