package com.respotfck.spotifck.webview

import android.content.Context
import android.content.SharedPreferences
import androidx.webkit.WebViewCompat

/** Engine choice plus the automatic fallback chain: System WebView -> Safe mode -> external browser. */
object WebEngine {
    const val KEY = "WebEngine"
    const val ACTIVE = "EngineActive"
    const val REASON = "EngineFallbackReason"
    const val AUTO = "EngineAutoFallback"

    const val SYSTEM = "system"
    const val SAFE = "safe"
    const val BROWSER = "browser"

    class Engine(val id: String, val label: String, val description: String)

    val engines = listOf(
        Engine(SYSTEM, "System WebView", "Full features: proxy, secure DNS, ad filtering, custom player"),
        Engine(SAFE, "Safe mode WebView", "No proxy and no GPU acceleration; ad filtering and player still work"),
        Engine(BROWSER, "External browser", "Opens Spotify in your browser; no app features")
    )

    fun label(id: String) = engines.firstOrNull { it.id == id }?.label ?: id

    fun preferred(p: SharedPreferences) = p.getString(KEY, SYSTEM) ?: SYSTEM

    fun active(p: SharedPreferences) = p.getString(ACTIVE, null) ?: preferred(p)

    fun isFallback(p: SharedPreferences) = active(p) != preferred(p)

    fun reason(p: SharedPreferences) = p.getString(REASON, null)

    fun autoFallback(p: SharedPreferences) = p.getBoolean(AUTO, true)

    fun next(id: String) = when (id) {
        SYSTEM -> SAFE
        SAFE -> BROWSER
        else -> null
    }

    fun fallBack(p: SharedPreferences, to: String, reason: String) =
        p.edit().putString(ACTIVE, to).putString(REASON, reason).apply()

    fun choose(p: SharedPreferences, id: String) =
        p.edit().putString(KEY, id).remove(ACTIVE).remove(REASON).apply()

    fun reset(p: SharedPreferences) = p.edit().remove(ACTIVE).remove(REASON).apply()

    fun provider(c: Context): String = runCatching {
        WebViewCompat.getCurrentWebViewPackage(c)?.let { "${it.packageName} ${it.versionName}" }
    }.getOrNull() ?: "No WebView provider found"

    fun providerAvailable(c: Context) = runCatching { WebViewCompat.getCurrentWebViewPackage(c) != null }.getOrDefault(false)
}
