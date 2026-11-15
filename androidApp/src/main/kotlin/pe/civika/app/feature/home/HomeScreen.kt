package pe.civika.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.app.R
import pe.civika.app.ui.components.AvisoDemostracion
import pe.civika.app.ui.components.CargandoEsqueleto
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.EstadoError
import pe.civika.app.ui.components.EstadoVacio
import pe.civika.app.ui.components.MarcadorSenal
import pe.civika.app.ui.components.TarjetaIncidente
import pe.civika.app.ui.components.encabezado
import pe.civika.app.ui.components.mensaje
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.ResumenPublico

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAbrirIncidente: (String) -> Unit,
    onNotificaciones: () -> Unit,
    onIniciarSesion: () -> Unit,
    onReportar: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Al volver de Notificaciones el contador de no leídas se actualiza.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (viewModel.estado.value is HomeUiState.Listo) viewModel.refrescar() }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        when (val e = estado) {
            HomeUiState.Cargando -> {
                Cabecera(nombre = null, noLeidas = 0, conSesion = false, onNotificaciones, onIniciarSesion)
                CargandoEsqueleto()
            }
            is HomeUiState.Error -> {
                Cabecera(nombre = null, noLeidas = 0, conSesion = false, onNotificaciones, onIniciarSesion)
                EstadoError(e.error.mensaje(), onReintentar = viewModel::refrescar)
            }
            is HomeUiState.Listo -> PullToRefreshBox(
                isRefreshing = e.refrescando,
                onRefresh = viewModel::refrescar,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("lista_inicio"),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Cabecera(e.nombre, e.noLeidas, conSesion = e.nombre != null, onNotificaciones, onIniciarSesion)
                    }
                    item { AvisoDemostracion(Modifier.padding(horizontal = 16.dp)) }
                    e.resumen?.let { item { ResumenFila(it) } }
                    item {
                        Text(
                            stringResource(R.string.inicio_urgente),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp).encabezado(),
                        )
                    }
                    if (e.prioritarios.isEmpty()) {
                        item {
                            EstadoVacio(
                                icono = Icons.Rounded.CheckCircle,
                                titulo = stringResource(R.string.inicio_vacio_titulo),
                                texto = stringResource(R.string.inicio_vacio_texto),
                                accion = stringResource(R.string.reportar_problema),
                                onAccion = onReportar,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    } else {
                        items(e.prioritarios, key = { it.id }) { incidente ->
                            TarjetaIncidente(
                                incidente,
                                onClick = { onAbrirIncidente(incidente.id) },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cabecera(
    nombre: String?,
    noLeidas: Int,
    conSesion: Boolean,
    onNotificaciones: () -> Unit,
    onIniciarSesion: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (nombre != null) stringResource(R.string.inicio_saludo, nombre.substringBefore(' ')) else stringResource(R.string.inicio_saludo_anonimo),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).encabezado(),
        )
        if (conSesion) {
            val descripcion = if (noLeidas > 0) {
                pluralStringResource(R.plurals.cd_notificaciones_sin_leer, noLeidas, noLeidas)
            } else {
                stringResource(R.string.cd_notificaciones)
            }
            IconButton(onClick = onNotificaciones, modifier = Modifier.semantics { contentDescription = descripcion }) {
                BadgedBox(badge = { if (noLeidas > 0) Badge { Text(if (noLeidas > 9) "9+" else noLeidas.toString()) } }) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null)
                }
            }
        } else if (nombre == null) {
            CivikaTextButton(stringResource(R.string.iniciar_sesion), onIniciarSesion)
        }
    }
}

@Composable
private fun ResumenFila(resumen: ResumenPublico) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ResumenTarjeta(Familia.ABIERTO, resumen.abiertos, stringResource(R.string.resumen_abiertos), Modifier.weight(1f))
        ResumenTarjeta(Familia.EN_ATENCION, resumen.enAtencion, stringResource(R.string.resumen_en_atencion), Modifier.weight(1f))
        ResumenTarjeta(Familia.RESUELTO, resumen.resueltos30d, stringResource(R.string.resumen_resueltos), Modifier.weight(1f))
    }
}

@Composable
private fun ResumenTarjeta(familia: Familia, cantidad: Int, etiqueta: String, modifier: Modifier) {
    Card(
        modifier = modifier.fillMaxHeight().semantics(mergeDescendants = true) { contentDescription = "$cantidad $etiqueta" },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MarcadorSenal(familia, 24.dp)
            Text(cantidad.toString(), style = MaterialTheme.typography.headlineSmall)
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
