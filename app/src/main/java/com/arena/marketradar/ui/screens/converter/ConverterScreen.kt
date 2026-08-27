package com.arena.marketradar.ui.screens.converter

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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arena.marketradar.data.model.PriceUnit
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.app
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.app.collectLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ConverterUiState(
    val loading: Boolean = true,
    val prices: List<MarketPrice> = emptyList(),
    val lang: String = "fa",
)

class ConverterViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(ConverterUiState(lang = app.settings.language.value))
    val state: StateFlow<ConverterUiState> = _state
    val tomanPerUsd: Double get() = app.market.tomanPerUsd

    init { load() }
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            _state.update { it.copy(loading = false, prices = app.market.getMarkets()) }
        }
    }

    /** Toman value of one unit of the given asset. */
    fun tomanPerUnit(p: MarketPrice): Double = when (p.unit) {
        PriceUnit.TOMAN, PriceUnit.RIAL, PriceUnit.GRAM, PriceUnit.COIN -> p.price
        else -> (p.priceUsd ?: p.price) * (tomanPerUsd.coerceAtLeast(1.0))
    }

    fun convert(amount: Double, from: MarketPrice?, to: MarketPrice?): Pair<Double, PriceUnit> {
        if (from == null || to == null || amount <= 0) return Pair(0.0, to?.unit ?: PriceUnit.TOMAN)
        val af = tomanPerUnit(from)
        val at = tomanPerUnit(to)
        if (af <= 0 || at <= 0) return Pair(0.0, to.unit)
        val result = amount * af / at
        return Pair(result, to.unit)
    }
}

@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel(factory = VMFactory { ConverterViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    var amount by remember { mutableStateOf("1") }
    var fromSymbol by remember { mutableStateOf("USD") }
    var toSymbol by remember { mutableStateOf("EUR") }
    var pickerFor by remember { mutableStateOf<String?>(null) }

    val from = state.prices.firstOrNull { it.symbol == fromSymbol }
    val to = state.prices.firstOrNull { it.symbol == toSymbol }
    val amt = amount.toDoubleOrNull() ?: 0.0
    val (result, unit) = viewModel.convert(amt, from, to)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(if (lang == "fa") "مبدل ارز و طلا" else "Currency & Gold Converter",
                modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(value = amount, onValueChange = { amount = it },
                        label = { Text(L.amount.l(lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    // From
                    Text(L.from.l(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    SelectableAssetRow(from, lang) { pickerFor = "from" }
                    Spacer(Modifier.height(18.dp))
                    Icon(Icons.Outlined.SwapHoriz, null, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(18.dp))
                    Text(L.to.l(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    SelectableAssetRow(to, lang) { pickerFor = "to" }
                    Spacer(Modifier.height(24.dp))
                    Text(L.result.l(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("${Formatters.plain(amt)} ${from?.nameEn ?: ""} = ${Formatters.money(result, unit)}",
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        if (state.loading) {
            item { Text(L.loading.l(lang), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    if (pickerFor != null) {
        val target = pickerFor
        AssetPickerDialog(
            title = if (target == "from") L.from.l(lang) else L.to.l(lang),
            prices = state.prices,
            lang = lang,
            onPick = { symbol ->
                if (target == "from") fromSymbol = symbol else toSymbol = symbol
                pickerFor = null
            },
            onDismiss = { pickerFor = null }
        )
    }
}

@Composable
private fun SelectableAssetRow(p: MarketPrice?, lang: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (p != null) {
                Text(p.symbol.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                Text(if (lang == "fa") p.nameFa else p.nameEn, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text(Formatters.money(p.price, p.unit), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AssetPickerDialog(title: String, prices: List<MarketPrice>, lang: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                items(prices, key = { it.symbol }) { p ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(p.symbol) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.symbol.take(1).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(26.dp))
                        Text(if (lang == "fa") p.nameFa else p.nameEn, modifier = Modifier.weight(1f))
                        Text(Formatters.money(p.price, p.unit), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(L.cancel.l(lang)) } }
    )
}
