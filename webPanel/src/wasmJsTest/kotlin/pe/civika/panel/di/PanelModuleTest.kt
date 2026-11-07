package pe.civika.panel.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import pe.civika.panel.feature.incidentes.PanelIncidentesViewModel
import pe.civika.panel.feature.indicadores.PanelIndicadoresViewModel
import pe.civika.panel.feature.moderacion.PanelModeracionViewModel
import pe.civika.panel.feature.sesion.PanelLoginViewModel
import pe.civika.shared.data.DenunciaRepository
import pe.civika.shared.data.SesionRepository
import pe.civika.shared.di.sharedModule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * ADR-10: Koin resuelve al ejecutar; en Wasm no existe koin-test (es JVM).
 * Arrancamos el mismo grafo que Main y resolvemos todo lo que el panel usa.
 */
class PanelModuleTest {

    @AfterTest
    fun cerrarKoin() {
        stopKoin()
    }

    private fun grafoDelPanel() = startKoin {
        modules(sharedModule, panelModule)
    }.koin

    @Test
    fun elGrafoDelPanelArrancaYResuelveElViewModelDeIncidentes() {
        assertNotNull(grafoDelPanel().get<PanelIncidentesViewModel>())
    }

    @Test
    fun resuelveLosViewModelsDeLasTresSeccionesYElLogin() {
        val koin = grafoDelPanel()

        assertNotNull(koin.get<PanelIndicadoresViewModel>())
        assertNotNull(koin.get<PanelModeracionViewModel>())
        assertNotNull(koin.get<PanelLoginViewModel>())
    }

    @Test
    fun resuelveLosRepositoriosDeSesionYDeModeracion() {
        val koin = grafoDelPanel()

        assertNotNull(koin.get<SesionRepository>())
        assertNotNull(koin.get<DenunciaRepository>())
    }
}
