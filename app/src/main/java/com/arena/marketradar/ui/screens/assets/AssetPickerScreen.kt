package com.arena.marketradar.ui.screens.assets

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.domain.util.Constants
import com.arena.marketradar.data.model.AssetDefinition
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp

class AssetPickerViewModel(private val app: MarketRadarApplication) : ViewModel() {
    fun current(): Set<String> = app.settings.watchlist().ifEmpty { Constants.DEFAULT_WATCHLIST }.toSet()
    fun save(sel: List<String>) { app.settings.saveWatchlist(sel) }
}

/**
 * Big, searchable, grouped list of every trackable asset (Iran fiat, gold coins,
 * crypto). Selecting here defines the "universe" that drives the home list, the
 * forecast, the report and the alerts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetPickerScreen(onSave: (List<String>) -> Unit, onBack: () -> Unit, viewModel: AssetPickerViewModel = viewModel(factory = VMFactory { AssetPickerViewModel(localApp()) })) {
    val lang = collectLang()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(viewModel.current()) }

    val all = Constants.ALL_SELECTABLE
    val filtered = if (query.isBlank()) all else all.filter {
        it.symbol.contains(query, true) || (if (lang == "fa") it.nameFa else it.nameEn).contains(query, true)
    }

    // Group by market section, preserving ordering (crypto, metal, fiat).
    val grouped = filtered.groupBy { Constants.groupKey(it.type, it.scope) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "انتخاب دارایی‌ها برای گزارش و هشدار" else "Select assets for report & alerts") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
            actions = {
                TextButton(onClick = { viewModel.save(selected.toList()); onSave(selected.toList()) }) {
                    Text(if (lang == "fa") "ذخیره" else "Save")
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val countBadge = "${selected.size} ${if (lang == "fa") "انتخاب" else "selected"}"
            Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(countBadge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lang == "fa") "گزارش و هشدارها فقط برای این موارد ساخته می‌شود." else "Report & alerts are generated only for these.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text(if (lang == "fa") "جستجوی نماد یا نام…" else "Search symbol or name…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            LazyColumn(Modifier.fillMaxWidth()) {
                grouped.forEach { (groupKey, items) ->
                    item { SectionLabel(sectionTitle(groupKey, lang)) }
                    items(items, key = { groupKey + it.symbol }) { asset ->
                        AssetRow(asset, selected.contains(asset.symbol), lang) {
                            val s = selected.toMutableSet()
                            if (!s.add(asset.symbol)) s.remove(asset.symbol)
                            selected = s
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

private fun sectionTitle(groupKey: String, lang: String): String = when (groupKey) {
    "crypto" -> if (lang == "fa") "ارز دیجیتال" else "Cryptocurrency"
    "metal_iran" -> if (lang == "fa") "طلا و سکه (بازار ایران)" else "Gold & Coins (Iran)"
    "metal_global" -> if (lang == "fa") "فلزات جهانی" else "Global Metals"
    "fiat_iran" -> if (lang == "fa") "ارزها (بازار ایران)" else "Currencies (Iran)"
    else -> groupKey
}

@Composable
private fun SectionLabel(title: String) {
    Text(title, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AssetRow(asset: AssetDefinition, checked: Boolean, lang: String, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clickable { onToggle() },
        shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(asset.emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (lang == "fa") asset.nameFa else asset.nameEn, fontWeight = FontWeight.Medium)
                Text(asset.symbol, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
