package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {

    /** Incorpora el producto al inventario y devuelve el producto ya identificado. */
    suspend fun registrar(producto: Producto): Result<Long>

    /** Entrega el inventario completo en el orden en que fue registrado. */
    suspend fun listar(): Result<List<Producto>>
}