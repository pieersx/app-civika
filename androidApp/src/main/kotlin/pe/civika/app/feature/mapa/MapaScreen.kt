package pe.civika.app.feature.mapa

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import pe.civika.app.R
import pe.civika.app.device.UbicacionProvider
import pe.civika.app.ui.components.BotonPrincipal
import pe.civika.app.ui.components.CargandoEsqueleto
import pe.civika.app.ui.components.ChipFiltro
import pe.civika.app.ui.components.EstadoError
import pe.civika.app.ui.components.EstadoVacio
import pe.civika.app.ui.components.SenalEstado
import pe.civika.app.ui.components.TarjetaIncidente
import pe.civika.app.ui.components.glifo
import pe.civika.app.ui.components.icono
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.textoDias
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.Coordenada
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.ZonaPiloto

/** Pide al mapa que se centre en un punto. [id] distingue dos pedidos al mismo punto. */
data class Centrado(val punto: Coordenada, val id: Int)

private val FAMILIAS_FILTRABLES = listOf(Familia.ABIERTO, Familia.EN_ATENCION, Familia.RESUELTO)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onAbrirIncidente: (String) -> Unit,
    viewModel: MapaViewModel = koinViewModel(),
    ubicacion: UbicacionProvider = koinInject(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val avisos = remember { SnackbarHostState() }
    var miUbicacion by remember { mutableStateOf<Coordenada?>(null) }
    var centrado by remember { mutableStateOf<Centrado?>(null) }
    val mensajeNegado = stringResource(R.string.mapa_permiso_ubicacion_negado)
    val mensajeNoDisponible = stringResource(R.string.mapa_ubicacion_no_disponible)

    fun buscarUbicacion() {
        alcance.launch {
            val u = ubicacion.ubicacionActual()
            if (u == null) {
                avisos.showSnackbar(mensajeNoDisponible)
            } else {
                val punto = Coordenada(u.latitud, u.longitud)
                miUbicacion = punto
                centrado = Centrado(punto, (centrado?.id ?: 0) + 1)
            }
        }
    }

    // El permiso se pide en el momento de tocar «mi ubicación», no al abrir la app.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) buscarUbicacion() else alcance.launch { avisos.showSnackbar(mensajeNegado) }
    }

    fun alTocarMiUbicacion() {
        val concedido = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
        }
        if (concedido) buscarUbicacion() else pedirPermiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Box(Modifier.fillMaxSize()) {
        when {
            estado.cargando && estado.todos.isEmpty() -> CargandoEsqueleto(Modifier.statusBarsPadding().padding(top = 64.dp))
            estado.error != null && estado.todos.isEmpty() -> EstadoError(estado.error!!.mensaje(), viewModel::cargar)
            estado.verComoLista -> ListaDelMapa(estado.visibles, onAbrirIncidente, viewModel::limpiarFiltros)
            else -> MapaIncidentes(
                incidentes = estado.visibles,
                miUbicacion = miUbicacion,
                centrar = centrado,
                onIncidenteTocado = viewModel::seleccionar,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(Modifier.statusBarsPadding().align(Alignment.TopStart)) {
            FiltrosMapa(estado, viewModel)
        }

        if (!estado.verComoLista) {
            FloatingActionButton(
                onClick = ::alTocarMiUbicacion,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.secondary,
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.mapa_mi_ubicacion))
            }
        }

        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp))
    }

    estado.seleccionado?.let { seleccionado ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.seleccionar(null) },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ResumenIncidente(seleccionado) {
                viewModel.seleccionar(null)
                onAbrirIncidente(seleccionado.id)
            }
        }
    }
}

