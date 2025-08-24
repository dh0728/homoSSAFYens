package com.example.dive

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.TideResponse
import com.example.dive.location.LocationRegistrar
import com.example.dive.service.AppFirebaseMessagingService
import com.example.dive.work.LocateWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessaging
import com.example.dive.wear.WearBridge
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvNearestSub: TextView
    private lateinit var tvDate: TextView
    private lateinit var chipGroup: ChipGroup
    private var lastLat: Double? = null
    private var lastLon: Double? = null

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        //fcm 토큰발급
        AppFirebaseMessagingService.initAndRegisterOnce(this)

        // (기존) 현재 기기 서버에 등록
        LocationRegistrar.register(this)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result
            Log.d("FCM", "token = $token") // Logcat로 확인
            // 여기서 서버 등록 호출(혹은 토큰만 복사해서 Postman으로 등록)
        }

        // (교체) 위치 권한 있을 때만 안전하게 등록
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                LocationRegistrar.register(this)
            } catch (_: SecurityException) { /* 권한 타이밍 이슈 방어 */ }
        }


        Log.d("MainActivity", "앱 실행됨!")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "채널 상태 확인!")
        dumpNotificationState(this);

        // ✅ Android 13+ 알림 권한 요청 (한 번만)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(perm), 1001)
            }
        }

        tvNearestSub = findViewById(R.id.tvNearestSub)
        tvDate = findViewById(R.id.tvDate)
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        chipGroup = findViewById(R.id.chipGroup)

        // chip 클릭 리스너
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                val chip = findViewById<Chip>(chipId)

                when (chip.id) {
                    R.id.chipTide1 -> replaceFragment(Tide1dayFragment())
                    R.id.chipTide7 -> replaceFragment(Tide7dayFragment())
                    R.id.chipWeather6h -> replaceFragment(Weather6hFragment())
                    R.id.chipWeather7 -> replaceFragment(Weather7dFragment())
                    R.id.chipTemp -> replaceFragment(SeatempFragment())
                    R.id.chipFishing -> replaceFragment(FishingFragment())
                    R.id.chipDust -> replaceFragment(FineDustFragment())
                }
            }
        }

        if (savedInstanceState == null) {
            // 기본 Fragment 설정
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, Tide1dayFragment())
//                .commit()
            getCurrentLocationAndCallApi(showFragment = true)

            // 기본 Chip 선택 상태로
            val chipToday = findViewById<Chip>(R.id.chipTide1)
            chipToday.isChecked = true
        }

        // 오늘 날짜 표시
        val today = Calendar.getInstance().time
        val sdf = SimpleDateFormat("MM.dd (E)", Locale.KOREAN)
        tvDate.text = sdf.format(today)

        // 위치 기반 API 호출
        getCurrentLocationAndCallApi()

        // 알림 권한 요청 (Android 13 이상)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                    102
//                )
//            }
//        }

        // ✅ 워치 브릿지 로컬 테스트 (원하면 임시 버튼으로 호출)
