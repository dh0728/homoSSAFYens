package com.example.dive.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dive.location.LocationRegistrar

/**
 * 부팅/시간변경 시 위치 업데이트 재등록
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LocationRegistrar.register(context)
    }
}