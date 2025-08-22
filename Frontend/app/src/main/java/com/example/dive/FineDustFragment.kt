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
                        tvTime.text = formatDateTime(data.time)

                        // ✅ PM10 (미세먼지) - 그대로
                        val pm10Value = data.data.pm10.value?.toInt() ?: 0
                        progressPm10.max = 150
                        progressPm10.progress = pm10Value
                        tvPm10Label.text = "미세"
                        tvPm10ValueCenter.text = pm10Value.toString()
                        setTextColorByLevel(tvPm10Label, tvPm10ValueCenter, data.data.pm10.level)

                        // ✅ PM2.5 (초미세먼지) - 그대로
                        val pm25Value = data.data.pm25.value?.toInt() ?: 0
                        progressPm25.max = 75
                        progressPm25.progress = pm25Value
                        tvPm25Label.text = "초미세"
                        tvPm25ValueCenter.text = pm25Value.toString()
                        setTextColorByLevel(tvPm25Label, tvPm25ValueCenter, data.data.pm25.level)

                        // ✅ 기타 항목 (항목명은 검정 + 등급만 색/이모지)
                        tvO3.text = "오존 : ${decorateLevel(data.data.o3.level)}"
                        tvO3.setTextColor(Color.BLACK)

                        tvCO.text = "일산화탄소 : ${decorateLevel(data.data.co.level)}"
                        tvCO.setTextColor(Color.BLACK)

                        tvSO2.text = "아황산가스 : ${decorateLevel(data.data.so2.level)}"
                        tvSO2.setTextColor(Color.BLACK)

                        tvNO2.text = "이산화질소 : ${decorateLevel(data.data.no2.level)}"
                        tvNO2.setTextColor(Color.BLACK)

                        tvKhai.text = "통합대기지수 : ${decorateLevel(data.data.khai.level)}"
                        tvKhai.setTextColor(Color.BLACK)
                    }
                }

                override fun onFailure(call: Call<FineDustResponse>, t: Throwable) {
                    Log.e("FineDust", "에러: ${t.message}")
                }
            })
    }

    // ✅ 등급 텍스트 + 이모지 붙이기 (색상은 여기서만 적용)
    private fun decorateLevel(level: String): CharSequence {
        val emoji = when (level) {
            "좋음" -> "😊"
            "보통" -> "😐"
            "나쁨" -> "😷"
            "매우나쁨" -> "🤢"
            else -> "❔"
        }
        val color = when (level) {
            "좋음" -> Color.parseColor("#2196F3") // 파랑
            "보통" -> Color.parseColor("#4CAF50") // 초록
            "나쁨" -> Color.parseColor("#FF9800") // 주황
            "매우나쁨" -> Color.parseColor("#F44336") // 빨강
            else -> Color.DKGRAY
        }

        // Spannable로 등급 부분만 색상 적용
        val text = "$level $emoji"
        return android.text.SpannableString(text).apply {
            setSpan(
                android.text.style.ForegroundColorSpan(color),
                0, level.length, // "좋음" 부분만 색 적용
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun formatDateTime(dateTime: String): String {
        val possibleFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss", // ISO 형태
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )

        for (pattern in possibleFormats) {
            try {
                val inputFormat = java.text.SimpleDateFormat(pattern, java.util.Locale.KOREA)
                val outputFormat = java.text.SimpleDateFormat("M월 d일 H시", java.util.Locale.KOREA)
                val date = inputFormat.parse(dateTime)
                if (date != null) return outputFormat.format(date)
            } catch (_: Exception) { }
        }
        return dateTime // 실패 시 원본 그대로
    }



    private fun setTextColorByLevel(label: TextView, value: TextView, level: String) {
        when (level) {
            "좋음" -> {
                label.setTextColor(Color.parseColor("#2196F3"))
                value.setTextColor(Color.parseColor("#2196F3"))
            }
            "보통" -> {
                label.setTextColor(Color.parseColor("#4CAF50"))
                value.setTextColor(Color.parseColor("#4CAF50"))
            }
            "나쁨" -> {
                label.setTextColor(Color.parseColor("#FF9800"))
                value.setTextColor(Color.parseColor("#FF9800"))
            }
            "매우나쁨" -> {
                label.setTextColor(Color.parseColor("#F44336"))
                value.setTextColor(Color.parseColor("#F44336"))
            }
            else -> {
                label.setTextColor(Color.DKGRAY)
                value.setTextColor(Color.DKGRAY)
            }
        }
    }
}
