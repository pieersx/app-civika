package pe.civika.app.feature.incidente

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.app.R
import pe.civika.app.ui.components.CargandoEsqueleto
import pe.civika.app.ui.components.ChipFiltro
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.EstadoError
import pe.civika.app.ui.components.EstadoVacio
import pe.civika.app.ui.components.TarjetaIncidente
import pe.civika.app.ui.components.encabezado
import pe.civika.app.ui.components.icono
import pe.civika.app.ui.components.iconoDeCategoria
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.textoDeMotivo
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.FiltroMisReportes
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Relacion
import pe.civika.shared.domain.ReportePendiente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisReportesScreen(
    onAbrirIncidente: (String) -> Unit,
    onReportar: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: MisReportesViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            stringResource(R.string.nav_mis_reportes),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp).encabezado(),
        )
        when (val e = estado) {
            MisReportesUiState.Cargando -> CargandoEsqueleto()
            MisReportesUiState.SinSesion -> EstadoVacio(
                icono = Icons.Rounded.Lock,
                titulo = stringResource(R.string.mis_reportes_sin_sesion_titulo),
                texto = stringResource(R.string.mis_reportes_sin_sesion_texto),
                accion = stringResource(R.string.iniciar_sesion),
                onAccion = onIniciarSesion,
            )
            is MisReportesUiState.Error -> EstadoError(e.error.mensaje(), viewModel::refrescar)
            is MisReportesUiState.Listo -> {
                if (e.total == 0) {
                    EstadoVacio(
                        icono = Icons.Rounded.Inbox,
                        titulo = stringResource(R.string.mis_reportes_vacio_titulo),
                        texto = stringResource(R.string.mis_reportes_vacio_texto),
                        accion = stringResource(R.string.reportar_problema),
                        onAccion = onReportar,
                    )
                } else {
                    Filtros(e.filtro, viewModel::cambiarFiltro)
                    PullToRefreshBox(isRefreshing = e.refrescando, onRefresh = viewModel::refrescar, modifier = Modifier.fillMaxSize()) {
                        if (e.items.isEmpty()) {
                            EstadoVacio(
                                icono = Icons.Rounded.SearchOff,
                                titulo = stringResource(R.string.mis_reportes_filtro_vacio_titulo),
                                texto = stringResource(R.string.mis_reportes_filtro_vacio_texto),
                                accion = stringResource(R.string.mis_reportes_ver_todos),
                                onAccion = { viewModel.cambiarFiltro(FiltroMisReportes.TODOS) },
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(e.items, key = { it.clave() }) { item ->
                                    when (item) {
                                        is ItemMiReporte.Local -> TarjetaEnvio(
                                            envio = item.envio,
                                            onAbrir = { item.envio.incidenteCreadoId?.let(onAbrirIncidente) },
                                            onReintentar = { viewModel.reintentar(item.envio.clienteId) },
                                            onDescartar = { viewModel.descartar(item.envio.clienteId) },
                                        )
                                        is ItemMiReporte.Remoto -> TarjetaIncidente(
                                            item.mi.incidente,
                                            onClick = { onAbrirIncidente(item.mi.incidente.id) },
                                            extra = {
                                                Text(
                                                    stringResource(if (item.mi.relacion == Relacion.AUTOR) R.string.mis_reportes_relacion_autor else R.string.mis_reportes_relacion_apoyo),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ItemMiReporte.clave(): String = when (this) {
    is ItemMiReporte.Local -> "local-${envio.clienteId}"
    is ItemMiReporte.Remoto -> "remoto-${mi.incidente.id}"
}

@Composable
private fun Filtros(filtro: FiltroMisReportes, onCambiar: (FiltroMisReportes) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(FiltroMisReportes.entries) { opcion ->
            ChipFiltro(
                texto = stringResource(
                    when (opcion) {
                        FiltroMisReportes.TODOS -> R.string.filtro_todos
                        FiltroMisReportes.ABIERTOS -> R.string.filtro_abiertos
                        FiltroMisReportes.RESUELTOS -> R.string.filtro_resueltos
                        FiltroMisReportes.MIS_APOYOS -> R.string.filtro_apoyos
                    },
                ),
                seleccionado = filtro == opcion,
                onClick = { onCambiar(opcion) },
            )
        }
    }
}

/** Un envío de la cola local: pendiente, enviando, enviado o con error. Nunca solo con color: ícono y palabra. */
@Composable
private fun TarjetaEnvio(envio: ReportePendiente, onAbrir: () -> Unit, onReintentar: () -> Unit, onDescartar: () -> Unit) {
    val enviado = envio.estado == EstadoEnvio.ENVIADO && envio.incidenteCreadoId != null
    Card(
        onClick = onAbrir,
        enabled = enviado,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            // Un envío sin terminar no se abre, pero su texto debe leerse con el mismo contraste que el resto.
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, CivikaTheme.colores.borde),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val etiqueta = stringResource(
                when (envio.estado) {
                    EstadoEnvio.PENDIENTE -> R.string.envio_pendiente
                    EstadoEnvio.ENVIANDO -> R.string.envio_enviando
                    EstadoEnvio.ERROR -> R.string.envio_error
                    EstadoEnvio.ENVIADO -> R.string.envio_enviado
                },
            )
            Row(
                Modifier.semantics(mergeDescendants = true) { contentDescription = etiqueta },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (envio.estado) {
                    EstadoEnvio.ENVIANDO -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    EstadoEnvio.PENDIENTE -> Icon(Icons.Rounded.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    EstadoEnvio.ERROR -> Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    EstadoEnvio.ENVIADO -> Icon(Icons.Rounded.CheckCircle, null, tint = CivikaTheme.colores.estado(Familia.RESUELTO).texto, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = if (enviado) "$etiqueta · ${envio.codigo.orEmpty()}" else etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (envio.estado == EstadoEnvio.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(envio.descripcion, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(envio.categoriaNombre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (envio.estado == EstadoEnvio.ERROR || (envio.estado == EstadoEnvio.PENDIENTE && envio.motivoError != null)) {
                Text(textoDeMotivo(envio.motivoError, envio.detalleError), style = MaterialTheme.typography.bodyMedium)
            }
            if (envio.estado == EstadoEnvio.ERROR) {
                Row {
                    CivikaTextButton(stringResource(R.string.reintentar), onReintentar)
                    CivikaTextButton(stringResource(R.string.envio_descartar), onDescartar)
                }
            }
        }
    }
}
