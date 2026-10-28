package pe.edu.upeu.agrosierra_gutierrez.data.repository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.agrosierra_gutierrez.db.AgroDb
import pe.edu.upeu.agrosierra_gutierrez.data.local.SesionManager
import pe.edu.upeu.agrosierra_gutierrez.data.remote.AgroApi
import pe.edu.upeu.agrosierra_gutierrez.data.remote.dto.*
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.*
import kotlin.random.Random

class AgroRepositoryImpl(private val db: AgroDb,private val api: AgroApi,private val sesion: SesionManager,
    private val plataforma: Plataforma) : AgroRepository {
    private val q=db.agroDbQueries
    private val mutex=Mutex()
    private fun propietario()=sesion.actual().usuarioId.toString()
    override fun observarLotes(busqueda: String,valle: String): Flow<List<Lote>> =
        q.observarLotes(propietario(),valle,busqueda).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map {
            Lote(it.id,it.codigo,it.nombre,it.hectareas,it.cultivo,it.valle,it.campaniaCerrada==1L,it.dosisMaxima)
        } }
    override fun observarLabores(loteId: Long): Flow<List<Labor>> =
        q.observarLaboresPorLote(propietario(),loteId).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.dominio() } }
    override fun observarUltimaSync()=q.ultimaSync(propietario()).asFlow().mapToOneOrNull(Dispatchers.Default)
    private fun pe.edu.upeu.agrosierragutierrez.db.Labor.dominio()=Labor(id,loteId,TipoLabor.valueOf(tipo),fecha,
        responsable,cantidad,observacion,pendiente==1L,autorId,registradoEn,error)
    private fun almacenar(owner: String,d: LaborDto) {
        // Una descarga nunca pisa una edición o eliminación todavía pendiente.
        if(q.porId(owner,d.id).executeAsOneOrNull()?.pendiente==1L) return
        q.guardarLabor(owner,d.id,d.loteId,d.tipo,d.fecha,d.responsable,d.cantidad,d.observacion,0,d.autorId,d.registradoEn,"","","")
    }
    private fun almacenar(owner: String,d: LoteDto)=q.guardarLote(owner,d.id,d.codigo,d.nombre,d.hectareas,d.cultivo,d.valle,if(d.campaniaCerrada) 1 else 0,d.dosisMaximaPorHectarea)
    private fun validar(p: FormularioLabor) {
        val campos=mutableMapOf<String,String>()
        if(!p.cantidad.isFinite() || p.cantidad<=0) campos["cantidad"]="Ingresa una cantidad mayor que cero"
        if(!Regex("\\d{4}-\\d{2}-\\d{2}").matches(p.fecha)) campos["fecha"]="Usa el formato AAAA-MM-DD"
        if(p.observacion.length>1000) campos["observacion"]="Máximo 1000 caracteres"
        if(campos.isNotEmpty()) throw ErrorApi.Validacion(campos)
    }
    override suspend fun guardar(formulario: FormularioLabor,id: Long?) = withContext(Dispatchers.Default) {
        mutex.withLock {
            validar(formulario); val s=sesion.actual(); val owner=s.usuarioId.toString()
            if((if(id==null) "LABOR_CREAR" else "LABOR_EDITAR") !in s.privilegios) throw ErrorApi.SinPrivilegio()
            val anterior=id?.let { q.porId(owner,it).executeAsOneOrNull() }
            val localId=id ?: minOf(-1L,q.siguienteIdLocal(owner).executeAsOne()-1)
            val cliente=anterior?.clienteId?.takeIf { it.isNotBlank() } ?: "${s.usuarioId}-${plataforma.ahora()}-${Random.nextLong()}"
            val operacion=if(localId<0) "POST" else "PUT"
            // Primero se guarda: el formulario funciona aunque la red desaparezca justo ahora.
            q.guardarLabor(owner,localId,formulario.loteId,formulario.tipo.name,formulario.fecha,
                anterior?.responsable ?: s.nombre,formulario.cantidad,formulario.observacion,1,
                anterior?.autorId ?: s.usuarioId,anterior?.registradoEn ?: "",operacion,cliente,"")
            try { enviar(owner,localId) }
            catch(e: ErrorApi.SinConexion) { }
            catch(e: ErrorApi.TiempoAgotado) { }
            catch(e: ErrorApi) { q.marcarError(e.message,owner,localId); throw e }
        }
    }
    private suspend fun enviar(owner: String,id: Long) {
        val row=q.porId(owner,id).executeAsOneOrNull() ?: return
        check(owner==propietario()) { "La sesión cambió durante la sincronización" }
        if(row.operacion=="DELETE") {
            try { api.eliminar(row.id) } catch (_: ErrorApi.NoEncontrado) { }
            q.borrarLabor(owner,row.id)
        } else {
            val result=api.guardar(LaborPeticionDto(row.loteId,row.tipo,row.fecha,row.cantidad,row.observacion,row.clienteId),row.id)
            db.transaction { q.borrarLabor(owner,row.id); almacenar(owner,result) }
        }
    }
    override suspend fun eliminar(id: Long) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val s=sesion.actual(); if("LABOR_ELIMINAR" !in s.privilegios) throw ErrorApi.SinPrivilegio()
            val owner=propietario(); val row=q.porId(owner,id).executeAsOneOrNull() ?: return@withLock
            if(id<0) { q.borrarLabor(owner,id); return@withLock }
            q.guardarLabor(owner,id,row.loteId,row.tipo,row.fecha,row.responsable,row.cantidad,row.observacion,1,
                row.autorId,row.registradoEn,"DELETE",row.clienteId,"")
            try { enviar(owner,id) } catch (_: ErrorApi.SinConexion) { } catch (_: ErrorApi.TiempoAgotado) { }
            catch(e: ErrorApi) { q.marcarError(e.message,owner,id); throw e }
        }
    }
    override suspend fun cerrar(loteId: Long) { mutex.withLock { val owner=propietario(); val lote=api.cerrar(loteId); almacenar(owner,lote) } }
    override suspend fun sincronizar(): ResumenSync = withContext(Dispatchers.Default) {
        mutex.withLock {
            val owner=propietario(); var enviados=0; var recibidos=0; var conflictos=0
            // 1. Subir todas las operaciones antes de iniciar cualquier GET.
            for(row in q.pendientes(owner).executeAsList()) {
                try { enviar(owner,row.id); enviados++ }
                catch(e: ErrorApi.Validacion) { q.marcarError(e.message+": "+e.campos.values.joinToString(),owner,row.id); conflictos++ }
                catch(e: ErrorApi.Conflicto) { q.marcarError(e.message,owner,row.id); conflictos++ }
                catch(e: ErrorApi.SinPrivilegio) { q.marcarError(e.message,owner,row.id); conflictos++ }
                catch(e: ErrorApi.NoEncontrado) { q.marcarError(e.message,owner,row.id); conflictos++ }
            }
            // 2. Descargar todas las páginas y aplicar cada lote completo en una transacción.
            val lotes=mutableListOf<LoteDto>(); var pagina=0
            do { val respuesta=api.lotes(pagina++); lotes+=respuesta.contenido } while(!respuesta.ultima)
            for(lote in lotes) {
                val labores=mutableListOf<LaborDto>(); pagina=0
                do { val respuesta=api.labores(lote.id,pagina++); labores+=respuesta.contenido } while(!respuesta.ultima)
                db.transaction {
                    almacenar(owner,lote); q.borrarConfirmadasLote(owner,lote.id)
                    labores.forEach { almacenar(owner,it) }
                }
                recibidos+=1+labores.size
            }
            q.guardarSync(owner,plataforma.ahora())
            ResumenSync(enviados,recibidos,conflictos)
        }
    }
}
