package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

fun ProductoDto.toDomain(): Producto {
    return Producto(
        id = this.id.toLong(),
        nombre = this.title,
        precio = this.price,
        stock = 0 // La API de práctica no envía stock. Tu dominio exige >= 0, así que 0 es válido.
    )
}