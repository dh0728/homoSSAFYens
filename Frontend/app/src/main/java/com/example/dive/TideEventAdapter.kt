package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.TideEvent

class TideEventAdapter(private var items: List<TideEvent>) :
    RecyclerView.Adapter<TideEventAdapter.TideViewHolder>() {

    class TideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvLevel: TextView = itemView.findViewById(R.id.tvLevel)
        val tvDelta: TextView = itemView.findViewById(R.id.tvDelta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tide_event, parent, false)
        return TideViewHolder(view)
    }

    override fun onBindViewHolder(holder: TideViewHolder, position: Int) {
        val item = items[position]

        holder.tvTime.text = item.time
        holder.tvLevel.text = "${item.levelCm} cm (${item.trend})"

        // delta 표시 + 색상
        if (item.deltaCm >= 0) {
            holder.tvDelta.text = "+${item.deltaCm}"
            holder.tvDelta.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // 초록
        } else {
            holder.tvDelta.text = "${item.deltaCm}"
            holder.tvDelta.setTextColor(android.graphics.Color.RED) // 빨강
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateEvents(newItems: List<TideEvent>) {
        items = newItems
        notifyDataSetChanged()
    }
}
