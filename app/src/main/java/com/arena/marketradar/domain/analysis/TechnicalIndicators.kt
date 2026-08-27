package com.arena.marketradar.domain.analysis

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Extended set of technical indicators computed on-device from a price series.
 * Beyond the classic MA/RSI/MACD/Bollinger we add VWAP, Fibonacci retracement,
 * support/resistance detection, RSI & MACD divergence and simple trend structure.
 */
object TechnicalIndicators {

    fun sma(values: List<Double>, period: Int): Double? {
        if (values.size < period) return null
        return values.takeLast(period).average()
    }

    fun ema(values: List<Double>, period: Int): Double? {
        if (values.isEmpty()) return null
        val k = 2.0 / (period + 1)
        var prev = values.first()
        for (i in 1 until values.size) prev = values[i] * k + prev * (1 - k)
        return prev
    }

    fun rsi(values: List<Double>, period: Int = 14): Double? {
        if (values.size < period + 1) return null
        var gain = 0.0; var loss = 0.0
        for (i in 1..period) { val d = values[i] - values[i - 1]; if (d > 0) gain += d else loss -= d }
        var avgGain = gain / period; var avgLoss = loss / period
        for (i in period + 1 until values.size) {
            val d = values[i] - values[i - 1]
            if (d > 0) { avgGain = (avgGain * (period - 1) + d) / period; avgLoss = (avgLoss * (period - 1)) / period }
            else { avgGain = (avgGain * (period - 1)) / period; avgLoss = (avgLoss * (period - 1) - d) / period }
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun macd(values: List<Double>, fast: Int = 12, slow: Int = 26, signalPeriod: Int = 9): Triple<Double?, Double?, Double?> {
        if (values.size < slow) return Triple(null, null, null)
        val emaFast = ema(values, fast) ?: return Triple(null, null, null)
        val emaSlow = ema(values, slow) ?: return Triple(null, null, null)
        val macdLine = emaFast - emaSlow
        val macdSeries = (0 until values.size).map { i ->
            val f = ema(values.take(i + 1), fast) ?: 0.0
            val s = ema(values.take(i + 1), slow) ?: 0.0
            f - s
        }
        val signalLine = ema(macdSeries, signalPeriod) ?: macdLine
        return Triple(macdLine, signalLine, macdLine - signalLine)
    }

    fun bollinger(values: List<Double>, period: Int = 20, k: Double = 2.0): Triple<Double?, Double?, Double?> {
        if (values.size < period) return Triple(null, null, null)
        val window = values.takeLast(period)
        val mid = window.average()
        val std = sqrt(window.map { (it - mid) * (it - mid) }.average())
        return Triple(mid + k * std, mid, mid - k * std)
    }

    fun momentum(values: List<Double>, period: Int = 5): Double? {
        if (values.size < period + 1) return null
        return values.last() - values[values.size - 1 - period]
    }

    fun pctChange(values: List<Double>, window: Int = 24): Double? {
        if (values.size < max(2, window + 1)) return null
        val end = values.last(); val start = values[values.size - 1 - window]
        if (start == 0.0) return null
        return (end - start) / start * 100.0
    }

    fun minOf(values: List<Double>): Double? = if (values.isEmpty()) null else values.min()
    fun maxOf(values: List<Double>): Double? = if (values.isEmpty()) null else values.max()

    fun volatility(values: List<Double>): Double {
        if (values.size < 2) return 0.02
        val rets = (1 until values.size).map { (values[it] - values[it - 1]) / values[it - 1].coerceAtLeast(1e-9) }
        val mean = rets.average()
        return min(1.0, max(0.0, sqrt(rets.map { (it - mean) * (it - mean) }.average())))
    }

    // ---- Advanced indicators ----

    /** Volume-Weighted Average Price over the series (uses index as proxy weight). */
    fun vwap(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        var sumPw = 0.0; var sumW = 0.0
        values.forEachIndexed { i, p -> val w = (i + 1).toDouble(); sumPw += p * w; sumW += w }
        return sumPw / sumW.coerceAtLeast(1.0)
    }

    /**
     * Returns (levels, max, min): classic 0.236/0.382/0.5/0.618/0.786 retracement
     * levels between the swing low and high of the series.
     */
    fun fibonacci(values: List<Double>): Triple<List<Double>, Double, Double> {
        val high = values.max()
        val low = values.min()
        val range = (high - low).coerceAtLeast(1e-9)
        val levels = listOf(0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0).map { high - range * it }
        return Triple(levels, high, low)
    }

    /**
     * Simple support & resistance using local swing highs/lows on the recent window.
     * Returns (resistance, support) as approximate pivot levels (or null if not enough data).
     */
    fun supportResistance(values: List<Double>, lookback: Int = 20): Pair<Double?, Double?> {
        if (values.size < 10) return Pair(null, null)
        val window = values.takeLast(lookback)
        val hi = window.max(); val lo = window.min()
        // Use 40th/60th percentile as soft S/R levels.
        val sorted = window.sorted()
        val resistance = sorted[(sorted.size * 0.85).toInt().coerceIn(0, sorted.size - 1)]
        val support = sorted[(sorted.size * 0.15).toInt().coerceIn(0, sorted.size - 1)]
        return Pair(resistance, support)
    }

    /**
     * Detect a bullish/bearish RSI divergence between price and RSI.
     * Returns +1 (bullish divergence), -1 (bearish) or 0 (none).
     */
    fun rsiDivergence(values: List<Double>): Int {
        if (values.size < 30) return 0
        val window = values.takeLast(30)
        val rsiSeries = (0 until window.size).mapNotNull { i ->
            rsi(window.take(i + 1), 14)
        }
        if (rsiSeries.size < 20) return 0
        val half = rsiSeries.lastIndex
        val priceLowA = window.takeLast(20).take(10).min()
        val priceLowB = window.takeLast(10).min()
        val rsiLowA = rsiSeries.takeLast(20).take(10).min()
        val rsiLowB = rsiSeries.takeLast(10).min()
        return when {
            priceLowB < priceLowA && rsiLowB > rsiLowA -> 1   // higher RSI low at lower price
            priceLowB > priceLowA && rsiLowB < rsiLowA -> -1
            else -> 0
        }
    }

    /** MACD histogram divergence: +1 bullish, -1 bearish, 0 none. */
    fun macdDivergence(values: List<Double>): Int {
        if (values.size < 30) return 0
        val window = values.takeLast(30)
        val hist2 = (0 until window.size).mapNotNull { i ->
            macd(window.take(i + 1), 12, 26).third
        }
        if (hist2.size < 15) return 0
        val priceLowA = window.takeLast(20).take(10).min()
        val priceLowB = window.takeLast(10).min()
        val hA = hist2.takeLast(20).take(10).min()
        val hB = hist2.takeLast(10).min()
        return when {
            priceLowB < priceLowA && hB > hA -> 1
            priceLowB > priceLowA && hB < hA -> -1
            else -> 0
        }
    }

    /** Simple swing structure: +1 uptrend (higher highs/ lows), -1 downtrend, 0 sideways. */
    fun trendStructure(values: List<Double>): Int {
        if (values.size < 10) return 0
        val last10 = values.takeLast(10)
        val first5 = last10.take(5).sorted()
        val last5 = last10.takeLast(5).sorted()
        val up = last5.last() > first5.last() && last5.first() > first5.first()
        val down = last5.last() < first5.last() && last5.first() < first5.first()
        return when { up -> 1; down -> -1; else -> 0 }
    }
}
