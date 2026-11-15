package pe.civika.shared.domain

import kotlin.time.Instant

enum class TipoNotificacion {
    /** El incidente cambió de estado. */
    ESTADO,

    /** Tres vecinos dijeron que el problema sigue igual y volvió a En atención. */
    REABIERTO,

    /** La entidad escribió un comentario público. */
    COMENTARIO,

    /** Un tipo que esta versión de la app no conoce. */
    OTRO;

    companion object {
        fun desde(texto: String?): TipoNotificacion = when (texto) {
            "estado" -> ESTADO
            "reabierto" -> REABIERTO
            "comentario" -> COMENTARIO
            else -> OTRO
        }
    }
}

/** Una fila de la bandeja del usuario (RF-11). Funciona aunque el push de FCM no esté configurado. */
data class Notificacion(
    val id: String,
    val tipo: TipoNotificacion,
    val estado: Estado?,
    val leida: Boolean,
    val fecha: Instant,
    val incidenteId: String?,
    val codigo: String?,
    val titulo: String?,
)
