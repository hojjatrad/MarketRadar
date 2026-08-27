package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.MarketScope
import com.arena.marketradar.data.model.PricePoint
import com.arena.marketradar.data.prefs.SettingsRepository
import com.arena.marketradar.data.remote.ApiClient
import com.arena.marketradar.domain.util.Constants
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches live prices from every public, no-key source. In offline mode it
 * returns the last cached prices. Sources: baha24 (Iran), gold-api (XAU),
 * CoinGecko (crypto).
 */
class MarketRepository(
    private val history: HistoryRepository,
    private val settings: SettingsRepository,
    private val context: Context? = null,
) {

    var tomanPerUsd: Double = 0.0
        private set

    suspend fun getMarkets(): List<MarketPrice> {
        if (settings.offline.value) {
            val cached = loadCache()
            if (cached.isNotEmpty()) return cached
        }
        return try {
            val list = fetchLive()
            saveCache(list)
            list
        } catch (e: Exception) {
            loadCache().ifEmpty { throw e }
        }
    }

    private suspend fun fetchLive(): List<MarketPrice> = coroutineScope {
        val bahaDeferred = async { safe { ApiClient.baha.all() } }
        val goldDeferred = async { safe { ApiClient.gold.spot("XAU") } }
        val cryptoDeferred = async { safe { ApiClient.coingecko.markets(ids = Constants.CRYPTO_IDS.joinToString(",")) } }

        val bahaItems = bahaDeferred.await() ?: emptyList()
        val gold = goldDeferred.await()?.price
        val coins = cryptoDeferred.await() ?: emptyList()

        tomanPerUsd = bahaItems.firstOrNull { it.symbol == "USDT" }?.sell
            ?: bahaItems.firstOrNull { it.symbol == "USD" }?.sell
            ?: 0.0

        val list = mutableListOf<MarketPrice>()

        for (item in bahaItems) {
            val def = Constants.ASSETS.firstOrNull { it.symbol == item.symbol && it.scope == MarketScope.IRAN } ?: continue
            val value = item.sell ?: 0.0
            if (value <= 0.0) continue
            list += MarketPrice(def.symbol, def.nameFa, def.nameEn, def.type, def.scope, def.unit, value,
                timestamp = System.currentTimeMillis(), source = "baha24.com")
            history.append(def.symbol, value)
        }

        if (gold != null && gold > 0) {
            val def = Constants.ASSETS.first { it.symbol == "XAU" }
            list += MarketPrice(def.symbol, def.nameFa, def.nameEn, def.type, def.scope, def.unit, gold,
                priceUsd = gold, timestamp = System.currentTimeMillis(), source = "gold-api.com")
            history.append(def.symbol, gold)
        }

        for (coin in coins) {
            val def = Constants.ASSETS.firstOrNull { it.coingeckoId == coin.id } ?: continue
            val p = coin.currentPrice ?: 0.0
            list += MarketPrice(def.symbol, def.nameFa, coin.name.ifBlank { def.nameEn }, def.type, def.scope, def.unit, p,
                priceUsd = p, change24h = coin.priceChange24h, changePercent24h = coin.priceChangePct24h,
                high24h = coin.high24h, low24h = coin.low24h, iconUrl = coin.image,
                timestamp = System.currentTimeMillis(), source = "CoinGecko")
            history.append(def.symbol, p)
        }
        list
    }

    suspend fun getHistory(symbol: String): List<PricePoint> {
        val def = Constants.ASSETS.firstOrNull { it.symbol == symbol }
        val coinId = def?.coingeckoId
        if (coinId != null && coinId in Constants.DEEP_HISTORY_COINS) {
            try {
                val chart = ApiClient.coingecko.marketChart(id = coinId, days = 30)
                val points = (chart.prices ?: emptyList()).mapNotNull { pair ->
                    if (pair.size >= 2) PricePoint(pair[0].toLong(), pair[1]) else null
                }
                if (points.isNotEmpty()) { history.replace(symbol, points); return points }
            } catch (_: Exception) {}
        }
        return history.history(symbol)
    }

    fun localHistory(symbol: String): List<PricePoint> = history.history(symbol)

    private fun saveCache(list: List<MarketPrice>) { context?.let { MarketCache.save(it, list) } }
    private fun loadCache(): List<MarketPrice> = context?.let { MarketCache.load(it) } ?: emptyList()

    private suspend fun <T> safe(block: suspend () -> T): T? =
        try { block() } catch (e: Exception) { null }
}
