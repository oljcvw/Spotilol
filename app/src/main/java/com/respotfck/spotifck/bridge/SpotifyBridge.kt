package com.respotfck.spotifck.bridge

import android.app.Activity
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.respotfck.spotifck.service.MediaNotificationService
import com.respotfck.spotifck.webview.LrcLib
import com.respotfck.spotifck.webview.TweakPrefs
import java.lang.ref.WeakReference

class SpotifyBridge(activityRef: WeakReference<Activity>) {

    private val activityRef = activityRef
    var onLoginDetected: (() -> Unit)? = null
    var onPlayLoaded: (() -> Unit)? = null
    var onMediaStatus: ((String) -> Unit)? = null
    var onMediaPosition: ((Long) -> Unit)? = null
    var onTimerDialogRequest: (() -> Unit)? = null
    var onSleepTimerDone: (() -> Unit)? = null
    var evalJs: ((String) -> Unit)? = null

    @JavascriptInterface
    fun loginDetected() {
        val activity = activityRef.get() ?: return
        activity.getSharedPreferences("spotilol_prefs", Activity.MODE_PRIVATE)
            .edit()
            .putBoolean("LoggedIn", true)
            .apply()
        activity.runOnUiThread {
            onLoginDetected?.invoke()
        }
    }

    @JavascriptInterface
    fun deferMessage(msg: String?) {
        val activity = activityRef.get() ?: return
        if (msg == "adblock") return
        val display = when (msg) {
            "unlock" -> "Player unlocked"
            "reload" -> "Reloading..."
            else -> msg
        }
        activity.runOnUiThread {
            Toast.makeText(activity, display, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun isWoke(): Boolean {
        val activity = activityRef.get() ?: return false
        return activity.window?.decorView?.visibility == View.VISIBLE
    }

    @JavascriptInterface
    fun wakeUp() {
    }

    @JavascriptInterface
    fun wakeOff() {
    }

    @JavascriptInterface
    fun cssInjected() {
    }

    @JavascriptInterface
    fun playLoaded() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            onPlayLoaded?.invoke()
        }
    }

    @JavascriptInterface
    fun recMediaPosition(position: Long) {
        onMediaPosition?.invoke(position)
        MediaNotificationService.instance?.updatePlaybackPosition(position)
    }

    @JavascriptInterface
    fun recMediaStatus(json: String?) {
        json?.let {
            onMediaStatus?.invoke(it)
            MediaNotificationService.instance?.updateFromMediaStatus(it)
        }
    }

    @JavascriptInterface
    fun manageTShut(enabled: Boolean) {
    }

    @JavascriptInterface
    fun manageTSleep(enabled: Boolean) {
    }

    @JavascriptInterface
    fun openTimerDialog() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            onTimerDialogRequest?.invoke()
        }
    }

    @JavascriptInterface
    fun savePlayerModes(shuffle: Boolean, repeat: String?) {
        val activity = activityRef.get() ?: return
        activity.getSharedPreferences("spotilol_prefs", 0).edit()
            .putBoolean(TweakPrefs.SAVED_SHUFFLE, shuffle)
            .putString(TweakPrefs.SAVED_REPEAT, repeat ?: "off")
            .apply()
    }

    @JavascriptInterface
    fun sleepTimerDone() {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread { onSleepTimerDone?.invoke() }
    }

    @JavascriptInterface
    fun fetchLyrics(artist: String?, title: String?, duration: Int) {
        val t = title?.trim().orEmpty()
        if (t.isEmpty()) return
        Thread {
            val res = LrcLib.fetch(artist.orEmpty().trim(), t, duration)
            evalJs?.invoke("window.rsLrcResult&&rsLrcResult(${res ?: "null"})")
        }.start()
    }
}
