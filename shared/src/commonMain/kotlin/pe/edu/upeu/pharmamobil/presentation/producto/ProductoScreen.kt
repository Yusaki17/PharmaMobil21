package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.copiarAlPortapapeles
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    compartidor: Compartidor,
    puedeRegistrar: Boolean = true, // Inyectado desde App.kt según permisos
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    LaunchedEffect(uiState.editando) {
        if (uiState.editando) mostrarFormulario = true
    }
    LaunchedEffect(uiState.mensajeExito) {
        if (uiState.mensajeExito != null) mostrarFormulario = false
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Indicador de sincronización en segundo plano
        if (uiState.sincronizando) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 2. Formulario (solo si tiene permiso)
        if (puedeRegistrar) {
            Button(onClick = { mostrarFormulario = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Nuevo producto")
            }
        }
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar por nombre o categoría") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (puedeRegistrar && mostrarFormulario) {
            Dialog(onDismissRequest = { mostrarFormulario = false; viewModel.cancelarEdicion() }) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            FormularioProductoCard(
                formulario = uiState.formulario,
                editando = uiState.editando,
                onCancelar = { mostrarFormulario = false; viewModel.cancelarEdicion() },
                registrando = uiState.operacion is ProductoUiState.Operacion.EnCurso,
                onNombreChange = viewModel::onNombreChange,
                onPrecioChange = viewModel::onPrecioChange,
                onStockChange = viewModel::onStockChange,
                onRegistrar = viewModel::registrar
            )
                }
            }
        }

        // 3. Mensaje de éxito
        uiState.mensajeExito?.let {
            MensajeExito(it)
        }

        // 4. Banner de aviso de red (aparece si hay fallo o pendientes)
        val hayPendientes = (uiState.fase as? ProductoUiState.Fase.ConProductos)
            ?.productos?.any { it.pendiente } == true

        if (uiState.avisoRed != null || hayPendientes) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        uiState.avisoRed?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        if (hayPendientes) {
                            Text(
                                "Hay productos pendientes de sincronizar",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Button(onClick = viewModel::sincronizar) {
                        Text("Sincronizar")
                    }
                }
            }
        }

        // 5. Encabezado con botón de refresh manual
        EncabezadoInventario(
            fase = uiState.fase,
            onSincronizar = viewModel::sincronizar
        )

        // 6. Contenido principal (Lista o Estados)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val fase = uiState.fase) {
                ProductoUiState.Fase.Cargando ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Cargando inventario…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                ProductoUiState.Fase.SinProductos ->
                    EstadoVacio(
                        icono = Icons.Default.Inventory2,
                        titulo = "Todavía no hay productos",
                        descripcion = "Pulsa Nuevo producto para registrar el primero.",
                        modifier = Modifier.align(Alignment.Center)
                    )

                is ProductoUiState.Fase.ConProductos ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = fase.productos.filter { it.nombre.contains(busqueda, ignoreCase = true) || it.categoria.contains(busqueda, ignoreCase = true) },
                            key = { it.id }
                        ) { producto ->
                            ProductoItem(
                                producto = producto,
                                onEditar = { viewModel.prepararEdicion(producto); mostrarFormulario = true },
                                onEliminar = { viewModel.eliminar(producto.id) },
                                onReactivar = { viewModel.reactivar(producto) },
                                onCompartir = {
                                    val texto = "📦 ${producto.nombre}\n💰 ${producto.precio}\n📦 Stock: ${producto.stock}"
                                    compartidor.compartir(texto)
                                },
                                onCopiar = {
                                    copiarAlPortapapeles("Código: ${producto.id} - ${producto.nombre}")
                                }
                            )
                        }
                    }

                is ProductoUiState.Fase.Error ->
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "No pudimos cargar el inventario",
                        descripcion = fase.mensaje,
                        colorIcono = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                        accion = {
                            FilledTonalButton(onClick = viewModel::sincronizar) {
                                Text("Reintentar")
                            }
                        }
                    )
            }
        }
    }
}

@Composable
private fun FormularioProductoCard(
    formulario: FormularioProducto,
    registrando: Boolean,
    editando: Boolean,
    onCancelar: () -> Unit,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onRegistrar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editando) "Editar producto" else "Registrar producto",
                style = MaterialTheme.typography.titleMedium
            )

            ValidatedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChange,
                label = "Nombre",
                error = formulario.nombreError,
                leadingIcon = Icons.Default.Medication,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ValidatedTextField(
                    value = formulario.precio,
                    onValueChange = onPrecioChange,
                    label = "Precio",
                    error = formulario.precioError,
                    ayuda = "En soles",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                ValidatedTextField(
                    value = formulario.stock,
                    onValueChange = onStockChange,
                    label = "Stock",
                    error = formulario.stockError,
                    ayuda = "Unidades",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onRegistrar,
                enabled = !registrando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (registrando) "Guardando…" else if (editando) "Guardar cambios" else "Registrar")
            }
            TextButton(onClick = onCancelar, enabled = !registrando) { Text("Cancelar") }
        }
    }
}

@Composable
private fun EncabezadoInventario(
    fase: ProductoUiState.Fase,
    onSincronizar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Inventario",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        if (fase is ProductoUiState.Fase.ConProductos) {
            val cantidad = fase.productos.size
            Text(
                text = if (cantidad == 1) "1 producto" else "$cantidad productos",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Botón de sincronización manual siempre visible
        IconButton(onClick = onSincronizar) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Sincronizar inventario",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProductoItem(
    producto: ProductoUi,
    onEditar: () -> Unit = {},
    onEliminar: () -> Unit = {},
    onReactivar: () -> Unit = {},
    onCompartir: () -> Unit = {},
    onCopiar: () -> Unit = {}
) {
    var menu by remember { mutableStateOf(false) }
    var detalle by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    if (detalle) {
        AlertDialog(
            onDismissRequest = { detalle = false },
            title = { Text(producto.nombre) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Categoría: ${producto.categoria.ifBlank { "Sin categoría" }}")
                    Text("Precio: ${producto.precio}")
                    Text("Stock: ${producto.stock}")
                    Text(producto.descripcion.ifBlank { "Sin descripción" })
                    Text(if (producto.estado) "Activo" else "Inactivo")
                }
            },
            confirmButton = { TextButton(onClick = { detalle = false }) { Text("Cerrar") } }
        )
    }
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Deseas eliminar ${producto.nombre}?") },
            confirmButton = { TextButton(onClick = { confirmarEliminar = false; onEliminar() }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } }
        )
    }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Default.Medication, contentDescription = null, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(producto.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(producto.categoria.ifBlank { "Sin categoría" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(producto.precio, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Stock: ${producto.stock}", style = MaterialTheme.typography.bodyMedium)
            if (producto.descripcion.isNotBlank()) {
                Text(producto.descripcion, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (producto.pendiente) Text("Pendiente de sincronizar", color = MaterialTheme.colorScheme.tertiary)
            if (!producto.estado) Text("Inactivo", color = MaterialTheme.colorScheme.error)
            if (producto.requiereReposicion && producto.estado) Text("Stock bajo · Reponer", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { detalle = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Ver detalle") }
                FilledTonalButton(onClick = if (producto.estado) onEditar else onReactivar, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Text(if (producto.estado) "Editar" else "Reactivar") }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Más acciones de ${producto.nombre}") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Copiar código") }, onClick = { menu = false; onCopiar() })
                        DropdownMenuItem(text = { Text("Compartir") }, onClick = { menu = false; onCompartir() })
                        if (producto.estado) DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmarEliminar = true })
                    }
                }
            }
        }
    }
}
