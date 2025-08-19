package com.example.dive.data.api

import FineDustResponse
import com.example.dive.data.model.FishingResponse
import com.example.dive.data.model.TempResponse
import com.example.dive.data.model.TideResponse
import com.example.dive.data.model.TideWeeklyResponse
import com.example.dive.data.model.Weather6hResponse
import com.example.dive.data.model.Weather7dResponse
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Query

interface ApiService {

    // 하루치 물때
    @GET("api/v1/tide/today")
    fun getTodayTide(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<TideResponse>

    // 주간 물때
    @GET("api/v1/tide/weekly")
    fun getWeeklyTide(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<TideWeeklyResponse>

    // 현재 날씨 6시간
    @GET("api/v1/current/weather/6hour")
    fun getWeather6h(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<Weather6hResponse>

    // 일주일 날씨
    @GET("api/v1/forecast/7days")
    fun getWeather7d(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<Weather7dResponse>

    // 수온
    @GET("api/v1/temp/all")
    fun getAllTemps(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<TempResponse>

    // 낚시 포인트
    @GET("api/v1/point/list")
    fun getFishingPoints(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<FishingResponse>

    // 미세먼지
    @GET("/api/v1/air/fine_dust")
    fun getFineDust(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<FineDustResponse>
}