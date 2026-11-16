package pe.civika.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.MiIncidente
import pe.civika.shared.domain.Notificacion
import pe.civika.shared.domain.PosibleDuplicado
import pe.civika.shared.domain.ReporteCreado
import pe.civika.shared.domain.ReporteNuevo
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.ResultadoRegistro
import pe.civika.shared.domain.ResumenPublico

// Los ViewModels solo conocen estas interfaces. Cada una tiene su implementación con supabase-kt y su `Fake…`.
// Todas las funciones lanzan `ErrorCivika` cuando algo falla.

/** Cuenta del ciudadano (RF-01). */
interface AuthRepository {
    val sesion: StateFlow<EstadoSesion>

    suspend fun iniciarSesion(correo: String, contrasena: String)

    /** Envía `nombre` y `version_politica` como metadata: el trigger crea el usuario y guarda el consentimiento. */
    suspend fun registrar(nombre: String, correo: String, contrasena: String, versionPolitica: String): ResultadoRegistro

    suspend fun recuperarContrasena(correo: String)

    suspend fun cerrarSesion()

    suspend fun actualizarNombre(nombre: String)

    /** Ley 29733: anonimiza la cuenta y borra el acceso (`eliminar_mi_cuenta`). */
    suspend fun eliminarCuenta()
}

/** Incidentes públicos y lo que el ciudadano hace con ellos. */
interface IncidenteRepository {
    /** Hasta 200 incidentes públicos (sin los rechazados). Se puede llamar sin sesión. */
    suspend fun listar(): List<Incidente>

    suspend fun categorias(): List<Categoria>

    suspend fun resumen(): ResumenPublico

    /** `null` si el incidente no existe o no es visible para quien pregunta. */
    suspend fun detalle(incidenteId: String): DetalleIncidente?

    suspend fun misIncidentes(): List<MiIncidente>

    suspend fun buscarDuplicados(latitud: Double, longitud: Double, categoriaId: Int): List<PosibleDuplicado>

    /** Idempotente. Devuelve el incidente apoyado; `fotos` son rutas ya subidas al bucket. */
    suspend fun apoyar(clienteId: String, incidenteId: String, fotos: List<String> = emptyList()): ReporteCreado

    suspend fun retirarApoyo(incidenteId: String)

    /** Devuelve `true` si con esta denuncia el incidente pasó a revisión (RF-16). */
    suspend fun denunciar(incidenteId: String, motivo: String): Boolean

    /** «¿Sigue arreglado?». Devuelve el estado en que quedó el incidente (puede reabrirse). */
    suspend fun verificarResolucion(incidenteId: String, sigueResuelto: Boolean): Estado
}

/** Envío al servidor. La app nunca lo llama directo desde una pantalla: pasa por [ColaReportesRepository]. */
interface ReporteRepository {
    /** Sube un JPEG a `fotos/{uid}/…` y devuelve su ruta. Reintentar la misma foto no falla. */
    suspend fun subirFoto(clienteId: String, indice: Int, jpeg: ByteArray): String

    suspend fun crear(reporte: ReporteNuevo): ReporteCreado
}

/** Cola local de envíos (RF-15, ADR-6). En Android la implementa Room + WorkManager. */
interface ColaReportesRepository {
    fun observar(usuarioId: String): Flow<List<ReportePendiente>>

    fun observarUno(clienteId: String): Flow<ReportePendiente?>

    /** Guarda el envío y programa la sincronización. */
    suspend fun encolar(reporte: ReportePendiente)

    suspend fun reintentar(clienteId: String)

    /** Borra el envío y sus fotos locales. */
    suspend fun descartar(clienteId: String)
}

/** Bandeja de avisos (RF-11) y registro del dispositivo para el push. */
interface NotificacionRepository {
    suspend fun listar(): List<Notificacion>

    suspend fun marcarLeida(id: String)

    suspend fun marcarTodasLeidas()

    suspend fun registrarDispositivo(token: String)

    suspend fun eliminarDispositivo(token: String)
}

/** Dirección aproximada de un punto (Nominatim, ADR-3). Nunca lanza: si falla, devuelve `null`. */
fun interface Geocodificador {
    suspend fun direccionAproximada(latitud: Double, longitud: Double): String?
}
