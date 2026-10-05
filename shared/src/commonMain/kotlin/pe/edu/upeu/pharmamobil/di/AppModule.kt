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
import pe.edu.upeu.pharmamobil.domain.usecase.*
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

// 1. Módulo de Red
val networkModule = module {
    single { crearHttpClient() } // Asegúrate de que esta función no requiera parámetros o ajusta según tu HttpClientFactory
    single { ProductoApi(get()) }

    //  CORREGIDO: ProductoRepositoryImpl ahora requiere 2 parámetros (api y categoriaPorDefecto)
    single<ProductoRepository> {
        ProductoRepositoryImpl(get(), categoriaPorDefecto = 1L)
    }
}

// 2. Módulo de Datos
val dataModule = module {
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }
}

// 3. Módulo de Dominio
val domainModule = module {
    // Productos
    factory { ListarProductosUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }

    // Clientes
    factory { RegistrarClienteUseCase(get()) }
    factory { ListarClientesUseCase(get()) }
}

// 4. Módulo de Presentación
val presentationModule = module {
    viewModel {
        ProductoViewModel(
            get(), // RegistrarProductoUseCase
            get(), // ListarProductosUseCase
            get(), // ActualizarProductoUseCase
            get()  // EliminarProductoUseCase
        )
    }

    viewModel {
        ClienteViewModel(get(), get())
    }
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