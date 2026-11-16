package pe.civika.app.feature.incidente

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pe.civika.app.R
import pe.civika.app.feature.mapa.CapasIncidentes
import pe.civika.app.feature.mapa.MapaBase
import pe.civika.app.ui.components.BarraSuperior
import pe.civika.app.ui.components.BotonPrincipal
import pe.civika.app.ui.components.BotonSecundario
import pe.civika.app.ui.components.CampoTexto
import pe.civika.app.ui.components.CampoUi
import pe.civika.app.ui.components.CargandoEsqueleto
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.EstadoError
import pe.civika.app.ui.components.EstadoVacio
import pe.civika.app.ui.components.MarcadorSenal
import pe.civika.app.ui.components.SenalEstado
import pe.civika.app.ui.components.encabezado
import pe.civika.app.ui.components.icono
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.mensajeDeCampo
import pe.civika.app.ui.components.rememberFecha
import pe.civika.app.ui.components.textoDias
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.Coordenada
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Validaciones
import pe.civika.shared.domain.ZonaPiloto
import kotlin.math.abs

@Composable
fun DetalleScreen(
    incidenteId: String,
    onVolver: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: DetalleViewModel = koinViewModel(parameters = { parametersOf(incidenteId) }),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    var evento by remember { mutableStateOf<EventoDetalle?>(null) }
    var denunciando by remember { mutableStateOf(false) }
    val contexto = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect {
            if (it == EventoDetalle.RequiereSesion) onIniciarSesion() else evento = it
        }
    }
    val textoEvento = when (val e = evento) {
        is EventoDetalle.Aviso -> stringResource(
            when (e.aviso) {
                AvisoDetalle.APOYO_AGREGADO -> R.string.aviso_apoyo_agregado
                AvisoDetalle.APOYO_RETIRADO -> R.string.aviso_apoyo_retirado
                AvisoDetalle.VERIFICACION_ENVIADA -> R.string.aviso_verificacion
                AvisoDetalle.REABIERTO -> R.string.aviso_reabierto
                AvisoDetalle.DENUNCIA_ENVIADA -> R.string.aviso_denuncia
                AvisoDetalle.DENUNCIA_A_REVISION -> R.string.aviso_denuncia_revision
            },
        )
        is EventoDetalle.ErrorAccion -> e.error.mensaje()
        else -> null
    }
    LaunchedEffect(textoEvento) {
        if (textoEvento != null) {
            avisos.showSnackbar(textoEvento)
            evento = null
        }
    }

    val listo = estado as? DetalleUiState.Listo
    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = listo?.detalle?.incidente?.codigo ?: stringResource(R.string.detalle_titulo),
                onVolver = onVolver,
                acciones = {
                    if (listo != null) {
                        val d = listo.detalle.incidente
                        val compartir = stringResource(R.string.compartir_texto, d.codigo, d.titulo)
                        IconButton(onClick = {
                            val intento = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, compartir)
                            }
                            contexto.startActivity(Intent.createChooser(intento, null))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.compartir_codigo))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(avisos) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            when (val e = estado) {
                DetalleUiState.Cargando -> CargandoEsqueleto(filas = 3)
                DetalleUiState.NoEncontrado -> EstadoVacio(
                    icono = Icons.Rounded.SearchOff,
                    titulo = stringResource(R.string.detalle_no_encontrado_titulo),
                    texto = stringResource(R.string.detalle_no_encontrado_texto),
                    accion = stringResource(R.string.volver),
                    onAccion = onVolver,
                )
                is DetalleUiState.Error -> EstadoError(e.error.mensaje(), viewModel::cargar)
                is DetalleUiState.Listo -> ContenidoDetalle(
                    detalle = e.detalle,
                    ocupado = e.ocupado,
                    onApoyar = viewModel::alternarApoyo,
                    onVerificar = viewModel::verificar,
                    onDenunciar = { denunciando = true },
                )
            }
        }
    }

    if (denunciando) {
        DialogoDenuncia(
            onCancelar = { denunciando = false },
            onEnviar = {
                denunciando = false
                viewModel.denunciar(it)
            },
        )
    }
}

