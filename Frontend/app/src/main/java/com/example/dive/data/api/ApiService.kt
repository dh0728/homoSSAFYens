package com.example.dive.data.api

import com.example.dive.data.model.TideResponse
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/tide/today")
    fun getTodayTide(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<TideResponse>

}