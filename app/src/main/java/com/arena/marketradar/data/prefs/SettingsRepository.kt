package com.arena.marketradar.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.arena.marketradar.data.model.PriceUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Holds all user preferences. Every setter persists and updates an in-memory StateFlow. */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("marketradar_settings", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, "fa") ?: "fa")
    val language: StateFlow<String> = _language

    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK, true))
    val darkMode: StateFlow<Boolean> = _darkMode

    private val _priceUnit = MutableStateFlow(
        PriceUnit.valueOf(prefs.getString(KEY_UNIT, PriceUnit.TOMAN.name) ?: PriceUnit.TOMAN.name)
    )
    val priceUnit: StateFlow<PriceUnit> = _priceUnit

    private val _refreshMinutes = MutableStateFlow(prefs.getInt(KEY_REFRESH, 5))
    val refreshMinutes: StateFlow<Int> = _refreshMinutes

    private val _notifPrice = MutableStateFlow(prefs.getBoolean(KEY_N_PRICE, true))
    val notifPrice: StateFlow<Boolean> = _notifPrice

    private val _notifTrend = MutableStateFlow(prefs.getBoolean(KEY_N_TREND, true))
    val notifTrend: StateFlow<Boolean> = _notifTrend

    private val _notifNews = MutableStateFlow(prefs.getBoolean(KEY_N_NEWS, false))
    val notifNews: StateFlow<Boolean> = _notifNews

    private val _persianDigits = MutableStateFlow(prefs.getBoolean(KEY_DIGITS, true))
    val persianDigits: StateFlow<Boolean> = _persianDigits

    private val _offline = MutableStateFlow(prefs.getBoolean(KEY_OFFLINE, false))
    val offline: StateFlow<Boolean> = _offline

    private val _dailyReport = MutableStateFlow(prefs.getBoolean(KEY_DAILY_REPORT, true))
    val dailyReport: StateFlow<Boolean> = _dailyReport

    fun setLanguage(v: String) { prefs.edit().putString(KEY_LANG, v).apply(); _language.value = v }
    fun setDarkMode(v: Boolean) { prefs.edit().putBoolean(KEY_DARK, v).apply(); _darkMode.value = v }
    fun setPriceUnit(v: PriceUnit) { prefs.edit().putString(KEY_UNIT, v.name).apply(); _priceUnit.value = v }
    fun setRefreshMinutes(v: Int) { prefs.edit().putInt(KEY_REFRESH, v).apply(); _refreshMinutes.value = v }
    fun setNotifPrice(v: Boolean) { prefs.edit().putBoolean(KEY_N_PRICE, v).apply(); _notifPrice.value = v }
    fun setNotifTrend(v: Boolean) { prefs.edit().putBoolean(KEY_N_TREND, v).apply(); _notifTrend.value = v }
    fun setNotifNews(v: Boolean) { prefs.edit().putBoolean(KEY_N_NEWS, v).apply(); _notifNews.value = v }
    fun setPersianDigits(v: Boolean) {
        prefs.edit().putBoolean(KEY_DIGITS, v).apply(); _persianDigits.value = v
        com.arena.marketradar.domain.util.Formatters.usePersianDigits = v
    }
    fun setOffline(v: Boolean) { prefs.edit().putBoolean(KEY_OFFLINE, v).apply(); _offline.value = v }
    fun setDailyReport(v: Boolean) { prefs.edit().putBoolean(KEY_DAILY_REPORT, v).apply(); _dailyReport.value = v }

    /** Live watchlist (the selected "universe") so the UI updates instantly. */
    private val _watchlist = MutableStateFlow(readWatchlist())
    val watchlistState: StateFlow<List<String>> = _watchlist

    fun watchlist(): List<String> = readWatchlist()

    fun saveWatchlist(list: List<String>) {
        prefs.edit().putString(KEY_WATCH, list.joinToString(",")).apply()
        _watchlist.value = list.distinct()
    }

    private fun readWatchlist(): List<String> =
        prefs.getString(KEY_WATCH, "")!!.split(",").filter { it.isNotBlank() }.distinct()

    private companion object {
        const val KEY_LANG = "lang"
        const val KEY_DARK = "dark"
        const val KEY_UNIT = "unit"
        const val KEY_REFRESH = "refresh_min"
        const val KEY_N_PRICE = "notif_price"
        const val KEY_N_TREND = "notif_trend"
        const val KEY_N_NEWS = "notif_news"
        const val KEY_DIGITS = "persian_digits"
        const val KEY_OFFLINE = "offline"
        const val KEY_DAILY_REPORT = "daily_report"
        const val KEY_WATCH = "watchlist"
    }
}
