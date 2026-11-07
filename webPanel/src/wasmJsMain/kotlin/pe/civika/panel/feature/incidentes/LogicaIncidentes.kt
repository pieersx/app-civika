package pe.civika.panel.feature.incidentes

import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.Rol
import pe.civika.shared.domain.TransicionesEstado

/**
 * Reglas de la pantalla de incidentes, separadas de la interfaz para poder probarlas
 * en `wasmJsTest` sin crear ViewModels (ADR-15: en Wasm no hay koin-test ni Turbine todavía).
 */

enum class FiltroIncidentes(val etiqueta: String) {
    TODOS("Todos"),
    ABIERTOS("Abiertos"),
    EN_ATENCION("En atención"),
    RESUELTOS("Resueltos"),
    ;

    fun incluye(incidente: Incidente) = when (this) {
        TODOS -> true
        ABIERTOS -> incidente.estado.familia == Familia.ABIERTO
        EN_ATENCION -> incidente.estado.familia == Familia.EN_ATENCION
        RESUELTOS -> incidente.estado.familia == Familia.RESUELTO
    }
}

/**
 * Filtro + búsqueda + orden por prioridad (RF-09). La búsqueda cubre código, título y zona,
 * que es la dirección de referencia que hoy trae el repositorio.
 */
fun filtrarIncidentes(
    incidentes: List<Incidente>,
    filtro: FiltroIncidentes,
    busqueda: String,
): List<Incidente> {
    val texto = busqueda.trim().lowercase()
    return incidentes
        .filter { filtro.incluye(it) }
        .filter {
            texto.isEmpty() ||
                it.codigo.lowercase().contains(texto) ||
                it.titulo.lowercase().contains(texto) ||
                it.zona.lowercase().contains(texto)
        }
        .sortedByDescending { it.prioridad }
}

/** Transiciones que el rol de la sesión puede hacer desde el estado actual (arquitectura §5). */
fun accionesDisponibles(estado: Estado, rol: Rol): List<Estado> =
    TransicionesEstado.siguientes(estado, rol).sortedBy { it.ordinal }

/**
 * Cambio de estado en la lista en memoria: el panel lo aplica al instante y, cuando exista el
 * repositorio real (Sprint 2), la RPC `cambiar_estado` valida lo mismo en el servidor.
 * Devuelve la misma lista si la transición no está permitida para el rol.
 */
fun aplicarCambioEstado(
    incidentes: List<Incidente>,
    codigo: String,
    destino: Estado,
    rol: Rol,
): List<Incidente> {
    val actual = incidentes.firstOrNull { it.codigo == codigo } ?: return incidentes
    if (!TransicionesEstado.permitida(actual.estado, destino, rol)) return incidentes
    return incidentes.map { if (it.codigo == codigo) it.copy(estado = destino) else it }
}

/**
 * Prioridad numérica → etiqueta del mockup (Alta / Media / Baja).
 * Los umbrales son de presentación: la prioridad sigue siendo el número de RF-09.
 */
fun etiquetaPrioridad(prioridad: Int) = when {
    prioridad >= UMBRAL_ALTA -> "Alta"
    prioridad >= UMBRAL_MEDIA -> "Media"
    else -> "Baja"
}

private const val UMBRAL_ALTA = 50
private const val UMBRAL_MEDIA = 30

fun antiguedadTexto(diasAbierto: Int) = when {
    diasAbierto <= 0 -> "Hoy"
    diasAbierto == 1 -> "1 día"
    else -> "$diasAbierto días"
}
