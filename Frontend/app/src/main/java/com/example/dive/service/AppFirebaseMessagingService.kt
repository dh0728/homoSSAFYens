package com.example.dive.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.dive.MainActivity
import com.example.dive.R
import com.example.dive.data.api.RetrofitProvider
import com.example.dive.data.model.RegisterReq
import com.example.dive.notify.Notif
import com.example.dive.wear.WearBridge
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * - onNewToken: FCM 토큰 갱신 시 서버 /register 호출
 * - onMessageReceived: 서버의 notification/data 메시지 수신 → 표준 Notification 표시(워치 자동 브릿지)
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registerTokenToServer(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {

        android.util.Log.i("FCM", "onMessageReceived data=${message.data} notif=${message.notification != null}")
        // 채널 보장 (서비스 컨텍스트에서도 확실히)
        Notif.ensureChannels(this);

        // Android 13+ 권한 체크 (서비스에선 요청 불가 → 없으면 리턴)
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        // payload 우선순위: notification → data
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "만조 알림"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: "만조 임박"

        val evtId = message.data["eventId"] ?: message.data["evtId"] ?: ""
        val notificationId = if (evtId.isNotBlank()) evtId.hashCode() else (System.currentTimeMillis() % 100000).toInt()

        // 📱 폰에 로컬 알림 표시
//       showLocalNotification(
//            ctx = this,
//            title = title,
//            body = body,
//            notificationId = notificationId,
//            evtId = evtId
//        )

        // ⌚ 워치에도 즉시 전달
        WearBridge.sendTideAlert(
            ctx = this,
            title = title,
            body  = body,
            id    = notificationId
        )
    }
    companion object {
        fun initAndRegisterOnce(ctx: Context) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { t ->
                val token = t.result ?: return@addOnCompleteListener
                registerTokenToServer(ctx, token)
            }
        }

        private fun registerTokenToServer(ctx: Context, token: String) {
            val deviceId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitProvider.api.register(RegisterReq(deviceId, token)).execute()
                } catch (_: Exception) { /* 네트워크 오류 무시 - 재시도는 앱 로직에서 */ }
            }
        }

//        /** 알림 채널 보장: IMPORTANCE_HIGH + 진동 */
//        fun ensureChannel(ctx: Context) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val nm = ctx.getSystemService(NotificationManager::class.java)
//                val existing = nm.getNotificationChannel(CH_ID)
//                if (existing == null) {
//                    val ch = NotificationChannel(
//                        CH_ID,
//                        "물때 알림",
//                        NotificationManager.IMPORTANCE_HIGH
//                    ).apply {
//                        description = "만조/간조 등 물때 알림"
//                        enableVibration(true)
//                        setShowBadge(true)
//                        // 필요하면 사운드 커스터마이즈 가능
//                    }
//                    nm.createNotificationChannel(ch)
//                }
//            }
//        }

        /**
         * 표준 Notification 표시 → Wear OS로 자동 브릿지
         * @param notificationId 동일 이벤트는 같은 ID로 갱신(중복 방지)
         */
        fun showLocalNotification(
            ctx: Context,
            title: String,
            body: String,
            notificationId: Int,
            evtId: String? = null
        ) {
            Notif.ensureChannels(ctx) // 채널 보장

            if (Build.VERSION.SDK_INT >= 33) {
                val granted = ContextCompat.checkSelfPermission(
                    ctx, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            // (선택) 알림 탭 시 앱 열기
            val contentIntent = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("fromPush", true)
                    .putExtra("evtId", evtId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(ctx, Notif.CH_TIDE)
                // ✅ 작은 아이콘: 단색 벡터/PNG 사용 (ic_launcher_background 금지)
                .setSmallIcon(R.drawable.ic_stat_tide)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)        // < Android 8
                .setLocalOnly(false)                                   // ✅ 워치 브릿지 허용
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)   // ✅ 잠금화면/워치 가시성
                .setOnlyAlertOnce(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)           // ✅ 사운드/진동/라이트(채널 정책과 합산)
                .build()

            try {
                NotificationManagerCompat.from(ctx).notify(notificationId, notif)
            } catch (_: SecurityException) {
                // 드물게 권한 타이밍 이슈 방어
            }
        }
    }
}