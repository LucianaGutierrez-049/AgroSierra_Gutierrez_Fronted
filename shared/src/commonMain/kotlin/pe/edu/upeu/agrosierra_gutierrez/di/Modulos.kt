package pe.edu.upeu.agrosierra_gutierrez.di
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
import pe.edu.upeu.agrosierra_gutierrez.data.local.*
import pe.edu.upeu.agrosierra_gutierrez.data.remote.*
import pe.edu.upeu.agrosierra_gutierrez.data.repository.AgroRepositoryImpl
import pe.edu.upeu.agrosierra_gutierrez.domain.repository.*
import pe.edu.upeu.agrosierra_gutierrez.domain.usecase.*
import pe.edu.upeu.agrosierra_gutierrez.presentation.AgroViewModel
val dataModule=module {
    single { SesionManager(get(),get()) }
    single { AgroApi(get(),get()) }
    single<AgroRepository> { AgroRepositoryImpl(get(),get(),get(),get()) }
}
val redModule=module { single { ApiConfig() }; single { crearCliente(get(),get(),get()) } }
val domainModule=module {
    factory { SincronizarUseCase(get()) }; factory { GuardarLaborUseCase(get()) }
    factory { EliminarLaborUseCase(get()) }; factory { CerrarCampaniaUseCase(get()) }
}
val presentationModule=module { viewModel { AgroViewModel(get(),get(),get(),get(),get(),get(),get(),get()) } }
