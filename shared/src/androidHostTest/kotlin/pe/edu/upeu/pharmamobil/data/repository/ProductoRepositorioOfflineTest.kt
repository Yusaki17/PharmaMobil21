package pe.edu.upeu.pharmamobil.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProductoRepositorioOfflineTest {
    @Test fun sinConexionConservaPendienteSinIntentarCrear() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PharmaDb.Schema.create(driver)
        val local = ProductoLocal(PharmaDb(driver))
        val metodos = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            metodos.add(request.method.value)
            throw kotlinx.io.IOException("Sin conexión")
        }) { expectSuccess = true; install(ContentNegotiation) { json() } }
        try {
            val repo = ProductoRepositorioOffline(local, ProductoApi(client))
            val producto = repo.registrar(Producto(id = 0, nombre = "Offline", precio = 10.0))
            assertTrue(producto.pendiente)
            assertTrue(!producto.fallida)
            assertEquals(listOf("GET"), metodos)
            assertEquals(1, local.pendientesPorEnviar(5).size)
        } finally { client.close(); driver.close() }
    }

    private fun pagina(numero: Int, ultima: Boolean, id: Int) =
        """{"contenido":[{"id":$id,"nombre":"Producto $id","precio":12.5,"stock":5}],"pagina":$numero,"tamanio":100,"totalElementos":2,"totalPaginas":2,"ultima":$ultima}"""

    @Test fun descargaTodasLasPaginasAntesDeReemplazarLaCache() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PharmaDb.Schema.create(driver)
        val local = ProductoLocal(PharmaDb(driver))
        local.reemplazarSincronizados(listOf(Producto(id = 99, nombre = "Anterior", precio = 1.0, stock = 1)))
        val solicitudes = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            val numero = request.url.parameters["pagina"]!!.toInt()
            solicitudes.add(numero)
            respond(pagina(numero, numero == 1, numero + 1), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            ProductoRepositorioOffline(local, ProductoApi(client)).sincronizar()
            assertEquals(listOf(0, 1), solicitudes)
            assertEquals(setOf(1L, 2L), local.observar().first().map { it.id }.toSet())
        } finally { client.close(); driver.close() }
    }

    @Test fun unaPaginaFallidaConservaElInventarioAnterior() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PharmaDb.Schema.create(driver)
        val local = ProductoLocal(PharmaDb(driver))
        local.reemplazarSincronizados(listOf(Producto(id = 99, nombre = "Anterior", precio = 1.0, stock = 1)))
        val client = HttpClient(MockEngine { request ->
            if (request.url.parameters["pagina"] == "1") respond("Error", HttpStatusCode.InternalServerError)
            else respond(pagina(0, false, 1), headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { expectSuccess = true; install(ContentNegotiation) { json() } }
        try {
            assertFailsWith<Exception> { ProductoRepositorioOffline(local, ProductoApi(client)).sincronizar() }
            assertEquals(listOf(99L), local.observar().first().map { it.id })
        } finally { client.close(); driver.close() }
    }

    @Test fun guardaPendientesConIdsLocalesYLosConservaAlActualizarLaCache() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PharmaDb.Schema.create(driver)
        try {
            val local = ProductoLocal(PharmaDb(driver))
            val uno = local.guardarPendiente(Producto(id = 0, nombre = "Uno", precio = 1.0, stock = 1))
            val dos = local.guardarPendiente(Producto(id = 0, nombre = "Dos", precio = 1.0, stock = 1))
            assertTrue(uno.id < 0 && dos.id < uno.id)
            local.reemplazarSincronizados(listOf(Producto(id = 1, nombre = "Remoto", precio = 1.0, stock = 1)))
            assertEquals(2, local.pendientes().size)
            assertEquals(3, local.observar().first().size)
        } finally { driver.close() }
    }
}
