package pe.edu.upeu.agrosierra_gutierrez.data.remote
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pe.edu.upeu.agrosierra_gutierrez.data.local.SesionManager
import pe.edu.upeu.agrosierra_gutierrez.data.remote.dto.*
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
fun traducirError(status: Int,e: ErrorDto): ErrorApi = when(status) {
    400 -> ErrorApi.Validacion(e.validationErrors,e.message)
    401 -> ErrorApi.NoAutorizado()
    403 -> ErrorApi.SinPrivilegio(e.message)
    404 -> ErrorApi.NoEncontrado()
    409 -> ErrorApi.Conflicto(e.message)
    else -> ErrorApi.Servidor()
}
class AgroApi(private val client: HttpClient,private val sesion: SesionManager) {
    private suspend fun ejecutar(login: Boolean=false,request: suspend ()->HttpResponse): HttpResponse {
        val response=try { request() }
        catch(e: CancellationException) { throw e }
        catch(e: HttpRequestTimeoutException) { throw ErrorApi.TiempoAgotado() }
        catch(e: SerializationException) { throw ErrorApi.Servidor() }
        catch(e: Exception) { println("AgroCampo conexion: ${e::class.simpleName}: ${e.message}"); throw ErrorApi.SinConexion() }
        if(response.status.value !in 200..299) {
            val error=runCatching { response.body<ErrorDto>() }.getOrDefault(ErrorDto())
            if(response.status.value==401 && !login) sesion.cerrar()
            if(response.status.value==401 && login) throw ErrorApi.Validacion(emptyMap(),"Usuario o contraseña incorrectos")
            throw traducirError(response.status.value,error)
        }
        return response
    }
    suspend fun login(usuario: String,clave: String) {
        val dto=ejecutar(login=true) { client.post("auth/login") { setBody(LoginDto(usuario,clave)) } }.body<SesionDto>()
        sesion.guardar(dto)
    }
    suspend fun lotes(pagina: Int)=ejecutar { client.get("lotes") { parameter("pagina",pagina); parameter("tamanio",100) } }.body<PaginaDto<LoteDto>>()
    suspend fun labores(lote: Long,pagina: Int)=ejecutar { client.get("lotes/$lote/labores") { parameter("pagina",pagina); parameter("tamanio",100) } }.body<PaginaDto<LaborDto>>()
    suspend fun guardar(p: LaborPeticionDto,id: Long?): LaborDto = ejecutar {
        if(id==null || id<0) client.post("labores") { setBody(p) }
        else client.put("labores/$id") { setBody(p) }
    }.body()
    suspend fun eliminar(id: Long) { ejecutar { client.delete("labores/$id") } /* 204: no deserializar cuerpo. */ }
    suspend fun cerrar(id: Long)=ejecutar { client.post("lotes/$id/cerrar-campania") }.body<LoteDto>()
}
