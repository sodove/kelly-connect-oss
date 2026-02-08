package com.kelly.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.kelly.app.di.appModule
import com.kelly.app.di.desktopModule
import com.kelly.app.domain.SettingsStore
import com.kelly.app.domain.repository.BmsRepository
import com.kelly.app.domain.repository.KellyRepository
import com.kelly.app.presentation.RootComponent
import org.koin.core.context.startKoin
import javax.swing.SwingUtilities

fun main() {
    val koinApp = startKoin {
        modules(appModule, desktopModule)
    }

    val lifecycle = LifecycleRegistry()
    val repository: KellyRepository = koinApp.koin.get()
    val settingsStore: SettingsStore = koinApp.koin.get()
    val bmsRepository: BmsRepository = koinApp.koin.get()

    val rootComponent = runOnUiThread {
        RootComponent(
            componentContext = DefaultComponentContext(lifecycle),
            repository = repository,
            settingsStore = settingsStore,
            bmsRepository = bmsRepository
        )
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kelly Connect - KBLS/KLS Controller Configuration",
            state = rememberWindowState(width = 1024.dp, height = 768.dp)
        ) {
            App(rootComponent)
        }
    }
}

private fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }
    var result: T? = null
    SwingUtilities.invokeAndWait {
        result = block()
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
}
