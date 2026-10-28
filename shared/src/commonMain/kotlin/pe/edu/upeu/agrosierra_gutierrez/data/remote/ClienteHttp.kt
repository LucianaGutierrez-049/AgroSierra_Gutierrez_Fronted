package pe.edu.upeu.agrosierra_gutierrez.data.remote
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pe.edu.upeu.agrosierra_gutierrez.data.local.SesionManager
data class ApiConfig(val baseUrl: String="http://10.0.2.2:8081/api/v1/")
fun crearCliente(engine: HttpClientEngine,sesion: SesionManager,config: ApiConfig)=HttpClient(engine) {
    expectSuccess=false
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true; encodeDefaults=true }) }
    install(HttpTimeout) { requestTimeoutMillis=15000; connectTimeoutMillis=8000; socketTimeoutMillis=15000 }
    install(Logging) { level=LogLevel.NONE; sanitizeHeader { it==HttpHeaders.Authorization } }
    defaultRequest {
        url(config.baseUrl)
        contentType(ContentType.Application.Json)
        sesion.sesion.value?.token?.let { bearerAuth(it) }
    }
}
