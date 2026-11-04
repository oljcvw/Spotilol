package com.respotfck.spotifck.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    companion object {
        private const val API_URL = "https://api.github.com/repos/ReSpotF-ck/-ReSpotif-ck-App/releases/latest"
        private const val PREFS_NAME = "spotilol_prefs"
        private const val KEY_HAS_UPDATE = "hasUpdateAvailable"
        private const val KEY_LAST_CHECK = "LastUpdateCheck"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }

    fun hasUpdateAvailable(): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_UPDATE, false)
    }

    fun clearUpdateAvailable() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAS_UPDATE, false).apply()
    }

    fun autoCheck(onResult: ((Boolean) -> Unit)? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
            onResult?.invoke(hasUpdateAvailable())
            return
        }

        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        fetchUpdateAvailable { result -> result?.let { onResult?.invoke(it) } }
    }

    fun checkForUpdates(onResult: (Boolean?) -> Unit) {
        fetchUpdateAvailable(onResult)
    }

    private fun fetchUpdateAvailable(onResult: ((Boolean?) -> Unit)? = null) {
        Thread {
            val available = try {
                val url = URL(API_URL)
                val conn = url.openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Accept", "application/vnd.github+json")
                    conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    conn.setRequestProperty("User-Agent", "Spotifck-Android")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                        throw IllegalStateException("GitHub returned HTTP ${conn.responseCode}")
                    }
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val tagName = JSONObject(body).getString("tag_name").removePrefix("v")
                    val currentVersion = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionName ?: ""
                    isNewer(tagName, currentVersion)
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                null
            }

            if (available != null) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_HAS_UPDATE, available).apply()
            }
            onResult?.let { callback ->
                Handler(Looper.getMainLooper()).post { callback(available) }
            }
        }.start()
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".")
        val currentParts = current.split(".")
        val size = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until size) {
            val l = latestParts.getOrNull(i)?.toIntOrNull() ?: 0
            val c = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
