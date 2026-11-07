package com.respotfck.spotifck.update

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateChecker(private val context: Context) {

    private data class ReleaseInfo(
        val version: String,
        val releaseUrl: String,
        val apkUrl: String?
    )

    companion object {
        private const val API_URL = "https://api.github.com/repos/ReSpotF-ck/-ReSpotif-ck-App/releases/latest"
        private const val ASSETS_API_URL = "https://api.github.com/repos/ReSpotF-ck/-ReSpotif-ck-App/contents/release-assets?ref=main"
        private const val RELEASES_PAGE_URL = "https://github.com/ReSpotF-ck/-ReSpotif-ck-App/releases/latest"
        private const val ASSETS_PAGE_URL = "https://github.com/ReSpotF-ck/-ReSpotif-ck-App/tree/main/release-assets"
        private const val PREFS_NAME = "spotilol_prefs"
        private const val KEY_HAS_UPDATE = "hasUpdateAvailable"
        private const val KEY_LAST_CHECK = "LastUpdateCheck"
        private const val KEY_LATEST_VERSION = "latestReleaseVersion"
        private const val KEY_RELEASE_URL = "latestReleaseUrl"
        private const val KEY_APK_URL = "latestReleaseApkUrl"
        private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private val RELEASE_APK_PATTERN = Regex("^(?:ReSpotifuck|Spotifck)-([0-9]+(?:\\.[0-9]+)*)-release\\.apk$", RegexOption.IGNORE_CASE)
    }

    fun hasUpdateAvailable(): Boolean {
        return preferences().getBoolean(KEY_HAS_UPDATE, false)
    }

    fun latestVersion(): String? = preferences().getString(KEY_LATEST_VERSION, null)

    fun downloadUrl(): String? = preferences().getString(KEY_APK_URL, null)

    fun releaseUrl(): String? = preferences().getString(KEY_RELEASE_URL, null)

    fun clearUpdateAvailable() {
        preferences().edit().putBoolean(KEY_HAS_UPDATE, false).apply()
    }

    fun autoCheck(onResult: ((Boolean) -> Unit)? = null) {
        val prefs = preferences()
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
        if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
            onResult?.invoke(hasUpdateAvailable())
            return
        }
        fetchUpdateAvailable { result -> result?.let { onResult?.invoke(it) } }
    }

    fun checkForUpdates(onResult: (Boolean?) -> Unit) {
        fetchUpdateAvailable(onResult)
    }

    private fun preferences() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun fetchUpdateAvailable(onResult: ((Boolean?) -> Unit)? = null) {
        Thread {
            val releaseInfo = runCatching { fetchLatestRelease() }.getOrNull()
            val available = releaseInfo?.let { info ->
                val currentVersion = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
                }.getOrDefault("")
                isNewer(info.version, currentVersion)
            }

            if (releaseInfo != null && available != null) {
                preferences().edit()
                    .putBoolean(KEY_HAS_UPDATE, available)
                    .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
                    .putString(KEY_LATEST_VERSION, releaseInfo.version)
                    .putString(KEY_RELEASE_URL, releaseInfo.releaseUrl)
                    .putString(KEY_APK_URL, releaseInfo.apkUrl)
                    .apply()
            }
            onResult?.let { callback ->
                Handler(Looper.getMainLooper()).post { callback(available) }
            }
        }.start()
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        val releaseJson = requestBody(API_URL)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val releaseVersion = releaseJson?.optString("tag_name")
            ?.removePrefix("v")
            ?.takeIf { it.matches(Regex("[0-9]+(?:\\.[0-9]+)*")) }

        if (releaseJson != null && releaseVersion != null) {
            val releaseUrl = releaseJson.optString("html_url").takeIf(String::isNotBlank) ?: RELEASES_PAGE_URL
            val releaseApkUrl = findReleaseApk(releaseJson.optJSONArray("assets"), releaseVersion)
            val apkUrl = releaseApkUrl ?: findDirectoryApk(releaseVersion)
            return ReleaseInfo(releaseVersion, releaseUrl, apkUrl)
        }

        // GitHub Releases may not be published yet. Fall back to signed APKs
        // checked into the repository's release-assets directory.
        val directoryRelease = findLatestDirectoryRelease()
        return directoryRelease ?: if (releaseJson != null) {
            val tag = releaseJson.optString("tag_name").removePrefix("v")
            if (tag.isNotBlank()) ReleaseInfo(tag, RELEASES_PAGE_URL, null) else null
        } else null
    }

    private fun findReleaseApk(assets: JSONArray?, version: String): String? {
        if (assets == null) return null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            if (isReleaseApkName(asset.optString("name"), version)) {
                return asset.optString("browser_download_url").takeIf(String::isNotBlank)
            }
        }
        return null
    }

    private fun findDirectoryApk(version: String): String? {
        val entries = requestBody(ASSETS_API_URL)?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return null
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            if (isReleaseApkName(entry.optString("name"), version)) {
                return entry.optString("download_url").takeIf(String::isNotBlank)
            }
        }
        return null
    }

    // Releases up to 1.0.7 were published as Spotifck-<version>-release.apk.
    private fun isReleaseApkName(name: String, version: String): Boolean =
        RELEASE_APK_PATTERN.matchEntire(name)?.groupValues?.get(1) == version

    private fun findLatestDirectoryRelease(): ReleaseInfo? {
        val entries = requestBody(ASSETS_API_URL)?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return null
        val candidates = mutableListOf<ReleaseInfo>()
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val name = entry.optString("name")
            val match = RELEASE_APK_PATTERN.matchEntire(name) ?: continue
            val version = match.groupValues[1]
            val downloadUrl = entry.optString("download_url").takeIf(String::isNotBlank) ?: continue
            candidates += ReleaseInfo(version, ASSETS_PAGE_URL, downloadUrl)
        }
        return candidates.maxWithOrNull { a, b -> compareVersions(a.version, b.version) }
    }

    private fun requestBody(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "ReSpotifuck-Android")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewer(latest: String, current: String): Boolean = compareVersions(latest, current) > 0

    private fun compareVersions(first: String, second: String): Int {
        val firstParts = first.removePrefix("v").substringBefore('-').split('.')
        val secondParts = second.removePrefix("v").substringBefore('-').split('.')
        val size = maxOf(firstParts.size, secondParts.size)
        for (i in 0 until size) {
            val comparison = (firstParts.getOrNull(i)?.toIntOrNull() ?: 0)
                .compareTo(secondParts.getOrNull(i)?.toIntOrNull() ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }
}
