package com.example.dive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.FishingPoint

class FishingPointAdapter(private var items: List<FishingPoint>) :
    RecyclerView.Adapter<FishingPointAdapter.PointViewHolder>() {

    class PointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPointName: TextView = itemView.findViewById(R.id.tvPointName)
        val tvDepth: TextView = itemView.findViewById(R.id.tvDepth)
        val tvMaterial: TextView = itemView.findViewById(R.id.tvMaterial)
        val tvFish: TextView = itemView.findViewById(R.id.tvFish)
        val tvAddr: TextView = itemView.findViewById(R.id.tvAddr)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fishing_point, parent, false)
        return PointViewHolder(view)
    }

    override fun onBindViewHolder(holder: PointViewHolder, position: Int) {
        val item = items[position]
        holder.tvPointName.text = item.pointName
        holder.tvDepth.text = "수심: ${item.depth.minM}m ~ ${item.depth.maxM}m"
        holder.tvMaterial.text = "지형: ${item.material} | 조류: ${item.tideTime}"

        // 타겟 어종 + 낚시 방법 요약
        val fishSummary = item.targetByFish.entries.joinToString("\n") { entry ->
            "${entry.key}: ${entry.value.joinToString(", ")}"
        }
        holder.tvFish.text = fishSummary
        holder.tvAddr.text = "주소: ${item.addr} (${item.pointDtKm}km)"
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<FishingPoint>) {
        items = newItems
        notifyDataSetChanged()
    }
}
