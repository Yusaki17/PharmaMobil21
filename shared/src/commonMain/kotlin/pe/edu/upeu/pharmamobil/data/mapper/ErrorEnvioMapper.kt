package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio

fun ErrorApi.aResultadoEnvio(): ResultadoEnvio =
    when (this) {
        ErrorApi.SinConexion ->
            ResultadoEnvio.Transitorio(
                "No hay conexión con el servidor."
            )

        ErrorApi.TiempoAgotado ->
            ResultadoEnvio.Transitorio(
                "El servidor no respondió a tiempo."
            )

        ErrorApi.Servidor ->
            ResultadoEnvio.Transitorio(
                "El servidor no está disponible temporalmente."
            )

        ErrorApi.NoAutorizado ->
            ResultadoEnvio.Permanente(
                "La sesión no es válida o ha vencido. Inicia sesión nuevamente."
            )

        ErrorApi.SinPrivilegio ->
            ResultadoEnvio.Permanente(
                "Tu cuenta no tiene permiso para realizar esta operación."
            )

        is ErrorApi.Validacion ->
            ResultadoEnvio.Permanente(
                porCampo.entries.joinToString(
                    separator = "; "
                ) { (campo, mensaje) ->
                    "$campo: $mensaje"
                }.ifBlank {
                    "Revisa los datos del producto."
                }
            )

        ErrorApi.NoEncontrado ->
            ResultadoEnvio.Permanente(
                "El recurso solicitado no existe."
            )

        is ErrorApi.Conflicto ->
            ResultadoEnvio.Permanente(
                mensaje.ifBlank {
                    "Existe un conflicto con los datos del servidor."
                }
            )
    }