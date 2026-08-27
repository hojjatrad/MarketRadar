package com.arena.marketradar.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.arena.marketradar.MainActivity
import com.arena.marketradar.R

class NotificationHelper(private val context: Context) {

    init { ensureChannel() }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CH_PRICE, "هشدار قیمت", NotificationManager.IMPORTANCE_HIGH)
            )
            manager.createNotificationChannel(
                NotificationChannel(CH_TREND, "هشدار روند/پیش‌بینی", NotificationManager.IMPORTANCE_HIGH)
            )
            manager.createNotificationChannel(
                NotificationChannel(CH_NEWS, "خبر مهم", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return true
    }

    private fun mainIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showPriceAlert(title: String, text: String, id: Int) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CH_PRICE)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun showSignal(title: String, text: String, id: Int) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CH_TREND)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun showNews(symbol: String, text: String, id: Int) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, CH_NEWS)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("خبر مهم • $symbol")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainIntent())
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CH_PRICE = "price_alerts"
        const val CH_TREND = "trend_alerts"
        const val CH_NEWS = "news_alerts"
    }
}
