package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

fun ProductoDto.toDomain(): Producto = Producto(
    id = this.id.toLong(),
    nombre = this.title,
    precio = this.price,
    descripcion = this.description,
    categoria = this.categoria?.name ?: "Sin categoría",
    stock = 0 // La API de práctica no envía stock
)