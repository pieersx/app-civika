package pe.civika.shared.di

import org.koin.dsl.module
import pe.civika.shared.data.FakeIncidenteRepository
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.ObtenerIncidentesPriorizados

/** Dependencias comunes. En el Sprint 2 el repositorio fake se cambia por el de supabase-kt solo aquí. */
val sharedModule = module {
    single<IncidenteRepository> { FakeIncidenteRepository() }
    factory { ObtenerIncidentesPriorizados(get()) }
}