@Composable
private fun ContenidoDetalle(
    detalle: DetalleIncidente,
    ocupado: Boolean,
    onApoyar: () -> Unit,
    onVerificar: (Boolean) -> Unit,
    onDenunciar: () -> Unit,
) {
    val incidente = detalle.incidente
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (detalle.fotos.isNotEmpty()) Fotos(detalle.fotos)

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SenalEstado(incidente.estado)
            Text(incidente.titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.encabezado())
            detalle.descripcion?.takeIf { it != incidente.titulo }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            detalle.motivoRechazo?.let { motivo ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.detalle_motivo_rechazo, motivo), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                }
            }
            Datos(detalle)
        }

        Mini(detalle)

        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (detalle.esMio) {
                Text(stringResource(R.string.detalle_es_tuyo), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (detalle.puedeApoyar) {
                if (detalle.yoApoyo) {
                    BotonSecundario(stringResource(R.string.detalle_ya_apoyas), onApoyar, habilitado = !ocupado, icono = Icons.Rounded.Check)
                } else {
                    BotonPrincipal(stringResource(R.string.detalle_apoyar), onApoyar, cargando = ocupado, icono = Icons.Rounded.ThumbUp)
                }
            }
            if (detalle.puedeVerificar) Verificacion(detalle.miVerificacion, ocupado, onVerificar)
        }

        if (detalle.historial.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.detalle_historial), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.encabezado())
                detalle.historial.forEach { evento ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightMin()) {
                        MarcadorSenal(evento.estado.familia, 28.dp)
                        Column {
                            Text(evento.estado.etiqueta, style = MaterialTheme.typography.titleMedium)
                            Text(rememberFecha(evento.fecha), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (detalle.comentarios.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.detalle_comentarios), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.encabezado())
                detalle.comentarios.forEach { comentario ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, CivikaTheme.colores.borde)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(comentario.texto, style = MaterialTheme.typography.bodyLarge)
                            Text(rememberFecha(comentario.fecha), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (detalle.puedeDenunciar) {
            CivikaTextButton(stringResource(R.string.denunciar), onDenunciar, Modifier.padding(horizontal = 8.dp))
        }
    }
}

private fun Modifier.heightMin(): Modifier = this.then(Modifier.height(48.dp))

@Composable
private fun Fotos(fotos: List<String>) {
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(fotos) { indice, url ->
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.detalle_foto_cd, indice + 1, fotos.size),
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(if (fotos.size == 1) 360.dp else 280.dp).height(220.dp).clip(MaterialTheme.shapes.medium),
            )
        }
    }
}

@Composable
private fun Datos(detalle: DetalleIncidente) {
    val i = detalle.incidente
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Fila(i.categoria.icono(), stringResource(R.string.tarjeta_meta_corta, i.categoria.nombre, i.zona))
        Fila(
            Icons.Rounded.ThumbUp,
            stringResource(R.string.tarjeta_meta_corta, pluralStringResource(R.plurals.apoyos_n, i.apoyos, i.apoyos), textoDias(i.diasAbierto)),
        )
        Fila(
            Icons.Rounded.Business,
            detalle.entidad?.let { stringResource(R.string.detalle_entidad, it) } ?: stringResource(R.string.detalle_sin_entidad),
        )
        if (i.estado.familia in pe.civika.shared.domain.FAMILIAS_POR_ATENDER) {
            val dias = i.diasParaElPlazo
            Fila(
                Icons.Rounded.Schedule,
                if (dias >= 0) {
                    pluralStringResource(R.plurals.detalle_plazo, i.categoria.plazoObjetivoDias, i.categoria.plazoObjetivoDias, dias)
                } else {
                    pluralStringResource(R.plurals.detalle_plazo_vencido, abs(dias), abs(dias))
                },
                color = if (dias < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Fila(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun Mini(detalle: DetalleIncidente) {
    val i = detalle.incidente
    val lat = i.latitud ?: return
    val lon = i.longitud ?: return
    val colores = CivikaTheme.colores
    val densidad = LocalDensity.current
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium),
    ) {
        MapaBase(
            centro = Coordenada(lat, lon),
            zoom = 16.0,
            modifier = Modifier.fillMaxSize(),
            gestos = false,
            descripcion = stringResource(R.string.detalle_mapa_cd),
            onEstiloListo = { _, estilo ->
                CapasIncidentes.instalar(estilo, colores, densidad, agrupar = false)
                CapasIncidentes.mostrar(estilo, listOf(i))
            },
        )
    }
}

@Composable
private fun Verificacion(respuesta: Boolean?, ocupado: Boolean, onResponder: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, CivikaTheme.colores.borde),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.verificar_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.encabezado())
            Text(stringResource(R.string.verificar_texto), style = MaterialTheme.typography.bodyMedium)
            if (respuesta != null) {
                Text(
                    stringResource(if (respuesta) R.string.verificar_tu_respuesta_si else R.string.verificar_tu_respuesta_no),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            BotonPrincipal(stringResource(R.string.verificar_si), { onResponder(true) }, habilitado = !ocupado)
            BotonSecundario(stringResource(R.string.verificar_no), { onResponder(false) }, habilitado = !ocupado)
        }
    }
}

@Composable
private fun DialogoDenuncia(onCancelar: () -> Unit, onEnviar: (String) -> Unit) {
    var motivo by remember { mutableStateOf("") }
    var intentado by remember { mutableStateOf(false) }
    val validacion = Validaciones.motivoDenuncia(motivo)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.denunciar_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.denunciar_texto), style = MaterialTheme.typography.bodyMedium)
                CampoTexto(
                    valor = motivo,
                    onCambio = { motivo = it.take(Validaciones.MOTIVO_MAX) },
                    etiqueta = stringResource(R.string.denunciar_motivo),
                    error = if (intentado) validacion.mensajeDeCampo(CampoUi.MOTIVO) else null,
                    minLineas = 3,
                )
            }
        },
        confirmButton = {
            CivikaTextButton(stringResource(R.string.denunciar_enviar), {
                intentado = true
                if (validacion.esValida) onEnviar(motivo)
            })
        },
        dismissButton = { CivikaTextButton(stringResource(R.string.cancelar), onCancelar) },
    )
}
