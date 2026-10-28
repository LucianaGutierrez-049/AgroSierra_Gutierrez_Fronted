package pe.edu.upeu.agrosierra_gutierrez.data.local
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import pe.edu.upeu.agrosierra_gutierrez.data.remote.dto.SesionDto
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.Plataforma
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
class SesionManager(private val almacen: AlmacenSeguro,private val plataforma: Plataforma) {
    private val json=Json { ignoreUnknownKeys=true }
    private val estado=MutableStateFlow<Sesion?>(null)
    val sesion=estado.asStateFlow()
    init {
        val previa=runCatching { almacen.leer()?.let { json.decodeFromString<SesionDto>(it) } }.getOrNull()
        if(previa!=null && previa.expiraEn>plataforma.ahora()) estado.value=previa.dominio()
        else almacen.borrar()
    }
    fun guardar(s: SesionDto) { almacen.guardar(json.encodeToString(s)); estado.value=s.dominio() }
    fun cerrar() { almacen.borrar(); estado.value=null }
    fun actual(): Sesion {
        val s=estado.value ?: throw ErrorApi.NoAutorizado()
        if(s.expiraEn<=plataforma.ahora()) { cerrar(); throw ErrorApi.NoAutorizado() }
        return s
    }
}
