package com.arena.marketradar.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arena.marketradar.MarketRadarApplication

/** Composable-safe accessor to the DI holder (the Application). */
@Composable
fun app(): MarketRadarApplication =
    LocalContext.current.applicationContext as MarketRadarApplication

/** Non-composable accessor. Safe to call from plain lambdas (e.g. ViewModel factories). */
fun localApp(): MarketRadarApplication = MarketRadarApplication.instance

/** Reactive current language so the whole UI switches instantly. */
@Composable
fun collectLang(): String {
    val a = app()
    val lang by a.settings.language.collectAsStateWithLifecycle()
    return lang
}

/** Tiny generic ViewModel factory. */
class VMFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
