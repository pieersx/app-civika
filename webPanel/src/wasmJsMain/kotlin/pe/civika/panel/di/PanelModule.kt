package pe.civika.panel.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import pe.civika.panel.feature.incidentes.PanelIncidentesViewModel
import pe.civika.panel.feature.indicadores.PanelIndicadoresViewModel
import pe.civika.panel.feature.moderacion.PanelModeracionViewModel
import pe.civika.panel.feature.sesion.PanelLoginViewModel
import pe.civika.shared.data.DenunciaRepository
import pe.civika.shared.data.FakeDenunciaRepository
import pe.civika.shared.data.FakeSesionRepository
import pe.civika.shared.data.SesionRepository

/**
 * Dependencias del panel. Los dos repositorios nuevos son fake: cuando exista Supabase Auth
 * (solo clave `anon`) y la cola de denuncias reales, solo cambia esta línea.
 */
val panelModule = module {
    single<SesionRepository> { FakeSesionRepository() }
    single<DenunciaRepository> { FakeDenunciaRepository() }

    viewModelOf(::PanelLoginViewModel)
    viewModelOf(::PanelIncidentesViewModel)
    viewModelOf(::PanelIndicadoresViewModel)
    viewModelOf(::PanelModeracionViewModel)
}