//        sendTestToWatch()
//        WearBridge.sendTest(this);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkAndRequestSosPermissions()
    }

    private fun checkAndRequestSosPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.SEND_SMS)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                104 // New request code for SOS
            )
        }
    }

    // 워치로 테스트 메시지 던지기
    private fun sendTestToWatch() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = Wearable.getMessageClient(this@MainActivity)
                val nodes = Tasks.await(Wearable.getNodeClient(this@MainActivity).connectedNodes)

                for (node in nodes) {
                    client.sendMessage(node.id, "/tide/test", "bridge-check".toByteArray())
                }
                Log.d("MainActivity", "워치로 test message 전송 완료")
            } catch (e: Exception) {
                Log.e("MainActivity", "워치 전송 실패: ${e.message}", e)
            }
        }
    }

    /** 알림 상태/채널 상태 로그 출력 */
    private fun dumpNotificationState(ctx: Context) {
        val nm = androidx.core.app.NotificationManagerCompat.from(ctx)
        val areEnabled = nm.areNotificationsEnabled()
        android.util.Log.w("NOTI", "app notifications enabled=$areEnabled")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sys = ctx.getSystemService(android.app.NotificationManager::class.java)
            val ch = sys.getNotificationChannel(com.example.dive.notify.Notif.CH_TIDE)
            if (ch == null) {
                android.util.Log.w("NOTI", "channel tide_alerts = NULL")
            } else {
                android.util.Log.w(
                    "NOTI",
                    "channel importance=${ch.importance}, sound=${ch.sound}, vibration=${ch.vibrationPattern!=null}"
                )
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun getCurrentLocationAndCallApi(showFragment: Boolean = false) {
        // 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CALL_PHONE
                ),
                100
            )
            return
        }

        // 최신 위치 가져오기
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    lastLat = lat
                    lastLon = lon

                    // 기존 코드 안의 위치 성공 블록에 ▼ 아래 6줄 추가 - 최초 1
                    val w = OneTimeWorkRequestBuilder<LocateWorker>()
                        .setInputData(workDataOf(
                            "lat" to lat,
                            "lon" to lon,
                            "ts" to (System.currentTimeMillis()/1000L)
                        ))
                        .build()
                    WorkManager.getInstance(this).enqueue(w)

                    Log.d("Location", "lat=$lat, lon=$lon")

                    // API 호출
                    RetrofitClient.instance.getTodayTide(lat, lon)
                        .enqueue(object : Callback<TideResponse> {
                            override fun onResponse(
                                call: Call<TideResponse>,
                                response: Response<TideResponse>
                            ) {
                                Log.d("API", "응답 코드=${response.code()}")
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    body?.let {
                                        val locationName = it.data.locationName
                                        val mul = it.data.mul
                                        tvNearestSub.text = "$locationName · $mul"
                                    }
                                } else {
                                    Log.e("API", "응답 실패: ${response.errorBody()?.string()}")
                                }
                            }

                            override fun onFailure(call: Call<TideResponse>, t: Throwable) {
                                Log.e("API", "요청 실패: ${t.message}")
                            }
                        })

                    // ✅ 위치까지 확보되었으면 fragment 띄우기
                    if (showFragment) {
                        replaceFragment(Tide1dayFragment())
                    }

                } else {
                    Log.e("Location", "location is null")
                }
            }
            .addOnFailureListener {
                Log.e("Location", "위치 가져오기 실패: ${it.message}")
            }
    }

    // 위도경도 fragment로 다 넘겨주기
    private fun replaceFragment(fragment: Fragment) {
        val bundle = Bundle().apply {
            lastLat?.let { putDouble("lat", it) }
            lastLon?.let { putDouble("lon", it) }
        }
        fragment.arguments = bundle
        Log.d("MainActivity", "전달할 위경도: $lastLat, $lastLon")

        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, fragment)
        }
    }

    // 권한 허용 후 다시 시도
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                getCurrentLocationAndCallApi()

                // ✅ 위치 권한 막 허용된 경우, 이제 안전하게 등록
                try {
                    LocationRegistrar.register(this)
                } catch (_: SecurityException) {}

                // ✅ 안드10+면 백그라운드 위치 권한 추가 요청
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val hasBg = ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasBg) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                            103
                        )
                    }
                }


            } else {
                // Handle case where not all permissions are granted
                Log.e("MainActivity", "Not all permissions granted.")
            }
        } else if (requestCode == 102) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted.")
            } else {
                Log.e("MainActivity", "Notification permission denied.")
            }
        } else if (requestCode == 103) {
            // 배경 위치 허용/거부 결과 로그 정도만
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Log.d("MainActivity", "Background location permission = $granted")
        } else if (requestCode == 104) { // SOS Permissions
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("MainActivity", "SOS permissions granted.")
            } else {
                Log.w("MainActivity", "SOS permissions were denied.")
            }
        }
    }
}