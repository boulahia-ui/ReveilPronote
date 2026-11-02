package com.reveilpronote.app

import android.app.Application

class ReveilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        if (Bridge.isLoggedIn(this)) Sync.schedulePeriodic(this)
    }
}
