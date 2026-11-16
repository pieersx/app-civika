package pe.civika.app.feature.incidente

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.FiltroMisReportes
import pe.civika.shared.domain.MiIncidente
import pe.civika.shared.domain.Relacion
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.TipoEnvio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MisReportesViewModelTest : PruebaViewModel() {

    private fun vm(mundo: Mundo) = MisReportesViewModel(mundo.incidentes, mundo.cola, mundo.auth, mundo.fotos)

    private suspend fun MisReportesViewModel.listo(): MisReportesUiState.Listo =
        assertIs<MisReportesUiState.Listo>(estado.first { it is MisReportesUiState.Listo })

    private fun envio(id: String, usuario: String, estado: EstadoEnvio = EstadoEnvio.PENDIENTE, tipo: TipoEnvio = TipoEnvio.NUEVO) =
        ReportePendiente(id, usuario, tipo, null, -12.0575, -77.0845, 1, "Pistas y veredas", "Bache profundo", listOf("/f.jpg"), estado = estado, creadoEnMs = 1)

    @Test
    fun sinSesionPideIniciarSesion() = runTest {
        val modelo = vm(Mundo(sesion = false))

        assertIs<MisReportesUiState.SinSesion>(modelo.estado.primeroEstado())
    }

    @Test
    fun muestraLosReportesYApoyosDelServidor() = runTest {
        val listo = vm(Mundo()).listo()

        assertEquals(setOf("demo-087", "demo-109"), listo.items.map { (it as ItemMiReporte.Remoto).mi.incidente.id }.toSet())
        assertEquals(2, listo.total)
    }

    @Test
    fun losEnviosPendientesVanPrimeroYSeMuestranComoLocales() = runTest {
        val mundo = Mundo()
        mundo.cola.encolar(envio("c-1", mundo.usuario.id))
        mundo.cola.encolar(envio("c-otro", "otro-usuario"))

        val listo = vm(mundo).listo()

        val primero = assertIs<ItemMiReporte.Local>(listo.items.first())
        assertEquals("c-1", primero.envio.clienteId)
        assertEquals(1, listo.items.count { it is ItemMiReporte.Local })
        assertEquals(3, listo.total)
    }

    @Test
    fun unEnvioEnviadoSeOcultaCuandoSuIncidenteYaVieneDelServidor() = runTest {
        val mundo = Mundo()
        mundo.cola.encolar(envio("c-1", mundo.usuario.id, EstadoEnvio.ENVIADO).copy(incidenteCreadoId = "demo-087"))

        val listo = vm(mundo).listo()

        assertTrue(listo.items.none { it is ItemMiReporte.Local })
    }

    @Test
    fun losFiltrosSeparanAbiertosResueltosYApoyos() = runTest {
        val modelo = vm(Mundo())
        modelo.listo()

        modelo.cambiarFiltro(FiltroMisReportes.RESUELTOS)
        var listo = modelo.listo()
        assertEquals(listOf("demo-087"), listo.items.map { (it as ItemMiReporte.Remoto).mi.incidente.id })

        modelo.cambiarFiltro(FiltroMisReportes.MIS_APOYOS)
        listo = modelo.listo()
        assertTrue(listo.items.all { (it as ItemMiReporte.Remoto).mi.relacion == Relacion.APOYO })
        assertEquals(2, listo.total) // el total no depende del filtro
    }

    @Test
    fun reintentarYDescartarActuanSobreLaCola() = runTest {
        val mundo = Mundo()
        mundo.cola.encolar(envio("c-1", mundo.usuario.id, EstadoEnvio.ERROR))
        val modelo = vm(mundo)
        modelo.listo()

        modelo.reintentar("c-1")
        assertEquals(EstadoEnvio.PENDIENTE, mundo.cola.todos().single().estado)

        modelo.descartar("c-1")
        assertTrue(mundo.cola.todos().isEmpty())
    }

    @Test
    fun siFallaElServidorPeroHayEnviosLocalesSeMuestranIgual() = runTest {
        val mundo = Mundo()
        mundo.cola.encolar(envio("c-1", mundo.usuario.id))
        val sinRed = object : IncidenteRepository by mundo.incidentes {
            override suspend fun misIncidentes(): List<MiIncidente> = throw ErrorCivika.SinConexion()
        }

        val listo = MisReportesViewModel(sinRed, mundo.cola, mundo.auth, mundo.fotos).listo()

        assertEquals(1, listo.items.size)
        assertIs<ItemMiReporte.Local>(listo.items.single())
    }

    @Test
    fun siFallaElServidorYNoHayNadaLocalMuestraElError() = runTest {
        val mundo = Mundo()
        val sinRed = object : IncidenteRepository by mundo.incidentes {
            override suspend fun misIncidentes(): List<MiIncidente> = throw ErrorCivika.SinConexion()
        }

        val estado = MisReportesViewModel(sinRed, mundo.cola, mundo.auth, mundo.fotos).estado.primeroEstado()

        assertIs<MisReportesUiState.Error>(estado)
    }

    private suspend fun StateFlow<MisReportesUiState>.primeroEstado(): MisReportesUiState =
        first { it !is MisReportesUiState.Cargando }
}
