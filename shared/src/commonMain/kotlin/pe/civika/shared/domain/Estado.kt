package pe.civika.shared.domain

/** Familia visual del estado: decide la forma del marcador (ver docs/diseno/direccion-visual.md). */
enum class Familia { ABIERTO, EN_ATENCION, RESUELTO, RECHAZADO }

/** Los 6 estados del incidente del RF-10. */
enum class Estado(val etiqueta: String, val familia: Familia) {
    REPORTADO("Reportado", Familia.ABIERTO),
    EN_REVISION("En revisión", Familia.ABIERTO),
    DERIVADO("Derivado", Familia.ABIERTO),
    EN_ATENCION("En atención", Familia.EN_ATENCION),
    RESUELTO("Resuelto", Familia.RESUELTO),
    RECHAZADO("Rechazado", Familia.RECHAZADO),
}

/** Quién intenta cambiar el estado. SISTEMA son las RPC y los triggers, no una persona. */
enum class Rol { CIUDADANO, MODERADOR, ENTIDAD, SISTEMA }
