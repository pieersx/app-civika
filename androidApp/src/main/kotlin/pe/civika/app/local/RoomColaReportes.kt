package pe.civika.app.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.civika.app.device.AlmacenFotos
import pe.civika.app.sync.ProgramadorSync
import pe.civika.shared.data.ColaReportesRepository
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.MotivoError
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.TipoEnvio

/** Cola de envíos en Room. Guardar un envío programa la sincronización con WorkManager (ADR-6). */
class RoomColaReportes(
    private val dao: ReporteDao,
    private val programador: ProgramadorSync,
    private val fotos: AlmacenFotos,
) : ColaReportesRepository {

    override fun observar(usuarioId: String): Flow<List<ReportePendiente>> =
        dao.observar(usuarioId).map { lista -> lista.map { it.aDominio() } }

    override fun observarUno(clienteId: String): Flow<ReportePendiente?> =
        dao.observarUno(clienteId).map { it?.aDominio() }

    override suspend fun encolar(reporte: ReportePendiente) {
        dao.guardar(reporte.aEntidad())
        programador.programar()
    }

    override suspend fun reintentar(clienteId: String) {
        dao.reintentar(clienteId)
        programador.programar()
    }

    override suspend fun descartar(clienteId: String) {
        dao.borrar(clienteId)
        fotos.borrarCarpeta(clienteId)
    }
}

private const val SEPARADOR = "\n"

internal fun ReportePendiente.aEntidad() = ReporteEntity(
    clienteId = clienteId,
    usuarioId = usuarioId,
    tipo = tipo.name,
    incidenteId = incidenteId,
    latitud = latitud,
    longitud = longitud,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre,
    descripcion = descripcion,
    fotos = fotos.joinToString(SEPARADOR),
    ubicacionSimulada = ubicacionSimulada,
    direccion = direccion,
    estado = estado.name,
    motivoError = motivoError?.name,
    detalleError = detalleError,
    intentos = intentos,
    creadoEnMs = creadoEnMs,
    incidenteCreadoId = incidenteCreadoId,
    codigo = codigo,
    estadoIncidente = estadoIncidente?.name,
    entidad = entidad,
)

internal fun ReporteEntity.aDominio() = ReportePendiente(
    clienteId = clienteId,
    usuarioId = usuarioId,
    tipo = TipoEnvio.entries.firstOrNull { it.name == tipo } ?: TipoEnvio.NUEVO,
    incidenteId = incidenteId,
    latitud = latitud,
    longitud = longitud,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre,
    descripcion = descripcion,
    fotos = fotos.split(SEPARADOR).filter { it.isNotBlank() },
    ubicacionSimulada = ubicacionSimulada,
    direccion = direccion,
    estado = EstadoEnvio.entries.firstOrNull { it.name == estado } ?: EstadoEnvio.PENDIENTE,
    motivoError = motivoError?.let { m -> MotivoError.entries.firstOrNull { it.name == m } },
    detalleError = detalleError,
    intentos = intentos,
    creadoEnMs = creadoEnMs,
    incidenteCreadoId = incidenteCreadoId,
    codigo = codigo,
    estadoIncidente = estadoIncidente?.let { e -> Estado.entries.firstOrNull { it.name == e } },
    entidad = entidad,
)
