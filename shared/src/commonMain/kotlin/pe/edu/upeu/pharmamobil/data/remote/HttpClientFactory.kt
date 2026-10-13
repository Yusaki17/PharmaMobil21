package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro // <-- IMPORTANTE

const val URL_BASE = "http://10.0.2.2:8080/api/v1/"

fun crearHttpClient(
    motor: HttpClientEngine, // Se inyecta desde la plataforma (Android/iOS)
    almacen: AlmacenSeguro,  // <-- NUEVO: Para leer el token guardado
    esDepuracion: Boolean = true
): HttpClient = HttpClient(motor) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }

    install(Auth) {
        bearer {
            loadTokens {
                // Lee el token del almacén seguro cada vez que se necesita
                almacen.leerToken()?.let {
                    BearerTokens(accessToken = it, refreshToken = "")
                }
            }
            // EVITA enviar el token en la petición de login (y evita el ciclo 401 -> reintento)
            sendWithoutRequest { request ->
                !request.url.encodedPath.endsWith("/auth/login")
            }
        }
    }

    install(Logging) {
        level = if (esDepuracion) LogLevel.HEADERS else LogLevel.NONE
        sanitizeHeader { header ->
            header == HttpHeaders.Authorization // Reemplaza el token con "***" en los logs
        }
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15000
        connectTimeoutMillis = 10000
    }

    defaultRequest {
        url(URL_BASE)
        contentType(ContentType.Application.Json)
    }
}