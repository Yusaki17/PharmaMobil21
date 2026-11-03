package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.repository.ClienteRepository

/** Repositorio de pruebas: no usa red ni almacenamiento del dispositivo. */
class FakeClienteRepository(
    clientes: MutableList<Cliente> = mutableListOf()
) : ClienteRepository {

    private val datos = clientes.toMutableList()
    private var siguienteId = (datos.maxOfOrNull { it.id } ?: 0L) + 1L

    var fallaAlRegistrar: Throwable? = null
    var fallaAlListar: Throwable? = null

    override suspend fun registrar(cliente: Cliente): Cliente {
        fallaAlRegistrar?.let { throw it }
        val guardado = cliente.copy(id = siguienteId++)
        datos.add(guardado)
        return guardado
    }

    override suspend fun listar(): List<Cliente> {
        fallaAlListar?.let { throw it }
        return datos.toList()
    }
}
