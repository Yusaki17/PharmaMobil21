package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ErrorEnvioMapperTest {

    @Test
    fun losFallosTemporalesSeClasificanComoTransitorios() {
        val errores = listOf(
            ErrorApi.SinConexion,
            ErrorApi.TiempoAgotado,
            ErrorApi.Servidor
        )

        errores.forEach { error ->
            assertIs<ResultadoEnvio.Transitorio>(
                error.aResultadoEnvio()
            )
        }
    }

    @Test
    fun validacionConservaElCampoYElMotivo() {
        val error = ErrorApi.Validacion(
            mapOf("nombre" to "El nombre es obligatorio")
        )

        val resultado =
            assertIs<ResultadoEnvio.Permanente>(
                error.aResultadoEnvio()
            )

        assertTrue(
            resultado.motivo.contains("nombre")
        )
        assertTrue(
            resultado.motivo.contains("El nombre es obligatorio")
        )
    }

    @Test
    fun conflictoConservaElMensajeDelServidor() {
        val resultado =
            assertIs<ResultadoEnvio.Permanente>(
                ErrorApi.Conflicto(
                    "El producto ya existe"
                ).aResultadoEnvio()
            )

        assertEquals(
            "El producto ya existe",
            resultado.motivo
        )
    }

    @Test
    fun recursoInexistenteEsUnFalloPermanente() {
        assertIs<ResultadoEnvio.Permanente>(
            ErrorApi.NoEncontrado.aResultadoEnvio()
        )
    }
    @Test
    fun sesionInvalidaNoSeClasificaComoFalloTransitorio() {
        val resultado = assertIs<ResultadoEnvio.Permanente>(
            ErrorApi.NoAutorizado.aResultadoEnvio()
        )

        assertTrue(
            resultado.motivo.contains("Inicia sesión")
        )
    }

    @Test
    fun faltaDePermisoNoSeClasificaComoFalloTransitorio() {
        val resultado = assertIs<ResultadoEnvio.Permanente>(
            ErrorApi.SinPrivilegio.aResultadoEnvio()
        )

        assertTrue(
            resultado.motivo.contains("permiso")
        )
    }
}