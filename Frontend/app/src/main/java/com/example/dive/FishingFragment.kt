package com.example.dive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.FishingResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FishingFragment : Fragment() {

    private lateinit var tvIntro: TextView
    private lateinit var tvForecast: TextView
    private lateinit var tvEbbf: TextView
    private lateinit var tvNotice: TextView
    private lateinit var rvPoints: RecyclerView
    private lateinit var adapter: FishingPointAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fishing, container, false)

        tvIntro = view.findViewById(R.id.tvIntro)
        tvForecast = view.findViewById(R.id.tvForecast)
        tvEbbf = view.findViewById(R.id.tvEbbf)
        tvNotice = view.findViewById(R.id.tvNotice)
        rvPoints = view.findViewById(R.id.rvPoints)

        rvPoints.layoutManager = LinearLayoutManager(requireContext())
        adapter = FishingPointAdapter(emptyList())
        rvPoints.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        loadFishingData()

        return view
    }

    private fun loadFishingData() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                300
            )
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Log.d("Fishing", "lat=$lat, lon=$lon")

                    RetrofitClient.instance.getFishingPoints(lat, lon)
                        .enqueue(object : Callback<FishingResponse> {
                            override fun onResponse(
                                call: Call<FishingResponse>,
                                response: Response<FishingResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val body = response.body()?.data ?: return
                                    tvIntro.text = "소개: ${body.info.intro}"
                                    tvForecast.text = "예보: ${body.info.forecast}"
                                    tvEbbf.text = "조류: ${body.info.ebbf}"
                                    tvNotice.text = "주의사항: ${body.info.notice}"

                                    adapter.updateData(body.points)
                                } else {
                                    Log.e("Fishing", "응답 실패: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<FishingResponse>, t: Throwable) {
                                Log.e("Fishing", "요청 실패: ${t.message}")
                            }
                        })
                }
            }
    }
}
