package com.arena.marketradar.ui.screens.detail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.AlertCondition
import com.arena.marketradar.data.model.AlertItem
import com.arena.marketradar.data.model.ForecastItem
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.NewsItem
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.app
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.components.AdvancedPriceChart
import com.arena.marketradar.ui.components.ChangePill
import com.arena.marketradar.ui.components.ChartOverlay
import com.arena.marketradar.ui.components.ConfidenceBadge
import com.arena.marketradar.ui.components.SignalBadge
import com.arena.marketradar.ui.components.StatChip
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val price: MarketPrice? = null,
    val history: List<Double> = emptyList(),
    val forecast: ForecastItem? = null,
    val alerts: List<AlertItem> = emptyList(),
    val news: List<NewsItem> = emptyList(),
    val inWatch: Boolean = false,
    val lang: String = "fa",
)

class DetailViewModel(private val app: MarketRadarApplication, private val symbol: String) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState(lang = app.settings.language.value))
    val state: StateFlow<DetailUiState> = _state

    init {
        _state.update { it.copy(inWatch = symbol in app.settings.watchlist(), alerts = app.alerts.alerts.value.filter { a -> a.symbol == symbol }) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val prices = app.market.getMarkets()
                val price = prices.firstOrNull { it.symbol == symbol }
                val history = app.market.getHistory(symbol).map { it.value }
                val key = assetKey(symbol)
                val news = app.news.fetch().filter { key in it.assets }.take(20)
                val sentiment = if (news.isNotEmpty()) news.map { it.sentiment }.average() else 0.0
                // Correlations with a few tracked assets (requires their local history).
                val corrList = mutableListOf<com.arena.marketradar.data.model.Correlation>()
                val corrSymbols = listOf("BTC", "ETH", "XAU", "USD", "USDT")
                for (s in corrSymbols) {
                    if (s == symbol) continue
                    val otherHist = app.market.getHistory(s).map { it.value }
                    val n = minOf(history.size, otherHist.size)
                    if (n >= 30) {
                        val v = com.arena.marketradar.domain.analysis.MarketAnalysis.pearsonOf(history.takeLast(n), otherHist.takeLast(n))
                        if (v != null) corrList.add(com.arena.marketradar.data.model.Correlation(s, s, v))
                    }
                }
                val forecast = if (price != null && history.size >= 3) {
                    app.engine.forecast(symbol, price.nameFa, price.nameEn, history, sentiment, corrList)
                } else null
                _state.update { it.copy(loading = false, price = price, history = history, forecast = forecast, news = news) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun addAlert(condition: AlertCondition, target: Double) {
        val p = _state.value.price ?: return
        app.alerts.add(symbol, p.nameFa, condition, target, p.unit)
        _state.update { it.copy(alerts = app.alerts.alerts.value.filter { a -> a.symbol == symbol }) }
    }

    fun removeAlert(id: String) {
        app.alerts.remove(id)
        _state.update { it.copy(alerts = app.alerts.alerts.value.filter { a -> a.symbol == symbol }) }
    }

    fun toggleAlert(id: String, enabled: Boolean) {
        app.alerts.toggle(id, enabled)
        _state.update { it.copy(alerts = app.alerts.alerts.value.filter { a -> a.symbol == symbol }) }
    }

    fun toggleWatch() {
        val cur = app.settings.watchlist().toMutableList()
        val inWatch = symbol in cur
        if (inWatch) cur.remove(symbol) else cur.add(symbol)
        app.settings.saveWatchlist(cur)
        _state.update { it.copy(inWatch = !inWatch) }
    }

    private fun assetKey(symbol: String): String = when (symbol) {
        "BTC" -> "BTC"; "ETH" -> "ETH"; "XAU", "GOL18", "EMAMI1", "AZADI1", "AZADI1_2", "AZADI1_4", "MITHQAL" -> "XAU"
        "USD" -> "USD"; "EUR" -> "EUR"; "USDT" -> "USDT"; "SOL" -> "SOL"; "XRP" -> "XRP"
        else -> symbol
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(symbol: String, onBack: () -> Unit, viewModel: DetailViewModel = viewModel(key = "detail_$symbol", factory = VMFactory { DetailViewModel(localApp(), symbol) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.price?.let { if (lang == "fa") it.nameFa else it.nameEn } ?: symbol) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatch() }) {
                        Icon(if (state.inWatch) Icons.Outlined.Star else Icons.Outlined.StarBorder, null,
                            tint = if (state.inWatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.load() }) { Icon(Icons.Outlined.Notifications, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            val price = state.price
            if (price != null) {
                item {
                    Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("${if (lang == "fa") "قیمت" else "Price"} • ${price.source}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Text(Formatters.money(price.price, price.unit),
                                fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            ChangePill(price.changePercent24h, lang)
                        }
                    }
                }
            }

            // Forecast card
            state.forecast?.let { f ->
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (lang == "fa") "سیگنال پیش‌بینی" else "Forecast signal",
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f))
                                SignalBadge(f.signal, lang)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatChip(if (lang == "fa") "احتمال" else "Probability", "${f.probability}%")
                                StatChip(if (lang == "fa") "حرکت" else "Expected", Formatters.percent(f.expectedMovePercent))
                                if (f.accuracy != null) StatChip(if (lang == "fa") "دقت" else "Accuracy", "${(f.accuracy * 100).toInt()}%")
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (f.prob24h != null) StatChip("24h", "${f.prob24h}%")
                                if (f.prob72h != null) StatChip("72h", "${f.prob72h}%")
                                if (f.prob1w != null) StatChip("1w", "${f.prob1w}%")
                                if (f.fearGreed != null) StatChip(if (lang == "fa") "ترس/طمع" else "FG", "${f.fearGreed}")
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ConfidenceBadge(f.confidence, lang)
                                if (f.support != null || f.resistance != null) {
                                    Text("${if (lang == "fa") "حمایت" else "Sup"}: ${Formatters.plain(f.support ?: 0.0, 2)} • ${if (lang == "fa") "مقاومت" else "Res"}: ${Formatters.plain(f.resistance ?: 0.0, 2)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(if (lang == "fa") f.summaryFa else f.summaryEn, fontSize = 13.sp)
                            if (f.factors.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("• " + f.factors.joinToString("\n• "), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (f.evidence.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (lang == "fa") "شواهد (وزن‌دار)" else "Weighted evidence", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                f.evidence.forEach { e ->
                                    Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (e.positive) "▲" else "▼", fontSize = 11.sp, color = if (e.positive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(4.dp))
                                        Text(e.label, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                        Text("×${(e.weight).toString().take(3)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (f.correlations.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (lang == "fa") "همبستگی" else "Correlations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                f.correlations.forEach { c ->
                                    Text("${c.nameEn}: ${Formatters.plain(c.value, 2)}", fontSize = 11.sp, color = if (c.value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Chart with indicator toggles
            item {
                var ov by androidx.compose.runtime.remember { mutableStateOf(ChartOverlay(true, true, true, true, true)) }
                Column(Modifier.padding(16.dp)) {
                    Text(if (lang == "fa") "چارت تکنیکال" else "Technical chart", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ToggleChip("SMA", ov.sma) { ov = ov.copy(sma = it) }
                        ToggleChip("EMA", ov.ema) { ov = ov.copy(ema = it) }
                        ToggleChip("Boll", ov.bollinger) { ov = ov.copy(bollinger = it) }
                        ToggleChip("Fib", ov.fibonacci) { ov = ov.copy(fibonacci = it) }
                        ToggleChip("S/R", ov.supportResistance) { ov = ov.copy(supportResistance = it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    AdvancedPriceChart(state.history, ov)
                    if (state.history.size < 20) {
                        Text(L.lowConfidenceNote.l(lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            // Stats
            price?.let { p ->
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatChip(L.high.l(lang), Formatters.plain(p.high24h ?: p.price))
                        StatChip(L.low.l(lang), Formatters.plain(p.low24h ?: p.price))
                        StatChip(L.change.l(lang), Formatters.percent(p.changePercent24h ?: 0.0))
                    }
                }
            }

            // Alerts
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(L.alerts.l(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
            }
            item {
                AddAlertChip(onAdd = { cond, target -> viewModel.addAlert(cond, target) }, lang = lang)
            }
            items(state.alerts, key = { it.id }) { alert ->
                AlertRow(alert, lang, onDelete = { viewModel.removeAlert(alert.id) }, onToggle = { viewModel.toggleAlert(alert.id, it) })
            }

            // News
            if (state.news.isNotEmpty()) {
                item { SectionHeaderTitle(L.news.l(lang)) }
                items(state.news) { n ->
                    NewsRow(n, lang)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeaderTitle(title: String) {
    Text(title, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ToggleChip(label: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(selected = selected, onClick = { onChange(!selected) }, label = { Text(label, fontSize = 10.sp) })
}

@Composable
private fun AddAlertChip(onAdd: (AlertCondition, Double) -> Unit, lang: String) {
    var show by androidx.compose.runtime.remember { mutableStateOf(false) }
    TextButton(onClick = { show = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
        Icon(Icons.Outlined.Add, null)
        Spacer(Modifier.width(4.dp))
        Text(L.addAlert.l(lang))
    }
    if (show) {
        var condition by androidx.compose.runtime.remember { mutableStateOf(AlertCondition.ABOVE) }
        var target by androidx.compose.runtime.remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(L.addAlert.l(lang)) },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = condition == AlertCondition.ABOVE, onClick = { condition = AlertCondition.ABOVE }, label = { Text(L.alertAbove.l(lang), fontSize = 10.sp) })
                        FilterChip(selected = condition == AlertCondition.BELOW, onClick = { condition = AlertCondition.BELOW }, label = { Text(L.alertBelow.l(lang), fontSize = 10.sp) })
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = condition == AlertCondition.CROSS_ABOVE, onClick = { condition = AlertCondition.CROSS_ABOVE }, label = { Text(if (lang == "fa") "عبور صعودی" else "Cross up", fontSize = 10.sp) })
                        FilterChip(selected = condition == AlertCondition.CROSS_BELOW, onClick = { condition = AlertCondition.CROSS_BELOW }, label = { Text(if (lang == "fa") "عبور نزولی" else "Cross down", fontSize = 10.sp) })
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text(L.targetPrice.l(lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(onClick = {
                    target.toDoubleOrNull()?.let { onAdd(condition, it) }
                    show = false
                }) { Text(L.save.l(lang)) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(L.cancel.l(lang)) } }
        )
    }
}

@Composable
private fun AlertRow(alert: AlertItem, lang: String, onDelete: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val condLabel = when (alert.condition) {
                    AlertCondition.ABOVE -> L.alertAbove.l(lang)
                    AlertCondition.BELOW -> L.alertBelow.l(lang)
                    AlertCondition.CROSS_ABOVE -> if (lang == "fa") "عبور صعودی" else "Cross up"
                    AlertCondition.CROSS_BELOW -> if (lang == "fa") "عبور نزولی" else "Cross down"
                }
                Text("${alert.nameFa} • $condLabel ${Formatters.plain(alert.targetPrice)}", fontWeight = FontWeight.Medium)
                Text(if (alert.enabled) L.enabled.l(lang) else L.disabled.l(lang), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onToggle(!alert.enabled) }) { Text(if (alert.enabled) L.disabled.l(lang) else L.enabled.l(lang), fontSize = 12.sp) }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun NewsRow(n: NewsItem, lang: String) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L.newsSentiment.l(lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(n.sentimentLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = when (n.sentimentLabel) {
                        "+" -> MaterialTheme.colorScheme.primary
                        "-" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
            }
            Spacer(Modifier.height(4.dp))
            Text(n.title, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 3)
            Spacer(Modifier.height(4.dp))
            Text("${n.source} • ${Formatters.timeShort(n.published)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
