package pe.civika.shared.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.ComentarioPublico
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.EventoHistorial
import pe.civika.shared.domain.FAMILIAS_POR_ATENDER
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.MiIncidente
import pe.civika.shared.domain.Notificacion
import pe.civika.shared.domain.PosibleDuplicado
import pe.civika.shared.domain.Relacion
import pe.civika.shared.domain.ReporteCreado
import pe.civika.shared.domain.ReporteNuevo
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.ResultadoRegistro
import pe.civika.shared.domain.ResumenPublico
import pe.civika.shared.domain.TipoNotificacion
import pe.civika.shared.domain.Usuario
import pe.civika.shared.domain.ZonaPiloto
import pe.civika.shared.domain.distanciaMetros
import pe.civika.shared.domain.usuario
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

// Repositorios de ejemplo: sirven para las pruebas y para el «modo demostración» de la app (sin Supabase).
// Comparten un mismo [DatosDemo] para que reportar, apoyar y «mis reportes» sean coherentes entre pantallas.

/** Estado en memoria del modo demostración. Se pierde al cerrar la app. */
class DatosDemo(val ahora: () -> Instant = { Clock.System.now() }) {
    internal val usuario = MutableStateFlow<Usuario?>(null)
    internal val incidentes = MutableStateFlow(incidentesIniciales())
    internal val apoyados = MutableStateFlow(setOf("demo-109"))
    internal val propios = MutableStateFlow(setOf("demo-087"))
    internal val verificaciones = MutableStateFlow(mapOf<String, Boolean>())
    internal val notificaciones = MutableStateFlow(notificacionesIniciales())
    internal val dispositivos = MutableStateFlow(setOf<String>())
    internal val fotosSubidas = MutableStateFlow(setOf<String>())
    internal var siguienteCodigo = 132

    private fun incidente(
        numero: Int,
        titulo: String,
        categoria: Categoria,
        estado: Estado,
        apoyos: Int,
        dias: Int,
        zona: String,
        lat: Double,
        lon: Double,
    ) = Incidente(
        id = "demo-" + numero.toString().padStart(3, '0'),
        codigo = "CVK-" + numero.toString().padStart(6, '0'),
        titulo = titulo,
        categoria = categoria,
        estado = estado,
        apoyos = apoyos,
        diasAbierto = dias,
        zona = zona,
        latitud = lat,
        longitud = lon,
        creadoEn = ahora() - dias.days,
    )

    private fun incidentesIniciales(): List<Incidente> {
        val campus = "Ciudad Universitaria UNMSM"
        val alrededores = "Alrededores de la Ciudad Universitaria"
        return listOf(
            incidente(124, "Bache profundo en la pista", CategoriasPiloto.VIAL, Estado.DERIVADO, 25, 3, alrededores, -12.0512, -77.0853),
            incidente(118, "Vereda sin rampa en el cruce", CategoriasPiloto.ACCESIBILIDAD, Estado.EN_REVISION, 11, 1, campus, -12.0561, -77.0843),
            incidente(109, "Basura acumulada en la esquina", CategoriasPiloto.RESIDUOS, Estado.EN_ATENCION, 15, 7, alrededores, -12.0641, -77.0812),
            incidente(102, "Pista hundida frente al mercado", CategoriasPiloto.VIAL, Estado.EN_ATENCION, 9, 6, alrededores, -12.0498, -77.0791),
            incidente(131, "Contenedor desbordado", CategoriasPiloto.RESIDUOS, Estado.REPORTADO, 4, 0, campus, -12.0584, -77.0871),
            incidente(87, "Rampa bloqueada por un poste", CategoriasPiloto.ACCESIBILIDAD, Estado.RESUELTO, 18, 9, campus, -12.0601, -77.0826),
            incidente(90, "Hueco grande en la avenida", CategoriasPiloto.VIAL, Estado.RECHAZADO, 0, 4, alrededores, -12.0655, -77.0902),
            incidente(127, "Vereda rota junto al paradero", CategoriasPiloto.VIAL, Estado.DERIVADO, 6, 2, campus, -12.0547, -77.0888),
            incidente(129, "Desmonte en la berma central", CategoriasPiloto.RESIDUOS, Estado.DERIVADO, 2, 6, alrededores, -12.0489, -77.0935),
        )
    }

