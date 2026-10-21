package pe.civika.panel

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.koin.core.context.startKoin
import pe.civika.panel.di.panelModule
import pe.civika.panel.feature.incidentes.PanelIncidentesScreen
import pe.civika.panel.ui.PanelTheme
import pe.civika.shared.di.sharedModule

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(sharedModule, panelModule)
    }
    ComposeViewport(document.body!!) {
        PanelTheme {
            PanelIncidentesScreen()
        }
    }
}
