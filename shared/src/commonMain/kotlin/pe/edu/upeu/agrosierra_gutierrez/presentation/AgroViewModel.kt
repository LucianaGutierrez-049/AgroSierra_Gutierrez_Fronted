package pe.edu.upeu.agrosierra_gutierrez.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.*
import pe.edu.upeu.agrosierra_gutierrez.domain.usecase.*
import pe.edu.upeu.agrosierra_gutierrez.data.local.SesionManager
import pe.edu.upeu.agrosierra_gutierrez.data.remote.AgroApi

data class UiState(val sesion: Sesion?=null,val lotes: List<Lote> = emptyList(),val labores: List<Labor> = emptyList(),
    val seleccionado: Lote?=null,val busqueda: String="",val valle: String="",val cargando: Boolean=false,
    val mensaje: String="",val campos: Map<String,String> = emptyMap(),val ultimaSync: Long?=null,
    val reintentar: Boolean=false,val perfil: Boolean=false,val formulario: Boolean=false,val editar: Labor?=null)
class AgroViewModel(private val repo: AgroRepository,private val api: AgroApi,private val sesiones: SesionManager,
    private val sync: SincronizarUseCase,private val guardar: GuardarLaborUseCase,
    private val eliminar: EliminarLaborUseCase,private val cerrar: CerrarCampaniaUseCase,val plataforma: Plataforma): ViewModel() {
    private val mutable=MutableStateFlow(UiState()); val estado=mutable.asStateFlow()
    private var listaJob: Job?=null; private var detalleJob: Job?=null; private var syncJob: Job?=null
    init { viewModelScope.launch {
        sesiones.sesion.collect { s ->
            listaJob?.cancel(); detalleJob?.cancel(); syncJob?.cancel()
            mutable.value=UiState(sesion=s)
            if(s!=null) { observarLista(); sincronizar() }
        }
    } }
    private fun observarLista() {
        listaJob?.cancel(); if(estado.value.sesion==null) return
        listaJob=viewModelScope.launch { try {
            combine(repo.observarLotes(estado.value.busqueda,estado.value.valle),repo.observarUltimaSync()) { lotes,ultima -> lotes to ultima }
                .collect { (lotes,ultima) -> mutable.update { st -> st.copy(lotes=lotes,ultimaSync=ultima,
                    seleccionado=st.seleccionado?.let { sel -> lotes.find { it.id==sel.id } ?: sel }) } }
        } catch(e: ErrorApi) { error(e) } }
    }
    fun filtrar(busqueda: String=estado.value.busqueda,valle: String=estado.value.valle) {
        mutable.update { it.copy(busqueda=busqueda,valle=valle) }; observarLista()
    }
    fun seleccionar(lote: Lote) {
        detalleJob?.cancel(); mutable.update { it.copy(seleccionado=lote,labores=emptyList(),mensaje="") }
        detalleJob=viewModelScope.launch { try { repo.observarLabores(lote.id).collect { rows -> mutable.update { it.copy(labores=rows) } } }
            catch(e: ErrorApi) { error(e) } }
    }
    fun volver() { detalleJob?.cancel(); mutable.update { it.copy(seleccionado=null,perfil=false,formulario=false,mensaje="") } }
    fun perfil() { mutable.update { it.copy(perfil=true,mensaje="") } }
    fun salir() { sesiones.cerrar() }
    fun formulario(labor: Labor?=null) { mutable.update { it.copy(formulario=true,editar=labor,campos=emptyMap(),mensaje="") } }
    fun cancelarFormulario() { mutable.update { it.copy(formulario=false,campos=emptyMap(),mensaje="") } }
    fun limpiarMensaje() { mutable.update { it.copy(mensaje="") } }
    private fun error(e: Throwable) {
        mutable.update { it.copy(cargando=false,mensaje=e.message ?: "No pudimos completar la operación",
            campos=(e as? ErrorApi.Validacion)?.campos ?: emptyMap(),
            reintentar=e is ErrorApi.Servidor || e is ErrorApi.SinConexion || e is ErrorApi.TiempoAgotado) }
    }
    private fun accion(block: suspend ()->Unit) { viewModelScope.launch {
        mutable.update { it.copy(cargando=true,mensaje="",campos=emptyMap()) }
        try { block() } catch(e: CancellationException) { throw e } catch(e: Throwable) { error(e) }
        finally { mutable.update { it.copy(cargando=false) } }
    } }
    fun login(usuario: String,clave: String) = accion {
        if(usuario.isBlank() || clave.isBlank()) throw ErrorApi.Validacion(emptyMap(),"Ingresa usuario y contraseña")
        api.login(usuario.trim(),clave)
    }
    fun sincronizar() {
        if(syncJob?.isActive==true || estado.value.sesion==null) return
        syncJob=viewModelScope.launch {
            mutable.update { it.copy(cargando=true,mensaje="") }
            try { val r=sync(); mutable.update { it.copy(mensaje="Sincronización: ${r.enviados} enviados · ${r.recibidos} recibidos"+
                if(r.conflictos>0) " · ${r.conflictos} pendientes requieren revisión" else "") } }
            catch(e: CancellationException) { throw e } catch(e: Throwable) { error(e) }
            finally { mutable.update { it.copy(cargando=false) } }
        }
    }
    fun guardarLabor(tipo: TipoLabor,fecha: String,cantidad: String,observacion: String) = accion {
        val lote=estado.value.seleccionado ?: return@accion
        val valor=cantidad.replace(',','.').toDoubleOrNull() ?: throw ErrorApi.Validacion(mapOf("cantidad" to "Ingresa un número válido"))
        guardar(FormularioLabor(lote.id,tipo,fecha,valor,observacion),estado.value.editar?.id)
        mutable.update { it.copy(formulario=false,mensaje="Labor guardada. Si no hay conexión, queda pendiente de sincronizar") }
    }
    fun eliminarLabor(id: Long) = accion { eliminar(id); mutable.update { it.copy(mensaje="Eliminación registrada") } }
    fun cerrarCampania() = accion {
        val lote=estado.value.seleccionado ?: return@accion
        cerrar(lote.id); mutable.update { it.copy(seleccionado=lote.copy(campaniaCerrada=true),mensaje="Campaña cerrada") }
    }
}