    private fun notificacionesIniciales(): List<Notificacion> = listOf(
        Notificacion("n-1", TipoNotificacion.ESTADO, Estado.EN_ATENCION, false, ahora() - 3.hours, "demo-109", "CVK-000109", "Basura acumulada en la esquina"),
        Notificacion("n-2", TipoNotificacion.COMENTARIO, null, false, ahora() - 1.days, "demo-109", "CVK-000109", "Basura acumulada en la esquina"),
        Notificacion("n-3", TipoNotificacion.ESTADO, Estado.RESUELTO, true, ahora() - 2.days, "demo-087", "CVK-000087", "Rampa bloqueada por un poste"),
    )

    internal fun exigirUsuario(): Usuario = usuario.value ?: throw ErrorCivika.SesionRequerida()
}

/** En el modo demostración cualquier correo y contraseña válidos inician sesión. */
class FakeAuthRepository(
    private val demo: DatosDemo = DatosDemo(),
    private val demoraMs: Long = 0,
    sesionInicial: Usuario? = null,
) : AuthRepository {

    private val _sesion = MutableStateFlow<EstadoSesion>(
        sesionInicial?.let { EstadoSesion.Activa(it) } ?: EstadoSesion.SinSesion,
    )
    override val sesion: StateFlow<EstadoSesion> = _sesion.asStateFlow()

    init {
        demo.usuario.value = sesionInicial
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String) {
        delay(demoraMs)
        if (contrasena == CONTRASENA_INCORRECTA) throw ErrorCivika.CredencialesInvalidas()
        entrar(Usuario(ID_DEMO, nombreDesde(correo), correo.trim()))
    }

    override suspend fun registrar(
        nombre: String,
        correo: String,
        contrasena: String,
        versionPolitica: String,
    ): ResultadoRegistro {
        delay(demoraMs)
        if (correo.trim() == CORREO_YA_REGISTRADO) throw ErrorCivika.CorreoYaRegistrado()
        if (correo.trim().startsWith(PREFIJO_CONFIRMAR)) return ResultadoRegistro.CONFIRMAR_CORREO
        entrar(Usuario(ID_DEMO, nombre.trim(), correo.trim()))
        return ResultadoRegistro.SESION_INICIADA
    }

    override suspend fun recuperarContrasena(correo: String) {
        delay(demoraMs)
    }

    override suspend fun cerrarSesion() {
        demo.usuario.value = null
        _sesion.value = EstadoSesion.SinSesion
    }

    override suspend fun actualizarNombre(nombre: String) {
        delay(demoraMs)
        val actual = demo.exigirUsuario()
        entrar(actual.copy(nombre = nombre.trim()))
    }

    override suspend fun eliminarCuenta() {
        delay(demoraMs)
        demo.exigirUsuario()
        cerrarSesion()
    }

    private fun entrar(usuario: Usuario) {
        demo.usuario.value = usuario
        _sesion.value = EstadoSesion.Activa(usuario)
    }

    private fun nombreDesde(correo: String): String =
        correo.substringBefore('@').replace('.', ' ').replaceFirstChar { it.uppercase() }.ifBlank { "Ciudadano" }

    companion object {
        const val ID_DEMO = "usuario-demo"
        const val CONTRASENA_INCORRECTA = "incorrecta"
        const val CORREO_YA_REGISTRADO = "ya@existe.pe"
        const val PREFIJO_CONFIRMAR = "confirmar"
    }
}

