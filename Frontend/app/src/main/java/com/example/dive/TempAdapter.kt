package com.example.dive

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.model.TempData
import java.text.SimpleDateFormat
import java.util.*

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

        // ✅ 수온 (소수점 유지, 숫자 부분만 색상 적용)
        val tempValue = String.format("%.1f℃", item.obsWt) // 소수점 첫째자리까지
        val tempText = "수온: $tempValue"
        val spannable = SpannableString(tempText)

        val color = when {
            item.obsWt >= 24 -> Color.RED   // 따뜻
            item.obsWt <= 18 -> Color.BLUE  // 차가움
            else -> Color.BLACK             // 중간
        }

        spannable.setSpan(
            ForegroundColorSpan(color),
            tempText.indexOf(tempValue),
            tempText.indexOf(tempValue) + tempValue.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        holder.tvObsWt.text = spannable

        // ✅ 수심 (정수만 표시)
        holder.tvObsDt.text = "수심: ${item.obsDt.toInt()} m"

        // ✅ 관측시간 (직관적인 형식: "8월 21일 14시")
        holder.tvObsTime.text = "관측시각: ${formatTime(item.obsTime)}"

        // 관측소 이름
        holder.tvObsName.text = item.obsName
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<TempData>) {
        items = newItems
        notifyDataSetChanged()
    }

    // ✅ 시간 포맷 변환: "2025-08-21T14:00:00" → "8월 21일 14시"
    private fun formatTime(time: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
            val outputFormat = SimpleDateFormat("M월 d일 HH시", Locale.KOREA)
            val date = inputFormat.parse(time)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            time
        }
    }
}
