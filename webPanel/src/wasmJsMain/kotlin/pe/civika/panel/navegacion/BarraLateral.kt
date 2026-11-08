package pe.civika.panel.navegacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.civika.panel.ui.ChipPanel
import pe.civika.panel.ui.FilaNavegacion
import pe.civika.panel.ui.IconoIndicadores
import pe.civika.panel.ui.IconoIncidentes
import pe.civika.panel.ui.IconoModeracion
import pe.civika.panel.ui.LogoCivika
import pe.civika.panel.ui.PanelColores
import pe.civika.shared.domain.Rol
import pe.civika.shared.domain.Sesion

/** Secciones del panel (mockup: barra lateral izquierda con las tres). */
enum class SeccionPanel(val titulo: String) {
    INCIDENTES("Incidentes"),
    INDICADORES("Indicadores"),
    MODERACION("Moderación"),
}

/** Título de la tarjeta de sesión según quién entra (como en el mockup). */
private fun tituloDeSesion(sesion: Sesion) = when (sesion.rol) {
    Rol.MODERADOR -> "Moderación de Cívika"
    else -> "Municipalidad del distrito piloto"
}

/** Barra lateral verde oscuro: logo, navegación y la tarjeta de sesión abajo. */
@Composable
fun BarraLateral(
    seccion: SeccionPanel,
    pendientes: Int,
    sesion: Sesion,
    alCambiarSeccion: (SeccionPanel) -> Unit,
    alCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = PanelColores.VerdeProfundo) {
        Column(Modifier.fillMaxSize().padding(vertical = 20.dp)) {
            LogoCivika(Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilaNavegacion(
                    texto = SeccionPanel.INCIDENTES.titulo,
                    seleccionado = seccion == SeccionPanel.INCIDENTES,
                    onClick = { alCambiarSeccion(SeccionPanel.INCIDENTES) },
                    icono = { IconoIncidentes() },
                )
                FilaNavegacion(
                    texto = SeccionPanel.INDICADORES.titulo,
                    seleccionado = seccion == SeccionPanel.INDICADORES,
                    onClick = { alCambiarSeccion(SeccionPanel.INDICADORES) },
                    icono = { IconoIndicadores() },
                )
                FilaNavegacion(
                    texto = SeccionPanel.MODERACION.titulo,
                    seleccionado = seccion == SeccionPanel.MODERACION,
                    onClick = { alCambiarSeccion(SeccionPanel.MODERACION) },
                    fin = { if (pendientes > 0) ContadorModeracion(pendientes) },
                    icono = { IconoModeracion() },
                )
            }
            Spacer(Modifier.weight(1f))
            TarjetaSesion(sesion, alCerrarSesion, Modifier.padding(horizontal = 12.dp))
        }
    }
}

/** Versión angosta (celular / panel revisado en 390 px): mismos controles en una fila superior. */
@Composable
fun BarraSuperiorPanel(
    seccion: SeccionPanel,
    pendientes: Int,
    alCambiarSeccion: (SeccionPanel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = PanelColores.VerdeProfundo) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoCivika()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SeccionPanel.entries.forEach { seccionActual ->
                    ChipPanel(
                        texto = if (seccionActual == SeccionPanel.MODERACION && pendientes > 0) {
                            "${seccionActual.titulo} ($pendientes)"
                        } else {
                            seccionActual.titulo
                        },
                        seleccionado = seccion == seccionActual,
                        onClick = { alCambiarSeccion(seccionActual) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContadorModeracion(pendientes: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = PanelColores.Amarillo,
        modifier = Modifier.size(22.dp),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("$pendientes", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1F1B16))
        }
    }
}

@Composable
private fun TarjetaSesion(sesion: Sesion, alCerrarSesion: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, color = PanelColores.VerdeProfundoSuperficie) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(tituloDeSesion(sesion), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(
                "Sesión de ${sesion.nombre}, ${sesion.cargo}",
                style = MaterialTheme.typography.bodyMedium,
                color = PanelColores.TextoSobreVerdeSuave,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color.Transparent,
                border = BorderStroke(1.dp, PanelColores.VerdeProfundoSuperficie),
                onClick = alCerrarSesion,
            ) {
                Text(
                    "Cerrar sesión",
                    style = MaterialTheme.typography.labelMedium,
                    color = PanelColores.TextoSobreVerdeSuave,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
            }
        }
    }
}
