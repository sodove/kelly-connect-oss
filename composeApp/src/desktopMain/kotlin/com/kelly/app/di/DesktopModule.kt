package com.kelly.app.di

import com.kelly.app.DesktopSettingsStore
import com.kelly.app.data.transport.TransportFactory
import com.kelly.app.domain.SettingsStore
import com.kelly.app.transport.DesktopTransportFactory
import org.koin.dsl.module

val desktopModule = module {
    single<TransportFactory> { DesktopTransportFactory() }
    single<SettingsStore> { DesktopSettingsStore() }
}
