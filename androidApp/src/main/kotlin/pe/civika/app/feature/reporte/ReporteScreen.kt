package pe.civika.app.feature.reporte

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import pe.civika.app.R
import pe.civika.app.device.AjustesApp
import pe.civika.app.feature.mapa.MapaBase
import pe.civika.app.ui.components.BarraSuperior
import pe.civika.app.ui.components.BotonPrincipal
import pe.civika.app.ui.components.BotonSecundario
import pe.civika.app.ui.components.CampoTexto
import pe.civika.app.ui.components.CampoUi
import pe.civika.app.ui.components.CargandoEsqueleto
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.encabezado
import pe.civika.app.ui.components.icono
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.mensajeDeCampo
import pe.civika.app.ui.components.textoDeMotivo
import pe.civika.app.ui.components.textoDias
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.Coordenada
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.PosibleDuplicado
import pe.civika.shared.domain.TipoEnvio
import pe.civika.shared.domain.Validaciones
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ReporteScreen(
    onCerrar: () -> Unit,
    onVerIncidente: (String) -> Unit,
    onVerMisReportes: () -> Unit,
    viewModel: ReporteViewModel = koinViewModel(),
    ajustes: AjustesApp = koinInject(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var camara by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }

    fun salir() {
        when {
            estado.paso == PasoReporte.ENVIADO -> onCerrar()
            viewModel.hayBorrador() -> confirmarSalida = true
            else -> {
                viewModel.descartarBorrador()
                onCerrar()
            }
        }
    }

    BackHandler(enabled = camara) { camara = false }
    BackHandler(enabled = !camara) { if (!viewModel.atras()) salir() }

    if (camara) {
        CamaraCaptura(
            onFoto = {
                viewModel.agregarFoto(it)
                camara = false
            },
            onCerrar = { camara = false },
        )
        return
    }

    Scaffold(
        topBar = { BarraSuperior(stringResource(R.string.reporte_titulo), onVolver = { if (!viewModel.atras()) salir() }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            Progreso(estado)
            Box(Modifier.weight(1f)) {
                when (estado.paso) {
                    PasoReporte.FOTO -> PasoFoto(estado, viewModel, onTomarFoto = { camara = true })
                    PasoReporte.UBICACION -> PasoUbicacion(estado, viewModel)
                    PasoReporte.CATEGORIA -> PasoCategoria(estado, viewModel)
                    PasoReporte.DUPLICADOS -> PasoDuplicados(estado, viewModel)
                    PasoReporte.ENVIADO -> PasoEnviado(estado, viewModel, ajustes, onVerIncidente, onVerMisReportes, onCerrar)
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text(stringResource(R.string.reporte_salir_titulo)) },
            text = { Text(stringResource(R.string.reporte_salir_texto)) },
            confirmButton = {
                CivikaTextButton(stringResource(R.string.reporte_salir_confirmar), {
                    confirmarSalida = false
                    viewModel.descartarBorrador()
                    onCerrar()
                })
            },
            dismissButton = { CivikaTextButton(stringResource(R.string.reporte_salir_seguir), { confirmarSalida = false }) },
        )
    }
}

@Composable
private fun nombreDelPaso(paso: PasoReporte): String = stringResource(
    when (paso) {
        PasoReporte.FOTO -> R.string.paso_foto
        PasoReporte.UBICACION -> R.string.paso_ubicacion
        PasoReporte.CATEGORIA -> R.string.paso_categoria
        PasoReporte.DUPLICADOS -> R.string.paso_duplicados
        PasoReporte.ENVIADO -> R.string.paso_enviado
    },
)

@Composable
private fun Progreso(estado: ReporteUiState) {
    val texto = stringResource(R.string.reporte_paso_de, estado.paso.ordinal + 1, PasoReporte.entries.size, nombreDelPaso(estado.paso))
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(texto, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.encabezado())
        LinearProgressIndicator(
            progress = { estado.progreso },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).semantics { contentDescription = texto },
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Cuerpo desplazable con el botón de acción fijo abajo. */
@Composable
private fun PasoMarco(accion: @Composable () -> Unit, contenido: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { contenido() }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accion()
        }
    }
}

// ---------------------------------------------------------------- Paso 1

@Composable
private fun PasoFoto(estado: ReporteUiState, viewModel: ReporteViewModel, onTomarFoto: () -> Unit) {
    val restantes = Validaciones.MAX_FOTOS - estado.fotos.size
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(Validaciones.MAX_FOTOS)) { uris: List<Uri> ->
        uris.take(restantes).forEach { viewModel.agregarFoto(it.toString()) }
    }
    PasoMarco(
        accion = {
            BotonPrincipal(stringResource(R.string.continuar), viewModel::siguiente, habilitado = estado.puedeContinuar)
        },
    ) {
        Text(stringResource(R.string.foto_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.encabezado())
        Text(stringResource(R.string.foto_texto, Validaciones.MAX_FOTOS), style = MaterialTheme.typography.bodyLarge)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium) {
            Text(stringResource(R.string.sin_emergencias), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            estado.fotos.forEachIndexed { indice, ruta ->
                MiniaturaFoto(ruta, indice + 1, onQuitar = { viewModel.quitarFoto(ruta) }, modifier = Modifier.weight(1f))
            }
            if (estado.procesandoFoto) {
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(MaterialTheme.shapes.medium).semantics { contentDescription = "" },
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            repeat((restantes - if (estado.procesandoFoto) 1 else 0).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
        if (estado.errorFoto) {
            Text(stringResource(R.string.foto_error), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        BotonPrincipal(
            texto = stringResource(R.string.foto_tomar),
            onClick = onTomarFoto,
            habilitado = estado.puedeAgregarFotos,
            icono = Icons.Rounded.AddAPhoto,
        )
        BotonSecundario(
            texto = stringResource(R.string.foto_galeria),
            onClick = { selector.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            habilitado = estado.puedeAgregarFotos,
            icono = Icons.Rounded.PhotoLibrary,
        )
    }
}

@Composable
private fun MiniaturaFoto(ruta: String, numero: Int, onQuitar: () -> Unit, modifier: Modifier) {
    Box(modifier.aspectRatio(1f)) {
        AsyncImage(
            model = File(ruta),
            contentDescription = stringResource(R.string.foto_cd, numero),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
        )
        IconButton(
            onClick = onQuitar,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.foto_quitar, numero),
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .size(28.dp)
                    // Fondo oscuro: la X blanca debe verse también sobre una foto clara.
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                    .padding(4.dp),
            )
        }
    }
}

// ---------------------------------------------------------------- Paso 2

private fun tienePermisoDeUbicacion(contexto: android.content.Context) =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
private fun PasoUbicacion(estado: ReporteUiState, viewModel: ReporteViewModel) {
    val contexto = LocalContext.current
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var usandoGps by remember { mutableStateOf(false) }
    var permisoNegado by remember { mutableStateOf(false) }
    val centro = remember { viewModel.centroInicial() }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) {
            usandoGps = true
            viewModel.ubicarme()
        } else {
            permisoNegado = true
        }
    }

    fun usarMiUbicacion() {
        permisoNegado = false
        if (tienePermisoDeUbicacion(contexto)) {
            usandoGps = true
            viewModel.ubicarme()
        } else {
            // El permiso se pide ahora, al tocar el botón, y no al abrir la app.
            pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Si ya hay permiso, se intenta ubicar al entrar al paso sin que el ciudadano tenga que pedirlo.
    LaunchedEffect(Unit) {
        if (estado.latitud == null && tienePermisoDeUbicacion(contexto)) {
            usandoGps = true
            viewModel.ubicarme()
        }
    }
    // Cuando llega la posición del GPS, el mapa se mueve hasta ella.
    LaunchedEffect(estado.buscandoUbicacion) {
        val m = mapa ?: return@LaunchedEffect
        if (!estado.buscandoUbicacion && usandoGps && estado.latitud != null && estado.longitud != null) {
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(estado.latitud, estado.longitud), 17.0))
            usandoGps = false
        }
    }

    PasoMarco(
        accion = {
            BotonSecundario(
                texto = stringResource(if (estado.buscandoUbicacion) R.string.ubicacion_buscando else R.string.ubicacion_usar_mia),
                onClick = ::usarMiUbicacion,
                habilitado = !estado.buscandoUbicacion,
                icono = Icons.Rounded.MyLocation,
            )
            BotonPrincipal(stringResource(R.string.continuar), viewModel::siguiente, habilitado = estado.puedeContinuar)
        },
    ) {
        Text(stringResource(R.string.ubicacion_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.encabezado())
        Text(stringResource(R.string.ubicacion_texto), style = MaterialTheme.typography.bodyLarge)
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(MaterialTheme.shapes.medium),
        ) {
            MapaBase(
                centro = centro,
                zoom = 17.0,
                modifier = Modifier.fillMaxSize(),
                descripcion = stringResource(R.string.ubicacion_mapa_cd),
                onMapaListo = { m ->
                    mapa = m
                    // El centro del mapa es el punto: se actualiza cada vez que el mapa deja de moverse.
                    m.addOnCameraIdleListener { m.cameraPosition.target?.let { viewModel.moverPunto(it.latitude, it.longitude) } }
                    m.cameraPosition.target?.let { viewModel.moverPunto(it.latitude, it.longitude) }
                },
            )
            Icon(
                Icons.Rounded.Place,
                contentDescription = stringResource(R.string.ubicacion_pin_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center).offset(y = (-24).dp).size(48.dp),
            )
        }
        if (estado.fueraDeZona) {
            Aviso(Icons.Rounded.Warning, stringResource(R.string.error_fuera_de_zona), MaterialTheme.colorScheme.error)
        } else if (estado.ubicacionValida) {
            Text(
                stringResource(R.string.ubicacion_dentro),
                style = MaterialTheme.typography.bodyMedium,
                color = CivikaTheme.colores.estado(pe.civika.shared.domain.Familia.RESUELTO).texto,
            )
        }
        if (permisoNegado) Aviso(Icons.Rounded.ErrorOutline, stringResource(R.string.mapa_permiso_ubicacion_negado), MaterialTheme.colorScheme.onSurface)
        if (estado.ubicacionNoDisponible) Aviso(Icons.Rounded.ErrorOutline, stringResource(R.string.mapa_ubicacion_no_disponible), MaterialTheme.colorScheme.onSurface)
        if (estado.ubicacionSimulada) Aviso(Icons.Rounded.Warning, stringResource(R.string.ubicacion_simulada), MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Aviso(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, color: androidx.compose.ui.graphics.Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

// ---------------------------------------------------------------- Paso 3

@Composable
private fun PasoCategoria(estado: ReporteUiState, viewModel: ReporteViewModel) {
    PasoMarco(
        accion = { BotonPrincipal(stringResource(R.string.continuar), viewModel::siguiente) },
    ) {
        Text(stringResource(R.string.categoria_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.encabezado())
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.categorias.forEach { categoria ->
                val seleccionada = estado.categoriaId == categoria.id
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (seleccionada) 2.dp else 1.dp, if (seleccionada) MaterialTheme.colorScheme.secondary else CivikaTheme.colores.borde),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = { viewModel.cambiarCategoria(categoria.id) })
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(categoria.icono(), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(categoria.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        RadioButton(selected = seleccionada, onClick = null)
                    }
                }
            }
            if (estado.categorias.isEmpty()) CargandoEsqueleto(filas = 2)
        }
        if (estado.intentoCategoria && estado.categoriaId == null) {
            Text(stringResource(R.string.categoria_elegir), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        CampoTexto(
            valor = estado.descripcion,
            onCambio = viewModel::cambiarDescripcion,
            etiqueta = stringResource(R.string.descripcion),
            ayuda = stringResource(R.string.descripcion_ayuda, estado.descripcion.length, Validaciones.DESCRIPCION_MAX),
            error = if (estado.intentoCategoria) estado.validacionDescripcion.mensajeDeCampo(CampoUi.DESCRIPCION) else null,
            minLineas = 4,
        )
    }
}

// ---------------------------------------------------------------- Paso 4

@Composable
private fun PasoDuplicados(estado: ReporteUiState, viewModel: ReporteViewModel) {
    if (estado.buscandoDuplicados || (estado.duplicados.isEmpty() && estado.enviando)) {
        Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
            Text(
                stringResource(R.string.duplicados_buscando),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp).encabezado(),
            )
            CargandoEsqueleto(filas = 2)
        }
        return
    }
    PasoMarco(
        accion = {
            BotonSecundario(stringResource(R.string.duplicados_otro), viewModel::enviarComoNuevo, habilitado = !estado.enviando)
        },
    ) {
        Text(stringResource(R.string.duplicados_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.encabezado())
        Text(stringResource(R.string.duplicados_texto), style = MaterialTheme.typography.bodyLarge)
        estado.duplicados.forEach { duplicado ->
            TarjetaDuplicado(duplicado, estado.categoria?.nombre, habilitado = !estado.enviando, onApoyar = { viewModel.apoyar(duplicado) })
        }
        estado.error?.let { Text(it.mensaje(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun TarjetaDuplicado(duplicado: PosibleDuplicado, categoria: String?, habilitado: Boolean, onApoyar: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CivikaTheme.colores.borde),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(duplicado.codigo, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.duplicados_meta,
                    categoria.orEmpty(),
                    duplicado.distanciaM.roundToInt(),
                    pluralStringResource(R.plurals.apoyos_n, duplicado.apoyos, duplicado.apoyos),
                    textoDias(duplicado.diasAbierto),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotonPrincipal(stringResource(R.string.duplicados_apoyar), onApoyar, habilitado = habilitado)
        }
    }
}

// ---------------------------------------------------------------- Paso 5

@Composable
private fun PasoEnviado(
    estado: ReporteUiState,
    viewModel: ReporteViewModel,
    ajustes: AjustesApp,
    onVerIncidente: (String) -> Unit,
    onVerMisReportes: () -> Unit,
    onCerrar: () -> Unit,
) {
    val envio = estado.envio
    val enviado = envio?.estado == EstadoEnvio.ENVIADO
    val conError = envio?.estado == EstadoEnvio.ERROR
    val esApoyo = envio?.tipo == TipoEnvio.APOYO

    PasoMarco(
        accion = {
            if (enviado && envio?.incidenteCreadoId != null) {
                BotonPrincipal(stringResource(R.string.enviado_ver_reporte), { onVerIncidente(envio.incidenteCreadoId!!) })
            } else {
                BotonPrincipal(stringResource(R.string.enviado_ver_mis_reportes), onVerMisReportes)
            }
            BotonSecundario(stringResource(R.string.enviado_cerrar), onCerrar)
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                enviado -> Icon(Icons.Rounded.CheckCircle, null, tint = CivikaTheme.colores.estado(pe.civika.shared.domain.Familia.RESUELTO).texto, modifier = Modifier.size(64.dp))
                conError -> Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                envio?.estado == EstadoEnvio.ENVIANDO -> CircularProgressIndicator(Modifier.size(56.dp))
                else -> Icon(Icons.Rounded.CloudQueue, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(64.dp))
            }
            Text(
                text = stringResource(
                    when {
                        enviado && esApoyo -> R.string.enviado_titulo_apoyo
                        enviado -> R.string.enviado_titulo
                        conError -> R.string.enviado_titulo_error
                        envio?.estado == EstadoEnvio.ENVIANDO -> R.string.enviado_titulo_enviando
                        else -> R.string.enviado_titulo_pendiente
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.encabezado(),
            )
            if (enviado) {
                envio?.codigo?.let { codigo ->
                    Text(stringResource(R.string.enviado_codigo), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(codigo, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(
                    text = envio?.entidad?.let { stringResource(R.string.enviado_entidad, it) } ?: stringResource(R.string.enviado_sin_entidad),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else if (conError) {
                Text(textoDeMotivo(envio?.motivoError, envio?.detalleError), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                BotonSecundario(stringResource(R.string.reintentar), viewModel::reintentarEnvio)
            } else {
                Text(stringResource(R.string.enviado_pendiente_texto), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
        AvisoDePermisoDeNotificaciones(ajustes)
    }
}

/** Después del primer reporte se explica para qué sirven las notificaciones y se pide el permiso (Android 13+). */
@android.annotation.SuppressLint("InlinedApi") // solo se usa en Android 13+: lo comprueba hacenFalta
@Composable
private fun AvisoDePermisoDeNotificaciones(ajustes: AjustesApp) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val preguntadas by ajustes.notificacionesPreguntadas.collectAsStateWithLifecycle(initialValue = true)
    val hacenFalta = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        alcance.launch { ajustes.marcarNotificacionesPreguntadas() }
    }
    if (!hacenFalta || preguntadas) return

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, CivikaTheme.colores.borde),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.avisos_titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.encabezado())
            Text(stringResource(R.string.avisos_texto), style = MaterialTheme.typography.bodyMedium)
            BotonPrincipal(stringResource(R.string.avisos_activar), { lanzador.launch(Manifest.permission.POST_NOTIFICATIONS) })
            CivikaTextButton(stringResource(R.string.avisos_ahora_no), { alcance.launch { ajustes.marcarNotificacionesPreguntadas() } })
        }
    }
}
