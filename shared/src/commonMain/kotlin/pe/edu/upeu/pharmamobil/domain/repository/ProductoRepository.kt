package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {
    fun observar(): Flow<List<Producto>>
    suspend fun registrar(producto: Producto): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
    suspend fun reintentarEnvio(id: Long) {
        throw UnsupportedOperationException("Reintento no disponible en este repositorio")
    }
    suspend fun sincronizarRevisadas() {
        throw UnsupportedOperationException("Reenvío no disponible")
    }
    suspend fun sincronizar()
}