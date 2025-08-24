package com.example.dive.debug

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.dive.R
import com.example.dive.notify.WearNotif
import com.example.dive.notify.WearNotifPermissionActivity

class WearNotifTestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_notif_test)

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            // 권한 없으면 요청 화면 띄우기
            if (Build.VERSION.SDK_INT >= 33) {
                val ok = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!ok) {
                    startActivity(Intent(this, WearNotifPermissionActivity::class.java))
                    return@setOnClickListener
                }
            }
            // 워치 자체 알림 발사
            WearNotif.show(
                ctx = this,
                title = "워치 단독 테스트",
                body = "이 알림이 워치에서 보이면 OK!",
                id = (System.currentTimeMillis() % 100000).toInt()
            )
        }
    }
}