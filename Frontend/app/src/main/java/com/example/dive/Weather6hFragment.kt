package com.example.dive

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.Weather6hResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Weather6hFragment : Fragment() {

    private lateinit var tvCity: TextView
    private lateinit var rvWeather: RecyclerView
    private lateinit var adapter: Weather6hAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather6h, container, false)

        tvCity = view.findViewById(R.id.tvCity6h)
        rvWeather = view.findViewById(R.id.rvWeather6h)
        rvWeather.layoutManager = LinearLayoutManager(requireContext())
        adapter = Weather6hAdapter(emptyList())
        rvWeather.adapter = adapter

        loadWeatherData()

        return view
    }

    private fun loadWeatherData() {
        // MainActivity → replaceFragment() 에서 전달한 lat/lon 받기
        val lat = arguments?.getDouble("lat")
        val lon = arguments?.getDouble("lon")

        if (lat == null || lon == null) {
            Log.e("Weather6h", "위경도 전달 안됨")
            return
        }

        Log.d("Weather6h", "전달된 좌표: $lat, $lon")

        RetrofitClient.instance.getWeather6h(lat, lon)
            .enqueue(object : Callback<Weather6hResponse> {
                override fun onResponse(
                    call: Call<Weather6hResponse>,
                    response: Response<Weather6hResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body() ?: return
                        tvCity.text = body.data.info.city
                        adapter.updateWeather(body.data.weather)
                    } else {
                        Log.e("Weather6h", "응답 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Weather6hResponse>, t: Throwable) {
                    Log.e("Weather6h", "요청 실패: ${t.message}")
                }
            })
    }
}
