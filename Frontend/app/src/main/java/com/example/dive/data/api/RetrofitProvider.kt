package com.example.dive.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private val http by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.badatime.p-e.kr/ ") // 배포서버
//            .baseUrl("http://10.10.0.74:8080/") // 에뮬레이터→로컬서버
            .addConverterFactory(GsonConverterFactory.create())
            .client(http)
            .build()
            .create(ApiService::class.java)
    }
}