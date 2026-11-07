package com.respotfck.spotifck.security

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * App PIN lock and screen-capture policy.
 *
 * Prefs live in their own file; allowBackup is false, so a reinstall starts with no PIN and
 * PROMPT_PENDING, which re-triggers the first-run PIN prompt.
 */
object AppLock {
    private const val PREFS = "respotifuck_security"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_ITER = "pin_iterations"
    private const val KEY_PROMPT = "pin_prompt"
    private const val KEY_TIMEOUT = "lock_timeout_ms"
    private const val KEY_FAILS = "pin_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"
    private const val KEY_ALLOW_CAPTURE = "allow_screen_capture"

    const val PROMPT_PENDING = "pending"
    const val PROMPT_SKIPPED = "skipped"
    const val PROMPT_DECLINED = "declined"
    const val PROMPT_DONE = "done"

    const val MIN_PIN = 4
    const val MAX_PIN = 8
    private const val ITERATIONS = 60_000
    private const val FREE_ATTEMPTS = 5
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 15 * 60_000L

    val timeoutOptions = listOf(
        0L to "Immediately",
        30_000L to "After 30 seconds",
        60_000L to "After 1 minute",
        5 * 60_000L to "After 5 minutes"
    )

    private lateinit var prefs: SharedPreferences

    /** True while the lock screen must cover protected content. */
    val locked = mutableStateOf(false)

    private var startedActivities = 0
    private var backgroundedAt = 0L
    // The first-run prompt can be skipped once per process.
    var promptHandledThisLaunch = false

    fun install(app: Application) {
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Cold start: always locked when a PIN exists.
        locked.value = isPinSet()
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivities++ == 0 && backgroundedAt != 0L && isPinSet() &&
                    SystemClock.elapsedRealtime() - backgroundedAt >= lockTimeoutMs()
                ) {
                    locked.value = true
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity.isChangingConfigurations) {
                    startedActivities--
                    return
                }
                if (--startedActivities <= 0) {
                    startedActivities = 0
                    backgroundedAt = SystemClock.elapsedRealtime()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        // Screen off always locks, whatever the timeout.
        ContextCompat.registerReceiver(app, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (isPinSet()) locked.value = true
            }
        }, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    // ---- PIN ----

    fun isPinSet(): Boolean = prefs.contains(KEY_HASH)

    fun promptState(): String = prefs.getString(KEY_PROMPT, PROMPT_PENDING) ?: PROMPT_PENDING

    fun setPromptState(state: String) = prefs.edit().putString(KEY_PROMPT, state).apply()

    fun shouldPromptForPin(): Boolean =
        !isPinSet() && !promptHandledThisLaunch &&
            promptState().let { it == PROMPT_PENDING || it == PROMPT_SKIPPED }

    fun isValidPin(pin: String) = pin.length in MIN_PIN..MAX_PIN && pin.all { it in '0'..'9' }

    fun setPin(pin: String) {
        require(isValidPin(pin))
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, b64(salt))
            .putString(KEY_HASH, b64(hash(pin, salt, ITERATIONS)))
            .putInt(KEY_ITER, ITERATIONS)
            .putString(KEY_PROMPT, PROMPT_DONE)
            .putInt(KEY_FAILS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .commit()
        locked.value = false
    }

    fun removePin() {
        prefs.edit().remove(KEY_HASH).remove(KEY_SALT).remove(KEY_ITER)
            .putInt(KEY_FAILS, 0).remove(KEY_LOCKOUT_UNTIL).commit()
        locked.value = false
    }

    sealed interface Result {
        data object Ok : Result
        data class Wrong(val attemptsLeft: Int) : Result
        data class LockedOut(val remainingMs: Long) : Result
    }

    /** Slow (PBKDF2); call off the main thread. */
    fun verify(pin: String): Result {
        lockoutRemainingMs().let { if (it > 0) return Result.LockedOut(it) }
        val salt = prefs.getString(KEY_SALT, null)?.let(::unb64) ?: return Result.Ok
        val expected = prefs.getString(KEY_HASH, null)?.let(::unb64) ?: return Result.Ok
        val iterations = prefs.getInt(KEY_ITER, ITERATIONS)
        if (MessageDigest.isEqual(expected, hash(pin, salt, iterations))) {
            prefs.edit().putInt(KEY_FAILS, 0).remove(KEY_LOCKOUT_UNTIL).commit()
            return Result.Ok
        }
        val fails = prefs.getInt(KEY_FAILS, 0) + 1
        val edit = prefs.edit().putInt(KEY_FAILS, fails)
        if (fails >= FREE_ATTEMPTS) {
            val shift = (fails - FREE_ATTEMPTS).coerceAtMost(5)
            val wait = (BASE_LOCKOUT_MS shl shift).coerceAtMost(MAX_LOCKOUT_MS)
            edit.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + wait).commit()
            return Result.LockedOut(wait)
        }
        edit.commit()
        return Result.Wrong(FREE_ATTEMPTS - fails)
    }

    fun lockoutRemainingMs(): Long {
        val until = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (until == 0L) return 0L
        val remaining = until - System.currentTimeMillis()
        // Clock moved backwards past the lockout window: clamp rather than lock forever.
        return remaining.coerceIn(0L, MAX_LOCKOUT_MS)
    }

    fun unlock() {
        locked.value = false
    }

    fun lockTimeoutMs(): Long = prefs.getLong(KEY_TIMEOUT, 0L)

    fun setLockTimeoutMs(ms: Long) = prefs.edit().putLong(KEY_TIMEOUT, ms).apply()

    // ---- Screen capture ----

    fun isScreenCaptureAllowed(): Boolean = prefs.getBoolean(KEY_ALLOW_CAPTURE, false)

    fun setScreenCaptureAllowed(allowed: Boolean) =
        prefs.edit().putBoolean(KEY_ALLOW_CAPTURE, allowed).apply()

    fun applyScreenCapturePolicy(window: Window) {
        if (isScreenCaptureAllowed()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun hash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
}
