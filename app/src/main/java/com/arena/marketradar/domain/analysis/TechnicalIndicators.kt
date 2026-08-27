package com.arena.marketradar.domain.analysis

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Pure technical indicators computed on-device from a price series. */
object TechnicalIndicators {

    fun sma(values: List<Double>, period: Int): Double? {
        if (values.size < period) return null
        val window = values.takeLast(period)
        return window.average()
    }

    fun ema(values: List<Double>, period: Int): Double? {
        if (values.isEmpty()) return null
        val k = 2.0 / (period + 1)
        var prev = values.first()
        for (i in 1 until values.size) {
            prev = values[i] * k + prev * (1 - k)
        }
        return prev
    }

    fun rsi(values: List<Double>, period: Int = 14): Double? {
        if (values.size < period + 1) return null
        var gain = 0.0
        var loss = 0.0
        for (i in 1..period) {
            val diff = values[i] - values[i - 1]
            if (diff > 0) gain += diff else loss -= diff
        }
        var avgGain = gain / period
        var avgLoss = loss / period
        for (i in period + 1 until values.size) {
            val diff = values[i] - values[i - 1]
            if (diff > 0) {
                avgGain = (avgGain * (period - 1) + diff) / period
                avgLoss = (avgLoss * (period - 1)) / period
            } else {
                avgGain = (avgGain * (period - 1)) / period
                avgLoss = (avgLoss * (period - 1) - diff) / period
            }
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /** Returns (macdLine, signalLine, histogram). */
    fun macd(values: List<Double>, fast: Int = 12, slow: Int = 26, signalPeriod: Int = 9): Triple<Double?, Double?, Double?> {
        if (values.size < slow) return Triple(null, null, null)
        val emaFast = ema(values, fast) ?: return Triple(null, null, null)
        val emaSlow = ema(values, slow) ?: return Triple(null, null, null)
        val macdLine = emaFast - emaSlow
        // Approximate signal line as a smoothing of the macd line.
        val macdSeries = (0 until values.size).map { i ->
            val f = ema(values.take(i + 1), fast) ?: 0.0
            val s = ema(values.take(i + 1), slow) ?: 0.0
            f - s
        }
        val signalLine = ema(macdSeries, signalPeriod) ?: macdLine
        return Triple(macdLine, signalLine, macdLine - signalLine)
    }

    /** Returns (upper, middle, lower). */
    fun bollinger(values: List<Double>, period: Int = 20, k: Double = 2.0): Triple<Double?, Double?, Double?> {
        if (values.size < period) return Triple(null, null, null)
        val window = values.takeLast(period)
        val mid = window.average()
        val variance = window.map { (it - mid) * (it - mid) }.average()
        val std = sqrt(variance)
        return Triple(mid + k * std, mid, mid - k * std)
    }

    fun momentum(values: List<Double>, period: Int = 5): Double? {
        if (values.size < period + 1) return null
        return values.last() - values[values.size - 1 - period]
    }

    /** Simple change percentage over the last `window` points. */
    fun pctChange(values: List<Double>, window: Int = 24): Double? {
        if (values.size < max(2, window + 1)) return null
        val end = values.last()
        val start = values[values.size - 1 - window]
        if (start == 0.0) return null
        return (end - start) / start * 100.0
    }

    fun minOf(values: List<Double>): Double? = if (values.isEmpty()) null else values.min()
    fun maxOf(values: List<Double>): Double? = if (values.isEmpty()) null else values.max()

    /** Standard deviation of returns used to size expected moves. */
    fun volatility(values: List<Double>): Double {
        if (values.size < 2) return 0.02
        val rets = (1 until values.size).map { (values[it] - values[it - 1]) / values[it - 1].coerceAtLeast(1e-9) }
        val mean = rets.average()
        return min(1.0, max(0.0, sqrt(rets.map { (it - mean) * (it - mean) }.average())))
    }
}
