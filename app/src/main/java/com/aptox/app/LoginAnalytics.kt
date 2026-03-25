package com.aptox.app

import androidx.core.os.bundleOf
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.firebase.analytics.FirebaseAnalytics

/** Google Credential Manager 기반 로그인 취소/실패 Firebase Analytics 이벤트 */
object LoginAnalytics {
    fun isGoogleLoginCancelled(e: Throwable): Boolean =
        e is GetCredentialCancellationException ||
            e.cause is GetCredentialCancellationException

    fun logLoginCancelled(analytics: FirebaseAnalytics, method: String, screen: String) {
        analytics.logEvent(
            "login_cancelled",
            bundleOf(
                "method" to method,
                "screen" to screen,
            ),
        )
    }

    fun logLoginFailed(analytics: FirebaseAnalytics, method: String, error: String) {
        val safe = error.take(100)
        analytics.logEvent(
            "login_failed",
            bundleOf(
                "method" to method,
                "error" to safe,
            ),
        )
    }
}
