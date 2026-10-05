package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto

class ProductoApi(private val client: HttpClient) {

    suspend fun listar(pagina: Int = 0, tamanio: Int = 20): PaginaResponseDto<ProductoResponseDto> =
        client.get("productos") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }.body()

    suspend fun obtener(id: Long): ProductoResponseDto =
        client.get("productos/$id").body()

    suspend fun crear(request: ProductoRequestDto): ProductoResponseDto =
        client.post("productos") {
            setBody(request)
        }.body()

    suspend fun actualizar(id: Long, request: ProductoRequestDto): ProductoResponseDto =
        client.put("productos/$id") {
            setBody(request)
        }.body()

    suspend fun eliminar(id: Long) {
        client.delete("productos/$id")
        // ¡OJO! No llames a body() - DELETE devuelve 204 sin contenido
    }
}