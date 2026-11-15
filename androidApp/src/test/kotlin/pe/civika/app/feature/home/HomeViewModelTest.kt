package pe.civika.app.feature.home

import kotlinx.coroutines.test.runTest
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Incidente
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeViewModelTest : PruebaViewModel() {

    @Test
    fun conSesionMuestraSaludoResumenYNoLeidas() = runTest {
        val mundo = Mundo()

        val listo = assertIs<HomeUiState.Listo>(
            HomeViewModel(mundo.incidentes, mundo.notificaciones, mundo.auth).estado.value,
        )

        assertEquals("Ana Pérez", listo.nombre)
        assertEquals(2, listo.noLeidas)
        assertTrue(listo.resumen!!.abiertos > 0)
    }

    @Test
    fun losPrioritariosSalenDeMayorAMenorSegunLaFormulaYSinCerrados() = runTest {
        val mundo = Mundo()

        val listo = assertIs<HomeUiState.Listo>(
            HomeViewModel(mundo.incidentes, mundo.notificaciones, mundo.auth).estado.value,
        )

        assertEquals(listo.prioritarios.sortedByDescending { it.prioridad }, listo.prioritarios)
        assertTrue(listo.prioritarios.none { it.estado == Estado.RESUELTO || it.estado == Estado.RECHAZADO })
    }

    @Test
    fun sinSesionSePuedeExplorarConSaludoGenericoYSinContador() = runTest {
        val mundo = Mundo(sesion = false)

        val listo = assertIs<HomeUiState.Listo>(
            HomeViewModel(mundo.incidentes, mundo.notificaciones, mundo.auth).estado.value,
        )

        assertNull(listo.nombre)
        assertEquals(0, listo.noLeidas)
        assertTrue(listo.prioritarios.isNotEmpty())
    }

    @Test
    fun siFallaElListadoMuestraUnErrorEnVezDeCerrarLaApp() = runTest {
        val mundo = Mundo()
        val sinRed = object : IncidenteRepository by mundo.incidentes {
            override suspend fun listar(): List<Incidente> = throw ErrorCivika.SinConexion()
        }

        val estado = HomeViewModel(sinRed, mundo.notificaciones, mundo.auth).estado.value

        assertIs<ErrorCivika.SinConexion>(assertIs<HomeUiState.Error>(estado).error)
    }

    @Test
    fun siSoloFallaElResumenLaPantallaSeMuestraIgual() = runTest {
        val mundo = Mundo()
        val sinResumen = object : IncidenteRepository by mundo.incidentes {
            override suspend fun resumen() = throw ErrorCivika.SinConexion()
        }

        val listo = assertIs<HomeUiState.Listo>(HomeViewModel(sinResumen, mundo.notificaciones, mundo.auth).estado.value)

        assertNull(listo.resumen)
    }

    @Test
    fun refrescarVuelveALeerLosDatosYBajaElIndicador() = runTest {
        val mundo = Mundo()
        val vm = HomeViewModel(mundo.incidentes, mundo.notificaciones, mundo.auth)
        mundo.notificaciones.marcarTodasLeidas()

        vm.refrescar()

        val listo = assertIs<HomeUiState.Listo>(vm.estado.value)
        assertEquals(0, listo.noLeidas)
        assertEquals(false, listo.refrescando)
    }
}
