package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.PaperTrade
import com.arena.marketradar.data.model.PaperTradeRow
import com.arena.marketradar.data.model.PriceUnit
import com.arena.marketradar.data.model.TradeSide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A risk-free paper-trading simulator. User starts with a virtual balance and
 * opens BUY/SELL trades at the current market price; unrealized P&L is computed
 * live against current prices.
 */
class PaperTradingRepository(context: Context) {

    private val prefs = context.getSharedPreferences("marketradar_paper", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _balance = MutableStateFlow(prefs.getFloat("balance", 100000f).toDouble())
    val balance: StateFlow<Double> = _balance

    private val _trades = MutableStateFlow(load())
    val trades: StateFlow<List<PaperTrade>> = _trades

    fun reset(balance: Double = 100000.0) {
        _balance.value = balance
        _trades.value = emptyList()
        persistBalance(); persist()
    }

    fun trade(symbol: String, nameFa: String, unit: PriceUnit, side: TradeSide, qty: Double, price: Double) {
        if (qty <= 0) return
        _trades.value = _trades.value + PaperTrade(symbol = symbol, nameFa = nameFa, unit = unit, side = side, qty = qty, entryPrice = price)
        persist()
    }

    fun close(id: String) {
        _trades.value = _trades.value.filterNot { it.id == id }
        persist()
    }

    /** Live rows with unrealized P&L. */
    fun rows(prices: List<MarketPrice>): List<PaperTradeRow> {
        val bySymbol = prices.associateBy { it.symbol }
        return _trades.value.mapNotNull { t ->
            val cur = bySymbol[t.symbol] ?: return@mapNotNull null
            val direction = if (t.side == TradeSide.BUY) 1.0 else -1.0
            val pnl = direction * (cur.price - t.entryPrice) * t.qty
            val pct = if (t.entryPrice > 0) pnl / (t.entryPrice * t.qty) * 100.0 else 0.0
            PaperTradeRow(t, cur.price, pnl, pct)
        }
    }

    fun unrealizedPnl(rows: List<PaperTradeRow>): Double = rows.sumOf { it.pnl }

    private fun load(): List<PaperTrade> {
        val json = prefs.getString("trades", null) ?: return emptyList()
        return try { val t = object : TypeToken<List<PaperTrade>>() {}.type; gson.fromJson<List<PaperTrade>>(json, t) ?: emptyList() }
        catch (e: Exception) { emptyList() }
    }
    private fun persist() { prefs.edit().putString("trades", gson.toJson(_trades.value)).apply() }
    private fun persistBalance() { prefs.edit().putFloat("balance", _balance.value.toFloat()).apply() }
}
