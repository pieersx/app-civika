package pe.civika.app.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.civika.app.device.AjustesApp
import pe.civika.app.device.AjustesDataStore
import pe.civika.app.device.AlmacenFotos
import pe.civika.app.device.AlmacenFotosAndroid
import pe.civika.app.device.RegistroDispositivo
import pe.civika.app.device.TokenDispositivo
import pe.civika.app.device.TokenFcm
import pe.civika.app.device.UbicacionFused
import pe.civika.app.device.UbicacionProvider
import pe.civika.app.feature.auth.AuthViewModel
import pe.civika.app.feature.home.HomeViewModel
import pe.civika.app.feature.incidente.DetalleViewModel
import pe.civika.app.feature.incidente.MisReportesViewModel
import pe.civika.app.feature.mapa.MapaViewModel
import pe.civika.app.feature.notificaciones.NotificacionesViewModel
import pe.civika.app.feature.perfil.PerfilViewModel
import pe.civika.app.feature.reporte.ReporteViewModel
import pe.civika.app.local.CivikaDatabase
import pe.civika.app.local.RoomColaReportes
import pe.civika.app.sync.ProgramadorSync
import pe.civika.app.sync.ProgramadorWorkManager
import pe.civika.shared.data.ColaReportesRepository
import pe.civika.shared.di.ALCANCE_APP

/** Dependencias de Android: dispositivo, base de datos local, cola y ViewModels. */
val appModule = module {
    single<AlmacenFotos> { AlmacenFotosAndroid(androidContext()) }
    single<UbicacionProvider> { UbicacionFused(androidContext()) }
    single<TokenDispositivo> { TokenFcm(androidContext()) }
    single<AjustesApp> { AjustesDataStore(androidContext()) }

    single { CivikaDatabase.crear(androidContext()) }
    single { get<CivikaDatabase>().reportes() }
    single<ProgramadorSync> { ProgramadorWorkManager(androidContext()) }
    single<ColaReportesRepository> { RoomColaReportes(get(), get(), get()) }
    single { RegistroDispositivo(get(), get(), get(), get(ALCANCE_APP)) }

    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::MapaViewModel)
    viewModelOf(::MisReportesViewModel)
    viewModelOf(::NotificacionesViewModel)
    viewModelOf(::PerfilViewModel)
    // El SavedStateHandle lo aporta Koin al crear el ViewModel.
    viewModel {
        ReporteViewModel(
            estadoGuardado = get(),
            incidentes = get(),
            cola = get(),
            auth = get(),
            fotos = get(),
            ubicacion = get(),
        )
    }
    viewModel { parametros -> DetalleViewModel(incidenteId = parametros.get(), incidentes = get(), auth = get()) }
}
