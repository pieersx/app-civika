package pe.civika.app.feature.notificaciones

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.app.R
import pe.civika.app.ui.components.BarraSuperior
import pe.civika.app.ui.components.CargandoEsqueleto
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.EstadoError
import pe.civika.app.ui.components.EstadoVacio
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.rememberFecha
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.Notificacion
import pe.civika.shared.domain.TipoNotificacion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    onVolver: () -> Unit,
    onAbrirIncidente: (String) -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: NotificacionesViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val listo = estado as? NotificacionesUiState.Listo

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = stringResource(R.string.notificaciones_titulo),
                onVolver = onVolver,
                acciones = {
                    if (listo != null && listo.noLeidas > 0) {
                        CivikaTextButton(stringResource(R.string.notificaciones_marcar_todas), viewModel::marcarTodasLeidas)
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when (val e = estado) {
                NotificacionesUiState.Cargando -> CargandoEsqueleto()
                NotificacionesUiState.SinSesion -> EstadoVacio(
                    icono = Icons.Rounded.Lock,
                    titulo = stringResource(R.string.notificaciones_sin_sesion_titulo),
                    texto = stringResource(R.string.notificaciones_sin_sesion_texto),
                    accion = stringResource(R.string.iniciar_sesion),
                    onAccion = onIniciarSesion,
                )
                is NotificacionesUiState.Error -> EstadoError(e.error.mensaje(), viewModel::cargar)
                is NotificacionesUiState.Listo -> PullToRefreshBox(isRefreshing = e.refrescando, onRefresh = viewModel::refrescar, modifier = Modifier.fillMaxSize()) {
                    if (e.items.isEmpty()) {
                        EstadoVacio(
                            icono = Icons.Rounded.NotificationsNone,
                            titulo = stringResource(R.string.notificaciones_vacio_titulo),
                            texto = stringResource(R.string.notificaciones_vacio_texto),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(e.items, key = { it.id }) { notificacion ->
                                FilaNotificacion(notificacion) {
                                    viewModel.marcarLeida(notificacion.id)
                                    notificacion.incidenteId?.let(onAbrirIncidente)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun textoDe(n: Notificacion): String {
    val titulo = n.titulo ?: n.codigo.orEmpty()
    return when (n.tipo) {
        TipoNotificacion.ESTADO -> stringResource(R.string.notificacion_estado, titulo, n.estado?.etiqueta.orEmpty())
        TipoNotificacion.REABIERTO -> stringResource(R.string.notificacion_reabierto, titulo)
        TipoNotificacion.COMENTARIO -> stringResource(R.string.notificacion_comentario, titulo)
        TipoNotificacion.OTRO -> stringResource(R.string.notificacion_otro, n.codigo.orEmpty())
    }
}

@Composable
private fun FilaNotificacion(n: Notificacion, onClick: () -> Unit) {
    val texto = textoDe(n)
    val fecha = rememberFecha(n.fecha)
    val sinLeer = stringResource(R.string.notificacion_sin_leer)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription = if (n.leida) "$texto. $fecha" else "$sinLeer. $texto. $fecha"
        },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (n.leida) 1.dp else 2.dp, if (n.leida) CivikaTheme.colores.borde else MaterialTheme.colorScheme.secondary),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = when (n.tipo) {
                    TipoNotificacion.REABIERTO -> Icons.Rounded.Replay
                    TipoNotificacion.COMENTARIO -> Icons.Rounded.ChatBubbleOutline
                    else -> Icons.Rounded.SwapHoriz
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Lo no leído se distingue con la palabra «Nuevo» y el peso del texto, no solo con el borde de color.
                if (!n.leida) Text(sinLeer, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (n.leida) FontWeight.Normal else FontWeight.Bold,
                )
                Text(fecha, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
