package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.isSuccess
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (cancelacion: CancellationException) {
        throw cancelacion // Nunca atrapes la cancelación
    } catch (e: ClientRequestException) {
        Result.failure(traducirCliente(e))
    } catch (e: ServerResponseException) {
        Result.failure(ErrorApiException(ErrorApi.Servidor))
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
    } catch (e: IOException) {
        Result.failure(ErrorApiException(ErrorApi.SinConexion))
    }

private suspend fun traducirCliente(e: ClientRequestException): ErrorApiException {
    val cuerpo = runCatching {
        e.response.body<ErrorResponseDto>()
    }.getOrNull()

    return when (e.response.status.value) {
        400 -> ErrorApiException(
            ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        )
        404 -> ErrorApiException(ErrorApi.NoEncontrado)
        409 -> ErrorApiException(
            ErrorApi.Conflicto(cuerpo?.message ?: "Operación no permitida")
        )
        else -> ErrorApiException(ErrorApi.Servidor)
    }
}