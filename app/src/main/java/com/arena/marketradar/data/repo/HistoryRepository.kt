package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.PricePoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Accumulates a rolling time-series of prices per symbol so the on-device
 * forecast engine can compute indicators even without a historical endpoint.
 * Points are persisted (JSON in SharedPreferences) so history survives restarts.
 */
class HistoryRepository(context: Context, private val maxPoints: Int = 400) {

    private val prefs = context.getSharedPreferences("marketradar_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cache = mutableMapOf<String, MutableList<PricePoint>>()

    fun append(symbol: String, value: Double, timestamp: Long = System.currentTimeMillis()) {
        val list = cache.getOrPut(symbol) { mutableListOf() }
        // Skip points too close together (avoid duplicates created by user refreshes)
        if (list.isNotEmpty() && timestamp - list.last().timestamp < 20_000) {
            list[list.lastIndex] = PricePoint(timestamp, value)
            return
        }
        list.add(PricePoint(timestamp, value))
        if (list.size > maxPoints) list.removeAt(0)
        persist(symbol, list)
    }

    fun history(symbol: String): List<PricePoint> {
        cache[symbol]?.let { return it.toList() }
        return load(symbol)
    }

    fun replace(symbol: String, points: List<PricePoint>) {
        val trimmed = points.takeLast(maxPoints).toMutableList()
        cache[symbol] = trimmed
        persist(symbol, trimmed)
    }

    fun values(symbol: String): List<Double> = history(symbol).map { it.value }

    // ---- persistence ----
    private fun persist(symbol: String, points: List<PricePoint>) {
        val json = gson.toJson(points)
        prefs.edit().putString("h_$symbol", json).apply()
    }

    private fun load(symbol: String): List<PricePoint> {
        val json = prefs.getString("h_$symbol", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PricePoint>>() {}.type
            val list: MutableList<PricePoint> = gson.fromJson(json, type) ?: mutableListOf()
            cache[symbol] = list
            list.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
