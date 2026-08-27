package com.arena.marketradar.data.remote

import com.arena.marketradar.data.model.BahaItem
import com.arena.marketradar.data.model.CoinMarket
import com.arena.marketradar.data.model.FrankfurterHistory
import com.arena.marketradar.data.model.FrankfurterRate
import com.arena.marketradar.data.model.GoldApi
import com.arena.marketradar.data.model.MarketChart
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** CoinGecko public API. */
interface CoingeckoApi {
    @GET("coins/markets")
    suspend fun markets(
        @Query("vs_currency") vs: String = "usd",
        @Query("ids") ids: String,
        @Query("price_change_percentage") pcp: String = "24h",
    ): List<CoinMarket>

    @GET("coins/{id}/market_chart")
    suspend fun marketChart(
        @Path("id") id: String,
        @Query("vs_currency") vs: String = "usd",
        @Query("days") days: Int = 30,
    ): MarketChart
}

/** Frankfurter ECB reference rates (global fiat). */
interface FrankfurterApi {
    @GET("latest")
    suspend fun latest(
        @Query("from") from: String = "USD",
        @Query("to") to: String,
    ): FrankfurterRate

    @GET("{start}..{end}")
    suspend fun history(
        @Path("start") start: String,
        @Path("end") end: String,
        @Query("from") from: String = "USD",
        @Query("to") to: String,
    ): FrankfurterHistory
}

/** gold-api.com free spot prices. */
interface GoldApi {
    @GET("price/{symbol}")
    suspend fun spot(@Path("symbol") symbol: String): GoldApi
}

/** baha24.com Iran market prices. */
interface BahaApi {
    @GET("api/v1/price")
    suspend fun all(): List<BahaItem>
}
