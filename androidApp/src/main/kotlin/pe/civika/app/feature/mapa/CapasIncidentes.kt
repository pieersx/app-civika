package pe.civika.app.feature.mapa

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import pe.civika.app.ui.theme.CivikaColores
import pe.civika.shared.domain.Coordenada
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente

/** Fuentes y capas de MapLibre para pintar incidentes: marcadores con forma de señal y agrupación de puntos. */
object CapasIncidentes {
    const val FUENTE = "incidentes"
    const val CAPA_MARCADORES = "marcadores"
    const val CAPA_GRUPOS = "grupos"
    const val CAPA_CONTEO = "grupos-conteo"
    const val FUENTE_YO = "yo"
    const val CAPA_YO = "yo-punto"

    fun instalar(estilo: Style, colores: CivikaColores, densidad: Density, agrupar: Boolean) {
        Familia.entries.forEach { familia ->
            CATEGORIAS_CON_ICONO.forEach { categoria ->
                estilo.addImage(idDeMarcador(familia, categoria), crearMarcador(familia, categoria, colores, densidad))
            }
        }

        val opciones = GeoJsonOptions().apply {
            // Con cientos de puntos (500 en la prueba de carga) se agrupan hasta acercarse bastante.
            if (agrupar) withCluster(true).withClusterMaxZoom(16).withClusterRadius(50)
        }
        estilo.addSource(GeoJsonSource(FUENTE, FeatureCollection.fromFeatures(emptyList<Feature>()), opciones))

        val colorGrupo = colores.verdeAndino.toArgb()
        val colorTextoGrupo = if (colores.esOscuro) Color(0xFF0F2A1D).toArgb() else Color.White.toArgb()
        if (agrupar) {
            estilo.addLayer(
                CircleLayer(CAPA_GRUPOS, FUENTE)
                    .withFilter(Expression.has("point_count"))
                    .withProperties(
                        PropertyFactory.circleColor(colorGrupo),
                        PropertyFactory.circleRadius(
                            Expression.step(
                                Expression.get("point_count"),
                                Expression.literal(18),
                                Expression.stop(10, 22),
                                Expression.stop(50, 28),
                            ),
                        ),
                        PropertyFactory.circleStrokeColor(colorTextoGrupo),
                        PropertyFactory.circleStrokeWidth(2f),
                    ),
            )
            estilo.addLayer(
                SymbolLayer(CAPA_CONTEO, FUENTE)
                    .withFilter(Expression.has("point_count"))
                    .withProperties(
                        PropertyFactory.textField(Expression.get("point_count_abbreviated")),
                        PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                        PropertyFactory.textSize(14f),
                        PropertyFactory.textColor(colorTextoGrupo),
                        PropertyFactory.textAllowOverlap(true),
                        PropertyFactory.textIgnorePlacement(true),
                    ),
            )
        }
        estilo.addLayer(
            SymbolLayer(CAPA_MARCADORES, FUENTE)
                .withFilter(Expression.not(Expression.has("point_count")))
                .withProperties(
                    PropertyFactory.iconImage(
                        Expression.concat(Expression.literal("m-"), Expression.get("familia"), Expression.literal("-"), Expression.get("cat")),
                    ),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                ),
        )

        // Punto de «mi ubicación».
        estilo.addSource(GeoJsonSource(FUENTE_YO, FeatureCollection.fromFeatures(emptyList<Feature>())))
        estilo.addLayer(
            CircleLayer(CAPA_YO, FUENTE_YO).withProperties(
                PropertyFactory.circleColor(colores.verdeAndino.toArgb()),
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                PropertyFactory.circleStrokeWidth(3f),
            ),
        )
    }

    fun mostrar(estilo: Style, incidentes: List<Incidente>) {
        val caracteristicas = incidentes.mapNotNull { incidente ->
            val lat = incidente.latitud ?: return@mapNotNull null
            val lon = incidente.longitud ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addStringProperty("id", incidente.id)
                addStringProperty("familia", incidente.estado.familia.name)
                addStringProperty("cat", categoriaDeMarcador(incidente.categoria.serviceCode))
            }
        }
        estilo.getSourceAs<GeoJsonSource>(FUENTE)?.setGeoJson(FeatureCollection.fromFeatures(caracteristicas))
    }

    fun mostrarYo(estilo: Style, ubicacion: Coordenada?) {
        val caracteristicas = ubicacion
            ?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.longitud, it.latitud))) }
            .orEmpty()
        estilo.getSourceAs<GeoJsonSource>(FUENTE_YO)?.setGeoJson(FeatureCollection.fromFeatures(caracteristicas))
    }
}
