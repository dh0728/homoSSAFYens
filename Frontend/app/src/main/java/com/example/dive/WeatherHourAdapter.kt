package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.WeatherHour

class WeatherHourAdapter(private var items: List<WeatherHour>) :
    RecyclerView.Adapter<WeatherHourAdapter.HourViewHolder>() {

    class HourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvHourTime)
        val tvSkyTemp: TextView = itemView.findViewById(R.id.tvHourSkyTemp)
        val tvWindWave: TextView = itemView.findViewById(R.id.tvHourWindWave)
        val tvHumidityRain: TextView = itemView.findViewById(R.id.tvHourHumidityRain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_hour, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        val item = items[position]

        // ✅ 시간 → 오전/오후 h시 변환
        val timeLabel = try {
            val hour = item.time.substring(11, 13).toInt()
            val ampm = if (hour < 12) "오전" else "오후"
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            "$ampm ${h}시"
        } catch (e: Exception) {
            item.time.substring(11, 16)
        }
        holder.tvTime.text = timeLabel

        // ✅ 소수점 제거
        val temp = item.temp.toInt()
        val humidity = item.humidity.toInt()
        val wind = item.windspd.toInt()
        val wave = item.waveHt.toInt()

        // ✅ 날씨 이모지 추가
        val emoji = getWeatherEmojiFromSky(item.sky)

        // ✅ 바인딩
        holder.tvSkyTemp.text = "$emoji ${item.sky} · ${temp}℃"
        holder.tvWindWave.text = "풍향: ${item.winddir} · 풍속: ${wind}m/s · 파고: ${wave}m"
        holder.tvHumidityRain.text = "습도: ${humidity}% · 강수: ${item.rain}% (${item.rainAmt.toInt()}mm)"
    }

    override fun getItemCount(): Int = items.size

    // ✅ sky → emoji 매핑
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
