package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.WeatherDay

class WeatherDayAdapter(private var items: List<WeatherDay>) :
    RecyclerView.Adapter<WeatherDayAdapter.DayViewHolder>() {

    // 날짜별 펼침 여부 저장
    private val expandedState = mutableSetOf<Int>()

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDayDate)
        val tvToggle: TextView = itemView.findViewById(R.id.tvToggle)
        val rvHours: RecyclerView = itemView.findViewById(R.id.rvDayHours)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = items[position]
        holder.tvDate.text = day.date

        // 시간별 RecyclerView
        holder.rvHours.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvHours.adapter = WeatherHourAdapter(day.hours)

        // 현재 펼침 여부
        val isExpanded = expandedState.contains(position)
        holder.rvHours.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.tvToggle.text = if (isExpanded) "▲" else "▼"

        // 클릭 시 토글
        holder.itemView.setOnClickListener {
            if (isExpanded) {
                expandedState.remove(position)
            } else {
                expandedState.add(position)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateDays(newItems: List<WeatherDay>) {
        items = newItems
        expandedState.clear()
        notifyDataSetChanged()
    }
}
