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
import com.example.dive.data.model.TideResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Tide1dayFragment : Fragment() {

    private lateinit var tvDate: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvSunMoon: TextView
    private lateinit var rvEvents: RecyclerView
    private lateinit var adapter: TideEventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tide1day, container, false)

        tvDate = view.findViewById(R.id.tvDate)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvSunMoon = view.findViewById(R.id.tvSunMoon)
        rvEvents = view.findViewById(R.id.rvEvents)

        rvEvents.layoutManager = LinearLayoutManager(requireContext())
        adapter = TideEventAdapter(emptyList())
        rvEvents.adapter = adapter

        loadTideData()

        return view
    }

    // "HH:mm:ss" → "HH:mm"
    private fun formatTime(raw: String): String {
        return try {
            raw.substring(0, 5) // "07:19:00" → "07:19"
        } catch (e: Exception) {
            raw
        }
    }

    // RISING / FALLING → 한글
    private fun convertType(raw: String): String {
        return when (raw.uppercase()) {
            "RISING" -> "만조"
            "FALLING" -> "간조"
            else -> raw
        }
    }

    private fun loadTideData() {
        val lat = arguments?.getDouble("lat") ?: 35.1
        val lon = arguments?.getDouble("lon") ?: 129.0

        RetrofitClient.instance.getTodayTide(lat, lon)
            .enqueue(object : Callback<TideResponse> {
                override fun onResponse(call: Call<TideResponse>, response: Response<TideResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data ?: return

                        // 1. 날짜/위치/일출일몰
                        tvDate.text = "${data.date} (${data.weekday}) · 음력 ${data.lunar}"
                        tvLocation.text = "${data.locationName} · ${data.mul}"

                        // 초 제거 함수 사용
                        tvSunMoon.text =
                            "일출: ${formatTime(data.sunrise)} · 일몰: ${formatTime(data.sunset)}\n" +
                                    "월출: ${formatTime(data.moonrise)} · 월몰: ${formatTime(data.moonset)}"

                        // 2. 이벤트 리스트 업데이트 (시간/상태/색상 변환 포함)
                        val convertedEvents = data.events.map { event ->
                            event.copy(
                                time = formatTime(event.time), // HH:mm:ss → HH:mm
                                trend = convertType(event.trend), // RISING/FALLING → 만조/간조
                            )
                        }

                        adapter.updateEvents(convertedEvents)

                    } else {
                        Log.e("Tide1day", "응답 실패: ${response.code()}")
                    }
                }


                override fun onFailure(call: Call<TideResponse>, t: Throwable) {
                    Log.e("Tide1day", "요청 실패: ${t.message}")
                }
            })
    }



}
