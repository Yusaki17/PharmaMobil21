package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    fun onUsuarioChange(usuario: String) {
        _uiState.update { it.copy(usuario = usuario, error = null) }
    }

    fun onClaveChange(clave: String) {
        _uiState.update { it.copy(clave = clave, error = null) }
    }

    fun iniciarSesion() {
        if (_uiState.value.cargando) return

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }

            try {
                val credenciales = Credenciales(
                    usuario = _uiState.value.usuario,
                    clave = _uiState.value.clave
                )
                val sesion = authRepository.iniciarSesion(credenciales)
                _uiState.update { it.copy(sesion = sesion, cargando = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = e.message ?: "Error al iniciar sesión"
                    )
                }
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            authRepository.cerrarSesion()
            _uiState.update { SesionUiState() }
        }
    }

    fun limpiarError() {
        _uiState.update { it.copy(error = null) }
    }
}