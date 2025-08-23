package com.example.dive.notify

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class WearNotifPermissionActivity : ComponentActivity() {

    private val launcher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted -> 바로 종료 */
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                launcher.launch(perm)
                return
            }
        }
        // 이미 허용됨 → 종료
        finish()
    }
}