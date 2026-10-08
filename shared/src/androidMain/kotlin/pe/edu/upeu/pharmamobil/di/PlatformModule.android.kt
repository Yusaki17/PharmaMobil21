package pe.edu.upeu.pharmamobil.di

import org.koin.core.module.Module
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    // androidContext() es una función de Koin que nos da el contexto de la app
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}