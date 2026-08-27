package com.arena.marketradar.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.PriceUnit
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.localApp
import com.arena.marketradar.work.AlertScheduler
import kotlin.math.roundToInt

class SettingsViewModel(private val app: MarketRadarApplication) : ViewModel() {
    val language = app.settings.language
    val darkMode = app.settings.darkMode
    val priceUnit = app.settings.priceUnit
    val refreshMinutes = app.settings.refreshMinutes
    val notifPrice = app.settings.notifPrice
    val notifTrend = app.settings.notifTrend
    val notifNews = app.settings.notifNews
    val persianDigits = app.settings.persianDigits
    val offline = app.settings.offline
    val dailyReport = app.settings.dailyReport

    fun setLang(v: String) = app.settings.setLanguage(v)
    fun setDark(v: Boolean) = app.settings.setDarkMode(v)
    fun setUnit(v: PriceUnit) = app.settings.setPriceUnit(v)
    fun setRefresh(v: Int) { app.settings.setRefreshMinutes(v); AlertScheduler.schedule(app) }
    fun setNPrice(v: Boolean) = app.settings.setNotifPrice(v)
    fun setNTrend(v: Boolean) = app.settings.setNotifTrend(v)
    fun setNNews(v: Boolean) = app.settings.setNotifNews(v)
    fun setPersianDigits(v: Boolean) = app.settings.setPersianDigits(v)
    fun setOffline(v: Boolean) = app.settings.setOffline(v)
    fun setDailyReport(v: Boolean) = app.settings.setDailyReport(v)
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = VMFactory { SettingsViewModel(localApp()) })) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val dark by viewModel.darkMode.collectAsStateWithLifecycle()
    val unit by viewModel.priceUnit.collectAsStateWithLifecycle()
    val refresh by viewModel.refreshMinutes.collectAsStateWithLifecycle()
    val nPrice by viewModel.notifPrice.collectAsStateWithLifecycle()
    val nTrend by viewModel.notifTrend.collectAsStateWithLifecycle()
    val nNews by viewModel.notifNews.collectAsStateWithLifecycle()
    val digits by viewModel.persianDigits.collectAsStateWithLifecycle()
    val offline by viewModel.offline.collectAsStateWithLifecycle()
    val daily by viewModel.dailyReport.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize()) {
        item { Text(if (lang == "fa") "تنظیمات" else "Settings", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

        item { SettingCard(if (lang == "fa") "زبان" else "Language") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(lang == "fa", { viewModel.setLang("fa") }, label = { Text("فارسی") })
                FilterChip(lang == "en", { viewModel.setLang("en") }, label = { Text("English") })
            }
        } }

        item { SettingCard(if (lang == "fa") "حالت تاریک" else "Dark mode") {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(if (lang == "fa") "فعال" else "On", modifier = Modifier.weight(1f)); Switch(checked = dark, onCheckedChange = { viewModel.setDark(it) }) }
        } }

        item { SettingCard(if (lang == "fa") "واحد قیمت" else "Price unit") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(unit == PriceUnit.TOMAN, { viewModel.setUnit(PriceUnit.TOMAN) }, label = { Text(L.toman.l(lang)) })
                FilterChip(unit == PriceUnit.RIAL, { viewModel.setUnit(PriceUnit.RIAL) }, label = { Text(L.rial.l(lang)) })
            }
        } }

        item { SettingCard(if (lang == "fa") "بازه بروزرسانی" else "Refresh interval") {
            Text("$refresh ${L.minutes.l(lang)}", fontSize = 13.sp)
            Slider(value = refresh.toFloat(), onValueChange = { viewModel.setRefresh(it.roundToInt()) }, valueRange = 15f..60f, steps = 8)
        } }

        item { SettingCard(L.notifications.l(lang)) {
            ToggleRow(L.notifPriceAlerts.l(lang), nPrice) { viewModel.setNPrice(it) }
            ToggleRow(L.notifTrendAlerts.l(lang), nTrend) { viewModel.setNTrend(it) }
            ToggleRow(L.notifNewsAlerts.l(lang), nNews) { viewModel.setNNews(it) }
            ToggleRow(L.dailyReport.l(lang), daily) { viewModel.setDailyReport(it) }
        } }

        item { SettingCard(if (lang == "fa") "نمایش" else "Display") {
            ToggleRow(if (lang == "fa") "ارقام فارسی" else "Persian digits", digits) { viewModel.setPersianDigits(it) }
            ToggleRow(if (lang == "fa") "حالت آفلاین (نمایش آخرین داده)" else "Offline mode (last data)", offline) { viewModel.setOffline(it) }
        } }

        item { Text(L.disclaimer.l(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }

        item { SettingCard(if (lang == "fa") "منابع داده" else "Data sources") {
            Text(if (lang == "fa") "بازار ایران: baha24.com\nطلا: gold-api.com\nارز دیجیتال: CoinGecko\nارز جهانی: Frankfurter (ECB)\nخبر: Google News (فارسی + انگلیسی)" else "Iran market: baha24.com\nGold: gold-api.com\nCrypto: CoinGecko\nGlobal FX: Frankfurter (ECB)\nNews: Google News (Persian + English)",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp)); content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
