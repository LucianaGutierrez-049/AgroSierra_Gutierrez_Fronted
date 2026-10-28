package pe.edu.upeu.agrosierra_gutierrez.domain.model

data class Lote(val id: Long,val codigo: String,val nombre: String,val hectareas: Double,val cultivo: String,
    val valle: String,val campaniaCerrada: Boolean,val dosisMaximaPorHectarea: Double)
enum class TipoLabor { RIEGO, FUMIGACION, COSECHA }
data class Labor(val id: Long,val loteId: Long,val tipo: TipoLabor,val fecha: String,val responsable: String,
    val cantidad: Double,val observacion: String,val pendiente: Boolean,val autorId: Long=0,
    val registradoEn: String="",val error: String="")
data class Sesion(val token: String,val usuario: String,val rol: String,val privilegios: Set<String>,
    val nombre: String="",val usuarioId: Long=0,val expiraEn: Long=0,val edicionPropia: Boolean=true)
data class FormularioLabor(val loteId: Long,val tipo: TipoLabor,val fecha: String,val cantidad: Double,val observacion: String)
data class ResumenSync(val enviados: Int,val recibidos: Int,val conflictos: Int=0)
sealed class ErrorApi(override val message: String): Exception(message) {
    class Validacion(val campos: Map<String,String>,mensaje: String="Revisa los datos") : ErrorApi(mensaje)
    class NoAutorizado : ErrorApi("Tu sesión venció. Vuelve a ingresar")
    class SinPrivilegio(m: String="No tienes permiso para esta operación") : ErrorApi(m)
    class NoEncontrado : ErrorApi("El registro ya no existe")
    class Conflicto(m: String) : ErrorApi(m)
    class Servidor : ErrorApi("El servidor no pudo responder. Intenta nuevamente")
    class SinConexion : ErrorApi("Sin conexión. Puedes continuar con tus datos guardados")
    class TiempoAgotado : ErrorApi("La conexión tardó demasiado. Intenta nuevamente")
}
