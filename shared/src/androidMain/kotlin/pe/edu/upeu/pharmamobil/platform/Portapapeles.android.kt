package pe.edu.upeu.pharmamobil.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import org.koin.core.context.GlobalContext

actual fun copiarAlPortapapeles(texto: String) {
    val contexto = GlobalContext.get().get<Context>()
    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Producto", texto)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(contexto, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
}