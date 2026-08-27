package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.Holding
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.PortfolioRow
import com.arena.marketradar.data.model.PriceUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Persists holdings and computes live P&L against market prices. */
class PortfolioRepository(context: Context) {

    private val prefs = context.getSharedPreferences("marketradar_portfolio", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _holdings = MutableStateFlow(load())
    val holdings: StateFlow<List<Holding>> = _holdings

    fun add(symbol: String, nameFa: String, unit: PriceUnit, quantity: Double, buyPrice: Double) {
        _holdings.value = _holdings.value + Holding(symbol = symbol, nameFa = nameFa, unit = unit, quantity = quantity, buyPrice = buyPrice)
        persist()
    }

    fun remove(id: String) {
        _holdings.value = _holdings.value.filterNot { it.id == id }
        persist()
    }

    fun rows(prices: List<MarketPrice>): List<PortfolioRow> {
        val bySymbol = prices.associateBy { it.symbol }
        return _holdings.value.mapNotNull { h ->
            val cur = bySymbol[h.symbol] ?: return@mapNotNull null
            val cost = h.quantity * h.buyPrice
            val market = h.quantity * cur.price
            val pnl = market - cost
            val pct = if (cost > 0) pnl / cost * 100.0 else 0.0
            PortfolioRow(h, cur.price, cost, market, pnl, pct)
        }
    }

    private fun load(): List<Holding> {
        val json = prefs.getString("holdings", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Holding>>() {}.type
            val list: MutableList<Holding> = gson.fromJson(json, type) ?: mutableListOf()
            list
        } catch (e: Exception) { emptyList() }
    }

    private fun persist() {
        prefs.edit().putString("holdings", gson.toJson(_holdings.value)).apply()
    }
}
