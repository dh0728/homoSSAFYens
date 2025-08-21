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

        val time = item.time.substring(11, 16)

        // ✅ 소수점 제거
        val temp = item.tempC.toString().toDoubleOrNull()?.toInt() ?: item.tempC
        val humidity = item.humidityPct.toString().toDoubleOrNull()?.toInt() ?: item.humidityPct
        val wind = item.windSpeedMs.toString().toDoubleOrNull()?.toInt() ?: item.windSpeedMs
        val wave = item.waveHeightM?.toDoubleOrNull()?.toInt()?.toString() ?: "정보 없음"

        holder.tvTime.text = time
        holder.tvSkyTemp.text = "${item.sky} · ${temp}℃"
        holder.tvRainHumidity.text = "강수량: ${item.rainMm.toString().toDoubleOrNull()?.toInt() ?: 0}mm · 습도: ${humidity}%"
        holder.tvWindWave.text = "풍향: ${item.windDir} · 풍속: ${wind}m/s · 파고: $wave m"

        // ✅ 미세먼지는 수치 빼고 상태만
        holder.tvDust.text = "PM10: ${item.pm10S} · PM2.5: ${item.pm25S}"
    }



    override fun getItemCount(): Int = items.size

    fun updateWeather(newItems: List<Weather6hItem>) {
        items = newItems
        notifyDataSetChanged()
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
}
