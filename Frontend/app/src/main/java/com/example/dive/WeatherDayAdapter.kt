package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.WeatherDay
import java.text.SimpleDateFormat
import java.util.*

class WeatherDayAdapter(private var items: List<WeatherDay>) :
    RecyclerView.Adapter<WeatherDayAdapter.DayViewHolder>() {

    private val expandedState = mutableSetOf<Int>()

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDayDate)
        val tvSky: TextView = itemView.findViewById(R.id.tvDaySky)
        val tvTemp: TextView = itemView.findViewById(R.id.tvDayTemp)
        val tvToggle: TextView = itemView.findViewById(R.id.tvToggle)
        val layoutDetail: View = itemView.findViewById(R.id.layoutDetail)
        val tvDayDesc: TextView = itemView.findViewById(R.id.tvDayDesc)
        val rvHours: RecyclerView = itemView.findViewById(R.id.rvDayHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = items[position]

        // ✅ 날짜 포맷 변환 (yyyy-MM-dd → "월 21")
        holder.tvDate.text = formatDate(day.date)

        // ✅ 최고 / 최저 온도 계산
        val maxTemp = day.hours.maxOfOrNull { it.temp }?.toInt() ?: 0
        val minTemp = day.hours.minOfOrNull { it.temp }?.toInt() ?: 0
        holder.tvTemp.text = "${maxTemp}° / ${minTemp}°"

        // ✅ 대표 날씨 → 첫 시간대 기준
        val sky = day.hours.firstOrNull()?.sky ?: ""
        holder.tvSky.text = getWeatherEmojiFromSky(sky)

        // ✅ 상세 설명 (간단하게 대표 하늘상태 표시)
        holder.tvDayDesc.text = "${sky} 중심의 하루"

        // ✅ 시간별 RecyclerView
        holder.rvHours.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvHours.adapter = WeatherHourAdapter(day.hours)

        // ✅ 토글 처리
        val isExpanded = expandedState.contains(position)
        holder.layoutDetail.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.tvToggle.text = if (isExpanded) "▲" else "▼"

        holder.itemView.setOnClickListener {
            if (isExpanded) expandedState.remove(position)
            else expandedState.add(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateDays(newItems: List<WeatherDay>) {
        items = newItems
        expandedState.clear()
        notifyDataSetChanged()
    }

    // ✅ 날짜 포맷 변환 (yyyy-MM-dd → "21일 (목)")
    private fun formatDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val date = sdf.parse(dateStr)
            val cal = Calendar.getInstance().apply { time = date!! }
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "일"
                Calendar.MONDAY -> "월"
                Calendar.TUESDAY -> "화"
                Calendar.WEDNESDAY -> "수"
                Calendar.THURSDAY -> "목"
                Calendar.FRIDAY -> "금"
                Calendar.SATURDAY -> "토"
                else -> ""
            }
            "${cal.get(Calendar.DAY_OF_MONTH)}일 ($dayOfWeek)"
        } catch (e: Exception) {
            dateStr
        }
    }


    // ✅ sky → emoji
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
}
