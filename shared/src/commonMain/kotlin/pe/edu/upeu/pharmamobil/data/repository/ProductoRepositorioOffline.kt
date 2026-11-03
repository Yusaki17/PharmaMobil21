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
import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.data.mapper.aResultadoEnvio
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
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
            val pagina = descargarPaginaConReintentos(numeroPagina)
            productos.addAll(pagina.contenido.map { it.toDomain() })
            numeroPagina++
        } while (!pagina.ultima && numeroPagina < pagina.totalPaginas)
        local.reemplazarSincronizados(productos)
    }

    private suspend fun subirPendientes() {
        local.pendientesPorEnviar(maxIntentos = 5).forEach { pendiente ->

            /*
             * Antes de enviar dejamos una marca persistente.
             * Si la app se cierra después de enviar, no repetirá
             * automáticamente una creación cuyo resultado desconocemos.
             */
            local.marcarFallida(
                id = pendiente.id,
                mensaje = "Envío iniciado. Si no se confirma, revisa el servidor antes de reenviar."
            )

            val resultado = ejecutarLlamada {
                remoto.crear(
                    pendiente.toRequest(categoriaPorDefecto)
                )
            }

            resultado.fold(
                onSuccess = { creado ->
                    local.marcarSincronizado(
                        idLocal = pendiente.id,
                        idReal = creado.id
                    )
                },
                onFailure = { fallo ->
                    val error = (fallo as? ErrorApiException)?.error

                    val clasificacion = error?.aResultadoEnvio()
                        ?: ResultadoEnvio.Permanente(
                            "No se pudo confirmar el envío."
                        )

                    val mensaje = when (clasificacion) {
                        is ResultadoEnvio.Transitorio ->
                            "${clasificacion.motivo} " +
                                    "Comprueba si el producto llegó al servidor antes de reenviarlo."

                        is ResultadoEnvio.Permanente ->
                            clasificacion.motivo

                        is ResultadoEnvio.Exito ->
                            "No se pudo confirmar el envío."
                    }

                    local.sumarIntento(
                        id = pendiente.id,
                        mensaje = mensaje,
                        maxIntentos = 5
                    )

                    local.marcarFallida(
                        id = pendiente.id,
                        mensaje = mensaje
                    )

                    // Detiene el envío de las demás filas si la sesión
                    // o los permisos impiden realizar la operación.
                    if (
                        error == ErrorApi.NoAutorizado ||
                        error == ErrorApi.SinPrivilegio
                    ) {
                        throw fallo
                    }
                }
            )
        }
    }
    private suspend fun descargarPaginaConReintentos(
        numeroPagina: Int
    ): PaginaResponseDto<ProductoResponseDto> {

        val maxIntentos = 5
        var intento = 0

        while (true) {
            try {
                return llamar {
                    remoto.listar(
                        pagina = numeroPagina,
                        tamanio = 100
                    )
                }
            } catch (fallo: ErrorApiException) {
                intento++

                val transitorio =
                    fallo.error.aResultadoEnvio() is ResultadoEnvio.Transitorio

                if (!transitorio || intento >= maxIntentos) {
                    throw fallo
                }

                // Esperas: 1, 2, 4 y 8 segundos.
                val esperaMillis = 1_000L shl (intento - 1)
                delay(esperaMillis)
            }
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