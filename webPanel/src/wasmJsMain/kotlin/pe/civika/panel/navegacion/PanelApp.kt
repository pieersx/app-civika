package pe.civika.panel.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.panel.feature.incidentes.PanelIncidentesScreen
import pe.civika.panel.feature.indicadores.PanelIndicadoresScreen
import pe.civika.panel.feature.moderacion.PanelModeracionScreen
import pe.civika.panel.feature.moderacion.PanelModeracionViewModel
import pe.civika.panel.feature.sesion.PanelLoginScreen
import pe.civika.shared.data.SesionRepository
import pe.civika.shared.domain.Sesion

/** Ancho a partir del cual entra la barra lateral verde (por debajo va la barra superior). */
private val ANCHO_CON_BARRA_LATERAL = 900.dp
private val ANCHO_BARRA_LATERAL = 280.dp

/**
 * Carcasa del panel: decide entre el login y el panel con sesión activa, y entre la barra lateral
 * (escritorio) y la barra superior (pantallas angostas, como la revisión en 390 px).
 */
@Composable
fun PanelApp() {
    val sesionRepository = koinInject<SesionRepository>()
    val sesion by sesionRepository.sesion.collectAsStateWithLifecycle()

    val sesionActual = sesion
    if (sesionActual == null) {
        PanelLoginScreen()
    } else {
        PanelConSesion(sesionActual, alCerrarSesion = sesionRepository::cerrarSesion)
    }
}

@Composable
private fun PanelConSesion(sesion: Sesion, alCerrarSesion: () -> Unit) {
    val moderacion: PanelModeracionViewModel = koinViewModel()
    val estadoModeracion by moderacion.estado.collectAsStateWithLifecycle()

    var seccion by remember { mutableStateOf(SeccionPanel.INCIDENTES) }
    // En pantallas angostas el detalle ocupa toda la pantalla y se vuelve con «Volver a la lista».
    var detalleEnPantallaCompleta by remember { mutableStateOf(false) }
    val alCambiarSeccion: (SeccionPanel) -> Unit = {
        seccion = it
        detalleEnPantallaCompleta = false
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= ANCHO_CON_BARRA_LATERAL) {
            Row(Modifier.fillMaxSize()) {
                BarraLateral(
                    seccion = seccion,
                    pendientes = estadoModeracion.pendientes,
                    sesion = sesion,
                    alCambiarSeccion = alCambiarSeccion,
                    alCerrarSesion = alCerrarSesion,
                    modifier = Modifier.width(ANCHO_BARRA_LATERAL).fillMaxHeight(),
                )
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    ContenidoDelPanel(
                        seccion = seccion,
                        detalleEnPantallaCompleta = detalleEnPantallaCompleta,
                        alAbrirDetalle = { detalleEnPantallaCompleta = true },
                        alVolverDeDetalle = { detalleEnPantallaCompleta = false },
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                BarraSuperiorPanel(
                    seccion = seccion,
                    pendientes = estadoModeracion.pendientes,
                    alCambiarSeccion = alCambiarSeccion,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    ContenidoDelPanel(
                        seccion = seccion,
                        detalleEnPantallaCompleta = detalleEnPantallaCompleta,
                        alAbrirDetalle = { detalleEnPantallaCompleta = true },
                        alVolverDeDetalle = { detalleEnPantallaCompleta = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContenidoDelPanel(
    seccion: SeccionPanel,
    detalleEnPantallaCompleta: Boolean,
    alAbrirDetalle: () -> Unit,
    alVolverDeDetalle: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when (seccion) {
            SeccionPanel.INCIDENTES -> PanelIncidentesScreen(
                mostrarDetalleEnAngosto = detalleEnPantallaCompleta,
                alAbrirDetalle = alAbrirDetalle,
                alVolverDeDetalle = alVolverDeDetalle,
            )

            SeccionPanel.INDICADORES -> PanelIndicadoresScreen()
            SeccionPanel.MODERACION -> PanelModeracionScreen()
        }
    }
}
