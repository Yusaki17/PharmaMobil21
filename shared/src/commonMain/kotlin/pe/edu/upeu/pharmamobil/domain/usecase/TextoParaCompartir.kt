package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.formatearSoles

fun Producto.comoTextoParaCompartir(): String =
    " $nombre\n ${formatearSoles(precio)}\n Stock: $stock u."