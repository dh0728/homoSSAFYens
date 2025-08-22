package com.example.dive

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.TideResponse
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

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvNearestSub: TextView
    private lateinit var tvDate: TextView
    private lateinit var chipGroup: ChipGroup
    private var lastLat: Double? = null
    private var lastLon: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "앱 실행됨!")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tvNearestSub = findViewById(R.id.tvNearestSub)
        tvDate = findViewById(R.id.tvDate)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    102
                )
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

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
        }
    }
}