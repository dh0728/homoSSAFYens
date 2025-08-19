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
import com.example.dive.data.model.TempResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SeatempFragment : Fragment() {

    private lateinit var rvTemps: RecyclerView
    private lateinit var adapter: TempAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_seatemp, container, false)

        rvTemps = view.findViewById(R.id.rvTemps)
        rvTemps.layoutManager = LinearLayoutManager(requireContext())
        adapter = TempAdapter(emptyList())
        rvTemps.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        loadTempData()

        return view
    }

    private fun loadTempData() {
        // 권한 체크
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

        // 현재 위치 가져오기
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Log.d("Seatemp", "lat=$lat, lon=$lon")

                    // API 호출
                    RetrofitClient.instance.getAllTemps(lat, lon)
                        .enqueue(object : Callback<TempResponse> {
                            override fun onResponse(
                                call: Call<TempResponse>,
                                response: Response<TempResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val temps = response.body()?.data ?: return
                                    adapter.updateData(temps)
                                } else {
                                    Log.e("Seatemp", "응답 실패: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<TempResponse>, t: Throwable) {
                                Log.e("Seatemp", "요청 실패: ${t.message}")
                            }
                        })
                }
            }
    }
}
