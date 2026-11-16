package pe.civika.app.feature.mapa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import pe.civika.app.R
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.Coordenada

/** Estilos vectoriales de OpenFreeMap (ADR-3): gratis y sin API key. */
object EstiloMapa {
    const val CLARO = "https://tiles.openfreemap.org/styles/liberty"
    const val OSCURO = "https://tiles.openfreemap.org/styles/dark"
}

/**
 * MapLibre Native dentro de Compose. Maneja el ciclo de vida del `MapView`, cambia el estilo con el tema y avisa
 * cuando el estilo está listo para añadirle fuentes y capas. Cada vez que el estilo se recarga (por ejemplo al
 * cambiar de claro a oscuro) se vuelve a llamar a [onEstiloListo], porque un estilo nuevo no trae las capas anteriores.
 */
@Composable
fun MapaBase(
    centro: Coordenada,
    zoom: Double,
    modifier: Modifier = Modifier,
    gestos: Boolean = true,
    descripcion: String? = null,
    onMapaListo: (MapLibreMap) -> Unit = {},
    onEstiloListo: (MapLibreMap, Style) -> Unit = { _, _ -> },
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val oscuro = CivikaTheme.colores.esOscuro
    val vista = remember { MapView(contexto) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    val alMapaListo by rememberUpdatedState(onMapaListo)
    val alEstiloListo by rememberUpdatedState(onEstiloListo)

    DisposableEffect(ciclo, vista) {
        var destruido = false
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_CREATE -> vista.onCreate(null)
                Lifecycle.Event.ON_START -> vista.onStart()
                Lifecycle.Event.ON_RESUME -> vista.onResume()
                Lifecycle.Event.ON_PAUSE -> vista.onPause()
                Lifecycle.Event.ON_STOP -> vista.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    destruido = true
                    vista.onDestroy()
                }
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            // La pantalla se fue pero la Activity sigue viva: hay que cerrar el mapa a mano.
            if (!destruido) {
                vista.onPause()
                vista.onStop()
                vista.onDestroy()
            }
        }
    }

    LaunchedEffect(vista) {
        vista.getMapAsync { m ->
            m.uiSettings.isLogoEnabled = false
            // La atribución va siempre visible como texto de Compose, no escondida detrás de un botón (i).
            m.uiSettings.isAttributionEnabled = false
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isCompassEnabled = false
            m.cameraPosition = CameraPosition.Builder().target(LatLng(centro.latitud, centro.longitud)).zoom(zoom).build()
            mapa = m
            alMapaListo(m)
        }
    }

    LaunchedEffect(mapa, gestos) {
        mapa?.uiSettings?.apply {
            isScrollGesturesEnabled = gestos
            isZoomGesturesEnabled = gestos
            isDoubleTapGesturesEnabled = gestos
            isQuickZoomGesturesEnabled = gestos
        }
    }

    LaunchedEffect(mapa, oscuro) {
        val m = mapa ?: return@LaunchedEffect
        m.setStyle(Style.Builder().fromUri(if (oscuro) EstiloMapa.OSCURO else EstiloMapa.CLARO)) { estilo ->
            alEstiloListo(m, estilo)
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { vista },
            modifier = Modifier
                .fillMaxSize()
                .semantics { if (descripcion != null) contentDescription = descripcion },
        )
        AtribucionMapa(Modifier.align(Alignment.BottomStart).padding(8.dp))
    }
}

/** Atribución que piden OpenFreeMap y OpenMapTiles (ADR-3). Siempre visible sobre el mapa. */
@Composable
fun AtribucionMapa(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    ) {
        Text(
            text = stringResource(R.string.mapa_atribucion),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
