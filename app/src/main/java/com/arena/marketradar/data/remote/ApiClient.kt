package com.arena.marketradar.data.remote

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "MarketRadar-Android/1.0")
                        .build()
                )
            }
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    val coingecko: CoingeckoApi by lazy {
        retrofit("https://api.coingecko.com/api/v3/").create(CoingeckoApi::class.java)
    }
    val frankfurter: FrankfurterApi by lazy {
        retrofit("https://api.frankfurter.app/").create(FrankfurterApi::class.java)
    }
    val gold: GoldApi by lazy {
        retrofit("https://api.gold-api.com/").create(GoldApi::class.java)
    }
    val baha: BahaApi by lazy {
        retrofit("https://baha24.com/").create(BahaApi::class.java)
    }
}
