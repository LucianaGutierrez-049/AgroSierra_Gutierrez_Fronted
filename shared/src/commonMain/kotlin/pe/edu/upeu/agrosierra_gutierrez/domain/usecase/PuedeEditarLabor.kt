package pe.edu.upeu.agrosierra_gutierrez.domain.usecase
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.Plataforma
fun puedeEditarLabor(sesion: Sesion,labor: Labor,plataforma: Plataforma): Boolean {
    if("LABOR_EDITAR" !in sesion.privilegios) return false
    if(!sesion.edicionPropia) return true
    return labor.autorId==sesion.usuarioId && (labor.id<0 || plataforma.dentroDe24Horas(labor.registradoEn))
}