/** Datos de ejemplo de la zona piloto para avanzar sin backend y para las pruebas. */
class FakeIncidenteRepository(
    private val demo: DatosDemo = DatosDemo(),
    private val demoraMs: Long = 0,
) : IncidenteRepository {

    /** Como la RPC pública: los rechazados no aparecen. */
    override suspend fun listar(): List<Incidente> {
        delay(demoraMs)
        return demo.incidentes.value.filter { it.estado != Estado.RECHAZADO }
    }

    /** Todos, también los rechazados: para las pruebas del dominio. */
    fun todos(): List<Incidente> = demo.incidentes.value

    override suspend fun categorias(): List<Categoria> = CategoriasPiloto.todas

    override suspend fun resumen(): ResumenPublico {
        delay(demoraMs)
        val todos = demo.incidentes.value
        return ResumenPublico(
            abiertos = todos.count { it.estado in setOf(Estado.REPORTADO, Estado.EN_REVISION, Estado.DERIVADO) },
            enAtencion = todos.count { it.estado == Estado.EN_ATENCION },
            resueltos30d = todos.count { it.estado == Estado.RESUELTO },
        )
    }

    override suspend fun detalle(incidenteId: String): DetalleIncidente? {
        delay(demoraMs)
        val incidente = demo.incidentes.value.firstOrNull { it.id == incidenteId } ?: return null
        val conSesion = demo.usuario.value != null
        val esMio = conSesion && incidenteId in demo.propios.value
        if (incidente.estado == Estado.RECHAZADO && !esMio) return null
        val creado = incidente.creadoEn ?: demo.ahora()
        val historial = buildList {
            add(EventoHistorial(Estado.DERIVADO.takeIf { incidente.estado != Estado.EN_REVISION } ?: Estado.EN_REVISION, creado))
            if (incidente.estado in setOf(Estado.EN_ATENCION, Estado.RESUELTO)) add(EventoHistorial(Estado.EN_ATENCION, creado + 1.days))
            if (incidente.estado == Estado.RESUELTO) add(EventoHistorial(Estado.RESUELTO, creado + 4.days))
            if (incidente.estado == Estado.RECHAZADO) add(EventoHistorial(Estado.RECHAZADO, creado + 1.days))
        }
        return DetalleIncidente(
            incidente = incidente,
            descripcion = incidente.titulo + ". Lleva varios días así y dificulta el paso de los vecinos.",
            entidad = if (incidente.estado == Estado.EN_REVISION) null else "Municipalidad del distrito piloto",
            yoApoyo = conSesion && incidenteId in demo.apoyados.value,
            esMio = esMio,
            miVerificacion = if (conSesion) demo.verificaciones.value[incidenteId] else null,
            motivoRechazo = "No corresponde a un problema de la vía pública.".takeIf { incidente.estado == Estado.RECHAZADO },
            fotos = emptyList(),
            historial = historial,
            comentarios = if (incidente.estado in setOf(Estado.EN_ATENCION, Estado.RESUELTO)) {
                listOf(ComentarioPublico("Una cuadrilla ya está trabajando en el lugar.", creado + 1.days + 2.hours))
            } else {
                emptyList()
            },
        )
    }

    override suspend fun misIncidentes(): List<MiIncidente> {
        delay(demoraMs)
        demo.exigirUsuario()
        val propios = demo.propios.value
        val apoyados = demo.apoyados.value
        return demo.incidentes.value
            .filter { it.id in propios || (it.id in apoyados && it.estado != Estado.RECHAZADO) }
            .map { MiIncidente(it, if (it.id in propios) Relacion.AUTOR else Relacion.APOYO, it.creadoEn) }
            .sortedByDescending { it.desde }
    }

    override suspend fun buscarDuplicados(latitud: Double, longitud: Double, categoriaId: Int): List<PosibleDuplicado> {
        delay(demoraMs)
        demo.exigirUsuario()
        val categoria = CategoriasPiloto.todas.firstOrNull { it.id == categoriaId }
            ?: throw ErrorCivika.DatosInvalidos("Categoría inexistente o inactiva")
        return demo.incidentes.value
            .filter { it.categoria.id == categoriaId && it.estado.familia in FAMILIAS_POR_ATENDER }
            .mapNotNull { incidente ->
                val lat = incidente.latitud ?: return@mapNotNull null
                val lon = incidente.longitud ?: return@mapNotNull null
                val distancia = distanciaMetros(latitud, longitud, lat, lon)
                if (distancia > categoria.radioDuplicadoM) return@mapNotNull null
                PosibleDuplicado(incidente.id, incidente.codigo, distancia, incidente.apoyos, incidente.diasAbierto)
            }
            .sortedBy { it.distanciaM }
    }

    override suspend fun apoyar(clienteId: String, incidenteId: String, fotos: List<String>): ReporteCreado {
        delay(demoraMs)
        demo.exigirUsuario()
        val incidente = demo.incidentes.value.firstOrNull { it.id == incidenteId }
            ?: throw ErrorCivika.DatosInvalidos("Incidente no encontrado")
        if (incidenteId !in demo.apoyados.value) {
            if (incidente.estado.familia !in FAMILIAS_POR_ATENDER) {
                throw ErrorCivika.DatosInvalidos("Este incidente ya está cerrado y no admite apoyos")
            }
            if (incidenteId in demo.propios.value) throw ErrorCivika.DatosInvalidos("No puedes apoyar tu propio reporte")
            demo.apoyados.update { it + incidenteId }
            cambiarApoyos(incidenteId, +1)
        }
        val actual = demo.incidentes.value.first { it.id == incidenteId }
        return ReporteCreado(actual.id, actual.codigo, actual.estado, "Municipalidad del distrito piloto")
    }

    override suspend fun retirarApoyo(incidenteId: String) {
        delay(demoraMs)
        demo.exigirUsuario()
        if (incidenteId in demo.apoyados.value) {
            demo.apoyados.update { it - incidenteId }
            cambiarApoyos(incidenteId, -1)
        }
    }

    override suspend fun denunciar(incidenteId: String, motivo: String): Boolean {
        delay(demoraMs)
        demo.exigirUsuario()
        if (incidenteId in demo.propios.value) throw ErrorCivika.DatosInvalidos("No puedes denunciar tu propio reporte")
        return false
    }

    override suspend fun verificarResolucion(incidenteId: String, sigueResuelto: Boolean): Estado {
        delay(demoraMs)
        demo.exigirUsuario()
        val incidente = demo.incidentes.value.firstOrNull { it.id == incidenteId }
            ?: throw ErrorCivika.DatosInvalidos("Incidente no encontrado")
        if (incidente.estado != Estado.RESUELTO) throw ErrorCivika.DatosInvalidos("Solo se verifica un incidente resuelto")
        demo.verificaciones.update { it + (incidenteId to sigueResuelto) }
        return incidente.estado
    }

    private fun cambiarApoyos(incidenteId: String, cambio: Int) {
        demo.incidentes.update { lista ->
            lista.map { if (it.id == incidenteId) it.copy(apoyos = (it.apoyos + cambio).coerceAtLeast(0)) else it }
        }
    }
}

