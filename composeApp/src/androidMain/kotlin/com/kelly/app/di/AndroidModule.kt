package com.kelly.app.di

import com.kelly.app.AndroidSettingsStore
import com.kelly.app.data.transport.TransportFactory
import com.kelly.app.domain.SettingsStore
import com.kelly.app.transport.AndroidTransportFactory
import org.koin.dsl.module

val androidModule = module {
    single<TransportFactory> { AndroidTransportFactory(get()) }
    single<SettingsStore> { AndroidSettingsStore(get()) }
}
