package com.example.dive.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dive.App
import com.example.dive.R

object WearNotif {
    const val CH_TIDE = "tide_alerts"

    // ---------- 사운드(통일용) ----------
    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    @Volatile private var soundLoaded: Boolean = false

    /** 앱 시작 시/서비스 시작 시 한번 호출해서 미리 로드 */
    fun init(ctx: Context) {
        if (soundPool != null && soundLoaded) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM) // Wear에서 들리도록 ALARM 계열 고정
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(attrs)
            .build().apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    soundLoaded = (status == 0 && sampleId != 0)
                    android.util.Log.w("WEAR-NOTI", "SoundPool loaded=$soundLoaded id=$sampleId status=$status")
                    if (!soundLoaded) {
                        release()
                        soundPool = null
                    }
                }
            }

        // 항상 같은 파일만 쓴다 → 첫/둘째 알림 소리 동일화
        soundId = soundPool!!.load(ctx, R.raw.tide_chime, 1)
    }

    // ---------- 알림 채널 ----------
    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val existing = nm.getNotificationChannel(CH_TIDE)
            if (existing == null) {
                val ch = NotificationChannel(
                    CH_TIDE,
                    "물때 알림",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "만조/간조 등 물때 알림"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                    enableLights(true)
                    // Wear에선 채널 사운드가 무시될 수 있으므로 채널 사운드는 사용하지 않음
                    setSound(null, null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setBypassDnd(true)
                }
                try { nm.createNotificationChannel(ch) } catch (_: SecurityException) {}
            }
        }
    }

    @ChecksSdkIntAtLeast(api = 33)
    private fun needsPostNotiPerm(): Boolean = Build.VERSION.SDK_INT >= 33

    private fun hasPostNotiPerm(ctx: Context): Boolean {
        return if (needsPostNotiPerm()) {
            ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun requestPostNotiPermissionIfNeeded(activity: android.app.Activity, requestCode: Int = 9910) {
        if (needsPostNotiPerm() &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCode
            )
        }
    }

    fun openChannelSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val i = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, CH_TIDE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { ctx.startActivity(i) }
            catch (_: ActivityNotFoundException) {
                val alt = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { ctx.startActivity(alt) } catch (_: Exception) {}
            } catch (_: SecurityException) {}
        }
    }

    // ---------- 알림 + 통일 사운드 ----------
    @SuppressLint("MissingPermission")
    fun show(
        ctx: Context,
        title: String,
        body: String,
        id: Int,
        playSound: Boolean = true
    ) {
        ensureChannel(ctx)
        init(ctx) // ← 보수적으로 여기서도 보장 (첫 알림 대비)

        if (!hasPostNotiPerm(ctx)) {
            android.util.Log.w("WEAR-NOTI", "POST_NOTIFICATIONS not granted; skip notify()")
            return
        }

        val notif = NotificationCompat.Builder(ctx, CH_TIDE)
            .setSmallIcon(R.drawable.ic_stat_tide)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true) // 동일 ID 갱신 시 소리 중복 방지
            .setAutoCancel(true)
            .build()

        try { NotificationManagerCompat.from(ctx).notify(id, notif) } catch (_: SecurityException) {}

        if (playSound) playUnifiedChime(ctx)
    }

    /** 항상 같은 효과음만 재생 → 첫/둘째 알림 사운드 통일 */
    private fun playUnifiedChime(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        // 메인 스레드에서 재생 (SoundPool은 메인/백 둘다 되지만 초기 지연 고려)
        Handler(Looper.getMainLooper()).post {
            // 1) SoundPool 준비됨 → 바로 재생
            if (soundPool != null && soundLoaded && soundId != 0) {
                val sid = soundPool!!.play(soundId, 1f, 1f, 1, 0, 1f)
                if (sid != 0) {
                    android.util.Log.w("WEAR-NOTI", "SoundPool played (sid=$sid)")
                    return@post
                }
            }
            // 2) 아직 로딩 중이면 150ms 뒤 재시도(최대 3회) → 첫 알림도 동일음 보장
            retryPlayWithDelay(times = 3, delayMs = 150)
        }
    }

    private fun retryPlayWithDelay(times: Int, delayMs: Long) {
        if (times <= 0) return
        Handler(Looper.getMainLooper()).postDelayed({
            if (soundPool != null && soundLoaded && soundId != 0) {
                val sid = soundPool!!.play(soundId, 1f, 1f, 1, 0, 1f)
                if (sid != 0) {
                    android.util.Log.w("WEAR-NOTI", "SoundPool played on retry (sid=$sid)")
                    return@postDelayed
                }
            }
            // 최후의 동일 파일 폴백: MediaPlayer로 raw 재생 (여기도 같은 파일만 사용)
            try {
                val mp = MediaPlayer.create(
                    // create(ctx,resId)가 내부에서 적절히 준비까지 해 줌
                    App.AppHolder.appContext, // 아래 AppHolder 참조
                    R.raw.tide_chime
                )
                mp?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                    }
                    setOnCompletionListener { release() }
                    setOnErrorListener { m, _, _ -> m.release(); true }
                    start()
                    android.util.Log.w("WEAR-NOTI", "MediaPlayer fallback played")
                    return@postDelayed
                }
            } catch (_: Exception) {}
            retryPlayWithDelay(times - 1, delayMs)
        }, delayMs)
    }
}
