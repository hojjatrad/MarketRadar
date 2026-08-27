package com.arena.marketradar.ui.screens.screener

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.arena.marketradar.domain.analysis.TechnicalIndicators
import com.arena.marketradar.domain.util.Constants
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.components.SignalBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Filter options for the screener. */
enum class ScreenerFilter { RISING_RSI, OVERSOLD, OVERBOUGHT, BELOW_SMA, ABOVE_SMA, BULLISH_SIGNAL, BEARISH_SIGNAL }

data class ScreenerUiState(val loading: Boolean = true, val rows: List<ScreenerRow> = emptyList(), val filter: ScreenerFilter = ScreenerFilter.RISING_RSI)
data class ScreenerRow(
    val symbol: String, val nameFa: String, val nameEn: String,
    val price: Double, val rsi: Double?, val pct: Double?,
    val signal: com.arena.marketradar.data.model.Signal,
    val prob: Int, val accuracy: Double?, val change24h: Double?,
)

class ScreenerViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(ScreenerUiState())
    val state: StateFlow<ScreenerUiState> = _state
    init { load() }
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val rows = mutableListOf<ScreenerRow>()
            try {
                val prices = app.market.getMarkets()
                for (p in prices) {
                    val hist = app.market.getHistory(p.symbol).map { it.value }
                    val rsi = TechnicalIndicators.rsi(hist)
                    val pct = TechnicalIndicators.pctChange(hist, 24)
                    val f = app.engine.forecast(p.symbol, p.nameFa, p.nameEn, hist, 0.0)
                    rows += ScreenerRow(p.symbol, p.nameFa, p.nameEn, p.price, rsi, pct, f.signal, f.probability, f.accuracy, p.changePercent24h)
                }
            } catch (e: Exception) {}
            _state.update { it.copy(loading = false, rows = rows) }
        }
    }
    fun setFilter(f: ScreenerFilter) = _state.update { it.copy(filter = f) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenerScreen(onOpen: (String) -> Unit, onBack: () -> Unit, viewModel: ScreenerViewModel = viewModel(factory = VMFactory { ScreenerViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    val filtered = state.rows.filter { r -> matches(r, state.filter) }.sortedByDescending { it.prob }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "اسکرینر بازار" else "Market Screener") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
            actions = { IconButton(onClick = { viewModel.load() }) { Icon(Icons.Outlined.Search, null) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val chips = listOf(
                    "RSI صعودی" to ScreenerFilter.RISING_RSI,
                    "اشباع فروش" to ScreenerFilter.OVERSOLD,
                    "اشباع خرید" to ScreenerFilter.OVERBOUGHT,
                    "زیر SMA" to ScreenerFilter.BELOW_SMA,
                    "بالای SMA" to ScreenerFilter.ABOVE_SMA,
                    "سیگنال صعود" to ScreenerFilter.BULLISH_SIGNAL,
                    "سیگنال نزول" to ScreenerFilter.BEARISH_SIGNAL,
                )
                LazyColumn(Modifier.weight(0.30f)) {
                    items(chips) { (label, filter) ->
                        FilterChip(state.filter == filter, { viewModel.setFilter(filter) }, label = { Text(label, fontSize = 11.sp) }, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                LazyColumn(Modifier.weight(1f)) {
                    if (filtered.isEmpty()) item { Text(if (lang == "fa") "هیچ‌کدام مطابقت نداشت." else "None matched.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(filtered, key = { it.symbol + state.filter.name }) { r ->
                        Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable { onOpen(r.symbol) }, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (lang == "fa") r.nameFa else r.nameEn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("RSI: ${if (r.rsi != null) Formatters.plain(r.rsi, 0) else "—"} • ${if (r.accuracy != null) "دقت ${(r.accuracy * 100).toInt()}%" else "—"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Formatters.money(r.price, com.arena.marketradar.data.model.PriceUnit.USD), fontSize = 12.sp)
                                    SignalBadge(r.signal, lang)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun matches(r: ScreenerRow, f: ScreenerFilter): Boolean = when (f) {
    ScreenerFilter.RISING_RSI -> r.rsi != null && r.rsi > 50 && r.rsi < 70
    ScreenerFilter.OVERSOLD -> r.rsi != null && r.rsi < 30
    ScreenerFilter.OVERBOUGHT -> r.rsi != null && r.rsi > 70
    ScreenerFilter.BELOW_SMA -> r.pct != null && r.pct < 0
    ScreenerFilter.ABOVE_SMA -> r.pct != null && r.pct > 0
    ScreenerFilter.BULLISH_SIGNAL -> r.signal == com.arena.marketradar.data.model.Signal.BULLISH
    ScreenerFilter.BEARISH_SIGNAL -> r.signal == com.arena.marketradar.data.model.Signal.BEARISH
}
