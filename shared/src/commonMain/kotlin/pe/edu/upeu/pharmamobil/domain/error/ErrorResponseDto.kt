package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val status: Int,
    val error: String,
    val message: String,
    val validationErrors: Map<String, String>? = null
)