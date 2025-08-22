package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.Weather6hItem

class Weather6hAdapter(private var items: List<Weather6hItem>) :
    RecyclerView.Adapter<Weather6hAdapter.Weather6hViewHolder>() {

    class Weather6hViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvTime6h)
        val tvSkyTemp: TextView = itemView.findViewById(R.id.tvSkyTemp6h)
        val tvRainHumidity: TextView = itemView.findViewById(R.id.tvRainHumidity6h)
        val tvWindWave: TextView = itemView.findViewById(R.id.tvWindWave6h)
        val tvDust: TextView = itemView.findViewById(R.id.tvDust6h)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Weather6hViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather6h, parent, false)
        return Weather6hViewHolder(view)
    }

    override fun onBindViewHolder(holder: Weather6hViewHolder, position: Int) {
        val item = items[position]

        // ✅ 오전/오후 h시 포맷
        val hour = item.time.substring(11, 13).toInt()
        val ampm = if (hour < 12) "오전" else "오후"
        val displayHour = if (hour % 12 == 0) 12 else hour % 12
        val timeFormatted = "$ampm ${displayHour}시"

        val temp = item.tempC.toInt()
        val humidity = item.humidityPct.toInt()
        val wind = item.windSpeedMs.toInt()
        val wave = item.waveHeightM?.toDoubleOrNull()?.let { "${it}m" } ?: "정보 없음"
        val rain = item.rainMm.toInt()
        val emoji = getWeatherEmojiFromSky(item.sky)

        holder.tvTime.text = timeFormatted
        holder.tvSkyTemp.text = "$emoji ${item.sky} · ${temp}℃"
        holder.tvRainHumidity.text = "강수량: ${rain}mm · 습도: ${humidity}%"
        holder.tvWindWave.text = "풍향: ${item.windDir} · 풍속: ${wind}m/s · 파고: $wave"
        holder.tvDust.text = "PM10: ${item.pm10S} · PM2.5: ${item.pm25S}"
    }


    override fun getItemCount(): Int = items.size

    fun updateWeather(newItems: List<Weather6hItem>) {
        items = newItems
        notifyDataSetChanged()
    }

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
