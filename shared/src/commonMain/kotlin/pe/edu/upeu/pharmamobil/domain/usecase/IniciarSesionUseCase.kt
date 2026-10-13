package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class IniciarSesionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(credenciales: Credenciales): Sesion {
        return authRepository.iniciarSesion(credenciales)
    }
}