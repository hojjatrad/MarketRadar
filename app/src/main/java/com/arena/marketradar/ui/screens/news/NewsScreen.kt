package com.arena.marketradar.ui.screens.news

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.NewsItem
import com.arena.marketradar.domain.util.Formatters
import com.arena.marketradar.domain.util.L
import com.arena.marketradar.domain.util.l
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val loading: Boolean = true,
    val all: List<NewsItem> = emptyList(),
    val filter: String = "fa",   // "all" | "fa" | "en"
)

class NewsViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val _state = MutableStateFlow(NewsUiState())
    val state: StateFlow<NewsUiState> = _state
    init { load() }
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            _state.update { it.copy(loading = false, all = app.news.fetch()) }
        }
    }
    fun setFilter(f: String) = _state.update { it.copy(filter = f) }
}

@Composable
fun NewsScreen(viewModel: NewsViewModel = viewModel(factory = VMFactory { NewsViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()
    val context = LocalContext.current

    val filtered = when (state.filter) {
        "fa" -> state.all.filter { it.isPersian }
        "en" -> state.all.filter { !it.isPersian }
        else -> state.all
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(if (lang == "fa") "اخبار و احساسات بازار" else "News & Market Sentiment",
                modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Text(L.newsNote.l(lang), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(state.filter == "fa", { viewModel.setFilter("fa") }, label = { Text("${L.newsFa.l(lang)} (${state.all.count { it.isPersian }})") })
                FilterChip(state.filter == "en", { viewModel.setFilter("en") }, label = { Text("${L.newsEn.l(lang)} (${state.all.count { !it.isPersian }})") })
                FilterChip(state.filter == "all", { viewModel.setFilter("all") }, label = { Text("${L.newsAll.l(lang)} (${state.all.size})") })
            }
        }
        if (state.loading) {
            item { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        }
        if (filtered.isEmpty() && !state.loading) {
            item { Text(L.emptyNews.l(lang), modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(filtered, key = { it.link + "_" + it.published + "_" + it.title }) { n ->
            NewsRow(n, lang) { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun NewsRow(n: NewsItem, lang: String, onOpen: (String) -> Unit) {
    val sentimentColor = when (n.sentimentLabel) {
        "+" -> MaterialTheme.colorScheme.primary
        "-" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onOpen(n.link) },
        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (n.isPersian) "خبر" else "News", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                if (n.assets.isNotEmpty()) {
                    Text(n.assets.joinToString(" · "), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                }
                Text(n.sentimentLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = sentimentColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(if (lang == "fa" && !n.titleFa.isNullOrBlank()) n.titleFa else n.title, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 4)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.source, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(Formatters.timeShort(n.published), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