class FakeReporteRepository(
    private val demo: DatosDemo = DatosDemo(),
    private val demoraMs: Long = 0,
) : ReporteRepository {

    private val creados = mutableMapOf<String, ReporteCreado>()

    override suspend fun subirFoto(clienteId: String, indice: Int, jpeg: ByteArray): String {
        delay(demoraMs)
        val usuario = demo.exigirUsuario()
        val ruta = "${usuario.id}/$clienteId-$indice.jpg"
        demo.fotosSubidas.update { it + ruta }
        return ruta
    }

    override suspend fun crear(reporte: ReporteNuevo): ReporteCreado {
        delay(demoraMs)
        demo.exigirUsuario()
        // Idempotente por cliente_id, como la RPC.
        creados[reporte.clienteId]?.let { return it }
        if (!ZonaPiloto.contiene(reporte.latitud, reporte.longitud)) throw ErrorCivika.FueraDeZona()
        if (reporte.fotos.isEmpty()) throw ErrorCivika.DatosInvalidos("El reporte necesita al menos una foto")
        val categoria = CategoriasPiloto.todas.firstOrNull { it.id == reporte.categoriaId }
            ?: throw ErrorCivika.DatosInvalidos("Categoría inexistente o inactiva")
        val numero = demo.siguienteCodigo++
        val estado = if (reporte.ubicacionSimulada) Estado.EN_REVISION else Estado.DERIVADO
        val incidente = Incidente(
            id = "demo-$numero",
            codigo = "CVK-" + numero.toString().padStart(6, '0'),
            titulo = reporte.descripcion.trim().replace(Regex("\\s+"), " ").take(80),
            categoria = categoria,
            estado = estado,
            apoyos = 0,
            diasAbierto = 0,
            zona = "Ciudad Universitaria UNMSM",
            latitud = reporte.latitud,
            longitud = reporte.longitud,
            creadoEn = demo.ahora(),
        )
        demo.incidentes.update { listOf(incidente) + it }
        demo.propios.update { it + incidente.id }
        val entidad = "Municipalidad del distrito piloto".takeIf { estado == Estado.DERIVADO }
        return ReporteCreado(incidente.id, incidente.codigo, estado, entidad).also { creados[reporte.clienteId] = it }
    }
}

