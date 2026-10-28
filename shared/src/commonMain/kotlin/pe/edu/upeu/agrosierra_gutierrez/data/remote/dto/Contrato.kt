package pe.edu.upeu.agrosierra_gutierrez.data.remote.dto
import kotlinx.serialization.Serializable
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
@Serializable data class LoginDto(val usuario: String,val contrasena: String)
@Serializable data class SesionDto(val token: String,val usuario: String,val nombre: String="",val rol: String,
    val privilegios: Set<String>,val usuarioId: Long,val expiraEn: Long,val edicionPropia: Boolean=true) {
    fun dominio()=Sesion(token,usuario,rol,privilegios,nombre,usuarioId,expiraEn,edicionPropia)
}
@Serializable data class LoteDto(val id: Long,val codigo: String,val nombre: String,val hectareas: Double,
    val cultivo: String,val valle: String,val campaniaCerrada: Boolean,val dosisMaximaPorHectarea: Double) {
    fun dominio()=Lote(id,codigo,nombre,hectareas,cultivo,valle,campaniaCerrada,dosisMaximaPorHectarea)
}
@Serializable data class LaborDto(val id: Long,val loteId: Long,val tipo: String,val fecha: String,
    val responsable: String,val cantidad: Double,val observacion: String,val pendiente: Boolean=false,
    val autorId: Long=0,val registradoEn: String="") {
    fun dominio()=Labor(id,loteId,TipoLabor.valueOf(tipo),fecha,responsable,cantidad,observacion,false,autorId,registradoEn)
}
@Serializable data class LaborPeticionDto(val loteId: Long,val tipo: String,val fecha: String,
    val cantidad: Double,val observacion: String,val clienteId: String?=null)
@Serializable data class PaginaDto<T>(val contenido: List<T>,val pagina: Int,val tamanio: Int,
    val totalElementos: Long,val totalPaginas: Int,val ultima: Boolean)
@Serializable data class ErrorDto(val message: String="No pudimos completar la operación",val validationErrors: Map<String,String> = emptyMap())
