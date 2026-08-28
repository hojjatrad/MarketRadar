package com.arena.marketradar.ui.screens.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.arena.marketradar.MarketRadarApplication
import com.arena.marketradar.data.model.GitHubRelease
import com.arena.marketradar.domain.util.Version
import com.arena.marketradar.ui.app.VMFactory
import com.arena.marketradar.ui.app.collectLang
import com.arena.marketradar.ui.app.localApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = true,
    val available: GitHubRelease? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val message: String? = null,
    val currentVersion: String = "1.0",
)

class UpdateViewModel(private val app: MarketRadarApplication) : ViewModel() {
    private val repo = com.arena.marketradar.data.repo.UpdateRepository(app)
    private val _state = MutableStateFlow(UpdateUiState(currentVersion = repo.installedVersionName()))
    val state: StateFlow<UpdateUiState> = _state

    init { check() }

    fun check() {
        viewModelScope.launch {
            _state.update { it.copy(checking = true, message = null) }
            val rel = repo.checkForUpdate()
            if (rel != null) {
                _state.update {
                    it.copy(checking = false, available = rel, message = "نسخهٔ جدید ${rel.tagName} موجود است.")
                }
            } else {
                _state.update { it.copy(checking = false, available = null, message = "برنامهٔ شما به‌روز است (${repo.installedVersionName()}).") }
            }
        }
    }

    fun downloadAndInstall() {
        val rel = _state.value.available ?: return
        val asset = repo.apkAsset(rel) ?: run { _state.update { it.copy(message = "فایل نصب (APK) در این نسخه پیوست نشده.") }; return }
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, progress = 0f, message = "در حال دانلود نسخهٔ ${rel.tagName}…") }
            val file = repo.download(asset) { p -> _state.update { it.copy(progress = p) } }
            if (file != null) {
                _state.update { it.copy(downloading = false, progress = 1f, message = "دانلود کامل شد. برنامه را نصب می‌کنید؟") }
                repo.install(file)
            } else {
                _state.update { it.copy(downloading = false, message = "خطا در دانلود. دوباره تلاش کنید.") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(onBack: () -> Unit, viewModel: UpdateViewModel = viewModel(factory = VMFactory { UpdateViewModel(localApp()) })) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lang = collectLang()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (lang == "fa") "به‌روزرسانی برنامه" else "Update") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "back") } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
            Text(if (lang == "fa") "نسخهٔ نصب‌شده: ${state.currentVersion}" else "Installed: ${state.currentVersion}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (state.checking) {
                Text(if (lang == "fa") "در حال بررسی به‌روزرسانی…" else "Checking for updates…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            state.available?.let { rel ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${if (lang == "fa") "نسخهٔ جدید" else "New version"}: ${rel.tagName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        rel.body?.let { Text(it.take(300), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 5) }
                        Spacer(Modifier.height(12.dp))
                        if (state.downloading) {
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("${(state.progress * 100).toInt()}%", fontSize = 12.sp)
                        } else {
                            Button(onClick = { viewModel.downloadAndInstall() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Download, null); Spacer(Modifier.size(6.dp)); Text(if (lang == "fa") "دانلود و نصب" else "Download & install")
                            }
                        }
                    }
                }
            }

            state.message?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            OutlinedButton(onClick = { viewModel.check() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (lang == "fa") "بررسی مجدد" else "Check again")
            }
        }
    }
}
