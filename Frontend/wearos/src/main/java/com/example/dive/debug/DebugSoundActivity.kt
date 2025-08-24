//package com.example.dive.debug
//
//import android.media.AudioManager
//import android.media.RingtoneManager
//import android.media.ToneGenerator
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.wear.compose.material.*
//import androidx.compose.foundation.layout.*
//import com.example.dive.notify.WearNotif
//import kotlin.random.Random
//
//class DebugSoundActivity : ComponentActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        WearNotif.ensureChannel(this)
//
//        setContent {
//            var lastLog by remember { mutableStateOf("ready") }
//
//            fun log(s: String) {
//                lastLog = s
//                android.util.Log.w("WEAR-SOUND", s)
//            }
//
//            Scaffold(
//                timeText = { TimeText() },
//                vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
//                positionIndicator = { /* 자동 스크롤 인디케이터 */ }
//            ) {
//                ScalingLazyColumn(
//                    modifier = Modifier.fillMaxSize().padding(8.dp),
//                    verticalArrangement = Arrangement.spacedBy(6.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//
//                    item {
//                        Text("알림/소리 디버그", modifier = Modifier.padding(4.dp))
//                    }
//
//                    // ① 시스템 알림음 직접 재생
//                    item {
//                        Button(onClick = {
//                            try {
//                                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//                                val ring = RingtoneManager.getRingtone(this@DebugSoundActivity, uri)
//                                ring.play()
//                                log("RingtoneManager: played notification sound")
//                            } catch (e: Exception) {
//                                log("RingtoneManager play failed: ${e.message}")
//                            }
//                        }) { Text("① ") }
//                    }
//
//                    // ② 현재 모든 볼륨/벨모드/BT 로그
//                    item {
//                        Button(onClick = {
//                            val am = getSystemService(AUDIO_SERVICE) as AudioManager
//                            val map = listOf(
//                                AudioManager.STREAM_NOTIFICATION to "NOTI",
//                                AudioManager.STREAM_RING to "RING",
//                                AudioManager.STREAM_ALARM to "ALARM",
//                                AudioManager.STREAM_SYSTEM to "SYSTEM",
//                                AudioManager.STREAM_MUSIC to "MUSIC",
//                            ).joinToString { (s, name) ->
//                                val v = am.getStreamVolume(s)
//                                val m = am.getStreamMaxVolume(s)
//                                "$name=$v/$m"
//                            }
//                            val mode = when (am.ringerMode) {
//                                AudioManager.RINGER_MODE_SILENT -> "SILENT"
//                                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
//                                else -> "NORMAL"
//                            }
//                            log("VOL: $map, RINGER=$mode, BT=${am.isBluetoothScoOn}")
//                        }) { Text("② ") }
//                    }
//
//                    // ②-1 알림 볼륨 올리기(표시 UI와 함께)
//                    item {
//                        Button(onClick = {
//                            val am = getSystemService(AUDIO_SERVICE) as AudioManager
//                            repeat(5) {
//                                am.adjustStreamVolume(
//                                    AudioManager.STREAM_NOTIFICATION,
//                                    AudioManager.ADJUST_RAISE,
//                                    AudioManager.FLAG_SHOW_UI
//                                )
//                            }
//                            log("Raised STREAM_NOTIFICATION volume")
//                        }) { Text("②-1 ↑") }
//                    }
//
//                    // ②-2 벨모드 NORMAL 강제
//                    item {
//                        Button(onClick = {
//                            val am = getSystemService(AUDIO_SERVICE) as AudioManager
//                            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
//                            log("Set ringerMode=NORMAL")
//                        }) { Text("②-2") }
//                    }
//
//                    // ①-2 다양한 스트림으로 비프 테스트 (스피커 라우팅 확인)
//                    item {
//                        Button(onClick = {
//                            try {
//                                listOf(
//                                    AudioManager.STREAM_NOTIFICATION,
//                                    AudioManager.STREAM_RING,
//                                    AudioManager.STREAM_ALARM,
//                                    AudioManager.STREAM_SYSTEM,
//                                    AudioManager.STREAM_MUSIC
//                                ).forEach { stream ->
//                                    try {
//                                        ToneGenerator(stream, 100).startTone(ToneGenerator.TONE_PROP_BEEP, 200)
//                                    } catch (_: Exception) {}
//                                }
//                                log("ToneGenerator beep on NOTI/RING/ALARM/SYSTEM/MUSIC")
//                            } catch (e: Exception) {
//                                log("ToneGenerator failed: ${e.message}")
//                            }
//                        }) { Text("①-2") }
//                    }
//
//                    // ③ 채널 설정 열기
//                    item {
//                        Button(onClick = {
//                            WearNotif.openChannelSettings(this@DebugSoundActivity)
//                        }) { Text("③") }
//                    }
//
//                    // ③-1 시스템 사운드 설정 열기(OEM에 따라 다름)
//                    item {
//                        Button(onClick = {
//                            try {
//                                startActivity(
//                                    android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
//                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
//                                )
//                            } catch (e: Exception) {
//                                log("Open sound settings failed: ${e.message}")
//                            }
//                        }) { Text("③-1") }
//                    }
//
//                    // ④ 테스트 알림(항상 새 ID)
//                    item {
//                        Button(onClick = {
//                            val id = Random.nextInt(10000, 99999)
//                            WearNotif.show(this@DebugSoundActivity, "소리 테스트", "id=$id", id)
//                            log("Posted notif id=$id")
//                        }) { Text("④") }
//                    }
//
//                    item {
//                        Button(onClick = {
//                            // ④-β 백그라운드에서 알림 테스트: 포그라운드 억제 우회
//                            val id = kotlin.random.Random.nextInt(10000, 99999)
//                            // 앱을 백그라운드로 이동
//                            moveTaskToBack(true)
//                            // 1초 뒤 알림 올리기
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                com.example.dive.notify.WearNotif.show(
//                                    this@DebugSoundActivity,
//                                    "소리 테스트(백그라운드)",
//                                    "id=$id",
//                                    id
//                                )
//                            }, 1000)
//                            android.util.Log.w("WEAR-SOUND", "Scheduled bg notif id=$id")
//                        }) { Text("④-β 백그라운드 알림 발송") }
//                    }
//
//                    item {
//                        Text("log: $lastLog", modifier = Modifier.padding(4.dp))
//                    }
//                }
//            }
//        }
//    }
//}
