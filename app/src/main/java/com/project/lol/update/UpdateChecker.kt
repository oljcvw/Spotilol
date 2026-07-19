package com.project.lol.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    companion object {
        private const val API_URL = "https://api.github.com/repos/lyssadev/Spotilol/releases/latest"
        private const val PREFS_NAME = "spotilol_prefs"
        private const val KEY_LAST_CHECK = "LastUpdateCheck"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }

    fun autoCheck() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) return

        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
        fetchAndNotify(showNoUpdate = false)
    }

    fun manualCheck() {
        fetchAndNotify(showNoUpdate = true)
    }

    private fun fetchAndNotify(showNoUpdate: Boolean) {
        Thread {
            try {
                val url = URL(API_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val tagName = json.getString("tag_name").removePrefix("v")
                    val releaseUrl = json.getString("html_url")
                    val currentVersion = context.packageManager
                        .getPackageInfo(context.packageName, 0).versionName ?: ""

                    if (isNewer(tagName, currentVersion)) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "A new version ($tagName) is available! Redirecting to download...",
                                Toast.LENGTH_LONG
                            ).show()
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                        }
                    } else if (showNoUpdate) {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "No new update available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (_: Exception) {
                if (showNoUpdate) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "No new update available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
