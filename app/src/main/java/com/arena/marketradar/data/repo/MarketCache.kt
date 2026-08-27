package com.arena.marketradar.data.repo

import android.content.Context
import com.arena.marketradar.data.model.MarketPrice
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Shared, last-known-price cache read by the repo (offline) and the widget. */
object MarketCache {

    private const val PREFS = "marketradar_cache"
    private const val KEY = "prices"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, list: List<MarketPrice>) {
        try { prefs(context).edit().putString(KEY, Gson().toJson(list)).apply() } catch (_: Exception) {}
    }

    fun load(context: Context): List<MarketPrice> {
        return try {
            val json = prefs(context).getString(KEY, null) ?: return emptyList()
            val type = object : TypeToken<List<MarketPrice>>() {}.type
            Gson().fromJson<List<MarketPrice>>(json, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
