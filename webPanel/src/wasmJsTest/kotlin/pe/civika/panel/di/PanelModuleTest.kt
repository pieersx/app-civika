package pe.civika.panel.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import pe.civika.panel.feature.incidentes.PanelIncidentesViewModel
import pe.civika.shared.di.sharedModule
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * ADR-10: Koin resuelve al ejecutar; en Wasm no existe koin-test (es JVM).
 * Arrancamos el mismo grafo que Main y resolvemos el ViewModel del panel.
 */
class PanelModuleTest {

    @AfterTest
    fun cerrarKoin() {
        stopKoin()
    }

    @Test
    fun elGrafoDelPanelArrancaYResuelveElViewModel() {
        val koin = startKoin {
            modules(sharedModule, panelModule)
        }.koin

        val viewModel = koin.get<PanelIncidentesViewModel>()
        assertNotNull(viewModel)
    }
}
