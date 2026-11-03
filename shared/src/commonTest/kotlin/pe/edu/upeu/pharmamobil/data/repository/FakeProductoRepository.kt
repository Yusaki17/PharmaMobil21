package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class FakeProductoRepository(
    productos: MutableList<Producto> = mutableListOf()
) : ProductoRepository {

    private val estado = MutableStateFlow(productos.toList())

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null

    private var siguienteId =
        (productos.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observar(): Flow<List<Producto>> = estado

    override suspend fun registrar(producto: Producto): Producto {
        fallaAlRegistrar?.let { throw it }

        val guardado = producto.copy(id = siguienteId++)
        estado.value = estado.value + guardado

        return guardado
    }

    override suspend fun actualizar(producto: Producto): Producto {
        estado.value = estado.value.map {
            if (it.id == producto.id) producto else it
        }

        return producto
    }

    override suspend fun eliminar(id: Long) {
        estado.value = estado.value.filterNot { it.id == id }
    }

    override suspend fun sincronizar() {
        fallaAlListar?.let { throw it }
    }
}