package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositoryImpl(
    private val api: ProductoApi
) : ProductoRepository {

    override suspend fun listar(): Result<List<Producto>> = runCatching {
        api.obtenerProductos().map { it.toDomain() }
    }

    override suspend fun registrar(producto: Producto): Result<Long> {
        return Result.failure(UnsupportedOperationException("POST no implementado aún"))
    }
}