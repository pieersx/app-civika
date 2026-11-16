package pe.civika.shared.domain

/** Lo que recibe la RPC `crear_reporte`. Las fotos son rutas del bucket `fotos` (`{uid}/archivo.jpg`). */
data class ReporteNuevo(
    val clienteId: String,
    val latitud: Double,
    val longitud: Double,
    val categoriaId: Int,
    val descripcion: String,
    val fotos: List<String>,
    val ubicacionSimulada: Boolean = false,
    val direccion: String? = null,
)

/** Respuesta de `crear_reporte` o de un apoyo: el incidente al que quedó unido el envío. */
data class ReporteCreado(
    val incidenteId: String,
    val codigo: String,
    val estado: Estado,
    /** Entidad responsable asignada por el ruteo (RF-06); `null` si pasó a revisión. */
    val entidad: String?,
)

enum class TipoEnvio {
    /** Un incidente nuevo: `crear_reporte`. */
    NUEVO,

    /** «Es el mismo problema»: `apoyar_incidente` con las fotos. */
    APOYO,
}

enum class EstadoEnvio {
    /** Guardado en el celular; espera conexión o el siguiente intento. */
    PENDIENTE,
    ENVIANDO,

    /** El servidor lo rechazó y reintentar no lo arregla (fuera de zona, datos inválidos). */
    ERROR,
    ENVIADO,
}

/** Por qué falló el último intento de envío, para explicarlo en «Mis reportes». */
enum class MotivoError { SIN_CONEXION, SESION, FUERA_DE_ZONA, DATOS, PERMISO, FOTO_PERDIDA, DESCONOCIDO }

/**
 * Un envío en la cola local (RF-15, ADR-6). Todo reporte pasa por aquí antes de ir al servidor; el `clienteId`
 * lo hace idempotente: reintentar nunca crea dos incidentes.
 */
data class ReportePendiente(
    val clienteId: String,
    val usuarioId: String,
    val tipo: TipoEnvio,
    /** Solo para [TipoEnvio.APOYO]: el incidente que se apoya. */
    val incidenteId: String? = null,
    val latitud: Double,
    val longitud: Double,
    val categoriaId: Int,
    val categoriaNombre: String,
    val descripcion: String,
    /** Rutas de los JPEG ya recomprimidos en el almacenamiento interno de la app. */
    val fotos: List<String>,
    val ubicacionSimulada: Boolean = false,
    val direccion: String? = null,
    val estado: EstadoEnvio = EstadoEnvio.PENDIENTE,
    val motivoError: MotivoError? = null,
    /** Mensaje del servidor, ya en español, cuando el error es de datos. */
    val detalleError: String? = null,
    val intentos: Int = 0,
    val creadoEnMs: Long = 0,
    // Resultado, cuando ya se envió:
    val incidenteCreadoId: String? = null,
    val codigo: String? = null,
    val estadoIncidente: Estado? = null,
    val entidad: String? = null,
)

fun ErrorCivika.aMotivo(): MotivoError = when (this) {
    is ErrorCivika.SinConexion -> MotivoError.SIN_CONEXION
    is ErrorCivika.SesionRequerida, is ErrorCivika.CorreoSinConfirmar -> MotivoError.SESION
    is ErrorCivika.FueraDeZona -> MotivoError.FUERA_DE_ZONA
    is ErrorCivika.DatosInvalidos -> MotivoError.DATOS
    is ErrorCivika.SinPermiso -> MotivoError.PERMISO
    else -> MotivoError.DESCONOCIDO
}
