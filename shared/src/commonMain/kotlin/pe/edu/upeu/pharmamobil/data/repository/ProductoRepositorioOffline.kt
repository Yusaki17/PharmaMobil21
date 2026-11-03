package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoApi,
    private val categoriaPorDefecto: Long = 1L
) : ProductoRepository {

    private val sincronizacion = Mutex()

    override fun observar(): Flow<List<Producto>> = local.observar()

    override suspend fun registrar(producto: Producto): Producto {
        val guardado = local.guardarPendiente(producto)
        intentar { sincronizar() }
        return guardado
    }

    override suspend fun actualizar(producto: Producto): Producto {
        val actualizado = llamar {
            remoto.actualizar(producto.id, producto.toRequest(categoriaPorDefecto))
        }.toDomain()
        sincronizar()
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        llamar { remoto.eliminar(id) }
        sincronizar()
    }

    override suspend fun sincronizar() = sincronizacion.withLock {
        subirPendientes()                                                // 1.º SUBE
        // Conserva la caché anterior hasta descargar todas las páginas.
        val productos = mutableListOf<Producto>()
        var numeroPagina = 0
        do {
            val pagina = llamar { remoto.listar(pagina = numeroPagina, tamanio = 100) }
            productos.addAll(pagina.contenido.map { it.toDomain() })
            numeroPagina++
        } while (!pagina.ultima && numeroPagina < pagina.totalPaginas)
        local.reemplazarSincronizados(productos)
    }

    private suspend fun subirPendientes() {
        local.pendientes().forEach { pendiente ->
            ejecutarLlamada { remoto.crear(pendiente.toRequest(categoriaPorDefecto)) }
                .onSuccess { creado -> local.marcarSincronizado(pendiente.id, creado.id) }
            // onFailure: la fila sigue pendiente; se reintenta en el próximo sincronizar()
        }
    }

    /** Traduce los errores crudos de Ktor a los errores tipados del proyecto. */
    private suspend fun <T> llamar(bloque: suspend () -> T): T =
        ejecutarLlamada(bloque).getOrThrow()

    /** Un fallo de red no debe tumbar la operación local. */
    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (fallo: ErrorApiException) {
            if (fallo.error !is ErrorApi.SinConexion) throw fallo
            // Sin conexión: queda pendiente y se reintenta al sincronizar
        }
    }
}