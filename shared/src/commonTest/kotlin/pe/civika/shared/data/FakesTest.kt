package pe.civika.shared.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import pe.civika.shared.di.ModoApp
import pe.civika.shared.di.crearSharedModule
import pe.civika.shared.di.sharedModule
import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Relacion
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.ResultadoRegistro
import pe.civika.shared.domain.TipoEnvio
import pe.civika.shared.domain.Usuario
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Los repositorios de ejemplo se comportan como el contrato SQL: son la base de las pruebas de los ViewModels. */
class FakesTest {

    private val demo = DatosDemo()
    private val auth = FakeAuthRepository(demo)
    private val incidentes = FakeIncidenteRepository(demo)

    @Test
    fun sinSesionSePuedeExplorarPeroNoApoyar() = runTest {
        assertTrue(incidentes.listar().isNotEmpty())
        assertNotNull(incidentes.detalle("demo-124"))
        assertFailsWith<ErrorCivika.SesionRequerida> { incidentes.apoyar("c", "demo-124") }
        assertFailsWith<ErrorCivika.SesionRequerida> { incidentes.misIncidentes() }
    }

    @Test
    fun laListaPublicaNoTraeRechazadosYElDetalleDeUnoAjenoNoExiste() = runTest {
        assertTrue(incidentes.listar().none { it.estado == Estado.RECHAZADO })
        assertNull(incidentes.detalle("demo-090"))
    }

    @Test
    fun registrarseIniciaSesionYCerrarSesionLaTermina() = runTest {
        assertEquals(ResultadoRegistro.SESION_INICIADA, auth.registrar("Ana", "ana@unmsm.edu.pe", "secreto123", "2026-10"))
        assertEquals("Ana", assertIs<EstadoSesion.Activa>(auth.sesion.value).usuario.nombre)

        auth.cerrarSesion()

        assertEquals(EstadoSesion.SinSesion, auth.sesion.value)
    }

    @Test
    fun elRegistroPuedePedirConfirmarElCorreo() = runTest {
        assertEquals(ResultadoRegistro.CONFIRMAR_CORREO, auth.registrar("Ana", "confirmar@unmsm.edu.pe", "secreto123", "2026-10"))
        assertEquals(EstadoSesion.SinSesion, auth.sesion.value)
    }

    @Test
    fun apoyarYRetirarElApoyoCambiaElContador() = runTest {
        auth.iniciarSesion("ana@unmsm.edu.pe", "secreto123")
        val antes = incidentes.detalle("demo-124")!!.incidente.apoyos

        incidentes.apoyar("c-1", "demo-124")
        incidentes.apoyar("c-2", "demo-124") // segundo toque: no duplica
        assertEquals(antes + 1, incidentes.detalle("demo-124")!!.incidente.apoyos)
        assertTrue(incidentes.detalle("demo-124")!!.yoApoyo)

        incidentes.retirarApoyo("demo-124")
        assertEquals(antes, incidentes.detalle("demo-124")!!.incidente.apoyos)
        assertFalse(incidentes.detalle("demo-124")!!.yoApoyo)
    }

    @Test
    fun noSeApoyaLoPropioNiLoCerrado() = runTest {
        auth.iniciarSesion("ana@unmsm.edu.pe", "secreto123")
        val reporte = FakeReporteRepository(demo).crear(
            pe.civika.shared.domain.ReporteNuevo("c-9", -12.0575, -77.0845, CategoriasPiloto.VIAL.id, "Bache profundo en la pista", listOf("u/f.jpg")),
        )

        assertFailsWith<ErrorCivika.DatosInvalidos> { incidentes.apoyar("c-3", reporte.incidenteId) }
        assertFailsWith<ErrorCivika.DatosInvalidos> { incidentes.apoyar("c-4", "demo-087") } // propio y resuelto
        assertEquals(Relacion.AUTOR, incidentes.misIncidentes().first { it.incidente.id == reporte.incidenteId }.relacion)
    }

