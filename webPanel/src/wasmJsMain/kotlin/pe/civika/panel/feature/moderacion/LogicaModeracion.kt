package pe.civika.panel.feature.moderacion

import pe.civika.shared.domain.Denuncia
import pe.civika.shared.domain.Rol

/**
 * Reglas de la cola de moderación (RF-16), separadas de la interfaz para poder probarlas en
 * `wasmJsTest` sin crear ViewModels (ADR-15: en Wasm no hay koin-test ni Turbine todavía).
 */

/** Acciones de cada tarjeta. La principal cambia según el motivo de la denuncia. */
enum class AccionModeracion {
    PRINCIPAL,
    DUPLICADO,
    RECHAZAR,
    ;

    fun etiqueta(denuncia: Denuncia) = when (this) {
        PRINCIPAL -> denuncia.motivo.accionPrincipal
        DUPLICADO -> "Marcar como duplicado"
        RECHAZAR -> "Rechazar el reporte"
    }
}

/** Solo la moderación resuelve denuncias: la entidad puede mirar la cola, no tocarla. */
fun puedeModerar(rol: Rol) = rol == Rol.MODERADOR

/** Quita la denuncia resuelta de la cola. Devuelve la misma lista si el id no está. */
fun quitarDenuncia(denuncias: List<Denuncia>, id: String): List<Denuncia> {
    val restantes = denuncias.filterNot { it.id == id }
    return if (restantes.size == denuncias.size) denuncias else restantes
}

/** Confirmación para quien resuelve: qué se hizo y con qué reporte. */
fun AccionModeracion.aviso(denuncia: Denuncia) = when (this) {
    AccionModeracion.PRINCIPAL ->
        "Listo: ${denuncia.motivo.accionPrincipal.lowercase()} en ${denuncia.codigoIncidente}."
    AccionModeracion.DUPLICADO ->
        "Listo: ${denuncia.codigoIncidente} quedó marcado como duplicado."
    AccionModeracion.RECHAZAR ->
        "Listo: ${denuncia.codigoIncidente} quedó rechazado."
}
