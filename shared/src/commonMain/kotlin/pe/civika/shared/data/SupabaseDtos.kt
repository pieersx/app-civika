package pe.civika.shared.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Filas tal como las devuelven PostgREST y las RPC. El contrato son las migraciones de `supabase/migrations/`.

@Serializable
internal data class CategoriaDto(
    val id: Int,
    @SerialName("service_code") val serviceCode: String,
    val nombre: String,
    @SerialName("peso_prioridad") val pesoPrioridad: Int,
    @SerialName("radio_duplicado_m") val radioDuplicadoM: Int,
    @SerialName("ventana_dias") val ventanaDias: Int,
    @SerialName("plazo_objetivo_dias") val plazoObjetivoDias: Int,
    val activa: Boolean = true,
)

/** `listar_incidentes_publicos` y `mis_incidentes` (esta añade `relacion` y `desde`). */
@Serializable
internal data class IncidenteDto(
    val id: String,
    val codigo: String,
    val titulo: String? = null,
    @SerialName("categoria_id") val categoriaId: Int,
    val estado: String,
    @SerialName("apoyos_count") val apoyosCount: Int = 0,
    val zona: String = "",
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("creado_en") val creadoEn: String,
    val relacion: String? = null,
    val desde: String? = null,
)

/** `detalle_incidente`. */
@Serializable
internal data class DetalleDto(
    val id: String,
    val codigo: String,
    val titulo: String? = null,
    val descripcion: String? = null,
    @SerialName("categoria_id") val categoriaId: Int,
    val estado: String,
    @SerialName("apoyos_count") val apoyosCount: Int = 0,
    val zona: String = "",
    val entidad: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("creado_en") val creadoEn: String,
    @SerialName("yo_apoyo") val yoApoyo: Boolean = false,
    @SerialName("es_mio") val esMio: Boolean = false,
    @SerialName("mi_verificacion") val miVerificacion: Boolean? = null,
    @SerialName("motivo_rechazo") val motivoRechazo: String? = null,
)

@Serializable
internal data class HistorialDto(val estado: String, @SerialName("creado_en") val creadoEn: String)

@Serializable
internal data class ComentarioDto(val texto: String, @SerialName("creado_en") val creadoEn: String)

@Serializable
internal data class FotoDto(val ruta: String)

@Serializable
internal data class ResumenDto(
    val abiertos: Long = 0,
    @SerialName("en_atencion") val enAtencion: Long = 0,
    @SerialName("resueltos_30d") val resueltos30d: Long = 0,
)

@Serializable
internal data class DuplicadoDto(
    @SerialName("incidente_id") val incidenteId: String,
    val codigo: String,
    @SerialName("distancia_m") val distanciaM: Double,
    @SerialName("apoyos_count") val apoyosCount: Int = 0,
    @SerialName("creado_en") val creadoEn: String,
)

@Serializable
internal data class ReporteCreadoDto(
    @SerialName("incidente_id") val incidenteId: String,
    val codigo: String,
    val estado: String,
    @SerialName("entidad_id") val entidadId: Int? = null,
)

@Serializable
internal data class ApoyoDto(@SerialName("incidente_id") val incidenteId: String, val apoyos: Int = 0)

@Serializable
internal data class NotificacionDto(
    val id: String,
    val tipo: String,
    val estado: String? = null,
    val leida: Boolean = false,
    @SerialName("creado_en") val creadoEn: String,
    @SerialName("incidente_id") val incidenteId: String? = null,
    val codigo: String? = null,
    val titulo: String? = null,
)

@Serializable
internal data class UsuarioDto(val id: String, val nombre: String)

@Serializable
internal data class EntidadDto(val id: Int, val nombre: String)
