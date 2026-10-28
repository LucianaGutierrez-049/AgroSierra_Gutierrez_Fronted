package pe.edu.upeu.agrosierra_gutierrez

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.agrosierra_gutierrez.db.AgroDb
import pe.edu.upeu.agrosierra_gutierrez.data.local.*
import pe.edu.upeu.agrosierra_gutierrez.data.remote.*
import pe.edu.upeu.agrosierra_gutierrez.data.remote.dto.*
import pe.edu.upeu.agrosierra_gutierrez.data.repository.AgroRepositoryImpl
import pe.edu.upeu.agrosierra_gutierrez.domain.model.*
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.Plataforma
import kotlin.test.*

expect fun driverPruebas(): SqlDriver
private class MemoriaSegura: AlmacenSeguro {
    var valor: String?=null
    override fun leer()=valor
    override fun guardar(valor: String) { this.valor=valor }
    override fun borrar() { valor=null }
}
class AgroOfflineTest {
    private val plataforma=object: Plataforma {
        override fun ahora()=1000L
        override fun fechaHoy()="2026-10-27"
        override fun dentroDe24Horas(fecha: String)=true
    }
    private fun sesion(almacen: MemoriaSegura=MemoriaSegura())=SesionManager(almacen,plataforma).apply {
        guardar(SesionDto("token","tecnico1","Técnica","TECNICO",setOf("LABOR_LEER","LABOR_CREAR","LABOR_EDITAR"),1,999999))
    }
    @Test fun convierteDtoSinContaminarDominio() {
        val dominio=LoteDto(1,"L-01","El Mirador",2.0,"Papa","Mantaro",false,2.5).dominio()
        assertEquals("El Mirador",dominio.nombre); assertEquals(5.0,dominio.hectareas*dominio.dosisMaximaPorHectarea)
    }
    @Test fun distingue401403YValidacion() {
        assertIs<ErrorApi.NoAutorizado>(traducirError(401,ErrorDto()))
        assertIs<ErrorApi.SinPrivilegio>(traducirError(403,ErrorDto()))
        val e=traducirError(400,ErrorDto("Datos inválidos",mapOf("cantidad" to "Dosis excedida")))
        assertEquals("Dosis excedida",assertIs<ErrorApi.Validacion>(e).campos["cantidad"])
    }
    @Test fun guardaSinRedConIdNegativoYPendiente()=runTest {
        val driver=driverPruebas(); val db=AgroDb(driver); val session=sesion()
        val client=crearCliente(MockEngine { throw IllegalStateException("sin red") },session,ApiConfig("http://localhost/api/v1/"))
        try {
            val repo=AgroRepositoryImpl(db,AgroApi(client,session),session,plataforma)
            repo.guardar(FormularioLabor(1,TipoLabor.RIEGO,"2026-10-27",2.0,"Registro offline"))
            repo.guardar(FormularioLabor(1,TipoLabor.RIEGO,"2026-10-27",3.0,"Segundo registro"))
            val rows=db.agroDbQueries.pendientes("1").executeAsList()
            assertEquals(2,rows.size); assertTrue(rows.all { it.id<0 && it.pendiente==1L }); assertEquals(2,rows.map { it.id }.toSet().size)
        } finally { client.close(); driver.close() }
    }
    @Test fun sincronizaPostAntesDeGetYReemplazaSinDuplicados()=runTest {
        val driver=driverPruebas(); val db=AgroDb(driver); val session=sesion(); val orden=mutableListOf<String>()
        val labor="""{"id":50,"loteId":1,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"Técnica","cantidad":2.0,"observacion":"offline","autorId":1}"""
        val lote="""{"id":1,"codigo":"L-01","nombre":"Mirador","hectareas":2.0,"cultivo":"Papa","valle":"Mantaro","campaniaCerrada":false,"dosisMaximaPorHectarea":2.5}"""
        fun pagina(contenido: String)="""{"contenido":[$contenido],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}"""
        val client=crearCliente(MockEngine { request ->
            orden+="${request.method.value} ${request.url.encodedPath}"
            val body=when {
                request.method==HttpMethod.Post -> labor
                request.url.encodedPath.endsWith("/labores") -> pagina(labor)
                else -> pagina(lote)
            }
            respond(body,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },session,ApiConfig("http://localhost/api/v1/"))
        try {
            db.agroDbQueries.guardarLabor("1",-1,1,"RIEGO","2026-10-27","Técnica",2.0,"offline",1,1,"","POST","peticion-unica","")
            val repo=AgroRepositoryImpl(db,AgroApi(client,session),session,plataforma)
            val resumen=repo.sincronizar()
            assertTrue(orden.first().startsWith("POST")); assertTrue(orden.drop(1).all { it.startsWith("GET") })
            assertEquals(1,resumen.enviados); assertTrue(db.agroDbQueries.pendientes("1").executeAsList().isEmpty())
            assertNull(db.agroDbQueries.porId("1",-1).executeAsOneOrNull())
            assertNotNull(db.agroDbQueries.porId("1",50).executeAsOneOrNull())
            repo.sincronizar()
            assertEquals(1,db.agroDbQueries.observarLaboresPorLote("1",1).executeAsList().size)
        } finally { client.close(); driver.close() }
    }
    @Test fun error403ConservaSesionY401LaBorra()=runTest {
        val session=sesion(); var status=HttpStatusCode.Forbidden
        val client=crearCliente(MockEngine { respond("""{"message":"Rechazado"}""",status,headersOf(HttpHeaders.ContentType,"application/json")) },session,ApiConfig("http://localhost/api/v1/"))
        try {
            val api=AgroApi(client,session)
            assertFailsWith<ErrorApi.SinPrivilegio> { api.lotes(0) }; assertNotNull(session.sesion.value)
            status=HttpStatusCode.Unauthorized
            assertFailsWith<ErrorApi.NoAutorizado> { api.lotes(0) }; assertNull(session.sesion.value)
        } finally { client.close() }
    }
    @Test fun delete204NoIntentaDeserializar()=runTest {
        val session=sesion()
        val client=crearCliente(MockEngine { respond("",HttpStatusCode.NoContent) },session,ApiConfig("http://localhost/api/v1/"))
        try { AgroApi(client,session).eliminar(12); assertNotNull(session.sesion.value) } finally { client.close() }
    }
    @Test fun separaLosDatosLocalesPorUsuario() {
        val driver=driverPruebas(); val db=AgroDb(driver)
        try {
            db.agroDbQueries.guardarLote("1",1,"L-01","Privado",2.0,"Papa","Mantaro",0,2.5)
            assertEquals(1,db.agroDbQueries.observarLotes("1","","").executeAsList().size)
            assertTrue(db.agroDbQueries.observarLotes("2","","").executeAsList().isEmpty())
        } finally { driver.close() }
    }
}
