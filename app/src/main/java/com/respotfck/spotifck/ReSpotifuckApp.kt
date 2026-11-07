package com.respotfck.spotifck

import android.app.Application
import com.respotfck.spotifck.proxy.DohResolver
import com.respotfck.spotifck.proxy.LocalProxyManager
import com.respotfck.spotifck.proxy.ProxySettings
import com.respotfck.spotifck.security.AppLock

class ReSpotifuckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLock.install(this)
        AppIdentity.load(this)
        val prefs = getSharedPreferences("spotilol_prefs", MODE_PRIVATE)
        DohResolver.configure(prefs)
        LocalProxyManager.reuseConnections = ProxySettings.reuseConnections(prefs)
    }
}
