package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.TideData

class WeeklyAdapter(private var items: List<TideData>) :
    RecyclerView.Adapter<WeeklyAdapter.WeeklyViewHolder>() {

    class WeeklyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvSunMoon: TextView = itemView.findViewById(R.id.tvSunMoon)
        val rvDayEvents: RecyclerView = itemView.findViewById(R.id.rvDayEvents)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeeklyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_day, parent, false)
        return WeeklyViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeeklyViewHolder, position: Int) {
        val item = items[position]

        holder.tvDate.text = "${item.date} (${item.weekday}) · 음력 ${item.lunar}"
        holder.tvLocation.text = "${item.locationName} · ${item.mul}"
        holder.tvSunMoon.text =
            "일출: ${item.sunrise} · 일몰: ${item.sunset}\n월출: ${item.moonrise ?: "-"} · 월몰: ${item.moonset ?: "-"}"

        // 하위 이벤트 리스트
        holder.rvDayEvents.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvDayEvents.adapter = TideEventAdapter(item.events)
    }

    override fun getItemCount(): Int = items.size

    fun updateWeekly(newItems: List<TideData>) {
        items = newItems
        notifyDataSetChanged()
    }
}
