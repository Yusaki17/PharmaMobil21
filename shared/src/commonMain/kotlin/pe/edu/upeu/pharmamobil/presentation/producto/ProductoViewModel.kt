package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val listarProductos: ListarProductosUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    // Lista completa de productos del dominio (para poder editar)
    private var productosCompletos: List<Producto> = emptyList()
    private var productoEnEdicion: Producto? = null

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }

            listarProductos().fold(
                onSuccess = { productos ->
                    productosCompletos = productos
                    val fase = if (productos.isEmpty()) {
                        ProductoUiState.Fase.SinProductos
                    } else {
                        ProductoUiState.Fase.ConProductos(productos.map { it.aUi() })
                    }
                    _uiState.update { it.copy(fase = fase) }
                },
                onFailure = { fallo ->
                    _uiState.update {
                        it.copy(fase = ProductoUiState.Fase.Error(
                            fallo.message ?: "No se pudo cargar el inventario"
                        ))
                    }
                }
            )
        }
    }

    // === FORMULARIO ===
    fun onNombreChange(nombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(nombre = nombre, nombreError = null),
                mensajeExito = null
            )
        }
    }

    fun onPrecioChange(precio: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(precio = precio, precioError = null),
                mensajeExito = null
            )
        }
    }


    fun onStockChange(stock: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(stock = stock, stockError = null),
                mensajeExito = null
            )
        }
    }

    // === CREAR o ACTUALIZAR ===
    fun registrar() {
        if (_uiState.value.operacion is ProductoUiState.Operacion.EnCurso) return

        viewModelScope.launch {
            val tipo = if (productoEnEdicion == null)
                ProductoUiState.Operacion.Tipo.Crear
            else
                ProductoUiState.Operacion.Tipo.Actualizar

            _uiState.update {
                it.copy(operacion = ProductoUiState.Operacion.EnCurso(tipo))
            }

            val formulario = _uiState.value.formulario

            val resultado = if (productoEnEdicion == null) {
                registrarProducto(
                    nombre = formulario.nombre,
                    precio = formulario.precio,
                    stock = formulario.stock,
                    descripcion = formulario.descripcion,
                    categoria = formulario.categoria
                )
            } else {
                productoEnEdicion?.let { producto ->
                    actualizarProducto(
                        producto.copy(
                            nombre = formulario.nombre,
                            precio = formulario.precio.toDoubleOrNull() ?: producto.precio,
                            stock = formulario.stock.toIntOrNull() ?: producto.stock
                        )
                    )
                } ?: Result.failure(Exception("No hay producto para actualizar"))
            }

            resultado.fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            operacion = ProductoUiState.Operacion.Inactiva,
                            formulario = FormularioProducto(),
                            mensajeExito = if (productoEnEdicion == null)
                                "Producto registrado correctamente"
                            else
                                "Producto actualizado correctamente"
                        )
                    }
                    productoEnEdicion = null
                    cargarProductos()
                },
                onFailure = { fallo ->
                    // ← AQUÍ ESTÁ LA CLAVE: Atrapar la excepción de validación del dominio
                    when (fallo) {
                        is ProductoInvalidoException -> {
                            println(" [DEBUG] Error de validación capturado:")
                            println("   - Nombre: ${fallo.errores.nombre}")
                            println("   - Precio: ${fallo.errores.precio}")
                            println("   - Stock: ${fallo.errores.stock}")

                            _uiState.update {
                                it.copy(
                                    operacion = ProductoUiState.Operacion.Inactiva,
                                    formulario = it.formulario.copy(
                                        nombreError = fallo.errores.nombre,
                                        precioError = fallo.errores.precio,
                                        stockError = fallo.errores.stock
                                    )
                                )
                            }
                        }
                        else -> {
                            println(" [DEBUG] Otro tipo de error: ${fallo.message}")
                            manejarFallo(fallo)
                        }
                    }
                }
            )
        }
    }

    // === ELIMINAR ===
    fun eliminar(id: Long) {
        println("🗑 Eliminando producto con ID: $id")

        viewModelScope.launch {
            _uiState.update {
                it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Eliminar))
            }

            try {
                eliminarProducto(id).fold(
                    onSuccess = {
                        println(" Producto eliminado correctamente")
                        _uiState.update {
                            it.copy(
                                operacion = ProductoUiState.Operacion.Inactiva,
                                mensajeExito = "Producto eliminado correctamente"
                            )
                        }
                        cargarProductos()
                    },
                    onFailure = { fallo ->
                        println(" Error al eliminar: ${fallo.message}")
                        fallo.printStackTrace()
                        manejarFallo(fallo)
                    }
                )
            } catch (e: Exception) {
                println(" Excepción no controlada: ${e.message}")
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            e.message ?: "Error desconocido al eliminar"
                        )
                    )
                }
            }
        }
    }

    // === PREPARAR EDICIÓN ===
    fun prepararEdicion(productoUi: ProductoUi) {
        // 1. Buscamos el producto en la lista local (es instantáneo, no bloquea)
        val producto = productosCompletos.find { it.id == productoUi.id }

        // 2. Si no lo encuentra, usamos los datos de la UI como respaldo
        val nombre = producto?.nombre ?: productoUi.nombre
        val precio = producto?.precio?.toString() ?: productoUi.precio.replace("S/ ", "").trim()
        val stock = producto?.stock?.toString() ?: productoUi.stock.replace(" u.", "").trim()
        val descripcion = producto?.descripcion ?: ""
        val categoria = producto?.categoria ?: "Farmacia"

        // 3. Actualizamos el estado de forma limpia y directa
        productoEnEdicion = producto
        _uiState.value = _uiState.value.copy(
            formulario = FormularioProducto(
                nombre = nombre,
                precio = precio,
                stock = stock,
                descripcion = descripcion,
                categoria = categoria,
                nombreError = null,
                precioError = null,
                stockError = null
            ),
            operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Actualizar),
            mensajeExito = null
        )
    }
    fun reactivar(productoUi: ProductoUi) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Actualizar))
            }

            val producto = productosCompletos.find { it.id == productoUi.id }

            if (producto != null) {
                actualizarProducto(
                    producto.copy(estado = true) // Forzamos estado = true
                ).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                operacion = ProductoUiState.Operacion.Inactiva,
                                mensajeExito = "Producto reactivado correctamente"
                            )
                        }
                        cargarProductos()
                    },
                    onFailure = { fallo ->
                        manejarFallo(fallo)
                    }
                )
            }
        }
    }
    fun cancelarEdicion() {
        productoEnEdicion = null
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(),
                operacion = ProductoUiState.Operacion.Inactiva
            )
        }
    }

    // === MANEJO DE ERRORES ===
    private fun manejarFallo(fallo: Throwable) {
        val error = (fallo as? ErrorApiException)?.error

        when (error) {
            is ErrorApi.Validacion -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        formulario = it.formulario.copy(
                            nombreError = error.porCampo["nombre"],
                            precioError = error.porCampo["precio"],
                            stockError = error.porCampo["stock"]
                        )
                    )
                }
            }
            is ErrorApi.NoEncontrado -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida("El producto ya no existe")
                    )
                }
                cargarProductos()
            }
            is ErrorApi.Conflicto -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(error.mensaje)
                    )
                }
            }
            is ErrorApi.SinConexion -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida("Sin conexión a internet")
                    )
                }
            }
            is ErrorApi.TiempoAgotado -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida("Tiempo de espera agotado")
                    )
                }
            }
            is ErrorApi.Servidor -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida("Error en el servidor")
                    )
                }
            }
            null -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            fallo.message ?: "Operación fallida"
                        )
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            error.toString()
                        )
                    )
                }
            }
        }
    }

    fun limpiarMensajeExito() {
        _uiState.update { it.copy(mensajeExito = null) }
    }
}