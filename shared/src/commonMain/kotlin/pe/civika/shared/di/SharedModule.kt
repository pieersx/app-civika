package pe.civika.shared.di

import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.DatosDemo
import pe.civika.shared.data.FakeAuthRepository
import pe.civika.shared.data.FakeGeocodificador
import pe.civika.shared.data.FakeIncidenteRepository
import pe.civika.shared.data.FakeNotificacionRepository
import pe.civika.shared.data.FakeReporteRepository
import pe.civika.shared.data.Geocodificador
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.data.NominatimGeocodificador
import pe.civika.shared.data.NotificacionRepository
import pe.civika.shared.data.ReporteRepository
import pe.civika.shared.data.SupabaseAuthRepository
import pe.civika.shared.data.SupabaseConfig
import pe.civika.shared.data.SupabaseIncidenteRepository
import pe.civika.shared.data.SupabaseNotificacionRepository
import pe.civika.shared.data.SupabaseReporteRepository
import pe.civika.shared.data.crearClienteSupabase
import pe.civika.shared.domain.ObtenerIncidentesPriorizados
import pe.civika.shared.domain.SincronizarReporte

/** Alcance de las corrutinas que viven lo mismo que la app (sesión, registro del dispositivo). */
val ALCANCE_APP = named("alcanceApp")

/** Pausa de los repositorios de ejemplo, para que en el modo demostración se vean los estados de carga. */
private const val DEMORA_DEMO_MS = 350L

/** Módulo en modo demostración (sin Supabase). Es el que usan las pruebas de grafo. */
val sharedModule: Module = crearSharedModule(null)

/**
 * Dependencias comunes. Con un [SupabaseConfig] (hay URL y clave `anon`) los repositorios son los reales; con `null`
 * la app arranca en **modo demostración** con los `Fake…`, sin red. Se decide al construir el módulo y no al resolver,
 * para que el resultado no dependa del orden de los módulos de Koin.
 */
fun crearSharedModule(config: SupabaseConfig?): Module = module {
    single(ALCANCE_APP) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<ModoApp> { if (config != null) ModoApp.REAL else ModoApp.DEMOSTRACION }

    single<Repositorios> {
        if (config != null) {
            val cliente: SupabaseClient = crearClienteSupabase(config)
            val incidentes = SupabaseIncidenteRepository(cliente, config)
            Repositorios(
                auth = SupabaseAuthRepository(cliente, get(ALCANCE_APP)),
                incidentes = incidentes,
                reportes = SupabaseReporteRepository(cliente, incidentes),
                notificaciones = SupabaseNotificacionRepository(cliente),
                geocodificador = NominatimGeocodificador(),
            )
        } else {
            val demo = DatosDemo()
            Repositorios(
                auth = FakeAuthRepository(demo, DEMORA_DEMO_MS),
                incidentes = FakeIncidenteRepository(demo, DEMORA_DEMO_MS),
                reportes = FakeReporteRepository(demo, DEMORA_DEMO_MS),
                notificaciones = FakeNotificacionRepository(demo, DEMORA_DEMO_MS),
                geocodificador = FakeGeocodificador(),
            )
        }
    }

    single<AuthRepository> { get<Repositorios>().auth }
    single<IncidenteRepository> { get<Repositorios>().incidentes }
    single<ReporteRepository> { get<Repositorios>().reportes }
    single<NotificacionRepository> { get<Repositorios>().notificaciones }
    single<Geocodificador> { get<Repositorios>().geocodificador }

    factory { ObtenerIncidentesPriorizados(get()) }
    factory { SincronizarReporte(get(), get(), get()) }
}

enum class ModoApp { REAL, DEMOSTRACION }

/** Los repositorios se crean juntos: o todos hablan con Supabase o todos comparten los mismos datos de ejemplo. */
class Repositorios(
    val auth: AuthRepository,
    val incidentes: IncidenteRepository,
    val reportes: ReporteRepository,
    val notificaciones: NotificacionRepository,
    val geocodificador: Geocodificador,
)
