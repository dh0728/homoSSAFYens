package com.example.dive

import android.app.Application
import com.example.dive.notify.Notif

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notif.ensureChannels(this)   // ✅ 앱 시작 시 채널 생성 (1회)
    }
}