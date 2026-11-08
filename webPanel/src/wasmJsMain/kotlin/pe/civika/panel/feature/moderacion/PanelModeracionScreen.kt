package pe.civika.panel.feature.moderacion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.panel.ui.PanelColores
import pe.civika.panel.ui.TarjetaPanel
import pe.civika.shared.domain.Denuncia

/**
 * Cola de moderación (RF-16): una tarjeta por denuncia, con la acción principal del motivo,
 * «Marcar como duplicado» y «Rechazar el reporte», como en el mockup del panel.
 */
@Composable
fun PanelModeracionScreen(viewModel: PanelModeracionViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when {
            estado.cargando -> Centrado {
                CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando la cola de moderación" })
            }
            estado.error != null -> Centrado {
                Text(
                    estado.error.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> Contenido(estado, viewModel)
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun Contenido(estado: PanelModeracionUiState, viewModel: PanelModeracionViewModel) {
    Column(
        Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Moderación",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Denuncias de vecinos sobre el contenido de un reporte. Resuélvelas antes de que el reporte siga su curso.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (estado.aviso != null) {
            TarjetaPanel {
                Text(
                    estado.aviso.orEmpty(),
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (estado.avisoError) MaterialTheme.colorScheme.error else PanelColores.VerdeAndino,
                )
            }
        }

        if (!puedeModerar(viewModel.rol)) {
            TarjetaPanel {
                Text(
                    "Esta cola es de la moderación. Con tu rol de entidad puedes revisarla, pero la resuelve Rosa T. " +
                        "desde la cuenta de la moderación.",
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (estado.vacia) {
            TarjetaPanel {
                Text(
                    "No hay denuncias pendientes. Cuando alguien reporte un problema con el contenido de un reporte, " +
                        "aparecerá aquí.",
                    Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            estado.denuncias.forEach { denuncia ->
                TarjetaDenuncia(
                    denuncia = denuncia,
                    puedeActuar = puedeModerar(viewModel.rol),
                    alResolver = { accion -> viewModel.resolver(denuncia.id, accion) },
                )
            }
        }
    }
}

@Composable
private fun TarjetaDenuncia(
    denuncia: Denuncia,
    puedeActuar: Boolean,
    alResolver: (AccionModeracion) -> Unit,
) {
    TarjetaPanel {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.small, color = PanelColores.VerdeAndino) {
                    Text(
                        denuncia.motivo.etiqueta,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    denuncia.id,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(denuncia.titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "${denuncia.codigoIncidente}, ${denuncia.meta}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(denuncia.detalle, style = MaterialTheme.typography.bodyMedium)

            if (!puedeActuar) {
                Text(
                    "Tu rol no resuelve denuncias; esta acción está disponible con la cuenta de la moderación.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { alResolver(AccionModeracion.PRINCIPAL) },
                enabled = puedeActuar,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PanelColores.VerdeAndino,
                    contentColor = Color.White,
                ),
            ) {
                Text(AccionModeracion.PRINCIPAL.etiqueta(denuncia), style = MaterialTheme.typography.labelLarge)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { alResolver(AccionModeracion.DUPLICADO) },
                    enabled = puedeActuar,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(AccionModeracion.DUPLICADO.etiqueta(denuncia), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { alResolver(AccionModeracion.RECHAZAR) },
                    enabled = puedeActuar,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(AccionModeracion.RECHAZAR.etiqueta(denuncia), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
