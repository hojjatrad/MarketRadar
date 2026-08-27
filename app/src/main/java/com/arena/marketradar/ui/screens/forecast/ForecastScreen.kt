package com.arena.marketradar.ui.screens.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.ForecastItem
import com.arena.marketradar.domain.util.Constants
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.app
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.components.ConfidenceBadge
import com.arena.marketradar.ui.components.SignalBadge
import com.arena.marketradar.ui.components.StatChip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForecastUiState(
    val loading: Boolean = true,
    val items: List<ForecastItem> = emptyList(),
    val lang: String = "fa",
)

class ForecastViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(ForecastUiState(lang = app.settings.language.value))
    val state: StateFlow<ForecastUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val prices = app.market.getMarkets()
                val news = app.news.fetch()
                val out = mutableListOf<ForecastItem>()
                for (p in prices) {
                    val history = app.market.getHistory(p.symbol).map { it.value }
                    if (history.size < 5) continue
                    val key = Constants.newsKey(p.symbol)
                    val related = news.filter { key in it.assets }
                    val sentiment = if (related.isNotEmpty()) related.map { it.sentiment }.average() else 0.0
                    out += app.engine.forecast(p.symbol, p.nameFa, p.nameEn, history, sentiment)
                }
                val sorted = out.sortedByDescending { kotlin.math.abs(it.probability - 50) + it.sentiment * 10 }
                _state.update { it.copy(loading = false, items = sorted) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false) }
            }
        }
    }
}

@Composable
fun ForecastScreen(viewModel: ForecastViewModel = viewModel(factory = VMFactory { ForecastViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(if (lang == "fa") "پیش‌بینی و سیگنال بازار" else "Market Forecast & Signals",
                modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Text(L.disclaimer.l(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (state.loading) {
            item { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        }
        if (state.items.isEmpty() && !state.loading) {
            item {
                Text(L.emptyForecast.l(lang), modifier = Modifier.padding(20.dp), fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.items, key = { it.symbol }) { f -> ForecastRow(f, lang) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ForecastRow(f: ForecastItem, lang: String) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (lang == "fa") f.nameFa else f.nameEn, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                SignalBadge(f.signal, lang)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip(L.probability.l(lang), "${f.probability}%")
                StatChip(L.horizon.l(lang), f.horizon)
                StatChip(L.expectedMove.l(lang), Formatters.percent(f.expectedMovePercent))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConfidenceBadge(f.confidence, lang)
                Spacer(Modifier.width(8.dp))
                Text("${L.dataPoints.l(lang)}: ${f.dataPoints}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            // Probability meter
            LinearProgressIndicator(
                progress = { f.probability / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = when (f.signal) {
                    com.arena.marketradar.data.model.Signal.BULLISH -> com.arena.marketradar.ui.theme.Green
                    com.arena.marketradar.data.model.Signal.BEARISH -> com.arena.marketradar.ui.theme.Red
                    else -> com.arena.marketradar.ui.theme.Neutral
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(if (lang == "fa") f.summaryFa else f.summaryEn, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (f.factors.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("• " + f.factors.joinToString("\n• "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
