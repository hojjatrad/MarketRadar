package com.arena.marketradar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arena.marketradar.data.model.ConfidenceLevel
import com.arena.marketradar.data.model.Signal
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.ui.theme.Amber
import com.arena.marketradar.ui.theme.Green
import com.arena.marketradar.ui.theme.Neutral
import com.arena.marketradar.ui.theme.Red

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SignalBadge(signal: Signal, lang: String, highContrast: Boolean = false) {
    val text = when (signal) {
        Signal.BULLISH -> if (lang == "fa") "صعودی" else "Bullish"
        Signal.BEARISH -> if (lang == "fa") "نزولی" else "Bearish"
        Signal.NEUTRAL -> if (lang == "fa") "خنثی" else "Neutral"
    }
    val bg = when (signal) {
        Signal.BULLISH -> Green.copy(alpha = 0.16f)
        Signal.BEARISH -> Red.copy(alpha = 0.16f)
        Signal.NEUTRAL -> Neutral.copy(alpha = 0.16f)
    }
    val fg = when (signal) {
        Signal.BULLISH -> Green
        Signal.BEARISH -> Red
        Signal.NEUTRAL -> Neutral
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = if (highContrast) Color.White else fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChangePill(change: Double?, lang: String) {
    val color = when {
        change == null -> Neutral
        change >= 0 -> Green
        else -> Red
    }
    Text(
        text = Formatters.percent(change ?: 0.0),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ConfidenceBadge(level: ConfidenceLevel, lang: String) {
    val text = when (level) {
        ConfidenceLevel.HIGH -> if (lang == "fa") "اطمینان بالا" else "High"
        ConfidenceLevel.MEDIUM -> if (lang == "fa") "اطمینان متوسط" else "Medium"
        ConfidenceLevel.LOW -> if (lang == "fa") "اطمینان کم" else "Low"
    }
    val bg = when (level) {
        ConfidenceLevel.HIGH -> Green.copy(alpha = 0.15f)
        ConfidenceLevel.MEDIUM -> Amber.copy(alpha = 0.18f)
        ConfidenceLevel.LOW -> Neutral.copy(alpha = 0.15f)
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 11.sp, color = when (level) {
            ConfidenceLevel.HIGH -> Green
            ConfidenceLevel.MEDIUM -> Amber.copy(red = 0.8f)
            ConfidenceLevel.LOW -> Neutral
        }, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Lightweight, dependency-free line/area chart drawn with Compose Canvas.
 * Shows a gradient area under the curve plus min/max labels.
 */
@Composable
fun PriceChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Green,
    lineWidth: Float = 3f,
) {
    if (values.size < 2) {
        Box(modifier = modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val min = values.min()
    val max = values.max()
    val span = (max - min).coerceAtLeast(1e-9)

    Canvas(
        modifier = modifier.fillMaxWidth().height(180.dp).padding(horizontal = 4.dp)
    ) {
        val h = size.height
        val w = size.width
        val padTop = 14.dp.toPx()
        val padBottom = 10.dp.toPx()
        val usableH = h - padTop - padBottom

        fun y(v: Double): Float = (padTop + usableH * (1 - ((v - min) / span))).toFloat()
        fun x(i: Int): Float = if (w <= 0) 0f else w * i.toFloat() / (values.size - 1)

        // Grid lines
        val grid = Color.Gray.copy(alpha = 0.12f)
        for (g in 0..3) {
            val gy = padTop + usableH * g / 3f
            drawLine(grid, Offset(0f, gy), Offset(w, gy), 1f)
        }

        // Area
        val areaPath = Path().apply {
            moveTo(x(0), y(values.first()))
            for (i in 1 until values.size) lineTo(x(i), y(values[i]))
            lineTo(w, padTop + usableH)
            lineTo(0f, padTop + usableH)
            close()
        }
        drawPath(
            areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                startY = padTop,
                endY = padTop + usableH
            )
        )

        // Line
        val linePath = Path().apply {
            moveTo(x(0), y(values.first()))
            for (i in 1 until values.size) lineTo(x(i), y(values[i]))
        }
        drawPath(linePath, color, style = Stroke(width = lineWidth.dp.toPx(), cap = StrokeCap.Round))

        // Last point highlight
        drawCircle(color, radius = 5.dp.toPx(), center = Offset(x(values.size - 1), y(values.last())))
    }

    // min/max labels
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(Formatters.plain(min), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(Formatters.plain(max), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
    }
}

/**
 * Advanced candlestick chart with optional indicator overlays, support/resistance
 * and Fibonacci levels. Synthesizes OHLC from a close-price series (open = prev
 * close, high/low = neighbour extremes) so it works without extra data.
 */
data class ChartOverlay(val sma: Boolean, val ema: Boolean, val bollinger: Boolean,
                        val fibonacci: Boolean, val supportResistance: Boolean)

@Composable
fun AdvancedPriceChart(
    values: List<Double>,
    overlay: ChartOverlay,
    modifier: Modifier = Modifier,
) {
    if (values.size < 3) {
        Box(modifier = modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    // Synthesis of OHLC candles from close series.
    val candles = (1 until values.size).map { i ->
        val o = values[i - 1]; val c = values[i]
        val hi = maxOf(o, c); val lo = minOf(o, c)
        Quad(o, hi, lo, c)
    }

    val min = candles.minOf { it.low }
    val max = candles.maxOf { it.high }
    val span = (max - min).coerceAtLeast(1e-9)

    // Precompute overlay series
    val smaLine = if (overlay.sma) TechnicalIndicatorsSeries.sma(values, 20) else emptyList()
    val emaLine = if (overlay.ema) TechnicalIndicatorsSeries.ema(values, 20) else emptyList()
    val bb = if (overlay.bollinger) TechnicalIndicatorsSeries.bollinger(values) else null
    val fib = if (overlay.fibonacci) TechnicalIndicatorsSeries.fibonacci(values) else null
    val sr = if (overlay.supportResistance) TechnicalIndicatorsSeries.supportResistance(values) else null

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp).padding(horizontal = 6.dp)) {
        val h = size.height
        val w = size.width
        val padTop = 12.dp.toPx()
        val padBottom = 10.dp.toPx()
        val usableH = h - padTop - padBottom
        fun y(v: Double): Float = (padTop + usableH * (1 - ((v - min) / span))).toFloat()
        fun x(i: Int): Float = if (candles.isEmpty()) 0f else w * i.toFloat() / candles.size

        // grid
        val grid = Color.Gray.copy(alpha = 0.12f)
        for (g in 0..4) drawLine(grid, Offset(0f, padTop + usableH * g / 4f), Offset(w, padTop + usableH * g / 4f), 1f)

        val candleW = (w / candles.size).coerceAtMost(10.dp.toPx()).coerceAtLeast(2.dp.toPx())
        candles.forEachIndexed { i, c ->
            val up = c.close >= c.open
            val color = if (up) Green else Red
            val cx = x(i)
            drawLine(color, Offset(cx, y(c.high)), Offset(cx, y(c.low)), 1.5f)
            val bodyTop = y(maxOf(c.open, c.close))
            val bodyH = (y(minOf(c.open, c.close)) - bodyTop).coerceAtLeast(1.dp.toPx())
            drawRect(color, topLeft = Offset(cx - candleW / 2f, bodyTop), size = androidx.compose.ui.geometry.Size(candleW, bodyH))
        }

        // overlay lines
        fun poly(points: List<Double>, color: Color, alpha: Float = 0.9f) {
            if (points.size < 2) return
            val path = Path().apply {
                moveTo(x(0), y(points[0]))
                for (i in 1 until points.size) lineTo(x(i), y(points[i]))
            }
            drawPath(path, color, alpha = alpha, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }
        if (overlay.sma) poly(smaLine, Blue)
        if (overlay.ema) poly(emaLine, Amber)
        bb?.let { pair ->
            if (pair.first.isNotEmpty() && pair.second.isNotEmpty()) {
                poly(pair.first, Purple)
                poly(pair.second, Purple)
            }
        }
        // Fibonacci + support/resistance horizontal levels
        fun hline(value: Double, color: Color, dash: Boolean = false) {
            val yy = y(value)
            if (dash) {
                val step = 8.dp.toPx()
                var xx = 0f
                while (xx < w) { drawLine(color, Offset(xx, yy), Offset(xx + step/2, yy), 1f); xx += step }
            } else drawLine(color, Offset(0f, yy), Offset(w, yy), 1f)
        }
        sr?.let { pair ->
            hline(pair.first, Green, dash = true)
            hline(pair.second, Red, dash = true)
        }
        fib?.forEach { hline(it, Blue, dash = true) }
    }
    // Legend
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        smallLegend(Green, if (overlay.supportResistance) "S/R" else "")
        if (overlay.sma) smallLegend(Blue, "SMA20")
        if (overlay.ema) smallLegend(Amber, "EMA20")
        if (overlay.bollinger) smallLegend(Purple, "Boll.")
        if (overlay.fibonacci) smallLegend(Blue, "Fib")
    }
}

@Composable
private fun smallLegend(color: Color, text: String) {
    if (text.isEmpty()) return
    Box(Modifier.padding(end = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Spacer(Modifier.width(3.dp))
            Text(text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Internal series helpers (wrap TechnicalIndicators but tolerating small data). */
private object TechnicalIndicatorsSeries {
    fun sma(v: List<Double>, p: Int): List<Double> =
        (0 until v.size).map { i -> if (i + 1 >= p) v.subList(0, i + 1).takeLast(p).average() else Double.NaN }

    fun ema(v: List<Double>, p: Int): List<Double> {
        if (v.isEmpty()) return emptyList()
        val k = 2.0 / (p + 1)
        var prev = v[0]
        return v.mapIndexed { i, x -> if (i == 0) x else { prev = x * k + prev * (1 - k); prev } }
    }

    fun bollinger(v: List<Double>): Pair<List<Double>, List<Double>>? {
        val upper = mutableListOf<Double>(); val lower = mutableListOf<Double>()
        for (i in 0 until v.size) {
            if (i < 19) { upper.add(Double.NaN); lower.add(Double.NaN); continue }
            val win = v.subList(0, i + 1).takeLast(20)
            val m = win.average(); val sd = win.map { (it - m) * (it - m) }.average().let { kotlin.math.sqrt(it) }
            upper.add(m + 2 * sd); lower.add(m - 2 * sd)
        }
        return Pair(upper, lower)
    }

    fun fibonacci(v: List<Double>): List<Double>? {
        if (v.size < 20) return null
        val hi = v.max(); val lo = v.min(); val r = (hi - lo).coerceAtLeast(1e-9)
        return listOf(0.236, 0.382, 0.5, 0.618, 0.786).map { hi - r * it }
    }

    fun supportResistance(v: List<Double>): Pair<Double, Double>? {
        if (v.size < 10) return null
        val win = v.takeLast(20).sorted()
        val res = win[(win.size * 0.85).toInt().coerceIn(0, win.size - 1)]
        val sup = win[(win.size * 0.15).toInt().coerceIn(0, win.size - 1)]
        return Pair(res, sup)
    }
}

private data class Quad(val open: Double, val high: Double, val low: Double, val close: Double)

// Extra material colors for the chart
private val Blue = Color(0xFF3B82F6)
private val Purple = Color(0xFF8B5CF6)
