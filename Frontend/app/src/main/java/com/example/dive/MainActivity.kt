package com.example.dive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.TideResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvNearestSub: TextView
    private lateinit var tvDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "앱 실행됨!")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        tvNearestSub = findViewById(R.id.tvNearestSub)
        tvDate = findViewById(R.id.tvDate)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 오늘 날짜 표시
        val today = Calendar.getInstance().time
        val sdf = SimpleDateFormat("MM.dd (E)", Locale.KOREAN)
        tvDate.text = sdf.format(today)

        // 위치 기반 API 호출
        getCurrentLocationAndCallApi()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getCurrentLocationAndCallApi() {
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
                    Manifest.permission.ACCESS_COARSE_LOCATION
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
                } else {
                    Log.e("Location", "location is null")
                }
            }
            .addOnFailureListener {
                Log.e("Location", "위치 가져오기 실패: ${it.message}")
            }
    }

    // 권한 허용 후 다시 시도
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocationAndCallApi()
        }
    }
}
