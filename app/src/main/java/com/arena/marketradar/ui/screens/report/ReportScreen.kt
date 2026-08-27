package com.arena.marketradar.ui.screens.report

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.arena.marketradar.domain.report.ReportGenerator
import com.arena.marketradar.domain.util.Constants
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ReportUiState(
    val loading: Boolean = true,
    val prices: List<MarketPrice> = emptyList(),
    val forecasts: List<ForecastItem> = emptyList(),
    val generating: Boolean = false,
    val message: String? = null,
)

class ReportViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state

    init {
        viewModelScope.launch {
            try {
                val prices = app.market.getMarkets()
                val news = app.news.fetch()
                val forecasts = prices.mapNotNull { p ->
                    val hist = app.market.getHistory(p.symbol).map { it.value }
                    if (hist.size < 5) return@mapNotNull null
                    val rel = news.filter { Constants.newsKey(p.symbol) in it.assets }
                    val sent = if (rel.isNotEmpty()) rel.map { it.sentiment }.average() else 0.0
                    app.engine.forecast(p.symbol, p.nameFa, p.nameEn, hist, sent)
                }
                _state.update { it.copy(loading = false, prices = prices, forecasts = forecasts) }
            } catch (e: Exception) { _state.update { it.copy(loading = false) } }
        }
    }

    fun generate() {
        viewModelScope.launch {
            _state.update { it.copy(generating = true, message = null) }
            val lang = app.settings.language.value
            val file = ReportGenerator.generate(app, _state.value.prices, _state.value.forecasts, lang)
            val msg = if (file != null) { openIntent(file, "application/pdf"); L.reportCreated.l(lang) } else L.reportFailed.l(lang)
            _state.update { it.copy(generating = false, message = msg) }
        }
    }

    fun share() {
        viewModelScope.launch {
            _state.update { it.copy(generating = true, message = null) }
            val lang = app.settings.language.value
            var file = ReportGenerator.generate(app, _state.value.prices, _state.value.forecasts, lang)
            if (file == null) { _state.update { it.copy(generating = false, message = L.reportFailed.l(lang)) }; return@launch }
            val png = ReportGenerator.renderToPng(app, file)
            if (png != null) file = png
            val msg = if (shareIntent(file)) L.reportShared.l(lang) else L.reportFailed.l(lang)
            _state.update { it.copy(generating = false, message = msg) }
        }
    }

    private fun openIntent(file: File, mime: String) {
        try {
            val uri = FileProvider.getUriForFile(app, "com.arena.marketradar.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun shareIntent(file: File): Boolean = try {
        val uri = FileProvider.getUriForFile(app, "com.arena.marketradar.fileprovider", file)
        val mime = if (file.extension == "png") "image/png" else "application/pdf"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(chooser)
        true
    } catch (e: Exception) { false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit, viewModel: ReportViewModel = viewModel(factory = VMFactory { ReportViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "گزارش PDF" else "PDF Report") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            if (state.loading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
            }
            if (!state.loading) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Outlined.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(if (lang == "fa") "خلاصهٔ هفتگی بازار" else "Weekly Market Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(if (lang == "fa") "این گزارش شامل قیمت‌های زنده، سیگنال‌های پیش‌بینی و سلب‌مسئولیت ریسک است و به‌صورت فایل PDF روی تلفن شما ذخیره و باز می‌شود. می‌توانید نسخهٔ تصویری آن را هم اشتراک بگذارید." else "Includes live prices, forecast signals and a risk disclaimer. Saved and opened as a PDF on your device. You can also share an image version.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.generate() }, enabled = !state.generating, modifier = Modifier.fillMaxWidth()) {
                    if (state.generating) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    else Text(if (lang == "fa") "ساخت و باز کردن گزارش" else "Generate & open report")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { viewModel.share() }, enabled = !state.generating, modifier = Modifier.fillMaxWidth()) {
                    Text(if (lang == "fa") "اشتراک گزارش (تصویر)" else "Share report (image)")
                }
                state.message?.let {
                    Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
        }
    }
}
