package pe.civika.shared.domain

import pe.civika.shared.data.IncidenteRepository

/**
 * Caso de uso: incidentes que todavía hay que atender, primero los más urgentes.
 * Los resueltos y los rechazados no se priorizan (sus días abiertos seguirían sumando sin sentido).
 */
class ObtenerIncidentesPriorizados(private val repositorio: IncidenteRepository) {

    suspend operator fun invoke(): List<Incidente> = priorizar(repositorio.listar())
}

/** Orden de la lista: por la fórmula de prioridad de `shared/domain`, nunca por el campo del servidor. */
fun priorizar(incidentes: List<Incidente>): List<Incidente> =
    incidentes
        .filter { it.estado.familia in FAMILIAS_POR_ATENDER }
        .sortedByDescending { it.prioridad }

/** Los más cercanos a un punto, del más próximo al más lejano. Los que no tienen coordenadas van al final. */
fun masCercanos(incidentes: List<Incidente>, latitud: Double, longitud: Double): List<Incidente> =
    incidentes
        .filter { it.estado.familia in FAMILIAS_POR_ATENDER }
        .sortedBy { incidente ->
            val lat = incidente.latitud
            val lon = incidente.longitud
            if (lat == null || lon == null) Double.MAX_VALUE else distanciaMetros(latitud, longitud, lat, lon)
        }
