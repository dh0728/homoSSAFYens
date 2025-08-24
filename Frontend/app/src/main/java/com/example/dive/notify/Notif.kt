package com.example.dive.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notif {
    /** 앱 전역에서 이 채널 ID만 사용 */
    const val CH_TIDE = "tide_alerts"

    /** 채널 생성은 여기서만 */
    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CH_TIDE)
            if (existing == null) {
                val ch = NotificationChannel(
                    CH_TIDE,
                    "물때 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "만조/간조 등 물때 알림"
                    enableVibration(true)
                    setShowBadge(true)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }
}