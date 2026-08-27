package com.arena.marketradar.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.domain.report.ReportGenerator

/** Runs once a day (when enabled): builds a short daily report and posts a summary notification. */
class DailyReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val app = applicationContext as MarketRadarApplication
            if (!app.settings.dailyReport.value) return Result.success()

            val prices = app.market.getMarkets()
            val lang = app.settings.language.value
            val report = ReportGenerator.generate(app, prices, emptyList(), lang)

            val top = prices.sortedByDescending { kotlin.math.abs(it.changePercent24h ?: 0.0) }.take(3)
            val lines = top.joinToString("\n") { p ->
                val delta = p.changePercent24h ?: 0.0
                "${p.nameFa}: ${if (delta >= 0) "+" else ""}${"%.1f".format(delta)}%"
            }

            val text = if (lang == "fa")
                "گزارش روزانه آماده است.\n$lines${if (report != null) "\n(PDF در حافظهٔ گزارش ذخیره شد)" else ""}"
            else
                "Daily report ready.\n$lines${if (report != null) "\n(PDF saved to the reports folder)" else ""}"

            app.notifications.showSignal("رصد بازار", text, "daily".hashCode())
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
