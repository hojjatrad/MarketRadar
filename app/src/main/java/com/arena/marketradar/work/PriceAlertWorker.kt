package com.arena.marketradar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.Signal
import com.arena.marketradar.domain.util.Constants
import java.util.concurrent.TimeUnit

/**
 * Periodic background job: refreshes prices, fires price alerts and posts
 * "strong signal" / important-news notifications when the user has enabled them.
 */
class PriceAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val app = applicationContext as MarketRadarApplication
        val prices = app.market.getMarkets()
        val currentById = prices.associateBy { it.symbol }

        // 1) Fire price alerts
        for (alert in app.alerts.triggered(prices)) {
            val current = currentById[alert.symbol]?.price ?: 0.0
            app.notifications.showPriceAlert(
                "${alert.nameFa} • ${if (alert.condition.name == "ABOVE") "بالا" else "پایین"} از هدف",
                "قیمت فعلی: ${"%.0f".format(current)} تومان (هدف: ${"%.0f".format(alert.targetPrice)})",
                alert.id.hashCode()
            )
            app.alerts.toggle(alert.id, enabled = false)
        }

        // 2) Trend / forecast notifications for the watchlist
        if (app.settings.notifTrend.value) {
            val watch = app.settings.watchlist().ifEmpty { Constants.DEFAULT_WATCHLIST }
            for (symbol in watch) {
                val price = currentById[symbol] ?: continue
                val history = app.market.getHistory(symbol)
                val values = history.map { it.value }
                if (values.size < 5) continue
                val sentiment = app.sentiment.score("")
                val f = app.engine.forecast(
                    symbol, price.nameFa, price.nameEn, values, sentiment
                )
                val strong = f.signal != Signal.NEUTRAL && f.confidence.name != "LOW"
                if (strong && shouldNotify(symbol)) {
                    app.notifications.showSignal(
                        "${price.nameFa} — ${if (f.signal == Signal.BULLISH) "سیگنال صعودی" else "سیگنال نزولی"}",
                        "احتمال ~${f.probability}٪ • افق ${f.horizon} • ${f.summaryFa}",
                        symbol.hashCode()
                    )
                    markNotified(symbol)
                }
            }
        }

        // 3) Important news notifications (very negative/positive for a tracked asset)
        if (app.settings.notifNews.value) {
            val items = app.news.fetch().take(30)
            for (n in items) {
                if (kotlin.math.abs(n.sentiment) > 0.5 && n.assets.isNotEmpty()) {
                    app.notifications.showNews(n.assets.first(), n.title, n.title.hashCode())
                    break
                }
            }
        }

        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    private fun shouldNotify(symbol: String): Boolean {
        val last = prefs.getLong("last_signal_$symbol", 0L)
        return System.currentTimeMillis() - last > TimeUnit.HOURS.toMillis(6)
    }

    private fun markNotified(symbol: String) {
        prefs.edit().putLong("last_signal_$symbol", System.currentTimeMillis()).apply()
    }

    private val prefs: android.content.SharedPreferences
        get() = applicationContext.getSharedPreferences("marketradar_notif", Context.MODE_PRIVATE)
}
