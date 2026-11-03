package pe.edu.upeu.pharmamobil.di

import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ClienteRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.*
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteViewModel
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.sesion.LoginViewModel

// 1. Módulo de Red
val networkModule = module {
    single { crearHttpClient(get(), get()) }
    single { ProductoApi(get()) }
    single<ProductoRepository> { ProductoRepositorioOffline(get(), get()) }
    single(createdAtStart = true) {
        PharmaDb(get<CreadorDeDriver>().crear()).apply {
            productoQueries.listarTodos().executeAsList()
        }
    }
    single { ProductoLocal(get()) }
}

// 2. Módulo de Datos
val dataModule = module {
    single<ClienteRepository> { ClienteRepositorioEnMemoria() }

    // Autenticación
    single<AuthRepository> { AuthRepositorioRest(get(), get()) }
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

    // Autenticación
    factory { IniciarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
}

// 4. Módulo de Presentación
val presentationModule = module {
    viewModel {
        ProductoViewModel(
            get(), // RegistrarProductoUseCase
            get(), // ListarProductosUseCase
            get(), // ActualizarProductoUseCase
            get(), // EliminarProductoUseCase
            get()  // ✅ NUEVO: ProductoRepository (para sincronizar)
        )
    }

    viewModel {
        ClienteViewModel(get(), get())
    }

    viewModel {
        DetalleProductoViewModel(get())
    }

    // ViewModel de Login
    viewModel { LoginViewModel(get()) }
}

fun initKoin(configuracionAdicional: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracionAdicional()
        modules(
            networkModule,
            dataModule,
            domainModule,
            presentationModule,
            platformModule // Contiene AlmacenSeguro, HttpClientEngine y CreadorDeDriver
        )
    }
}