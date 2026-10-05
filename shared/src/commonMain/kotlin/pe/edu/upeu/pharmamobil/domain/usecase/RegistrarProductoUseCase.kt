package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

data class ErroresDeProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null,
    val descripcion: String? = null,
    val categoria: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null || descripcion != null || categoria != null
}

class ProductoInvalidoException(
    val errores: ErroresDeProducto
) : IllegalArgumentException("Los datos del producto no cumplen las reglas del negocio")

class RegistrarProductoUseCase(
    private val productoRepository: ProductoRepository
) {
    suspend operator fun invoke(
        nombre: String,
        precio: String,
        stock: String,
        descripcion: String = "",
        categoria: String = "Sin categoría"
    ): Result<Producto> {

        val errores = ErroresDeProducto(
            nombre = validarNombre(nombre),
            precio = validarPrecio(precio),
            stock = validarStock(stock)
        )

        if (errores.hayErrores) {
            return Result.failure(ProductoInvalidoException(errores))
        }

        return runCatching {
            productoRepository.registrar(
                Producto(
                    id = 0L,
                    nombre = nombre.trim(),
                    precio = precio.toDouble(),
                    descripcion = descripcion.trim(),
                    categoria = categoria.trim(),
                    stock = stock.toInt()
                )
            )
        }
    }

    private fun validarNombre(nombre: String): String? {
        return if (nombre.isBlank()) "El nombre es obligatorio" else null
    }

    private fun validarPrecio(precio: String): String? {
        val precioValor = precio.toDoubleOrNull()
        return when {
            precio.isBlank() -> "El precio es obligatorio"
            precioValor == null || !precioValor.isFinite() -> "El precio debe ser un número válido"
            precioValor <= 0 -> "El precio debe ser mayor a 0"  // ← Esto debería atrapar -4
            else -> null
        }
    }

    private fun validarStock(stock: String): String? {
        val stockValor = stock.toIntOrNull()
        return when {
            stock.isBlank() -> "El stock es obligatorio"
            stockValor == null -> "El stock debe ser un número entero"
            stockValor < 0 -> "El stock no puede ser negativo"  // ← Esto debería atrapar -20
            else -> null
        }
    }
}