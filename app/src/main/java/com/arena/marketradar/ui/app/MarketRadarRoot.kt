package com.arena.marketradar.ui.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arena.marketradar.ui.screens.assets.AssetPickerScreen
import com.arena.marketradar.ui.screens.calendar.CalendarScreen
import com.arena.marketradar.ui.screens.converter.ConverterScreen
import com.arena.marketradar.ui.screens.copytrading.CopyTradingScreen
import com.arena.marketradar.ui.screens.detail.DetailScreen
import com.arena.marketradar.ui.screens.forecast.ForecastScreen
import com.arena.marketradar.ui.screens.home.HomeScreen
import com.arena.marketradar.ui.screens.news.NewsScreen
import com.arena.marketradar.ui.screens.paper.PaperTradingScreen
import com.arena.marketradar.ui.screens.portfolio.PortfolioScreen
import com.arena.marketradar.ui.screens.report.ReportScreen
import com.arena.marketradar.ui.screens.screener.ScreenerScreen
import com.arena.marketradar.ui.screens.settings.SettingsScreen
import com.arena.marketradar.ui.screens.update.UpdateScreen
import com.arena.marketradar.ui.theme.MarketRadarTheme
import kotlinx.coroutines.delay

private data class Tab(val route: String, val icon: ImageVector, val fa: String, val en: String)

private val tabs = listOf(
    Tab("home", Icons.Outlined.Home, "خانه", "Home"),
    Tab("forecast", Icons.Outlined.TrendingUp, "پیش‌بینی", "Forecast"),
    Tab("news", Icons.Outlined.Newspaper, "اخبار", "News"),
    Tab("converter", Icons.Outlined.Calculate, "مبدل", "Converter"),
    Tab("settings", Icons.Outlined.Settings, "تنظیمات", "Settings"),
)

@Composable
fun MarketRadarRoot() {
    val appInst = app()
    val lang by appInst.settings.language.collectAsStateWithLifecycle()
    val dark by appInst.settings.darkMode.collectAsStateWithLifecycle()

    MarketRadarTheme(darkTheme = dark) {
        var showSplash by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) { delay(1100); showSplash = false }

        if (showSplash) {
            SplashScreen()
        } else {
            MainContent()
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
            Text("رصد بازار", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            Text("ارز، طلا و ارز دیجیتال", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MainContent() {
    val appInst = app()
    val lang by appInst.settings.language.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides if (lang == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showBottomBar = currentRoute?.startsWith("detail") != true &&
            currentRoute != "portfolio" && currentRoute != "calendar" && currentRoute != "report" &&
            currentRoute != "paper" && currentRoute != "screener" && currentRoute != "picker" && currentRoute != "update" && currentRoute != "copytrade"

        Scaffold(bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(if (lang == "fa") tab.fa else tab.en, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }) { padding ->
            NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(padding)) {
                composable("home") {
                    HomeScreen(
                        onOpen = { sym -> navController.navigate("detail/$sym") },
                        onOpenPortfolio = { navController.navigate("portfolio") },
                        onOpenCalendar = { navController.navigate("calendar") },
                        onOpenReport = { navController.navigate("report") },
                        onOpenScreener = { navController.navigate("screener") },
                        onOpenPaper = { navController.navigate("paper") },
                        onOpenPicker = { navController.navigate("picker") },
                        onOpenUpdate = { navController.navigate("update") },
                        onOpenCopyTrade = { navController.navigate("copytrade") }
                    )
                }
                composable("forecast") { ForecastScreen(onOpen = { sym -> navController.navigate("detail/$sym") }) }
                composable("news") { NewsScreen() }
                composable("converter") { ConverterScreen() }
                composable("settings") { SettingsScreen() }
                composable("portfolio") { PortfolioScreen(onBack = { navController.popBackStack() }) }
                composable("calendar") { CalendarScreen(onBack = { navController.popBackStack() }) }
                composable("report") { ReportScreen(onBack = { navController.popBackStack() }) }
                composable("paper") { PaperTradingScreen(onBack = { navController.popBackStack() }, onOpen = { sym -> navController.navigate("detail/$sym") }) }
                composable("screener") { ScreenerScreen(onOpen = { sym -> navController.navigate("detail/$sym") }, onBack = { navController.popBackStack() }) }
                composable("picker") { AssetPickerScreen(onSave = { navController.previousBackStackEntry?.savedStateHandle?.set("sel", it); navController.popBackStack() }, onBack = { navController.popBackStack() }) }
                composable("update") { UpdateScreen(onBack = { navController.popBackStack() }) }
                composable("copytrade") { CopyTradingScreen(onBack = { navController.popBackStack() }) }
                composable(
                    route = "detail/{symbol}",
                    arguments = listOf(navArgument("symbol") { type = NavType.StringType })
                ) { entry ->
                    val symbol = entry.arguments?.getString("symbol") ?: ""
                    DetailScreen(symbol = symbol, onBack = { navController.popBackStack() })
                }
            }
        }

        // First-launch notification permission (Android 13+).
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        val ctx = LocalContext.current
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = android.content.pm.PackageManager.PERMISSION_GRANTED == ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
