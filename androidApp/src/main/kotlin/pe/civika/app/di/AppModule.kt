package pe.civika.app.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.civika.app.feature.incidentes.IncidentesViewModel

val appModule = module {
    viewModelOf(::IncidentesViewModel)
}
