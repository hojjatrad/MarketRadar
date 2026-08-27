package com.arena.marketradar.domain.analysis

import com.arena.marketradar.data.model.MarketPrice
import kotlin.math.sqrt

/**
 * Cross-asset analytics computed on-device from the last-known prices:
 *  - Fear & Greed index (0..100) built from a blend of RSI, momentum, volatility.
 *  - Pairwise correlation estimates between tracked assets (uses the on-device
 *    price history as a proxy). Values in [-1, +1].
 */
object MarketAnalysis {

    /**
     * Fear & Greed for the given asset history.
     * 0 → extreme fear, 100 → extreme greed. Uses RSI + momentum + volatility.
     */
    fun fearGreed(values: List<Double>): Int? {
        if (values.size < 30) return null
        val rsi = TechnicalIndicators.rsi(values, 14) ?: return null
        val mom = TechnicalIndicators.momentum(values, 5) ?: 0.0
        val vol = TechnicalIndicators.volatility(values)
        val price = values.last()
        val momPct = (mom / price.coerceAtLeast(1e-9)) * 100.0

        // RSI drives most of the score; momentum & low volatility push toward greed.
        var score = rsi
        score += momPct.coerceIn(-5.0, 5.0)            // +strength
        score += (0.05 - vol) * 120                    // low vol → greed, high vol → fear
        return score.toInt().coerceIn(0, 100)
    }

    /** Helper to color the Fear/Greed meter (red=fear, green=greed). */
    fun fearGreedColor(v: Int?): androidx.compose.ui.graphics.Color = when {
        v == null -> com.arena.marketradar.ui.theme.Neutral
        v < 40 -> com.arena.marketradar.ui.theme.Red
        v < 60 -> com.arena.marketradar.ui.theme.Amber
        else -> com.arena.marketradar.ui.theme.Green
    }

    fun fearGreedLabel(v: Int?, lang: String = "fa"): String {
        if (v == null) return if (lang == "fa") "—" else "—"
        return when {
            v < 20 -> if (lang == "fa") "ترس شدید" else "Extreme Fear"
            v < 40 -> if (lang == "fa") "ترس" else "Fear"
            v < 60 -> if (lang == "fa") "خنثی" else "Neutral"
            v < 80 -> if (lang == "fa") "طمع" else "Greed"
            else -> if (lang == "fa") "طمع شدید" else "Extreme Greed"
        }
    }

    /**
     * Estimate pairwise correlations between the tracked assets that have local
     * history. Returns a map keyed by "SYM_A|SYM_B".
     */
    fun correlations(historyProvider: (String) -> List<Double>, symbols: List<String>): Map<String, Double> {
        val series = symbols.associateWith { historyProvider(it) }
        val keys = series.keys.toList()
        val out = mutableMapOf<String, Double>()
        for (i in 0 until keys.size) {
            for (j in i + 1 until keys.size) {
                val a = series[keys[i]].orEmpty()
                val b = series[keys[j]].orEmpty()
                val n = minOf(a.size, b.size)
                if (n < 30) continue
                val aa = a.takeLast(n); val bb = b.takeLast(n)
                val c = pearson(aa, bb) ?: continue
                out["${keys[i]}|${keys[j]}"] = c
            }
        }
        return out
    }

    /** Public accessor for the pairwise correlation of two equally-sized series. */
    fun pearsonOf(a: List<Double>, b: List<Double>): Double? = pearson(a, b)

    private fun pearson(a: List<Double>, b: List<Double>): Double? {
        if (a.size != b.size || a.isEmpty()) return null
        val ma = a.average(); val mb = b.average()
        var num = 0.0; var da = 0.0; var db = 0.0
        for (i in a.indices) {
            val xa = a[i] - ma; val xb = b[i] - mb
            num += xa * xb; da += xa * xa; db += xb * xb
        }
        val den = sqrt(da * db)
        if (den == 0.0) return null
        return (num / den).coerceIn(-1.0, 1.0)
    }
}
