package com.example.dive

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dive.data.api.RetrofitClient
import com.example.dive.data.model.FishingResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FishingFragment : Fragment() {

    private lateinit var tvIntroHeader: TextView
    private lateinit var layoutDetails: LinearLayout
    private lateinit var tvIntro: TextView
    private lateinit var tvForecast: TextView
    private lateinit var tvEbbf: TextView
    private lateinit var tvNotice: TextView
    private lateinit var rvPoints: RecyclerView
    private lateinit var adapter: FishingPointAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fishing, container, false)

        // View 초기화
        tvIntroHeader = view.findViewById(R.id.tvIntroHeader)
        layoutDetails = view.findViewById(R.id.layoutDetails)
        tvIntro = view.findViewById(R.id.tvIntro)
        tvForecast = view.findViewById(R.id.tvForecast)
        tvEbbf = view.findViewById(R.id.tvEbbf)
        tvNotice = view.findViewById(R.id.tvNotice)
        rvPoints = view.findViewById(R.id.rvPoints)

        rvPoints.layoutManager = LinearLayoutManager(requireContext())
        adapter = FishingPointAdapter(emptyList())
        rvPoints.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // ⬇️ 소개 헤더 클릭 시 상세 영역 토글
        tvIntroHeader.setOnClickListener {
            if (isExpanded) {
                layoutDetails.visibility = View.GONE
            } else {
                layoutDetails.visibility = View.VISIBLE
            }
            isExpanded = !isExpanded
        }

        loadFishingData()

        return view
    }

    // 🔹 Bold 적용해주는 함수
    private fun makeBoldLabel(label: String, content: String, emoji: String): SpannableString {
        val fullText = "$emoji $label: $content"
        val spannable = SpannableString(fullText)
        // label 부분만 bold 처리
        val start = fullText.indexOf(label)
        val end = start + label.length
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }


    private fun loadFishingData() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                300
            )
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    Log.d("Fishing", "lat=$lat, lon=$lon")

                    RetrofitClient.instance.getFishingPoints(lat, lon)
                        .enqueue(object : Callback<FishingResponse> {
                            override fun onResponse(
                                call: Call<FishingResponse>,
                                response: Response<FishingResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val body = response.body()?.data ?: return

                                    // Intro 처리
                                    val rawIntro = body.info.intro.trimStart('\n') // 앞 줄바꿈 제거
                                    val introLines = rawIntro.split("\n")
                                    val areaName = introLines[0]                       // "수영만 부근"
                                    val introText = introLines.drop(1).joinToString("\n") // 나머지 설명

                                    // 헤더에 지역명만
                                    tvIntroHeader.text = "▶️ $areaName 소개"
                                    // 본문 intro는 설명만
                                    tvIntro.text = introText

                                    // 예보, 조류, 주의사항 (이모지 + Bold)
                                    tvForecast.text = makeBoldLabel("예보 ", body.info.forecast, "🌤")
                                    tvEbbf.text = makeBoldLabel("조류 ", body.info.ebbf, "🌊")
                                    tvNotice.text = makeBoldLabel("주의사항 ", body.info.notice, "⚠️")

                                    // 포인트 리스트 채우기
                                    adapter.updateData(body.points)

                                } else {
                                    Log.e("Fishing", "응답 실패: ${response.code()}")
                                }
                            }

                            override fun onFailure(call: Call<FishingResponse>, t: Throwable) {
                                Log.e("Fishing", "요청 실패: ${t.message}")
                            }
                        })
                }
            }
    }
}
