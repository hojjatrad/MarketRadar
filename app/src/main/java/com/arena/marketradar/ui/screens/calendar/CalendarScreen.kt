package com.arena.marketradar.ui.screens.calendar

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.arena.marketradar.data.model.EconEvent
import com.arena.marketradar.data.repo.EconomicCalendarRepository
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<EconEvent>>(emptyList())
    val events: StateFlow<List<EconEvent>> = _events
    init { viewModelScope.launch { _events.update { EconomicCalendarRepository().upcoming() } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, viewModel: CalendarViewModel = viewModel(factory = VMFactory { CalendarViewModel() })) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val lang = collectLang()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "تقویم اقتصاد" else "Economic Calendar") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } },
            actions = { Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.padding(end = 14.dp)) }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Text(if (lang == "fa") "رویدادهای تأثیرگذار بر بازار (اطلاع‌رسانی)" else "Events that may move markets (informational)",
                    modifier = Modifier.padding(16.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(events, key = { it.title + it.date }) { e -> EventRow(e, lang) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EventRow(e: EconEvent, lang: String) {
    val impactColor = when (e.impact) {
        "HIGH" -> com.arena.marketradar.ui.theme.Red
        "MEDIUM" -> com.arena.marketradar.ui.theme.Amber
        else -> com.arena.marketradar.ui.theme.Neutral
    }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.country, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = impactColor)
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == "fa") e.tagFa else e.tagEn, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(if (lang == "fa") e.title else e.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatDate(e.date), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(formatTime(e.date), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatDate(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(millis))
private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(millis))
