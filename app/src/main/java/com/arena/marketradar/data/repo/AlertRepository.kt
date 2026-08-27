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

/** Stores user price alerts and detects which have triggered. */
class AlertRepository(context: Context) {

    private val prefs = context.getSharedPreferences("marketradar_alerts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _alerts = MutableStateFlow(load())
    val alerts: StateFlow<List<AlertItem>> = _alerts

    fun add(symbol: String, nameFa: String, condition: AlertCondition, target: Double, unit: PriceUnit) {
        val item = AlertItem(
            id = UUID.randomUUID().toString(),
            symbol = symbol,
            nameFa = nameFa,
            condition = condition,
            targetPrice = target,
            unit = unit,
            enabled = true,
        )
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

    /** Returns alerts that have triggered against the latest live prices. */
    fun triggered(prices: List<MarketPrice>): List<AlertItem> {
        val out = mutableListOf<AlertItem>()
        for (alert in _alerts.value) {
            if (!alert.enabled) continue
            val current = prices.firstOrNull { it.symbol == alert.symbol } ?: continue
            val hit = when (alert.condition) {
                AlertCondition.ABOVE -> current.price >= alert.targetPrice
                AlertCondition.BELOW -> current.price <= alert.targetPrice
            }
            if (hit) out.add(alert)
        }
        return out
    }

    private fun load(): List<AlertItem> {
        val json = prefs.getString("alerts", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AlertItem>>() {}.type
            val list: MutableList<AlertItem> = gson.fromJson(json, type) ?: mutableListOf()
            list
        } catch (e: Exception) { emptyList() }
    }

    private fun persist() {
        prefs.edit().putString("alerts", gson.toJson(_alerts.value)).apply()
    }
}
