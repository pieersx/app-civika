package pe.civika.shared.domain

import kotlinx.coroutines.CancellationException
import pe.civika.shared.data.Geocodificador
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.data.ReporteRepository

sealed interface ResultadoSync {
    data class Enviado(val creado: ReporteCreado, val direccion: String?) : ResultadoSync

    /** Falló por algo pasajero (sin red, sin sesión): la cola lo intenta de nuevo más tarde. */
    data class Reintentar(val motivo: MotivoError) : ResultadoSync

    /** El servidor lo rechazó: no se reintenta solo y se le explica al ciudadano. */
    data class Definitivo(val motivo: MotivoError, val detalle: String?) : ResultadoSync
}

/**
 * Envía un reporte de la cola (ADR-6): sube las fotos, pide la dirección aproximada (una sola consulta a
 * Nominatim por reporte, ADR-3) y llama a `crear_reporte` o a `apoyar_incidente`.
 *
 * Es seguro repetirlo: las fotos tienen un nombre fijo por `clienteId` y las RPC son idempotentes.
 */
class SincronizarReporte(
    private val reportes: ReporteRepository,
    private val incidentes: IncidenteRepository,
    private val geocodificador: Geocodificador,
) {
    /** @param leerFoto devuelve los bytes del JPEG local, o `null` si el archivo ya no existe. */
    suspend operator fun invoke(pendiente: ReportePendiente, leerFoto: suspend (String) -> ByteArray?): ResultadoSync {
        return try {
            val rutas = pendiente.fotos.mapIndexed { indice, rutaLocal ->
                val bytes = leerFoto(rutaLocal)
                    ?: return ResultadoSync.Definitivo(MotivoError.FOTO_PERDIDA, null)
                reportes.subirFoto(pendiente.clienteId, indice, bytes)
            }
            when (pendiente.tipo) {
                TipoEnvio.NUEVO -> {
                    val direccion = pendiente.direccion ?: direccionSinFallar(pendiente)
                    val creado = reportes.crear(
                        ReporteNuevo(
                            clienteId = pendiente.clienteId,
                            latitud = pendiente.latitud,
                            longitud = pendiente.longitud,
                            categoriaId = pendiente.categoriaId,
                            descripcion = pendiente.descripcion,
                            fotos = rutas,
                            ubicacionSimulada = pendiente.ubicacionSimulada,
                            direccion = direccion,
                        ),
                    )
                    ResultadoSync.Enviado(creado, direccion)
                }
                TipoEnvio.APOYO -> {
                    val incidenteId = pendiente.incidenteId
                        ?: return ResultadoSync.Definitivo(MotivoError.DATOS, null)
                    ResultadoSync.Enviado(incidentes.apoyar(pendiente.clienteId, incidenteId, rutas), pendiente.direccion)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorCivika) {
            if (e.esDefinitivo) {
                ResultadoSync.Definitivo(e.aMotivo(), (e as? ErrorCivika.DatosInvalidos)?.detalle)
            } else {
                ResultadoSync.Reintentar(e.aMotivo())
            }
        } catch (e: Exception) {
            ResultadoSync.Reintentar(MotivoError.DESCONOCIDO)
        }
    }

    private suspend fun direccionSinFallar(pendiente: ReportePendiente): String? = try {
        geocodificador.direccionAproximada(pendiente.latitud, pendiente.longitud)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null // la dirección es un extra: sin ella el reporte se envía igual
    }
}
