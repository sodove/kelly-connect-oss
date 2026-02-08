package com.kelly.app.di

import com.kelly.app.data.repository.KableBmsRepository
import com.kelly.app.data.repository.KellyRepositoryImpl
import com.kelly.app.domain.repository.BmsRepository
import com.kelly.app.domain.repository.KellyRepository
import com.kelly.app.domain.usecase.*
import org.koin.dsl.module

val appModule = module {
    single<KellyRepository> { KellyRepositoryImpl(get()) }
    single<BmsRepository> { KableBmsRepository() }
    factory { ConnectUseCase(get()) }
    factory { ReadCalibrationUseCase(get()) }
    factory { WriteCalibrationUseCase(get()) }
    factory { MonitorUseCase(get()) }
}
