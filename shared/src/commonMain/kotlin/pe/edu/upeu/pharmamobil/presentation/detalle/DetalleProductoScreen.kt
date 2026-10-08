import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.detalle.DetalleProductoViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetalleProductoScreen(
    producto: Producto,
    onVolver: () -> Unit
) {
    // Inyectamos el ViewModel
    val viewModel: DetalleProductoViewModel = koinViewModel()

    // ... tu UI actual ...

    // Botón para compartir
    Button(
        onClick = { viewModel.compartirProducto(producto) }
    ) {
        Icon(Icons.Default.Share, contentDescription = "Compartir")
        Text(" Compartir Producto")
    }
}