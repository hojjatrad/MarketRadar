package com.arena.marketradar.ui.screens.paper

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.arena.marketradar.data.model.MarketPrice
import com.arena.marketradar.data.model.PaperTradeRow
import com.arena.marketradar.data.model.TradeSide
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.theme.Green
import com.arena.marketradar.ui.theme.Red
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaperUiState(val loading: Boolean = true, val prices: List<MarketPrice> = emptyList(), val rows: List<PaperTradeRow> = emptyList(), val balance: Double = 100000.0)

class PaperViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(PaperUiState())
    val state: StateFlow<PaperUiState> = _state
    init { load() }
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val prices = app.market.getMarkets()
                val rows = app.paper.rows(prices)
                _state.update { it.copy(loading = false, prices = prices, rows = rows, balance = app.paper.balance.value) }
            } catch (e: Exception) { _state.update { it.copy(loading = false) } }
        }
    }
    fun trade(symbol: String, nameFa: String, unit: com.arena.marketradar.data.model.PriceUnit, side: TradeSide, qty: Double) {
        val price = _state.value.prices.firstOrNull { it.symbol == symbol }?.price ?: return
        app.paper.trade(symbol, nameFa, unit, side, qty, price); load()
    }
    fun close(id: String) { app.paper.close(id); load() }
    fun reset() { app.paper.reset(100000.0); load() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: PaperViewModel = viewModel(factory = VMFactory { PaperViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    val unreal = state.rows.sumOf { it.pnl }
    val equity = state.balance + unreal
    val cols = state.rows.sumOf { it.trade.qty * it.currentPrice }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "شبیه‌ساز معاملات" else "Paper Trading") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
            actions = { IconButton(onClick = { viewModel.reset() }) { Icon(Icons.Outlined.RestartAlt, null) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(if (lang == "fa") "موجودی مجازی" else "Virtual balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(Formatters.money(state.balance, com.arena.marketradar.data.model.PriceUnit.USD), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("${if (lang == "fa") "سود/زیان باز" else "Unrealized"} (${Formatters.money(unreal, com.arena.marketradar.data.model.PriceUnit.USD)})", color = if (unreal >= 0) Green else Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${if (lang == "fa") "حقوق صاحبان" else "Equity"}: ${Formatters.money(equity, com.arena.marketradar.data.model.PriceUnit.USD)}", fontSize = 12.sp)
                        }
                    }
                }
            }
            item { NewPaperTradeCard(state.prices, { s, n, u, side, q -> viewModel.trade(s, n, u, side, q) }, lang) }
            item { Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Text(if (lang == "fa") "معاملات باز" else "Open trades", fontWeight = FontWeight.Bold) } }
            if (state.rows.isEmpty()) item { Text(if (lang == "fa") "هنوز معامله‌ای باز نیست. یک معاملهٔ مجازی باز کنید." else "No open trades yet.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.rows, key = { it.trade.id }) { r -> PaperTradeRowItem(r, lang, onOpen = { onOpen(r.trade.symbol) }, onClose = { viewModel.close(r.trade.id) }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PaperTradeRowItem(r: PaperTradeRow, lang: String, onOpen: () -> Unit, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onOpen() }, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (lang == "fa") r.trade.nameFa else r.trade.symbol, fontWeight = FontWeight.Bold)
                Text("${if (r.trade.side == TradeSide.BUY) "Buy" else "Sell"} ${Formatters.plain(r.trade.qty)} @ ${Formatters.money(r.trade.entryPrice, r.trade.unit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Formatters.signed(r.pnl)} (${Formatters.percent(r.pnlPercent)})", fontWeight = FontWeight.Bold, color = if (r.pnl >= 0) Green else Red)
                TextButton(onClick = onClose, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) { Text(if (lang == "fa") "بستن" else "Close", fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun NewPaperTradeCard(prices: List<MarketPrice>, onAdd: (String, String, com.arena.marketradar.data.model.PriceUnit, TradeSide, Double) -> Unit, lang: String) {
    var show by androidx.compose.runtime.remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(if (lang == "fa") "+ معامله‌ی مجازی" else "+ Virtual trade")
    }
    if (show) {
        var symbol by androidx.compose.runtime.remember { mutableStateOf("BTC") }
        var qty by androidx.compose.runtime.remember { mutableStateOf("") }
        var side by androidx.compose.runtime.remember { mutableStateOf(TradeSide.BUY) }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(if (lang == "fa") "معامله‌ی مجازی" else "Virtual trade") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("BTC", "ETH", "USDT", "XAU", "USD").forEach { s -> OutlinedButton(onClick = { symbol = s }, modifier = Modifier.padding(2.dp)) { Text(s, fontSize = 11.sp) } }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(side == TradeSide.BUY, { side = TradeSide.BUY }, label = { Text(if (lang == "fa") "خرید" else "Buy") })
                        FilterChip(side == TradeSide.SELL, { side = TradeSide.SELL }, label = { Text(if (lang == "fa") "فروش" else "Sell") })
                    }
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text(if (lang == "fa") "مقدار" else "Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = qty.toDoubleOrNull(); val p = prices.firstOrNull { it.symbol == symbol }
                    if (q != null && q > 0 && p != null) onAdd(symbol, if (lang == "fa") p.nameFa else p.nameEn, p.unit, side, q)
                    show = false
                }) { Text(if (lang == "fa") "باز کردن" else "Open") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(if (lang == "fa") "انصراف" else "Cancel") } }
        )
    }
}
