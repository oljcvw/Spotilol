package com.project.lol.bridge

import android.app.Activity
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.project.lol.service.MediaNotificationService
import java.lang.ref.WeakReference

class SpotifyBridge(activityRef: WeakReference<Activity>) {

    private val activityRef = activityRef
    var onLoginDetected: (() -> Unit)? = null
    var onPlayLoaded: (() -> Unit)? = null
    var onMediaStatus: ((String) -> Unit)? = null
    var onMediaPosition: ((Long) -> Unit)? = null
    var onTimerDialogRequest: (() -> Unit)? = null

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
}
