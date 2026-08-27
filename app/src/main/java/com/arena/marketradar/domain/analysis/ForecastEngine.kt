package com.arena.marketradar.domain.analysis

import com.arena.marketradar.data.model.ConfidenceLevel
import com.arena.marketradar.data.model.ForecastItem
import com.arena.marketradar.data.model.Signal
import kotlin.math.abs
import kotlin.math.min

/**
 * Combines technical indicators and news sentiment into a directional signal,
 * a probability and an expected move. This is an *estimate*, never a guarantee.
 */
class ForecastEngine {

    fun forecast(
        symbol: String,
        nameFa: String,
        nameEn: String,
        values: List<Double>,
        sentiment: Double,
    ): ForecastItem {
        val dataPoints = values.size
        val price = values.lastOrNull() ?: 0.0
        val vol = TechnicalIndicators.volatility(values)
        val factors = mutableListOf<String>()

        // --- Technical components (each contributes a score in [-1,1]) ---
        val sma5 = TechnicalIndicators.sma(values, 5)
        val sma20 = TechnicalIndicators.sma(values, 20)
        val sma50 = TechnicalIndicators.sma(values, 50)
        val rsi = TechnicalIndicators.rsi(values)
        val macd = TechnicalIndicators.macd(values)
        val momentum = TechnicalIndicators.momentum(values, 5)
        val boll = TechnicalIndicators.bollinger(values, 20, 2.0)
        val pct = TechnicalIndicators.pctChange(values, 24)

        var score = 0.0
        var weightSum = 0.0

        fun add(s: Double, w: Double, reason: String) {
            score += s * w
            weightSum += w
            if (abs(s) > 0.2) factors.add(reason)
        }

        // Trend: price vs SMA20 and SMA5 vs SMA20
        if (sma20 != null && price > 0) {
            val t = ((price - sma20) / sma20).coerceIn(-0.1, 0.1) / 0.1
            add(t, 1.0, "قیمت بالای میانگین ۲۰ ➜ روند صعودی")
        }
        if (sma5 != null && sma20 != null) {
            val cross = (sma5 - sma20) / sma20.coerceAtLeast(1e-9) * 100.0
            val s = cross.coerceIn(-3.0, 3.0) / 3.0
            add(s, 0.8, "میانگین کوتاه‌مدت بالای بلندمدت ➜ مثبت")
        }
        if (sma50 != null && sma20 != null) {
            val s = if (sma20 > sma50) 0.4 else -0.4
            add(s, 0.4, "میانگین ۲۰ نسبت به ۵۰ ➜ ${if (s > 0) "صعودی" else "نزولی"}")
        }

        // RSI
        if (rsi != null) {
            val s = when {
                rsi >= 70 -> -( (rsi - 70) / 30 )  // overbought → bearish pullback
                rsi <= 30 -> ( (30 - rsi) / 30 )    // oversold → bullish bounce
                else -> (rsi - 50) / 50.0           // momentum between 50-70 → bullish
            }
            add(s.coerceIn(-1.0, 1.0), 0.7, "RSI=${rsi.toInt()} ${when { rsi >= 70 -> "بیش‌خرید"; rsi <= 30 -> "بیش‌فروش"; rsi > 50 -> "مثبت"; else -> "منفی" }}")
        }

        // MACD histogram
        val hist = macd.third
        if (hist != null) {
            val h = hist / (price.coerceAtLeast(1e-9)) * 100.0
            val s = h.coerceIn(-2.0, 2.0) / 2.0
            add(s, 0.7, "هیستوگرام MACD ${if (h >= 0) "مثبت" else "منفی"}")
        }

        // Momentum
        if (momentum != null && price > 0) {
            val s = (momentum / price).coerceIn(-0.06, 0.06) / 0.06
            add(s, 0.6, "تکانه ${if (s >= 0) "صعودی" else "نزولی"}")
        }

        // Bollinger position
        val (upper, mid, _) = boll
        if (upper != null && mid != null && price > 0) {
            val denom = (upper - mid).coerceAtLeast(1e-9)
            val pos = (price - mid) / denom
            val s = pos.coerceIn(-1.0, 1.0)
            add(s, 0.3, "موقعیت باند بولینگر")
        }

        // Sentiment
        add(sentiment, 0.9, "احساسات خبری ${sentimentLabel(sentiment)}")

        val normalised = if (weightSum > 0) score / weightSum else 0.0

        val signal = when {
            normalised > 0.12 -> Signal.BULLISH
            normalised < -0.12 -> Signal.BEARISH
            else -> Signal.NEUTRAL
        }

        // Probability: map score to a percentage (50% = neutral midpoint).
        val probability = (50 + normalised * 42).toInt().coerceIn(8, 92)

        // Expected move sized by volatility.
        val horizon = "72h"
        val expectedMove = normalised * vol * 6.0 * 100.0

        // Confidence based on data availability & indicator agreement.
        val confidence = confidenceOf(dataPoints, abs(normalised))

        // Summary text.
        val summaryFa = buildString {
            append("بر اساس ")
            append(if (dataPoints >= 30) "تحلیل تکنیکال و احساسات خبری" else "دادهٔ محدود فعلی")
            append("، سیگنال «")
            append(signalLabel(signal))
            append("» با احتمال حدود ")
            append(probability)
            append("٪")
        }
        val summaryEn = buildString {
            append("Based on ")
            append(if (dataPoints >= 30) "technical analysis and news sentiment" else "limited current data")
            append(", the signal is \"")
            append(signalLabel(signal, en = true))
            append("\" with ~")
            append(probability)
            append("% probability")
        }

        return ForecastItem(
            symbol = symbol,
            nameFa = nameFa,
            nameEn = nameEn,
            signal = signal,
            probability = probability,
            horizon = horizon,
            expectedMovePercent = expectedMove,
            confidence = confidence,
            summaryFa = summaryFa,
            summaryEn = summaryEn,
            factors = factors.distinct().take(5),
            dataPoints = dataPoints,
            sentiment = sentiment,
        )
    }

    private fun confidenceOf(points: Int, conviction: Double): ConfidenceLevel = when {
        points >= 30 && conviction > 0.25 -> ConfidenceLevel.HIGH
        points >= 15 && conviction > 0.12 -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }

    private fun signalLabel(signal: Signal, en: Boolean = false): String = when (signal) {
        Signal.BULLISH -> if (en) "Bullish" else "صعودی"
        Signal.BEARISH -> if (en) "Bearish" else "نزولی"
        Signal.NEUTRAL -> if (en) "Neutral" else "خنثی"
    }

    private fun sentimentLabel(s: Double): String = when {
        s > 0.15 -> "مثبت"
        s < -0.15 -> "منفی"
        else -> "خنثی"
    }
}
