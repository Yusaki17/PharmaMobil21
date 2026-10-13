package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String>
)

fun LoginRequestDto.toDomain(): Credenciales =
    Credenciales(usuario = username, clave = password)

fun LoginResponseDto.toDomain(): Sesion =
    Sesion(
        token = token,
        usuario = username,
        nombreCompleto = nombreCompleto,
        permisos = permisos.toSet()
    )