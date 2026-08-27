package com.arena.marketradar.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.domain.util.Constants
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.components.ChangePill
import com.arena.marketradar.ui.components.SectionHeader
import com.arena.marketradar.ui.components.SignalBadge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val prices: List<MarketPrice> = emptyList(),
    val signals: Map<String, ForecastItem> = emptyMap(),
    val watchlist: Set<String> = emptySet(),
)

class HomeViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        _state.update { it.copy(watchlist = app.settings.watchlist().ifEmpty { Constants.DEFAULT_WATCHLIST }.toSet()) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val prices = app.market.getMarkets()
                val signals = mutableMapOf<String, ForecastItem>()
                for (p in prices) {
                    val values = app.market.localHistory(p.symbol).map { it.value }
                    if (values.size >= 2) signals[p.symbol] = app.engine.forecast(p.symbol, p.nameFa, p.nameEn, values, 0.0)
                }
                _state.update { it.copy(loading = false, prices = prices, signals = signals) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun toggleWatch(symbol: String) {
        val current = _state.value.watchlist.toMutableSet()
        if (!current.add(symbol)) current.remove(symbol)
        app.settings.saveWatchlist(current.toList())
        _state.update { it.copy(watchlist = current) }
    }
}

@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
    onOpenPortfolio: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = VMFactory { HomeViewModel(localApp()) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("رصد بازار", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Outlined.Refresh, contentDescription = L.refresh.l(lang)) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(if (lang == "fa") "سرمایه‌گذاری" else "Portfolio", "💰", onOpenPortfolio, Modifier.weight(1f))
                QuickAction(if (lang == "fa") "تقویم" else "Calendar", "📅", onOpenCalendar, Modifier.weight(1f))
                QuickAction(if (lang == "fa") "گزارش" else "Report", "📄", onOpenReport, Modifier.weight(1f))
            }
        }

        if (state.loading && state.prices.isEmpty()) {
            item { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        }
        for (e in listOf<String?>(state.error).filterNotNull()) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WifiOff, null); Spacer(Modifier.width(8.dp)); Text(e, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        val current = state.prices
        val watch = state.watchlist
        if (watch.isNotEmpty()) {
            item { SectionHeader(L.watchlist.l(lang)) }
            items(current.filter { it.symbol in watch }, key = { "w_" + it.symbol }) { p ->
                MarketRow(p, state.signals[p.symbol], lang, star = true, onOpen = onOpen, onToggleStar = { viewModel.toggleWatch(p.symbol) })
            }
        }

        val groups = linkedMapOf<String, List<MarketPrice>>()
        current.forEach { p -> groups.getOrPut(groupTitle(p.type, p.scope, lang)) { mutableListOf() }.let { (it as MutableList).add(p) } }
        groups.forEach { (title, items) ->
            item { SectionHeader(title) }
            items(items, key = { "g_" + it.symbol }) { p ->
                MarketRow(p, state.signals[p.symbol], lang, star = p.symbol in watch, onOpen = onOpen, onToggleStar = { viewModel.toggleWatch(p.symbol) })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun groupTitle(type: com.arena.marketradar.data.model.AssetType, scope: com.arena.marketradar.data.model.MarketScope, lang: String): String =
    when (type) {
        com.arena.marketradar.data.model.AssetType.CRYPTO -> if (lang == "fa") "ارز دیجیتال" else "Cryptocurrency"
        com.arena.marketradar.data.model.AssetType.METAL -> if (lang == "fa") "طلا و فلزات" else "Gold & Metals"
        com.arena.marketradar.data.model.AssetType.FIAT -> if (lang == "fa") "ارز (بازار ایران)" else "Fiat (Iran Market)"
    }

@Composable
private fun QuickAction(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp); Spacer(Modifier.height(4.dp)); Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MarketRow(p: MarketPrice, signal: ForecastItem?, lang: String, star: Boolean, onOpen: (String) -> Unit, onToggleStar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onOpen(p.symbol) },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Text(p.symbol.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (lang == "fa") p.nameFa else p.nameEn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(if (lang == "fa") Formatters.money(p.price, p.unit) else Formatters.money(p.price, p.unit, "en"),
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                if (signal != null) SignalBadge(signal.signal, lang) else ChangePill(p.changePercent24h, lang)
                Spacer(Modifier.height(4.dp))
                Text(if (lang == "fa") "سیگنال" else "signal", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onToggleStar) {
                Icon(if (star) Icons.Outlined.Star else Icons.Outlined.StarBorder, null,
                    tint = if (star) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
