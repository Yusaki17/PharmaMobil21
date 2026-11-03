package pe.edu.upeu.pharmamobil.domain.usecase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.repository.FakeProductoRepository
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
class ListarProductosUseCaseTest {
    @Test fun devuelveElInventarioDelRepositorio() = runTest {
        val repo = FakeProductoRepository(mutableListOf(Producto(id = 1, nombre = "Paracetamol", precio = 12.5, stock = 5)))
        assertEquals("Paracetamol", ListarProductosUseCase(repo)().first().single().nombre)
    }
    @Test fun reflejaLasAltasEnElInventario() = runTest {
        val repo = FakeProductoRepository()
        val listar = ListarProductosUseCase(repo)
        assertEquals(0, listar().first().size)
        repo.registrar(Producto(id = 0, nombre = "Ibuprofeno", precio = 8.9, stock = 3))
        assertEquals("Ibuprofeno", listar().first().single().nombre)
    }
}
