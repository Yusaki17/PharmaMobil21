package pe.edu.upeu.pharmamobil.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositoryImpl
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ListarClientesUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarClienteUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

// 1. Módulo de Red (NUEVO - Productos viene de la API)
val networkModule = module {
    single { crearHttpClient() }
    single { ProductoApi(get()) }
    single<ProductoRepository> { ProductoRepositoryImpl(get()) } // ✅ Solo este
}

// 2. Módulo de Datos (Solo Cliente en memoria, Productos ya NO)
val dataModule = module {
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }
    // ❌ ELIMINADO: single<ProductoRepository> { ProductoRepositorioEnMemoria() }
}

// 3. Módulo de Dominio
val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
    factory { ListarProductosUseCase(get()) }
    factory { RegistrarClienteUseCase(get()) }
    factory { ListarClientesUseCase(get()) }
}

// 4. Módulo de Presentación
val presentationModule = module {
    viewModel { ProductoViewModel(get(), get()) }
    viewModel { ClienteViewModel(get(), get()) }
}

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(
            networkModule,
            dataModule,
            domainModule,
            presentationModule
        )
    }
}