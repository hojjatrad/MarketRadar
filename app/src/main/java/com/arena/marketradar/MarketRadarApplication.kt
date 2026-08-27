package com.arena.marketradar

import android.app.Application
import com.arena.marketradar.data.prefs.SettingsRepository
import com.arena.marketradar.data.repo.AlertRepository
import com.arena.marketradar.data.repo.HistoryRepository
import com.arena.marketradar.data.repo.MarketRepository
import com.arena.marketradar.data.repo.NewsRepository
import com.arena.marketradar.data.repo.PortfolioRepository
import com.arena.marketradar.domain.analysis.ForecastEngine
import com.arena.marketradar.domain.analysis.SentimentAnalyzer
import com.arena.marketradar.work.AlertScheduler
import com.arena.marketradar.work.NotificationHelper

class MarketRadarApplication : Application() {

    lateinit var settings: SettingsRepository
        private set
    lateinit var history: HistoryRepository
        private set
    lateinit var market: MarketRepository
        private set
    lateinit var sentiment: SentimentAnalyzer
        private set
    lateinit var news: NewsRepository
        private set
    lateinit var alerts: AlertRepository
        private set
    lateinit var portfolio: PortfolioRepository
        private set
    lateinit var engine: ForecastEngine
        private set
    lateinit var notifications: NotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        com.arena.marketradar.domain.util.Formatters.usePersianDigits = settings.persianDigits.value
        history = HistoryRepository(this)
        market = MarketRepository(history, settings, applicationContext)
        sentiment = SentimentAnalyzer()
        news = NewsRepository(sentiment)
        alerts = AlertRepository(this)
        portfolio = PortfolioRepository(this)
        engine = ForecastEngine()
        notifications = NotificationHelper(this)
        AlertScheduler.schedule(this)
    }

    companion object {
        /** Non-composable handle to the Application (safe to use outside Compose). */
        @JvmStatic
        lateinit var instance: MarketRadarApplication
            private set
    }
}
