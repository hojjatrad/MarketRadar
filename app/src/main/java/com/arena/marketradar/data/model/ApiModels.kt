package com.arena.marketradar.data.model

import com.google.gson.annotations.SerializedName

/** CoinGecko: /coins/markets */
data class CoinMarket(
    @SerializedName("id") val id: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String?,
    @SerializedName("current_price") val currentPrice: Double?,
    @SerializedName("high_24h") val high24h: Double?,
    @SerializedName("low_24h") val low24h: Double?,
    @SerializedName("price_change_24h") val priceChange24h: Double?,
    @SerializedName("price_change_percentage_24h") val priceChangePct24h: Double?,
    @SerializedName("market_cap") val marketCap: Double?,
)

/** CoinGecko: /coins/{id}/market_chart */
data class MarketChart(
    @SerializedName("prices") val prices: List<List<Double>>?,
    @SerializedName("market_caps") val marketCaps: List<List<Double>>?,
    @SerializedName("total_volumes") val totalVolumes: List<List<Double>>?,
)

/** Frankfurter: /latest?from=USD&to=USD,EUR,GBP */
data class FrankfurterRate(
    @SerializedName("amount") val amount: Double,
    @SerializedName("base") val base: String,
    @SerializedName("date") val date: String,
    @SerializedName("rates") val rates: Map<String, Double>,
)

/** Frankfurter: /{start}..{end}?from=USD&to=EUR */
data class FrankfurterHistory(
    @SerializedName("amount") val amount: Double,
    @SerializedName("base") val base: String,
    @SerializedName("start_date") val start: String,
    @SerializedName("end_date") val end: String,
    @SerializedName("rates") val rates: Map<String, Map<String, Double>>,
)

/** baha24.com: /api/v1/price   (Iran market, values mostly in Toman) */
data class BahaItem(
    @SerializedName("title") val title: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("sell") val sell: Double?,
    @SerializedName("buy") val buy: Double?,
    @SerializedName("last_update") val lastUpdate: String?,
)

/** gold-api.com: /price/XAU   (USD per troy ounce) */
data class GoldApi(
    @SerializedName("name") val name: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("updatedAt") val updatedAt: String?,
    @SerializedName("updatedAtReadable") val updatedAtReadable: String?,
)
