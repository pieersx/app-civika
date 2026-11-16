package pe.civika.shared.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Coordenada(val latitud: Double, val longitud: Double)

/**
 * Zona piloto (ADR-12): Ciudad Universitaria de la UNMSM y sus alrededores. Es el mismo rectángulo del seed
 * (`supabase/seed.sql`), para avisar al ciudadano **antes** de enviar. Quien decide de verdad es `crear_reporte`
 * con `ST_Covers`: si el seed cambia, el servidor sigue teniendo la razón.
 */
object ZonaPiloto {
    const val LAT_SUR = -12.0680
    const val LAT_NORTE = -12.0470
    const val LON_OESTE = -77.0980
    const val LON_ESTE = -77.0710

    val centro = Coordenada(latitud = -12.0575, longitud = -77.0845)

    fun contiene(latitud: Double, longitud: Double): Boolean =
        latitud in LAT_SUR..LAT_NORTE && longitud in LON_OESTE..LON_ESTE
}

/** Distancia en metros entre dos puntos (haversine). Sirve para ordenar «cerca de ti», no para medir duplicados. */
fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = (lat2 - lat1).aRadianes()
    val dLon = (lon2 - lon1).aRadianes()
    val a = sin(dLat / 2).pow(2) + cos(lat1.aRadianes()) * cos(lat2.aRadianes()) * sin(dLon / 2).pow(2)
    return 2 * RADIO_TIERRA_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

private const val RADIO_TIERRA_M = 6_371_000.0

private fun Double.aRadianes() = this * PI / 180
