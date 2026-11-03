package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * viewModelScope corre sobre Dispatchers.Main, que en una prueba no existe:
 * setMain lo sustituye por un dispatcher de prueba antes de cada caso.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun instalarMain() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restaurarMain() {
        Dispatchers.resetMain()
    }

    private fun nuevoViewModel(
        repositorio: FakeProductoRepository = FakeProductoRepository()
    ) = ProductoViewModel(
        registrarProducto = RegistrarProductoUseCase(repositorio),
        listarProductos = ListarProductosUseCase(repositorio),
        actualizarProducto = ActualizarProductoUseCase(repositorio),
        eliminarProducto = EliminarProductoUseCase(repositorio),
        repositorio = repositorio
    )

    @Test
    fun distinguePendientesDeFallidasYConservaElMotivo() = runTest {
        val repositorio = FakeProductoRepository(mutableListOf(
            Producto(id = -1, nombre = "Pendiente", precio = 10.0, pendiente = true),
            Producto(id = -2, nombre = "Fallida", precio = 12.0, pendiente = true,
                fallida = true, ultimoError = "Conflicto con el servidor"),
            Producto(id = 3, nombre = "Sincronizado", precio = 15.0)
        ))
        val estado = nuevoViewModel(repositorio).uiState.value
        assertEquals(1L, estado.pendientes)
        assertEquals(1L, estado.fallidas)
        val lista = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)
        val fallida = lista.productos.first { it.id == -2L }
        assertTrue(fallida.fallida)
        assertEquals("Conflicto con el servidor", fallida.ultimoError)
    }

    @Test
    fun arrancaEnSinProductosCuandoElInventarioEstaVacio() = runTest {

        val viewModel = nuevoViewModel()

        assertEquals(ProductoUiState.Fase.SinProductos, viewModel.uiState.value.fase)
    }

    @Test
    fun muestraElInventarioConElPrecioYaFormateado() = runTest {

        val repositorio = FakeProductoRepository(
            mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol", precio = 12.5, stock = 5)
            )
        )

        val fase = assertIs<ProductoUiState.Fase.ConProductos>(
            nuevoViewModel(repositorio).uiState.value.fase
        )

        assertEquals("S/ 12.50", fase.productos.first().precio)
        assertEquals("5 u.", fase.productos.first().stock)
        assertTrue(fase.productos.first().requiereReposicion)
    }

    @Test
    fun pasaAFaseErrorCuandoElRepositorioFalla() = runTest {

        val repositorio = FakeProductoRepository().apply {
            fallaAlListar = IllegalStateException("Sin conexión")
        }

        val estado = nuevoViewModel(repositorio).uiState.value
        assertIs<ProductoUiState.Fase.Error>(estado.fase)
        assertEquals("No se pudo sincronizar. Los datos guardados se conservan.", estado.avisoRed)
    }

    @Test
    fun losErroresDeValidacionCaenEnElFormularioNoEnLaFase() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("")
        viewModel.onPrecioChange("abc")
        viewModel.onStockChange("-1")
        viewModel.registrar()

        val estado = viewModel.uiState.value

        assertEquals("El nombre es obligatorio", estado.formulario.nombreError)
        assertEquals("El precio debe ser un número válido", estado.formulario.precioError)
        assertEquals("El stock no puede ser negativo", estado.formulario.stockError)
        assertEquals(ProductoUiState.Fase.SinProductos, estado.fase)
        assertNull(estado.mensajeExito)
    }

    @Test
    fun registrarLimpiaElFormularioYRecargaElInventario() = runTest {

        val viewModel = nuevoViewModel()

        viewModel.onNombreChange("Paracetamol")
        viewModel.onPrecioChange("12.50")
        viewModel.onStockChange("5")
        viewModel.registrar()

        val estado = viewModel.uiState.value
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(estado.fase)

        assertEquals("Paracetamol", fase.productos.single().nombre)
        assertEquals("", estado.formulario.nombre)
        assertEquals("", estado.formulario.precio)
        assertEquals("", estado.formulario.stock)
        assertEquals(
            "Producto registrado correctamente",
            estado.mensajeExito
        )
    }
    @Test
    fun editarPermiteGuardarLosCambios() = runTest {
        val vm = nuevoViewModel(FakeProductoRepository(mutableListOf(
            Producto(id = 1, nombre = "Paracetamol", precio = 12.5, stock = 5)
        )))
        val producto = assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase).productos.single()
        vm.prepararEdicion(producto)
        assertEquals(ProductoUiState.Operacion.Inactiva, vm.uiState.value.operacion)
        assertTrue(vm.uiState.value.editando)
        vm.onNombreChange("Paracetamol actualizado")
        vm.registrar()
        assertEquals("Paracetamol actualizado", assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase).productos.single().nombre)
        assertEquals(false, vm.uiState.value.editando)
    }
    @Test
    fun cancelarEdicionLimpiaElFormulario() = runTest {
        val vm = nuevoViewModel(FakeProductoRepository(mutableListOf(
            Producto(id = 1, nombre = "Paracetamol", precio = 12.5, stock = 5)
        )))
        vm.prepararEdicion(assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase).productos.single())
        vm.cancelarEdicion()
        assertEquals(false, vm.uiState.value.editando)
        assertEquals("", vm.uiState.value.formulario.nombre)
    }
}


