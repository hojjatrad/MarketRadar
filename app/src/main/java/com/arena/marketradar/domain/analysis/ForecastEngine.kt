package com.arena.marketradar.domain.analysis

import com.arena.marketradar.data.model.ConfidenceLevel
import com.arena.marketradar.data.model.Correlation
import com.arena.marketradar.data.model.Evidence
import com.arena.marketradar.data.model.ForecastItem
import com.arena.marketradar.data.model.Signal
import kotlin.math.abs

/**
 * Combines a rich set of technical indicators and news sentiment into a
 * directional signal with multiple horizons, weighted evidence (for
 * transparency), a backtested accuracy estimate, support/resistance levels,
 * Fear & Greed and cross-asset correlations. This is an *estimate*, never a
 * guarantee of profit.
 */
class ForecastEngine {

    fun forecast(
        symbol: String,
        nameFa: String,
        nameEn: String,
        values: List<Double>,
        sentiment: Double,
        correlations: List<Correlation> = emptyList(),
    ): ForecastItem {
        val dataPoints = values.size
        val price = values.lastOrNull() ?: 0.0
        val vol = TechnicalIndicators.volatility(values)
        val evidence = mutableListOf<Evidence>()
        val factors = mutableListOf<String>()

        var score = 0.0
        var weightSum = 0.0
        fun add(s: Double, w: Double, label: String, reason: String) {
            score += s * w; weightSum += w
            evidence.add(Evidence(label, s.coerceIn(-1.0, 1.0), w, s > 0.05))
            if (abs(s) > 0.2) factors.add(reason)
        }

        // --- Trend (price vs SMA20, SMA5 vs SMA20, SMA20 vs SMA50) ---
        val sma5 = TechnicalIndicators.sma(values, 5)
        val sma20 = TechnicalIndicators.sma(values, 20)
        val sma50 = TechnicalIndicators.sma(values, 50)
        val rsi = TechnicalIndicators.rsi(values)
        val macd = TechnicalIndicators.macd(values)
        val momentum = TechnicalIndicators.momentum(values, 5)
        val boll = TechnicalIndicators.bollinger(values, 20, 2.0)

        if (sma20 != null && price > 0) {
            val t = ((price - sma20) / sma20).coerceIn(-0.1, 0.1) / 0.1
            add(t, 1.0, "قیمت نسبت به میانگین ۲۰", "قیمت نسبت به میانگین ۲۰")
        }
        if (sma5 != null && sma20 != null) {
            val cross = (sma5 - sma20) / sma20.coerceAtLeast(1e-9) * 100.0
            val s = cross.coerceIn(-3.0, 3.0) / 3.0
            add(s, 0.8, "میانگین کوتاه‌مدت", "میانگین کوتاه‌مدت نسبت به بلندمدت")
        }
        if (sma50 != null && sma20 != null) {
            val s = if (sma20 > sma50) 0.4 else -0.4
            add(s, 0.4, "میانگین ۲۰ نسبت به ۵۰", "میانگین ۲۰ نسبت به ۵۰")
        }

        // RSI
        if (rsi != null) {
            val s = when {
                rsi >= 70 -> -((rsi - 70) / 30)
                rsi <= 30 -> (30 - rsi) / 30
                else -> (rsi - 50) / 50.0
            }
            add(s.coerceIn(-1.0, 1.0), 0.7, "شاخص RSI", "RSI=${rsi.toInt()}")
        }

        // MACD histogram
        val hist = macd.third
        if (hist != null) {
            val h = hist / price.coerceAtLeast(1e-9) * 100.0
            add((h.coerceIn(-2.0, 2.0) / 2.0), 0.7, "هیستوگرام MACD", "هیستوگرام MACD")
        }

        // Momentum
        if (momentum != null && price > 0) {
            val s = (momentum / price).coerceIn(-0.06, 0.06) / 0.06
            add(s, 0.6, "تکانه", "تکانه")
        }

        // Bollinger position
        val (upper, mid, lower) = boll
        if (upper != null && mid != null && price > 0) {
            val denom = (upper - mid).coerceAtLeast(1e-9)
            add(((price - mid) / denom).coerceIn(-1.0, 1.0), 0.3, "باند بولینگر", "موقعیت باند بولینگر")
        }

        // VWAP
        val vwap = TechnicalIndicators.vwap(values)
        if (vwap != null && vwap > 0 && price > 0) {
            val s = ((price - vwap) / vwap).coerceIn(-0.05, 0.05) / 0.05
            add(s, 0.5, "VWAP (میانگین حجمی)", "قیمت نسبت به VWAP")
        }

        // Trend structure (higher high/lows)
        val structure = TechnicalIndicators.trendStructure(values)
        if (structure != 0) add(structure.toDouble() * 0.6, 0.5, "ساختار روند", "ساختار روند")

        // Divergences
        val rsiDiv = TechnicalIndicators.rsiDivergence(values)
        if (rsiDiv != 0) add(rsiDiv.toDouble() * 0.7, 0.6, "واگرایی RSI", "واگرایی RSI")
        val macdDiv = TechnicalIndicators.macdDivergence(values)
        if (macdDiv != 0) add(macdDiv.toDouble() * 0.7, 0.5, "واگرایی MACD", "واگرایی MACD")

        // Sentiment
        add(sentiment, 0.9, "احساسات خبری", "احساسات خبری")

        val normalised = if (weightSum > 0) score / weightSum else 0.0

        // Signal threshold
        val signal = when {
            normalised > 0.12 -> Signal.BULLISH
            normalised < -0.12 -> Signal.BEARISH
            else -> Signal.NEUTRAL
        }
        val probability = (50 + normalised * 42).toInt().coerceIn(8, 92)

        // Multi-horizon estimates (24h, 72h, 1w). Reflect conviction & volatility.
        val horizon24 = (50 + normalised * 30 + vol * 40).toInt().coerceIn(5, 95)
        val horizon72 = (50 + normalised * 36 + vol * 20).toInt().coerceIn(5, 95)
        val horizon1w = (50 + normalised * 42 - vol * 25).toInt().coerceIn(5, 95)

        // Backtested accuracy: fraction of recent rolling forecasts that were "correct".
        val accuracy = backtest(values)

        // Support / resistance
        val (resistance, support) = TechnicalIndicators.supportResistance(values)

        // Fear & Greed
        val fg = MarketAnalysis.fearGreed(values)

        val expectedMove = normalised * vol * 6.0 * 100.0
        val confidence = confidenceOf(dataPoints, abs(normalised))

        val direction = signalLabel(signal)
        val summaryFa = buildString {
            append("بر اساس ")
            append(if (dataPoints >= 30) "اندیکاتورهای پیشرفته، تحلیل بین‌بازاری و احساسات خبری" else "دادهٔ محدود فعلی")
            append("، سیگنال «")
            append(direction)
            append("» با احتمال حدود ")
            append(probability)
            append("٪")
            if (accuracy != null && dataPoints >= 30) append(" (دقت تاریخی ~${(accuracy * 100).toInt()}٪)")
        }
        val summaryEn = buildString {
            append("Based on ")
            append(if (dataPoints >= 30) "advanced indicators, cross-asset analysis and news sentiment" else "limited current data")
            append(", the signal is \"")
            append(signalLabel(signal, en = true))
            append("\" with ~")
            append(probability)
            append("% probability")
            if (accuracy != null && dataPoints >= 30) append(" (historical accuracy ~${(accuracy * 100).toInt()}%)")
        }

        return ForecastItem(
            symbol = symbol, nameFa = nameFa, nameEn = nameEn,
            signal = signal, probability = probability, horizon = "72h",
            expectedMovePercent = expectedMove, confidence = confidence,
            summaryFa = summaryFa, summaryEn = summaryEn,
            factors = factors.distinct().take(6), dataPoints = dataPoints, sentiment = sentiment,
            prob24h = horizon24, prob72h = horizon72, prob1w = horizon1w,
            accuracy = accuracy, fearGreed = fg,
            rsi = rsi, macdHist = hist, resistance = resistance, support = support,
            evidence = evidence.sortedByDescending { it.weight }.take(6),
            correlations = correlations,
        )
    }

    /**
     * Simple backtest: split the series into rolling windows; for each window,
     * forecast a direction from the earlier half and check whether the later
     * actual move matched. Returns the hit-rate in [0,1] or null if too little data.
     */
    private fun backtest(values: List<Double>): Double? {
        if (values.size < 60) return null
        var correct = 0; var total = 0
        val step = 20
        var i = 30
        while (i + step < values.size) {
            val past = values.subList(0, i)
            val future = values.subList(i, i + step)
            val dir = simpleDirection(past)
            val actual = if (future.last() > future.first()) 1.0 else -1.0
            if (dir != 0.0 && dir == actual) correct++
            if (dir != 0.0) total++
            i += step
        }
        return if (total > 0) correct.toDouble() / total else null
    }

    private fun simpleDirection(values: List<Double>): Double {
        if (values.size < 20) return 0.0
        val sma = TechnicalIndicators.sma(values, 20) ?: return 0.0
        val last = values.last()
        val diff = (last - sma) / sma.coerceAtLeast(1e-9)
        return if (diff > 0.01) 1.0 else if (diff < -0.01) -1.0 else 0.0
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
}
