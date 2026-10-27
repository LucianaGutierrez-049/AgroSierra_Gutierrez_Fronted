package pe.edu.upeu.agrosierra_gutierrez.domain.usecase
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.AgroRepository
import pe.edu.upeu.agrosierra_gutierrez.domain.model.FormularioLabor
class SincronizarUseCase(private val repo: AgroRepository) { suspend operator fun invoke() = repo.sincronizar() }
class GuardarLaborUseCase(private val repo: AgroRepository) {
    suspend operator fun invoke(p: FormularioLabor,id: Long?) = repo.guardar(p,id)
}
class EliminarLaborUseCase(private val repo: AgroRepository) { suspend operator fun invoke(id: Long) = repo.eliminar(id) }
class CerrarCampaniaUseCase(private val repo: AgroRepository) { suspend operator fun invoke(id: Long) = repo.cerrar(id) }
