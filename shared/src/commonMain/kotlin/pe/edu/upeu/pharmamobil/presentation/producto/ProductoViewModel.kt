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
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.resultadoDe

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val listarProductos: ListarProductosUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase,
    private val repositorio: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    private var productosCompletos: List<Producto> = emptyList()
    private var productoEnEdicion: Producto? = null

    init {
        viewModelScope.launch {
            listarProductos().collect { productos ->
                productosCompletos = productos
                val fase = when {
                    productos.isNotEmpty() ->
                        ProductoUiState.Fase.ConProductos(productos.map { it.aUi() })
                    _uiState.value.sincronizando -> ProductoUiState.Fase.Cargando
                    else -> ProductoUiState.Fase.SinProductos
                }
                _uiState.update { it.copy(fase = fase) }
            }
        }
        sincronizar()
    }

    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        _uiState.update { it.copy(sincronizando = true, avisoRed = null) }
        viewModelScope.launch {
            val resultado = resultadoDe { repositorio.sincronizar() }
            _uiState.update {
                it.copy(
                    sincronizando = false,
                    fase = when {
                        productosCompletos.isNotEmpty() -> ProductoUiState.Fase.ConProductos(productosCompletos.map { producto -> producto.aUi() })
                        resultado.isFailure -> ProductoUiState.Fase.Error("No se pudo actualizar el inventario. Intenta nuevamente.")
                        else -> ProductoUiState.Fase.SinProductos
                    },
                    avisoRed = resultado.exceptionOrNull()?.let {
                        "No se pudo sincronizar. Los datos guardados se conservan."
                    }
                )
            }
        }
    }

    // === FORMULARIO (sin cambios) ===
    fun onNombreChange(nombre: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(nombre = nombre, nombreError = null), mensajeExito = null)
        }
    }

    fun onPrecioChange(precio: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(precio = precio, precioError = null), mensajeExito = null)
        }
    }

    fun onStockChange(stock: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(stock = stock, stockError = null), mensajeExito = null)
        }
    }

    // === CREAR o ACTUALIZAR ===
    fun registrar() {
        if (_uiState.value.operacion is ProductoUiState.Operacion.EnCurso) return

        viewModelScope.launch {
            val tipo = if (productoEnEdicion == null)
                ProductoUiState.Operacion.Tipo.Crear
            else ProductoUiState.Operacion.Tipo.Actualizar

            _uiState.update { it.copy(operacion = ProductoUiState.Operacion.EnCurso(tipo)) }

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
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            operacion = ProductoUiState.Operacion.Inactiva,
                            formulario = FormularioProducto(),
                            editando = false,
                            mensajeExito = if (productoEnEdicion == null)
                                "Producto registrado correctamente"
                            else "Producto actualizado correctamente"
                        )
                    }
                    productoEnEdicion = null
                    // YA NO se recarga: el Flow emite solo al escribir en la tabla
                },
                onFailure = { fallo ->
                    when (fallo) {
                        is ProductoInvalidoException -> _uiState.update {
                            it.copy(
                                operacion = ProductoUiState.Operacion.Inactiva,
                                formulario = it.formulario.copy(
                                    nombreError = fallo.errores.nombre,
                                    precioError = fallo.errores.precio,
                                    stockError = fallo.errores.stock
                                )
                            )
                        }
                        else -> manejarFallo(fallo)
                    }
                }
            )
        }
    }

    // === ELIMINAR ===
    fun eliminar(id: Long) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Eliminar))
            }
            eliminarProducto(id).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(operacion = ProductoUiState.Operacion.Inactiva, mensajeExito = "Producto eliminado correctamente")
                    }
                    // El Flow emite solo: no se recarga a mano
                },
                onFailure = { fallo -> manejarFallo(fallo) }
            )
        }
    }

    // === PREPARAR EDICIÓN (sin cambios) ===
    fun prepararEdicion(productoUi: ProductoUi) {
        val producto = productosCompletos.find { it.id == productoUi.id }
        productoEnEdicion = producto
        _uiState.value = _uiState.value.copy(
            formulario = FormularioProducto(
                nombre = producto?.nombre ?: productoUi.nombre,
                precio = producto?.precio?.toString() ?: productoUi.precio.replace("S/ ", "").trim(),
                stock = producto?.stock?.toString() ?: productoUi.stock.replace(" u.", "").trim(),
                descripcion = producto?.descripcion ?: "",
                categoria = producto?.categoria ?: "Farmacia",
                nombreError = null, precioError = null, stockError = null
            ),
            operacion = ProductoUiState.Operacion.Inactiva,
            editando = true,
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
                actualizarProducto(producto.copy(estado = true)).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(operacion = ProductoUiState.Operacion.Inactiva, mensajeExito = "Producto reactivado correctamente")
                        }
                    },
                    onFailure = { fallo -> manejarFallo(fallo) }
                )
            }
        }
    }

    fun cancelarEdicion() {
        productoEnEdicion = null
        _uiState.update {
            it.copy(formulario = FormularioProducto(), editando = false, operacion = ProductoUiState.Operacion.Inactiva)
        }
    }

    // === MANEJO DE ERRORES (sin cambios, salvo NoEncontrado) ===
    private fun manejarFallo(fallo: Throwable) {
        val error = (fallo as? ErrorApiException)?.error
        when (error) {
            is ErrorApi.Validacion -> _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.Inactiva,
                    formulario = it.formulario.copy(
                        nombreError = error.porCampo["nombre"],
                        precioError = error.porCampo["precio"],
                        stockError = error.porCampo["stock"]
                    )
                )
            }
            is ErrorApi.NoEncontrado -> {
                _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida("El producto ya no existe")) }
                sincronizar()
            }
            is ErrorApi.Conflicto -> _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida(error.mensaje)) }
            is ErrorApi.SinConexion -> _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida("Sin conexión a internet")) }
            is ErrorApi.TiempoAgotado -> _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida("Tiempo de espera agotado")) }
            is ErrorApi.Servidor -> _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida("Error en el servidor")) }
            null -> _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida(fallo.message ?: "Operación fallida")) }
            else -> _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Fallida(error.toString())) }
        }
    }

    fun limpiarMensajeExito() {
        _uiState.update { it.copy(mensajeExito = null) }
    }
}