package com.example.dive

import FineDustResponse
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.dive.data.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FineDustFragment : Fragment() {

    private lateinit var tvStation: TextView
    private lateinit var tvTime: TextView
    private lateinit var progressPm10: ProgressBar
    private lateinit var progressPm25: ProgressBar
    private lateinit var tvPm10Label: TextView
    private lateinit var tvPm10ValueCenter: TextView
    private lateinit var tvPm25Label: TextView
    private lateinit var tvPm25ValueCenter: TextView

    private lateinit var tvO3: TextView
    private lateinit var tvCO: TextView
    private lateinit var tvSO2: TextView
    private lateinit var tvNO2: TextView
    private lateinit var tvKhai: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_finedust, container, false)

        tvStation = view.findViewById(R.id.tvStation)
        tvTime = view.findViewById(R.id.tvTime)

        progressPm10 = view.findViewById(R.id.progressPm10)
        progressPm25 = view.findViewById(R.id.progressPm25)
        tvPm10Label = view.findViewById(R.id.tvPm10Label)
        tvPm10ValueCenter = view.findViewById(R.id.tvPm10ValueCenter)
        tvPm25Label = view.findViewById(R.id.tvPm25Label)
        tvPm25ValueCenter = view.findViewById(R.id.tvPm25ValueCenter)

        tvO3 = view.findViewById(R.id.tvO3)
        tvCO = view.findViewById(R.id.tvCO)
        tvSO2 = view.findViewById(R.id.tvSO2)
        tvNO2 = view.findViewById(R.id.tvNO2)
        tvKhai = view.findViewById(R.id.tvKhai)

        loadFineDust()
        return view
    }

    private fun loadFineDust() {
        // MainActivity → replaceFragment() 에서 전달한 lat/lon 받기
        val lat = arguments?.getDouble("lat") ?: return
        val lon = arguments?.getDouble("lon") ?: return
        Log.d("FineDust", "전달된 좌표: $lat, $lon")

        RetrofitClient.instance.getFineDust(lat, lon)
            .enqueue(object : Callback<FineDustResponse> {
                override fun onResponse(
                    call: Call<FineDustResponse>,
                    response: Response<FineDustResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data ?: return

                        tvStation.text = data.stationName
                        tvTime.text = data.time

                        // PM10
                        val pm10Value = data.data.pm10.value?.toInt() ?: 0
                        progressPm10.max = 150   // 환경부 기준
                        progressPm10.progress = pm10Value
                        tvPm10Label.text = "미세"
                        tvPm10ValueCenter.text = pm10Value.toString()
                        setTextColorByLevel(tvPm10Label, tvPm10ValueCenter, data.data.pm10.level)

                        // PM2.5
                        val pm25Value = data.data.pm25.value?.toInt() ?: 0
                        progressPm25.max = 75   // 환경부 기준
                        progressPm25.progress = pm25Value
                        tvPm25Label.text = "초미세"
                        tvPm25ValueCenter.text = pm25Value.toString()
                        setTextColorByLevel(tvPm25Label, tvPm25ValueCenter, data.data.pm25.level)

                        // 기타 항목
                        tvO3.text = "오존: ${data.data.o3.value}${data.data.o3.unit} (${data.data.o3.level})"
                        tvCO.text = "일산화탄소: ${data.data.co.value}${data.data.co.unit} (${data.data.co.level})"
                        tvSO2.text = "아황산가스: ${data.data.so2.value}${data.data.so2.unit} (${data.data.so2.level})"
                        tvNO2.text = "이산화질소: ${data.data.no2.value}${data.data.no2.unit} (${data.data.no2.level})"
                        tvKhai.text = "통합대기지수: ${data.data.khai.value} (${data.data.khai.level})"
                    }
                }

                override fun onFailure(call: Call<FineDustResponse>, t: Throwable) {
                    Log.e("FineDust", "에러: ${t.message}")
                }
            })
    }

    private fun setTextColorByLevel(label: TextView, value: TextView, level: String) {
        when (level) {
            "좋음" -> {
                label.setTextColor(Color.parseColor("#2196F3")) // 파란색
                value.setTextColor(Color.parseColor("#2196F3"))
            }
            "보통" -> {
                label.setTextColor(Color.parseColor("#4CAF50")) // 초록색
                value.setTextColor(Color.parseColor("#4CAF50"))
            }
            else -> {
                label.setTextColor(Color.DKGRAY)
                value.setTextColor(Color.DKGRAY)
            }
        }
    }
}
