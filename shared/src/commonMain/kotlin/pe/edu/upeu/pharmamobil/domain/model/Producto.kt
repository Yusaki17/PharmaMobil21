package pe.edu.upeu.pharmamobil.domain.model

data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val descripcion: String = "",          // ← Agregado: la API sí lo envía
    val categoria: String = "Sin categoría", // ← Agregado: la API lo envía como objeto anidado
    val stock: Int = 0,
    val estado: Boolean = true // ← Valor por defecto: la API de práctica no envía stock
) {
    init {
        require(nombre.isNotBlank()) {
            "El nombre del producto no puede estar vacío"
        }
        require(precio > 0 && precio.isFinite()) {
            "El precio debe ser un número mayor que cero"
        }
        require(stock >= 0) {
            "El stock no puede ser negativo"
        }
    }

    /**
     * Un producto necesita reposición cuando su stock cae por debajo del
     * mínimo que la botica mantiene en góndola.
     */
    val requiereReposicion: Boolean
        get() = stock < STOCK_MINIMO

    companion object {
        const val STOCK_MINIMO = 10
    }
}