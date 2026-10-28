package pe.edu.upeu.agrosierra_gutierrez
import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pe.edu.upeu.agrosierra_gutierrez.db.AgroDb
import pe.edu.upeu.agrosierra_gutierrez.di.*
import pe.edu.upeu.agrosierra_gutierrez.data.local.AlmacenSeguro
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.Plataforma
class AgroApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val platformModule=module {
            single<AlmacenSeguro> { AlmacenSeguroAndroid(this@AgroApplication) }
            single<Plataforma> { object: Plataforma {
                override fun ahora()=System.currentTimeMillis()
                override fun fechaHoy()=java.time.LocalDate.now().toString()
                override fun dentroDe24Horas(fecha: String)=runCatching {
                    java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).isBefore(java.time.LocalDateTime.parse(fecha).plusHours(24))
                }.getOrDefault(false)
            } }
            single { AgroDb(AndroidSqliteDriver(AgroDb.Schema,this@AgroApplication,"agrocampo.db")) }
            single<HttpClientEngine> { OkHttp.create() }
        }
        startKoin { modules(platformModule,dataModule,redModule,domainModule,presentationModule) }
    }
}
