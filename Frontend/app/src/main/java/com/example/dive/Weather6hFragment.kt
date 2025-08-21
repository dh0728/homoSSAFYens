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

    // ✅ 현재 날씨 뷰
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvCurrentSky: TextView
    private lateinit var tvCurrentHumidity: TextView
    private lateinit var tvCurrentWind: TextView
    private lateinit var tvCurrentWindDir: TextView
    private lateinit var tvCurrentWave: TextView
    private lateinit var tvCurrentDust: TextView
    private lateinit var tvCurrentEmoji: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weather6h, container, false)

        // ✅ 현재 날씨 뷰 초기화
        tvCurrentTemp = view.findViewById(R.id.tvCurrentTemp)
        tvCurrentSky = view.findViewById(R.id.tvCurrentSky)
        tvCurrentHumidity = view.findViewById(R.id.tvCurrentHumidity)
        tvCurrentWind = view.findViewById(R.id.tvCurrentWind)
        tvCurrentWindDir = view.findViewById(R.id.tvCurrentWindDir)
        tvCurrentWave = view.findViewById(R.id.tvCurrentWave)
        tvCurrentDust = view.findViewById(R.id.tvCurrentDust)
        tvCurrentEmoji = view.findViewById(R.id.tvCurrentEmoji)


        tvCity = view.findViewById(R.id.tvCity6h)
        rvWeather = view.findViewById(R.id.rvWeather6h)
        rvWeather.layoutManager = LinearLayoutManager(requireContext())
        adapter = Weather6hAdapter(emptyList())
        rvWeather.adapter = adapter

        loadWeatherData()

        return view
    }

    // ✅ sky → emoji 매핑 함수
    private fun getWeatherEmojiFromSky(sky: String): String {
        return when {
            sky.contains("맑음") -> "☀️"
            sky.contains("구름많음") -> "☁️"
            sky.contains("구름조금") -> "🌤️"
            sky.contains("흐림") -> "☁️"
            sky.contains("비/눈") -> "🌧️"
            sky.contains("비") -> "🌧️"
            sky.contains("눈") -> "🌨️"
            else -> "❔"
        }
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
                        val weatherList = body.data.weather

                        if (weatherList.isNotEmpty()) {
                            val current = weatherList[0]

                            val temp = current.tempC.toString().toDoubleOrNull()?.toInt() ?: current.tempC
                            val humidity = current.humidityPct.toString().toDoubleOrNull()?.toInt() ?: current.humidityPct
                            val wind = current.windSpeedMs.toString().toDoubleOrNull()?.toInt() ?: current.windSpeedMs
                            val wave = current.waveHeightM.toString().toDoubleOrNull()?.toInt()?.toString() ?: "정보 없음"
                            val emoji = getWeatherEmojiFromSky(current.sky)

                            // ✅ 현재 날씨 세팅
                            tvCurrentEmoji.text = emoji
                            tvCurrentTemp.text = "${temp}℃"
                            tvCurrentSky.text = current.sky
                            tvCurrentHumidity.text = "습도 ${humidity}%"
                            tvCurrentWind.text = "풍속 ${wind}m/s"
                            tvCurrentWindDir.text = "풍향 ${current.windDir}"
                            tvCurrentWave.text = "파고 $wave m"
                            tvCurrentDust.text = "미세먼지 ${current.pm10S}"

                            // ✅ 리스트에는 나머지 5개만 전달
                            val nextFive = weatherList.drop(1).take(5)
                            adapter.updateWeather(nextFive)
                        }

                        tvCity.text = body.data.info.city
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
