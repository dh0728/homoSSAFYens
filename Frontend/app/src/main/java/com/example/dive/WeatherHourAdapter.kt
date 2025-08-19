package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.WeatherHour

// 일주일치 날씨 시간대
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
        val time = item.time.substring(11, 16) // HH:mm 추출

        holder.tvTime.text = time
        holder.tvSkyTemp.text = "${item.sky} · ${item.temp}℃"
        holder.tvWindWave.text =
            "풍향: ${item.winddir} · 풍속: ${item.windspd}m/s · 파고: ${item.waveHt}m"
        holder.tvHumidityRain.text =
            "습도: ${item.humidity}% · 강수: ${item.rain}% (${item.rainAmt}mm)"
    }

    override fun getItemCount(): Int = items.size
}
