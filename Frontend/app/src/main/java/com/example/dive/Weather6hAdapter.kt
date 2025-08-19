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

        // 시간만 추출 (T 뒤부터 HH:mm)
        val time = item.time.substring(11, 16)

        holder.tvTime.text = time
        holder.tvSkyTemp.text = "${item.sky} · ${item.tempC}℃"
        holder.tvRainHumidity.text = "강수량: ${item.rainMm}mm · 습도: ${item.humidityPct}%"
        // 파고 처리 (NaN → "정보 없음")
        val waveText = item.waveHeightM?.toDoubleOrNull()?.let { "${it}m" } ?: "정보 없음"

        holder.tvWindWave.text =
            "풍향: ${item.windDir} · 풍속: ${item.windSpeedMs}m/s · 파고: $waveText"

        holder.tvDust.text =
            "PM10: ${item.pm10S}(${item.pm10}) · PM2.5: ${item.pm25S}(${item.pm25})"
    }

    override fun getItemCount(): Int = items.size

    fun updateWeather(newItems: List<Weather6hItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
