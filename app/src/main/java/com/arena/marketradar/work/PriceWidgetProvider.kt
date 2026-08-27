package com.arena.marketradar.work

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.arena.marketradar.MainActivity
import com.arena.marketradar.R
import com.arena.marketradar.data.model.PriceUnit
import com.arena.marketradar.data.repo.MarketCache
import com.arena.marketradar.domain.util.Formatters

/** Multi-line home-screen widget showing key prices from the last cached data. */
class PriceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prices = MarketCache.load(context)
        val bySymbol = prices.associateBy { it.symbol }

        val usd = bySymbol["USD"]?.price
        val eur = bySymbol["EUR"]?.price
        val btc = bySymbol["BTC"]?.price
        val eth = bySymbol["ETH"]?.price
        val gold = bySymbol["XAU"]?.price
        val sekke = bySymbol["EMAMI1"]?.price

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_price).apply {
                setTextViewText(R.id.widget_line1, "دلار: ${fmt(usd, PriceUnit.TOMAN)}")
                setTextViewText(R.id.widget_line2, "یورو: ${fmt(eur, PriceUnit.TOMAN)}")
                setTextViewText(R.id.widget_line3, "بیت‌کوین: ${fmt(btc, PriceUnit.USD)}")
                setTextViewText(R.id.widget_line4, "اتریوم: ${fmt(eth, PriceUnit.USD)}")
                setTextViewText(R.id.widget_line5, "اونس طلا: ${fmt(gold, PriceUnit.OUNCE)}")
                setTextViewText(R.id.widget_line6, "سکه امامی: ${fmt(sekke, PriceUnit.COIN)}")

                val intent = Intent(context, MainActivity::class.java)
                val pi = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                setOnClickPendingIntent(R.id.widget_title, pi)
                setOnClickPendingIntent(R.id.widget_line1, pi)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun fmt(v: Double?, unit: PriceUnit): String =
        if (v != null) Formatters.money(v, unit) else "—"
}
