package com.arena.marketradar.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.arena.marketradar.MarketRadarApplication
import java.util.concurrent.TimeUnit
import kotlin.math.max

object AlertScheduler {

    private const val WORK_NAME = "marketradar_price_check"
    private const val DAILY_WORK = "marketradar_daily_report"

    /** Run only when on a network and battery is not critically low → saves battery. */
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun schedule(context: Context) {
        val app = context.applicationContext as? MarketRadarApplication
        val interval = max(15, app?.settings?.refreshMinutes?.value ?: 15)
        val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(interval.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)

        val daily = PeriodicWorkRequestBuilder<DailyReportWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.UPDATE, daily)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(DAILY_WORK)
    }
}
