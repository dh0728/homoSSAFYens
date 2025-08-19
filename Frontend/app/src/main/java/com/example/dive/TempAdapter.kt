package com.example.dive

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.TempData

class TempAdapter(private var items: List<TempData>) :
    RecyclerView.Adapter<TempAdapter.TempViewHolder>() {

    class TempViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvObsName: TextView = itemView.findViewById(R.id.tvObsName)
        val tvObsWt: TextView = itemView.findViewById(R.id.tvObsWt)
        val tvObsDt: TextView = itemView.findViewById(R.id.tvObsDt)
        val tvObsTime: TextView = itemView.findViewById(R.id.tvObsTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TempViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_temp, parent, false)
        return TempViewHolder(view)
    }

    override fun onBindViewHolder(holder: TempViewHolder, position: Int) {
        val item = items[position]

        holder.tvObsName.text = item.obsName
        holder.tvObsWt.text = "수온: ${item.obsWt} ℃"
        holder.tvObsDt.text = "수심: ${item.obsDt} m"
        holder.tvObsTime.text = "관측시각: ${item.obsTime}"

        // 수온 강조 색상
        when {
            item.obsWt >= 24 -> holder.tvObsWt.setTextColor(Color.RED)   // 따뜻 → 빨강
            item.obsWt <= 18 -> holder.tvObsWt.setTextColor(Color.BLUE)  // 차가움 → 파랑
            else -> holder.tvObsWt.setTextColor(Color.BLACK)             // 중간 → 검정
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TempData>) {
        items = newItems
        notifyDataSetChanged()
    }
}
