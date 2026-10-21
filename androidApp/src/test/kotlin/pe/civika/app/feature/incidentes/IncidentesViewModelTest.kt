package pe.civika.app.feature.incidentes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.civika.shared.data.FakeIncidenteRepository
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.ObtenerIncidentesPriorizados
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelCon(repositorio: IncidenteRepository) =
        IncidentesViewModel(ObtenerIncidentesPriorizados(repositorio))

    @Test
    fun empiezaCargandoYLuegoMuestraLosIncidentesPriorizados() = runTest(dispatcher) {
        val viewModel = viewModelCon(FakeIncidenteRepository())
        assertEquals(IncidentesUiState.Cargando, viewModel.estado.value)

        advanceUntilIdle()

        val listo = assertIs<IncidentesUiState.Listo>(viewModel.estado.value)
        assertEquals(listo.incidentes.sortedByDescending { it.prioridad }, listo.incidentes)
    }

    @Test
    fun sinIncidentesMuestraElEstadoVacio() = runTest(dispatcher) {
        val viewModel = viewModelCon(object : IncidenteRepository {
            override suspend fun listar(): List<Incidente> = emptyList()
        })

        advanceUntilIdle()

        assertEquals(IncidentesUiState.Vacio, viewModel.estado.value)
    }

    @Test
    fun siElRepositorioFallaMuestraUnErrorEnVezDeCerrarLaApp() = runTest(dispatcher) {
        val viewModel = viewModelCon(object : IncidenteRepository {
            override suspend fun listar(): List<Incidente> = error("sin conexión")
        })

        advanceUntilIdle()

        assertIs<IncidentesUiState.Error>(viewModel.estado.value)
    }
}
