package com.example.dive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.TideWeeklyResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Tide7dayFragment : Fragment() {

    private lateinit var rvWeekly: RecyclerView
    private lateinit var adapter: WeeklyAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tide7day, container, false)

        rvWeekly = view.findViewById(R.id.rvWeekly)
        rvWeekly.layoutManager = LinearLayoutManager(requireContext())
        adapter = WeeklyAdapter(emptyList())
        rvWeekly.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        loadWeeklyTide()

        return view
    }

    // HH:mm:ss → HH:mm
    private fun formatTime(raw: String): String {
        return try {
            raw.substring(0, 5)
        } catch (e: Exception) {
            raw
        }
    }

    // RISING/FALLING → 한글 변환
    private fun convertTrend(raw: String): String {
        return when (raw.uppercase()) {
            "RISING" -> "만조"
            "FALLING" -> "간조"
            else -> raw
        }
    }


    private fun loadWeeklyTide() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                200
            )
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Log.d("Tide7day", "lat=$lat, lon=$lon")

                    RetrofitClient.instance.getWeeklyTide(lat, lon)
                        .enqueue(object : Callback<TideWeeklyResponse> {
                            override fun onResponse(
                                call: Call<TideWeeklyResponse>,
                                response: Response<TideWeeklyResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val weekly = response.body()?.data ?: return

                                    // 변환: 시간 HH:mm, 추세 한글화
                                    val convertedWeekly = weekly.map { day ->
                                        day.copy(
                                            events = day.events.map { event ->
                                                event.copy(
                                                    time = formatTime(event.time),
                                                    trend = convertTrend(event.trend)
                                                )
                                            }
                                        )
                                    }

                                    adapter.updateWeekly(convertedWeekly)
                                } else {
                                    Log.e("Tide7day", "응답 실패: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<TideWeeklyResponse>, t: Throwable) {
                                Log.e("Tide7day", "요청 실패: ${t.message}")
                            }
                        })
                } else {
                    Log.e("Tide7day", "위치 가져오기 실패: null")
                }
            }
            .addOnFailureListener {
                Log.e("Tide7day", "위치 가져오기 실패: ${it.message}")
            }
    }
}