class FakeNotificacionRepository(
    private val demo: DatosDemo = DatosDemo(),
    private val demoraMs: Long = 0,
) : NotificacionRepository {

    override suspend fun listar(): List<Notificacion> {
        delay(demoraMs)
        demo.exigirUsuario()
        return demo.notificaciones.value.sortedByDescending { it.fecha }
    }

    override suspend fun marcarLeida(id: String) {
        demo.exigirUsuario()
        demo.notificaciones.update { lista -> lista.map { if (it.id == id) it.copy(leida = true) else it } }
    }

    override suspend fun marcarTodasLeidas() {
        demo.exigirUsuario()
        demo.notificaciones.update { lista -> lista.map { it.copy(leida = true) } }
    }

    override suspend fun registrarDispositivo(token: String) {
        demo.exigirUsuario()
        demo.dispositivos.update { it + token }
    }

    override suspend fun eliminarDispositivo(token: String) {
        demo.dispositivos.update { it - token }
    }

    /** Tokens registrados, para las pruebas. */
    fun dispositivos(): Set<String> = demo.dispositivos.value
}

/** Cola en memoria. No envía nada sola: las pruebas deciden cuándo cambia el estado de un envío. */
class FakeColaReportesRepository : ColaReportesRepository {

    private val envios = MutableStateFlow(listOf<ReportePendiente>())

    override fun observar(usuarioId: String): Flow<List<ReportePendiente>> =
        envios.map { lista -> lista.filter { it.usuarioId == usuarioId } }

    override fun observarUno(clienteId: String): Flow<ReportePendiente?> =
        envios.map { lista -> lista.firstOrNull { it.clienteId == clienteId } }

    override suspend fun encolar(reporte: ReportePendiente) {
        envios.update { lista -> lista.filterNot { it.clienteId == reporte.clienteId } + reporte }
    }

    override suspend fun reintentar(clienteId: String) {
        actualizar(clienteId) { it.copy(estado = EstadoEnvio.PENDIENTE, motivoError = null, detalleError = null) }
    }

    override suspend fun descartar(clienteId: String) {
        envios.update { lista -> lista.filterNot { it.clienteId == clienteId } }
    }

    fun actualizar(clienteId: String, cambio: (ReportePendiente) -> ReportePendiente) {
        envios.update { lista -> lista.map { if (it.clienteId == clienteId) cambio(it) else it } }
    }

    fun todos(): List<ReportePendiente> = envios.value
}

class FakeGeocodificador(private val direccion: String? = "Av. Universitaria, Cercado de Lima") : Geocodificador {
    var consultas = 0
        private set

    override suspend fun direccionAproximada(latitud: Double, longitud: Double): String? {
        consultas++
        return direccion
    }
}
