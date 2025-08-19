package com.example.dive

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.Weather7dResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Weather7dFragment : Fragment() {

    private lateinit var rvWeather7d: RecyclerView
    private lateinit var adapter: WeatherDayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather7d, container, false)

        rvWeather7d = view.findViewById(R.id.rvWeather7d)
        rvWeather7d.layoutManager = LinearLayoutManager(requireContext())
        adapter = WeatherDayAdapter(emptyList())
        rvWeather7d.adapter = adapter

        loadWeather7d()

        return view
    }

    private fun loadWeather7d() {
        val lat = 35.1
        val lon = 129.0

        RetrofitClient.instance.getWeather7d(lat, lon)
            .enqueue(object : Callback<Weather7dResponse> {
                override fun onResponse(
                    call: Call<Weather7dResponse>,
                    response: Response<Weather7dResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body() ?: return
                        adapter.updateDays(body.data.days)
                    } else {
                        Log.e("Weather7d", "응답 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<Weather7dResponse>, t: Throwable) {
                    Log.e("Weather7d", "요청 실패: ${t.message}")
                }
            })
    }
}
