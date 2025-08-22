package com.example.dive.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dive.R

object WearNotif {
    const val CH_TIDE = "tide_alerts"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val existing = nm.getNotificationChannel(CH_TIDE)

            if (existing == null) {
                // 최초 생성: 반드시 소리/진동 포함해 채널 생성
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val ch = NotificationChannel(
                    CH_TIDE,
                    "물때 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "만조/간조 등 물때 알림"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                    enableLights(true)
                    setSound(soundUri, attrs)
                }

                nm.createNotificationChannel(ch)

                // ✅ 생성 직후 상태 로그
                val created = nm.getNotificationChannel(CH_TIDE)
                android.util.Log.w(
                    "WEAR-NOTI",
                    "CREATED: exists=${created != null}, importance=${created?.importance}, sound=${created?.sound}, vib=${created?.vibrationPattern != null}"
                )
            } else {
                // ⚠️ 주의: 한 번 생성된 채널의 소리/중요도는 코드로 변경 불가 (사용자 설정 or 재설치 필요)
                android.util.Log.w(
                    "WEAR-NOTI",
                    "EXISTING: exists=true, importance=${existing.importance}, sound=${existing.sound}, vib=${existing.vibrationPattern != null}"
                )
            }
        } else {
            android.util.Log.w("WEAR-NOTI", "API<26: channels not supported")
        }
    }

    private fun hasPostNotiPerm(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * 서비스(리스너)에서 호출될 수 있으므로:
     * - 권한 미보유시 notify 호출하지 않음(서비스는 권한 요청 UI를 띄울 수 없음)
     * - 권한 요청은 별도 Activity에서 처리
     */
    fun show(ctx: Context, title: String, body: String, id: Int) {
        ensureChannel(ctx)
        if (!hasPostNotiPerm(ctx)) return

        val notif = NotificationCompat.Builder(ctx, CH_TIDE)
            .setSmallIcon(R.drawable.ic_stat_tide)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // 26 미만 대비 기본 사운드/진동/라이트
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(id, notif)
        } catch (se: SecurityException) {
            android.util.Log.e("WearNotif", "notify() SecurityException: ${se.message}", se)
        }
    }

    /** (옵션) 채널 설정 화면 바로 열기: 사용자가 소리를 꺼뒀는지 확인/수정할 때 활용 */
    fun openChannelSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent =
                Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CH_TIDE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            ctx.startActivity(intent)
        }
    }
}
