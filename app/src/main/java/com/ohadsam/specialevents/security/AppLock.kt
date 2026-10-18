package com.ohadsam.specialevents.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent

/** When the app asks for the phone's lock (PIN, pattern, password or biometrics) before showing anything. Pure: times come from a monotonic clock. */
object AppLockPolicy {
    val TIMEOUTS_SECONDS = listOf(0, 30, 60, 300)

    /**
     * Lock on a cold start ([backgroundedAtMs] null) and after the app was out of sight for at least [timeoutSeconds]. A clock that
     * ran backwards counts as "long enough": unclear is safer than open.
     */
    fun shouldLock(enabled: Boolean, deviceSecure: Boolean, backgroundedAtMs: Long?, nowMs: Long, timeoutSeconds: Int): Boolean {
        if (!enabled || !deviceSecure) return false
        if (backgroundedAtMs == null || nowMs < backgroundedAtMs) return true
        return nowMs - backgroundedAtMs >= timeoutSeconds * MILLIS_PER_SECOND
    }

    private const val MILLIS_PER_SECOND = 1_000L
}

/** The system's own "confirm it is you" screen - the app never sees or stores a PIN or a fingerprint. */
object DeviceAuth {
    fun isSecure(context: Context): Boolean = (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

    fun intent(context: Context, title: String, description: String?): Intent? =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).createConfirmDeviceCredentialIntent(title, description)
}
