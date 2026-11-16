package pe.civika.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Geocodificación inversa con la instancia pública de Nominatim (ADR-3): una sola consulta por reporte confirmado,
 * con User-Agent propio. Se pide a nivel de calle (`zoom=17`), sin número de puerta. Si falla, el reporte se envía
 * sin dirección.
 */
class NominatimGeocodificador(
    private val http: HttpClient = HttpClient(),
    private val urlBase: String = "https://nominatim.openstreetmap.org",
) : Geocodificador {

    override suspend fun direccionAproximada(latitud: Double, longitud: Double): String? = try {
        withTimeoutOrNull(TIEMPO_MAXIMO_MS) {
            val respuesta = http.get("$urlBase/reverse") {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.AcceptLanguage, "es")
                parameter("format", "jsonv2")
                parameter("lat", latitud)
                parameter("lon", longitud)
                parameter("zoom", 17)
                parameter("addressdetails", 1)
            }
            if (respuesta.status.isSuccess()) direccionDesdeNominatim(respuesta.bodyAsText()) else null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        const val USER_AGENT = "Civika/0.1 (app de reporte ciudadano; piloto UNMSM)"
        private const val TIEMPO_MAXIMO_MS = 6_000L
    }
}

/** «Avenida Venezuela, Cercado de Lima» a partir de la respuesta `jsonv2` de Nominatim. `null` si no hay datos. */
internal fun direccionDesdeNominatim(json: String): String? {
    val raiz = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return null
    val direccion = raiz["address"] as? JsonObject
    fun campo(vararg claves: String): String? =
        claves.firstNotNullOfOrNull { clave ->
            runCatching { direccion?.get(clave)?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotBlank() }
        }

    val via = campo("road", "pedestrian", "footway", "path", "amenity")
    val barrio = campo("neighbourhood", "suburb", "city_district", "town", "city")
    val partes = listOfNotNull(via, barrio).distinct()
    if (partes.isNotEmpty()) return partes.joinToString(", ").take(MAX_DIRECCION)

    val nombre = runCatching { raiz["display_name"]?.jsonPrimitive?.contentOrNull }.getOrNull()
    return nombre?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(2)?.joinToString(", ")
        ?.take(MAX_DIRECCION)?.takeIf { it.isNotBlank() }
}

private const val MAX_DIRECCION = 120
