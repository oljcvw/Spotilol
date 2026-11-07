package com.respotfck.spotifck

import android.app.Application
import com.respotfck.spotifck.security.AppLock

class ReSpotifuckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLock.install(this)
    }
}
