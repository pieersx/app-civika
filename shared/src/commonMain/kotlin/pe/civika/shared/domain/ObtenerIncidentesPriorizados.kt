package pe.civika.shared.domain

import pe.civika.shared.data.IncidenteRepository

/**
 * Caso de uso común a la app y al panel: incidentes que todavía hay que atender, primero los más urgentes.
 * Los resueltos y los rechazados no se priorizan (sus días abiertos seguirían sumando sin sentido).
 */
class ObtenerIncidentesPriorizados(private val repositorio: IncidenteRepository) {

    suspend operator fun invoke(): List<Incidente> =
        repositorio.listar()
            .filter { it.estado.familia in FAMILIAS_POR_ATENDER }
            .sortedByDescending { it.prioridad }

    private companion object {
        val FAMILIAS_POR_ATENDER = setOf(Familia.ABIERTO, Familia.EN_ATENCION)
    }
}
