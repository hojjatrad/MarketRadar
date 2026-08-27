package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.AlertCondition
import com.arena.marketradar.data.model.AlertItem
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.PriceUnit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/** Stores user price alerts and detects which have triggered (incl. crossings). */
class AlertRepository(context: Context) {

    private val prefs = context.getSharedPreferences("marketradar_alerts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _alerts = MutableStateFlow(load())
    val alerts: StateFlow<List<AlertItem>> = _alerts

    fun add(symbol: String, nameFa: String, condition: AlertCondition, target: Double, unit: PriceUnit) {
        val item = AlertItem(id = UUID.randomUUID().toString(), symbol = symbol, nameFa = nameFa,
            condition = condition, targetPrice = target, unit = unit, enabled = true)
        _alerts.value = (_alerts.value + item).distinctBy { it.id + it.symbol + it.condition + it.targetPrice }
        persist()
    }

    fun remove(id: String) {
        _alerts.value = _alerts.value.filterNot { it.id == id }
        persist()
    }

    fun toggle(id: String, enabled: Boolean) {
        _alerts.value = _alerts.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        persist()
    }

    /** Returns alerts that triggered against the latest live prices. */
    fun triggered(prices: List<MarketPrice>): List<AlertItem> {
        val out = mutableListOf<AlertItem>()
        var changed = false
        val updated = _alerts.value.map { alert ->
            if (!alert.enabled) return@map alert
            val current = prices.firstOrNull { it.symbol == alert.symbol } ?: return@map alert
            val prev = alert.lastPrice ?: current.price
            val hit = when (alert.condition) {
                AlertCondition.ABOVE -> current.price >= alert.targetPrice
                AlertCondition.BELOW -> current.price <= alert.targetPrice
                AlertCondition.CROSS_ABOVE -> prev < alert.targetPrice && current.price >= alert.targetPrice
                AlertCondition.CROSS_BELOW -> prev > alert.targetPrice && current.price <= alert.targetPrice
            }
            if (hit) { out.add(alert); changed = true }
            alert.copy(lastPrice = current.price)
        }
        if (changed) { _alerts.value = updated; persist() }
        return out
    }

    private fun load(): List<AlertItem> {
        val json = prefs.getString("alerts", null) ?: return emptyList()
        return try { val t = object : TypeToken<List<AlertItem>>() {}.type; val l: MutableList<AlertItem> = gson.fromJson(json, t) ?: mutableListOf(); l }
        catch (e: Exception) { emptyList() }
    }

    private fun persist() { prefs.edit().putString("alerts", gson.toJson(_alerts.value)).apply() }
}
