package pe.civika.shared.domain

import kotlinx.coroutines.test.runTest
import pe.civika.shared.data.DatosDemo
import pe.civika.shared.data.FakeAuthRepository
import pe.civika.shared.data.FakeGeocodificador
import pe.civika.shared.data.FakeIncidenteRepository
import pe.civika.shared.data.FakeReporteRepository
import pe.civika.shared.data.ReporteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** ADR-6: la cola envía sin duplicar, reintenta lo pasajero y explica lo definitivo. */
class SincronizarReporteTest {

    private val usuario = Usuario("u-1", "Ana", "ana@unmsm.edu.pe")
    private val demo = DatosDemo()
    private val incidentes = FakeIncidenteRepository(demo)
    private val reportes = FakeReporteRepository(demo)
    private val geocodificador = FakeGeocodificador()

    init {
        FakeAuthRepository(demo, sesionInicial = usuario)
    }

    private fun pendiente(
        tipo: TipoEnvio = TipoEnvio.NUEVO,
        lat: Double = ZonaPiloto.centro.latitud,
        lon: Double = ZonaPiloto.centro.longitud,
        incidenteId: String? = null,
        direccion: String? = null,
    ) = ReportePendiente(
        clienteId = "c-1",
        usuarioId = usuario.id,
        tipo = tipo,
        incidenteId = incidenteId,
        latitud = lat,
        longitud = lon,
        categoriaId = CategoriasPiloto.VIAL.id,
        categoriaNombre = CategoriasPiloto.VIAL.nombre,
        descripcion = "Bache profundo frente a la puerta 3",
        fotos = listOf("/fotos/0.jpg", "/fotos/1.jpg"),
        direccion = direccion,
    )

    private val leer: suspend (String) -> ByteArray? = { ByteArray(4) }

    @Test
    fun enviaUnReporteNuevoYDevuelveSuCodigo() = runTest {
        val resultado = SincronizarReporte(reportes, incidentes, geocodificador)(pendiente(), leer)

        val enviado = assertIs<ResultadoSync.Enviado>(resultado)
        assertEquals("CVK-000132", enviado.creado.codigo)
        assertEquals("Av. Universitaria, Cercado de Lima", enviado.direccion)
        assertEquals(1, geocodificador.consultas)
    }

    @Test
    fun reintentarElMismoClienteIdNoCreaOtroIncidente() = runTest {
        val sincronizar = SincronizarReporte(reportes, incidentes, geocodificador)

        val primero = assertIs<ResultadoSync.Enviado>(sincronizar(pendiente(), leer))
        val segundo = assertIs<ResultadoSync.Enviado>(sincronizar(pendiente(), leer))

        assertEquals(primero.creado.incidenteId, segundo.creado.incidenteId)
        assertEquals(1, incidentes.todos().count { it.codigo == primero.creado.codigo })
    }

    @Test
    fun siYaHayDireccionNoVuelveAConsultarANominatim() = runTest {
        SincronizarReporte(reportes, incidentes, geocodificador)(pendiente(direccion = "Av. Venezuela"), leer)

        assertEquals(0, geocodificador.consultas)
    }

    @Test
    fun fueraDeLaZonaPilotoEsDefinitivoYNoSeReintenta() = runTest {
        val resultado = SincronizarReporte(reportes, incidentes, geocodificador)(pendiente(lat = -12.0464, lon = -77.0428), leer)

        assertEquals(MotivoError.FUERA_DE_ZONA, assertIs<ResultadoSync.Definitivo>(resultado).motivo)
    }

    @Test
    fun unErrorDeValidacionEsDefinitivoYConservaElMensajeDelServidor() = runTest {
        val rechaza = object : ReporteRepository by reportes {
            override suspend fun crear(reporte: ReporteNuevo): ReporteCreado =
                throw ErrorCivika.DatosInvalidos("Demasiados reportes en la última hora")
        }

        val resultado = SincronizarReporte(rechaza, incidentes, geocodificador)(pendiente(), leer)

        val definitivo = assertIs<ResultadoSync.Definitivo>(resultado)
        assertEquals(MotivoError.DATOS, definitivo.motivo)
        assertEquals("Demasiados reportes en la última hora", definitivo.detalle)
    }

    @Test
    fun sinConexionSeReintentaMasTarde() = runTest {
        val sinRed = object : ReporteRepository by reportes {
            override suspend fun subirFoto(clienteId: String, indice: Int, jpeg: ByteArray): String =
                throw ErrorCivika.SinConexion()
        }

        val resultado = SincronizarReporte(sinRed, incidentes, geocodificador)(pendiente(), leer)

        assertEquals(MotivoError.SIN_CONEXION, assertIs<ResultadoSync.Reintentar>(resultado).motivo)
    }

    @Test
    fun sinSesionSeReintentaCuandoElUsuarioVuelvaAEntrar() = runTest {
        val sinSesion = DatosDemo()
        val resultado = SincronizarReporte(FakeReporteRepository(sinSesion), FakeIncidenteRepository(sinSesion), geocodificador)(pendiente(), leer)

        assertEquals(MotivoError.SESION, assertIs<ResultadoSync.Reintentar>(resultado).motivo)
    }

    @Test
    fun unErrorInesperadoNoRompeLaColaYSeReintenta() = runTest {
        val roto = object : ReporteRepository by reportes {
            override suspend fun crear(reporte: ReporteNuevo): ReporteCreado = error("respuesta inesperada")
        }

        val resultado = SincronizarReporte(roto, incidentes, geocodificador)(pendiente(), leer)

        assertEquals(MotivoError.DESCONOCIDO, assertIs<ResultadoSync.Reintentar>(resultado).motivo)
    }

    @Test
    fun siSePerdioUnaFotoLocalElEnvioQuedaConErrorDefinitivo() = runTest {
        val resultado = SincronizarReporte(reportes, incidentes, geocodificador)(pendiente()) { null }

        assertEquals(MotivoError.FOTO_PERDIDA, assertIs<ResultadoSync.Definitivo>(resultado).motivo)
    }

    @Test
    fun siNominatimFallaElReporteSeEnviaSinDireccion() = runTest {
        val resultado = SincronizarReporte(reportes, incidentes, { _, _ -> error("sin red") })(pendiente(), leer)

        assertNull(assertIs<ResultadoSync.Enviado>(resultado).direccion)
    }

    @Test
    fun unApoyoSubeLasFotosYSumaUnApoyoAlIncidente() = runTest {
        val antes = incidentes.todos().first { it.id == "demo-124" }.apoyos

        val resultado = SincronizarReporte(reportes, incidentes, geocodificador)(
            pendiente(tipo = TipoEnvio.APOYO, incidenteId = "demo-124"),
            leer,
        )

        assertEquals("CVK-000124", assertIs<ResultadoSync.Enviado>(resultado).creado.codigo)
        assertEquals(antes + 1, incidentes.todos().first { it.id == "demo-124" }.apoyos)
        assertEquals(0, geocodificador.consultas)
    }

    @Test
    fun apoyarDosVecesNoDuplica() = runTest {
        val sincronizar = SincronizarReporte(reportes, incidentes, geocodificador)
        val apoyo = pendiente(tipo = TipoEnvio.APOYO, incidenteId = "demo-124")
        val antes = incidentes.todos().first { it.id == "demo-124" }.apoyos

        sincronizar(apoyo, leer)
        sincronizar(apoyo, leer)

        assertEquals(antes + 1, incidentes.todos().first { it.id == "demo-124" }.apoyos)
    }
}
