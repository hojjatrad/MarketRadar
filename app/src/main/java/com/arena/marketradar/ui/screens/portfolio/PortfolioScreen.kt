package com.arena.marketradar.ui.screens.portfolio

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.arena.marketradar.data.model.PortfolioRow
import com.arena.marketradar.data.model.PriceUnit
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

data class PortfolioUiState(
    val loading: Boolean = true,
    val prices: List<MarketPrice> = emptyList(),
    val rows: List<PortfolioRow> = emptyList(),
)

class PortfolioViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state
    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val prices = app.market.getMarkets()
                val rows = app.portfolio.rows(prices)
                _state.update { it.copy(loading = false, prices = prices, rows = rows) }
            } catch (e: Exception) { _state.update { it.copy(loading = false) } }
        }
    }

    fun add(symbol: String, nameFa: String, unit: PriceUnit, qty: Double, buy: Double) {
        app.portfolio.add(symbol, nameFa, unit, qty, buy); load()
    }
    fun remove(id: String) { app.portfolio.remove(id); load() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(onBack: () -> Unit, viewModel: PortfolioViewModel = viewModel(factory = VMFactory { PortfolioViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    val totalCost = state.rows.sumOf { it.costValue }
    val totalMarket = state.rows.sumOf { it.marketValue }
    val totalPnl = totalMarket - totalCost
    val totalPct = if (totalCost > 0) totalPnl / totalCost * 100.0 else 0.0

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "سرمایه‌گذاری" else "Portfolio") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
            actions = { IconButton(onClick = { viewModel.load() }) { Icon(Icons.Outlined.Wallet, null) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp)) {
                        Text(if (lang == "fa") "ارزش کل پرتفوی" else "Total value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(Formatters.money(totalMarket, PriceUnit.TOMAN), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(Formatters.money(totalPnl, PriceUnit.TOMAN), color = if (totalPnl >= 0) Green else Red, fontWeight = FontWeight.Bold)
                            Text(Formatters.percent(totalPct), color = if (totalPnl >= 0) Green else Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { AddHoldingCard(state.prices, { s, n, u, q, b -> viewModel.add(s, n, u, q, b) }, lang) }
            item { Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Text(if (lang == "fa") "دارایی‌های من" else "My assets", fontWeight = FontWeight.Bold) } }
            if (state.rows.isEmpty()) {
                item { Text(if (lang == "fa") "هنوز دارایی‌ای ثبت نکرده‌اید." else "No holdings yet.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.rows, key = { it.holding.id }) { r -> HoldingRow(r, lang, { viewModel.remove(r.holding.id) }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HoldingRow(r: PortfolioRow, lang: String, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (lang == "fa") r.holding.nameFa else r.holding.symbol, fontWeight = FontWeight.Bold)
                Text("${Formatters.plain(r.holding.quantity)} × ${Formatters.money(r.holding.buyPrice, r.holding.unit)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Formatters.money(r.marketValue, r.holding.unit), fontWeight = FontWeight.Bold)
                Text("${Formatters.signed(r.pnl)} (${Formatters.percent(r.pnlPercent)})", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = if (r.pnl >= 0) Green else Red)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun AddHoldingCard(prices: List<MarketPrice>, onAdd: (String, String, PriceUnit, Double, Double) -> Unit, lang: String) {
    var show by androidx.compose.runtime.remember { mutableStateOf(false) }
    OutlinedButton(onClick = { show = true }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(if (lang == "fa") "+ افزودن دارایی" else "+ Add asset")
    }
    if (show) {
        var symbol by androidx.compose.runtime.remember { mutableStateOf("USD") }
        var qty by androidx.compose.runtime.remember { mutableStateOf("") }
        var buy by androidx.compose.runtime.remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(if (lang == "fa") "افزودن دارایی" else "Add asset") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("USD", "BTC", "ETH", "USDT", "XAU").forEach { s ->
                            OutlinedButton(onClick = { symbol = s }, modifier = Modifier.padding(2.dp)) { Text(s) }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text(if (lang == "fa") "نماد" else "Symbol") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text(if (lang == "fa") "مقدار" else "Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    val buyLabel = if (lang == "fa") "قیمت خرید (تومان)" else "Buy price (Toman)"
                    OutlinedTextField(value = buy, onValueChange = { buy = it }, label = { Text(buyLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = qty.toDoubleOrNull(); val b = buy.toDoubleOrNull(); val p = prices.firstOrNull { it.symbol == symbol }
                    if (q != null && b != null && q > 0 && b > 0 && p != null) onAdd(symbol, if (lang == "fa") p.nameFa else p.nameEn, p.unit, q, b)
                    show = false
                }) { Text(if (lang == "fa") "افزودن" else "Add") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(if (lang == "fa") "انصراف" else "Cancel") } }
        )
    }
}
