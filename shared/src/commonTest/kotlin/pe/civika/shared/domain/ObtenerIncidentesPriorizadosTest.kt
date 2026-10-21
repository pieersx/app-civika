package pe.civika.shared.domain

import kotlinx.coroutines.test.runTest
import pe.civika.shared.data.FakeIncidenteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObtenerIncidentesPriorizadosTest {

    @Test
    fun devuelveLosIncidentesDeMayorAMenorPrioridad() = runTest {
        val incidentes = ObtenerIncidentesPriorizados(FakeIncidenteRepository())()

        assertTrue(incidentes.isNotEmpty())
        assertEquals(incidentes.sortedByDescending { it.prioridad }, incidentes)
    }

    @Test
    fun noIncluyeIncidentesRechazados() = runTest {
        val incidentes = ObtenerIncidentesPriorizados(FakeIncidenteRepository())()

        assertTrue(incidentes.none { it.estado == Estado.RECHAZADO })
    }

    @Test
    fun noIncluyeIncidentesResueltosPorqueYaNoHayQueAtenderlos() = runTest {
        val incidentes = ObtenerIncidentesPriorizados(FakeIncidenteRepository())()

        assertTrue(incidentes.none { it.estado == Estado.RESUELTO })
    }
}