    @Test
    fun buscarDuplicadosSoloDevuelveLosDeLaMismaCategoriaDentroDelRadio() = runTest {
        auth.iniciarSesion("ana@unmsm.edu.pe", "secreto123")

        val cerca = incidentes.buscarDuplicados(-12.05121, -77.08531, CategoriasPiloto.VIAL.id)
        val otraCategoria = incidentes.buscarDuplicados(-12.05121, -77.08531, CategoriasPiloto.RESIDUOS.id)
        val lejos = incidentes.buscarDuplicados(-12.0600, -77.0800, CategoriasPiloto.VIAL.id)

        assertEquals(listOf("CVK-000124"), cerca.map { it.codigo })
        assertTrue(otraCategoria.isEmpty())
        assertTrue(lejos.isEmpty())
    }

    @Test
    fun marcarLeidasBajaElContadorDeLaBandeja() = runTest {
        auth.iniciarSesion("ana@unmsm.edu.pe", "secreto123")
        val bandeja = FakeNotificacionRepository(demo)
        assertEquals(2, bandeja.listar().count { !it.leida })

        bandeja.marcarLeida("n-1")
        assertEquals(1, bandeja.listar().count { !it.leida })

        bandeja.marcarTodasLeidas()
        assertEquals(0, bandeja.listar().count { !it.leida })
    }

    @Test
    fun laColaSoloMuestraLosEnviosDelUsuario() = runTest {
        val cola = FakeColaReportesRepository()
        fun envio(id: String, usuario: String) = ReportePendiente(id, usuario, TipoEnvio.NUEVO, null, 0.0, 0.0, 1, "Vial", "Bache profundo", emptyList())
        cola.encolar(envio("a", "u-1"))
        cola.encolar(envio("b", "u-2"))

        assertEquals(listOf("a"), cola.observar("u-1").first().map { it.clienteId })

        cola.actualizar("a") { it.copy(estado = EstadoEnvio.ERROR) }
        cola.reintentar("a")
        assertEquals(EstadoEnvio.PENDIENTE, cola.observarUno("a").first()?.estado)

        cola.descartar("a")
        assertNull(cola.observarUno("a").first())
    }
}

/** Koin elige los repositorios reales o los de ejemplo según haya configuración de Supabase. */
class SharedModuleTest {

    @Test
    fun sinConfiguracionResuelveElFake() {
        val koin = koinApplication { modules(sharedModule) }.koin

        assertIs<FakeIncidenteRepository>(koin.get<IncidenteRepository>())
        assertIs<FakeAuthRepository>(koin.get<AuthRepository>())
        assertEquals(ModoApp.DEMOSTRACION, koin.get<ModoApp>())
    }

    @Test
    fun conConfiguracionResuelveElRepositorioReal() {
        val koin = koinApplication { modules(crearSharedModule(SupabaseConfig("http://127.0.0.1:54321", "clave"))) }.koin

        // Solo se comprueba el tipo: no se hace ninguna llamada de red.
        assertIs<SupabaseIncidenteRepository>(koin.get<IncidenteRepository>())
        assertIs<SupabaseReporteRepository>(koin.get<ReporteRepository>())
        assertIs<NominatimGeocodificador>(koin.get<Geocodificador>())
        assertEquals(ModoApp.REAL, koin.get<ModoApp>())
    }

    @Test
    fun enModoDemostracionLosRepositoriosCompartenLosMismosDatos() = runTest {
        val koin = koinApplication { modules(sharedModule) }.koin
        val usuario = Usuario("x", "Ana", "ana@unmsm.edu.pe")

        koin.get<AuthRepository>().iniciarSesion(usuario.correo, "secreto123")

        // Si cada Fake tuviera sus propios datos, «mis reportes» fallaría por falta de sesión.
        assertTrue(koin.get<IncidenteRepository>().misIncidentes().isNotEmpty())
    }
}
