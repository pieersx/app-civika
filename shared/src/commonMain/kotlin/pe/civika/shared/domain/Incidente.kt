package pe.civika.shared.domain

import kotlin.time.Instant

/** Un problema en la calle, tal como lo ve cualquier persona: sin autor y con la ubicación redondeada. */
data class Incidente(
    val id: String,
    val codigo: String,
    val titulo: String,
    val categoria: Categoria,
    val estado: Estado,
    val apoyos: Int,
    val diasAbierto: Int,
    val zona: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val creadoEn: Instant? = null,
) {
    /** La app ordena con esta fórmula e ignora el campo `prioridad` del servidor (docs/arquitectura.md §6). */
    val prioridad: Int
        get() = calcularPrioridad(
            pesoCategoria = categoria.pesoPrioridad,
            apoyos = apoyos,
            diasAbierto = diasAbierto,
            plazoObjetivoDias = categoria.plazoObjetivoDias,
        )

    /** Días que faltan para el plazo objetivo de la categoría; negativo si ya venció. */
    val diasParaElPlazo: Int
        get() = categoria.plazoObjetivoDias - diasAbierto

    val plazoVencido: Boolean
        get() = estado.familia in FAMILIAS_POR_ATENDER && diasParaElPlazo < 0
}

/** Los incidentes que alguien todavía tiene que atender. */
val FAMILIAS_POR_ATENDER = setOf(Familia.ABIERTO, Familia.EN_ATENCION)

data class EventoHistorial(val estado: Estado, val fecha: Instant)

data class ComentarioPublico(val texto: String, val fecha: Instant)

/** Detalle público de un incidente (RF-10) más lo que el usuario actual tiene que ver con él. */
data class DetalleIncidente(
    val incidente: Incidente,
    val descripcion: String?,
    val entidad: String?,
    val yoApoyo: Boolean,
    val esMio: Boolean,
    /** Respuesta del usuario a «¿sigue arreglado?»; `null` si todavía no respondió. */
    val miVerificacion: Boolean?,
    val motivoRechazo: String?,
    val fotos: List<String> = emptyList(),
    val historial: List<EventoHistorial> = emptyList(),
    val comentarios: List<ComentarioPublico> = emptyList(),
) {
    /** Sigue el incidente quien lo reportó o lo apoyó: recibe avisos y puede verificar la resolución. */
    val loSigo: Boolean get() = esMio || yoApoyo

    /** Igual que `apoyar_incidente`: no se apoya lo propio ni lo que ya está cerrado. */
    val puedeApoyar: Boolean get() = !esMio && incidente.estado.familia in FAMILIAS_POR_ATENDER

    val puedeVerificar: Boolean get() = loSigo && incidente.estado == Estado.RESUELTO

    val puedeDenunciar: Boolean get() = !esMio && incidente.estado != Estado.RECHAZADO
}

enum class Relacion { AUTOR, APOYO }

/** Un incidente que el usuario reportó o apoyó (RF-12). */
data class MiIncidente(val incidente: Incidente, val relacion: Relacion, val desde: Instant?)

/** Totales públicos de la pantalla de inicio. */
data class ResumenPublico(val abiertos: Int, val enAtencion: Int, val resueltos30d: Int)

/** Incidente cercano de la misma categoría: candidato a ser «el mismo problema» (RF-05). */
data class PosibleDuplicado(
    val incidenteId: String,
    val codigo: String,
    val distanciaM: Double,
    val apoyos: Int,
    val diasAbierto: Int,
)
