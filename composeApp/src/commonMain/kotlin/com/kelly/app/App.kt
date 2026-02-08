package com.kelly.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.kelly.app.presentation.RootComponent
import com.kelly.app.presentation.calibration.CalibrationScreen
import com.kelly.app.presentation.common.KellyTheme
import com.kelly.app.presentation.common.KeepScreenOn
import com.kelly.app.presentation.common.StatusBar
import com.kelly.app.presentation.connection.ConnectionScreen
import com.kelly.app.presentation.bms.BmsScreen
import com.kelly.app.presentation.dashboard.DashboardScreen
import com.kelly.app.presentation.monitor.MonitorScreen
import com.kelly.app.presentation.settings.SettingsScreen
import kotlinx.coroutines.launch

private data class TabItem(
    val title: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem("Dashboard", Icons.Default.Home),
    TabItem("BMS", Icons.Default.BatteryChargingFull),
    TabItem("Calibration", Icons.Default.Build),
    TabItem("Monitor", Icons.AutoMirrored.Filled.List),
    TabItem("Settings", Icons.Default.Settings)
)

@Composable
fun App(rootComponent: RootComponent) {
    KellyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Children(
                stack = rootComponent.stack,
                animation = stackAnimation(slide())
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Connection -> {
                        ConnectionScreen(instance.component)
                    }
                    is RootComponent.Child.Main -> {
                        MainScreen(instance)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(main: RootComponent.Child.Main) {
    KeepScreenOn()

    val connectionState by main.connectionState.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    // Reload dashboard settings when returning from settings tab
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 0) {
            main.dashboardComponent.reloadSettings()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            StatusBar(
                connectionState = connectionState,
                onDisconnect = main.onDisconnect
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> DashboardScreen(main.dashboardComponent)
                1 -> BmsScreen(main.bmsComponent)
                2 -> CalibrationScreen(main.calibrationComponent)
                3 -> MonitorScreen(main.monitorComponent)
                4 -> SettingsScreen(main.settingsComponent)
            }
        }
    }
}
