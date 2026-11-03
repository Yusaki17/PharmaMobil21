package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto

class ProductoLocal(base: PharmaDb) {

    private val consultas = base.productoQueries

    fun observar(): Flow<List<Producto>> =
        consultas.listarTodos()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.aDominio() } }

    fun pendientes(): List<Producto> =
        consultas.listarPendientes().executeAsList().map { it.aDominio() }

    fun guardarPendiente(producto: Producto): Producto {
        val menor = consultas.siguienteIdLocal().executeAsOne()
        val local = producto.copy(id = minOf(menor, 0L) - 1L)
        consultas.insertar(
            id = local.id,
            nombre = local.nombre,
            precio = local.precio,
            stock = local.stock.toLong(),
            descripcion = local.descripcion,
            categoria = local.categoria,
            estado = if (local.estado) 1L else 0L,
            pendiente = 1L
        )
        return local
    }

    fun reemplazarSincronizados(productos: List<Producto>) {
        consultas.transaction {
            consultas.borrarSincronizados()
            productos.forEach {
                consultas.insertar(
                    id = it.id,
                    nombre = it.nombre,
                    precio = it.precio,
                    stock = it.stock.toLong(),
                    descripcion = it.descripcion,
                    categoria = it.categoria,
                    estado = if (it.estado) 1L else 0L,
                    pendiente = 0L
                )
            }
        }
    }

    fun marcarSincronizado(idLocal: Long, idReal: Long) {
        consultas.marcarSincronizado(idReal = idReal, idLocal = idLocal)
    }
}

private fun pe.edu.upeu.pharmamobil.db.Producto.aDominio(): Producto =
    Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock.toInt(),
        descripcion = descripcion,
        categoria = categoria,
        estado = estado == 1L,
        pendiente = pendiente == 1L
    )