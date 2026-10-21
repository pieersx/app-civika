package pe.civika.panel.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.civika.panel.feature.incidentes.PanelIncidentesViewModel

val panelModule = module {
    viewModelOf(::PanelIncidentesViewModel)
}
