package com.arena.marketradar.ui.screens.copytrading

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RestartAlt
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.data.model.Trader
import com.arena.marketradar.data.model.CopyTradeState
import com.arena.marketradar.data.model.PriceUnit
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.ui.theme.Green
import com.arena.marketradar.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyTradingScreen(onBack: () -> Unit, viewModel: CopyTradingViewModel = viewModel(factory = VMFactory { CopyTradingViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "کپی‌ترید (شبیه‌سازی)" else "Copy Trading (Simulation)") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
            actions = { IconButton(onClick = { viewModel.reset() }) { Icon(Icons.Outlined.RestartAlt, null) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            // Wallet summary
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(if (lang == "fa") "کیف پول مجازی" else "Virtual wallet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(Formatters.money(state.state.balance, PriceUnit.TOMAN), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text("${if (lang == "fa") "سود/زیان" else "P&L"}: ${Formatters.money(state.state.totalPnl, PriceUnit.TOMAN)}", color = if (state.state.totalPnl >= 0) Green else Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${if (lang == "fa") "نرخ برد" else "Win rate"}: ${(state.state.winRate * 100).toInt()}%", fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(if (lang == "fa") "مبنای برتری (بیت‌کوین): ${Formatters.percent(state.state.benchmarkReturn)}" else "Benchmark (BTC): ${Formatters.percent(state.state.benchmarkReturn)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Followed traders
            if (state.state.followers.isNotEmpty()) {
                item { SectionLabel(if (lang == "fa") "معامله‌گران دنبال‌شده" else "Following", lang) }
                items(state.state.followers, key = { it.traderId }) { f ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(f.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.Bold)
                                Text("${f.strategy} • ${f.style}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(Formatters.money(f.equity, PriceUnit.TOMAN), fontWeight = FontWeight.Bold)
                                Text("${Formatters.signed(f.pnl)} (${Formatters.percent(f.pnlPercent)})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (f.pnl >= 0) Green else Red)
                            }
                            TextButton(onClick = { viewModel.unfollow(f.traderId) }) { Text(if (lang == "fa") "توقف" else "Stop", fontSize = 11.sp) }
                        }
                    }
                }
            }

            // Leaderboard
            item { SectionLabel(if (lang == "fa") "رتبه‌بندی معامله‌گران" else "Leaderboard", lang) }
            items(state.roster, key = { it.id }) { trader ->
                TraderRow(trader, state.followedIds.contains(trader.id), lang, onFollow = { viewModel.follow(trader, 1_000_000.0) }, onUnfollow = { viewModel.unfollow(trader.id) })
            }

            item {
                Text(if (lang == "fa") "⚠️ این کپی‌ترید فقط شبیه‌سازی و بدون پول واقعی است؛ هدف آن شناخت مفهوم و سنجش عملکرد معامله‌گران است و توصیهٔ سرمایه‌گذاری نیست." else "⚠️ This is a simulation only, no real money; for educational purposes and not investment advice.",
                    modifier = Modifier.padding(16.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionLabel(title: String, lang: String) {
    Text(title, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TraderRow(t: Trader, followed: Boolean, lang: String, onFollow: () -> Unit, onUnfollow: () -> Unit) {
    val monthly = com.arena.marketradar.data.repo.CopyTradingRepository.monthlyReturn(t)
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, fontWeight = FontWeight.Bold)
                Text("${t.strategy} • ${t.style}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${if (lang == "fa") "برد" else "Win"}: ${(t.winRate * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("${if (lang == "fa") "۳۰ روزه" else "30d"}: ${Formatters.percent(monthly)}", fontSize = 11.sp, color = if (monthly >= 0) Green else Red)
                    Spacer(Modifier.width(8.dp))
                    Text("${t.followers}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (followed) {
                TextButton(onClick = onUnfollow) { Text(if (lang == "fa") "توقف" else "Stop", fontSize = 11.sp) }
            } else {
                Button(onClick = onFollow, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(if (lang == "fa") "دنبال‌کردن" else "Follow", fontSize = 12.sp)
                }
            }
        }
    }
}