@Composable
private fun FiltrosMapa(estado: MapaUiState, viewModel: MapaViewModel) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.94f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FAMILIAS_FILTRABLES) { familia ->
                    ChipFiltro(
                        texto = etiquetaFamilia(familia),
                        seleccionado = familia in estado.filtro.familias,
                        onClick = { viewModel.alternarFamilia(familia) },
                        icono = familia.glifo(),
                    )
                }
                items(estado.categorias, key = { it.serviceCode }) { categoria ->
                    ChipFiltro(
                        texto = categoria.nombre,
                        seleccionado = categoria.serviceCode in estado.filtro.categorias,
                        onClick = { viewModel.alternarCategoria(categoria.serviceCode) },
                        icono = categoria.icono(),
                    )
                }
                if (estado.filtro.activo) {
                    item {
                        ChipFiltro(
                            texto = stringResource(R.string.mapa_quitar_filtros),
                            seleccionado = false,
                            onClick = viewModel::limpiarFiltros,
                            icono = Icons.Rounded.FilterAltOff,
                        )
                    }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp)) {
                // Alternativa accesible al mapa: los mismos incidentes como lista, que TalkBack sí recorre.
                FilledTonalButton(onClick = viewModel::alternarLista, shape = MaterialTheme.shapes.small) {
                    Icon(
                        if (estado.verComoLista) Icons.Rounded.Map else Icons.Rounded.FormatListBulleted,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        stringResource(if (estado.verComoLista) R.string.mapa_ver_mapa else R.string.mapa_ver_lista),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun etiquetaFamilia(familia: Familia): String = stringResource(
    when (familia) {
        Familia.ABIERTO -> R.string.familia_abierto
        Familia.EN_ATENCION -> R.string.familia_en_atencion
        Familia.RESUELTO -> R.string.familia_resuelto
        Familia.RECHAZADO -> R.string.familia_rechazado
    },
)

@Composable
private fun ListaDelMapa(incidentes: List<Incidente>, onAbrir: (String) -> Unit, onQuitarFiltros: () -> Unit) {
    if (incidentes.isEmpty()) {
        EstadoVacio(
            icono = Icons.Rounded.FilterAltOff,
            titulo = stringResource(R.string.mapa_sin_resultados_titulo),
            texto = stringResource(R.string.mapa_sin_resultados_texto),
            accion = stringResource(R.string.mapa_quitar_filtros),
            onAccion = onQuitarFiltros,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Deja libre el espacio de los filtros, que van encima.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 148.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(incidentes, key = { it.id }) { incidente ->
            TarjetaIncidente(incidente, onClick = { onAbrir(incidente.id) })
        }
    }
}

@Composable
private fun ResumenIncidente(incidente: Incidente, onVerDetalle: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SenalEstado(incidente.estado)
        Text(incidente.titulo, style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.tarjeta_meta_corta, incidente.categoria.nombre, textoDias(incidente.diasAbierto)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BotonPrincipal(stringResource(R.string.mapa_ver_detalle), onVerDetalle)
    }
}

/** El mapa con los marcadores. Tocar un marcador abre el resumen; tocar un grupo acerca el mapa. */
@Composable
fun MapaIncidentes(
    incidentes: List<Incidente>,
    miUbicacion: Coordenada?,
    centrar: Centrado?,
    onIncidenteTocado: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = CivikaTheme.colores
    val densidad = LocalDensity.current
    var estilo by remember { mutableStateOf<Style?>(null) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    val alTocar by rememberUpdatedState(onIncidenteTocado)

    MapaBase(
        centro = ZonaPiloto.centro,
        zoom = 14.5,
        modifier = modifier,
        descripcion = stringResource(R.string.mapa_descripcion),
        onMapaListo = { m ->
            mapa = m
            m.addOnMapClickListener { puntoTocado ->
                // El área de toque es de 48 dp aunque el marcador mida menos (RNF-07).
                val centro = m.projection.toScreenLocation(puntoTocado)
                val radio = 24f * densidad.density
                val zona = RectF(centro.x - radio, centro.y - radio, centro.x + radio, centro.y + radio)
                val marcador = m.queryRenderedFeatures(zona, CapasIncidentes.CAPA_MARCADORES).firstOrNull()
                val id = marcador?.getStringProperty("id")
                when {
                    id != null -> {
                        alTocar(id)
                        true
                    }
                    m.queryRenderedFeatures(zona, CapasIncidentes.CAPA_GRUPOS).isNotEmpty() -> {
                        m.animateCamera(CameraUpdateFactory.newLatLngZoom(puntoTocado, m.cameraPosition.zoom + 2.0))
                        true
                    }
                    else -> false
                }
            }
        },
        onEstiloListo = { _, nuevo ->
            CapasIncidentes.instalar(nuevo, colores, densidad, agrupar = true)
            estilo = nuevo
        },
    )
    LaunchedEffect(estilo, incidentes) { estilo?.let { CapasIncidentes.mostrar(it, incidentes) } }
    LaunchedEffect(estilo, miUbicacion) { estilo?.let { CapasIncidentes.mostrarYo(it, miUbicacion) } }
    LaunchedEffect(mapa, centrar) {
        val m = mapa ?: return@LaunchedEffect
        centrar?.let { m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.punto.latitud, it.punto.longitud), 16.0)) }
    }
}
