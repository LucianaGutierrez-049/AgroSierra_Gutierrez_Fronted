package pe.edu.upeu.agrosierra_gutierrez.domain.repository
import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
interface AgroRepository {
    fun observarLotes(busqueda: String,valle: String): Flow<List<Lote>>
    fun observarLabores(loteId: Long): Flow<List<Labor>>
    fun observarUltimaSync(): Flow<Long?>
    suspend fun guardar(formulario: FormularioLabor,id: Long?=null)
    suspend fun eliminar(id: Long)
    suspend fun cerrar(loteId: Long)
    suspend fun sincronizar(): ResumenSync
}
interface Plataforma { fun ahora(): Long; fun fechaHoy(): String; fun dentroDe24Horas(fecha: String): Boolean }
