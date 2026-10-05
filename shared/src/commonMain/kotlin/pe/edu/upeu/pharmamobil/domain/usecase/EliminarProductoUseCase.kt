package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

class EliminarProductoUseCase(
    private val productoRepository: ProductoRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return runCatching {
            productoRepository.eliminar(id)
            Unit // Retorna Unit explícitamente
        }
    }
}