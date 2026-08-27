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
