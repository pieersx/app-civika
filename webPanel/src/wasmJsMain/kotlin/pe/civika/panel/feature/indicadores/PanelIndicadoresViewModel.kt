package pe.civika.panel.feature.indicadores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.Incidente

data class PanelIndicadoresUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val periodo: PeriodoIndicadores = PeriodoIndicadores.MES,
    val indicadores: Indicadores? = null,
) {
    val sinDatos: Boolean get() = !cargando && error == null && (indicadores?.recibidos == 0)
}

/**
 * Indicadores del distrito (RF-14): se calculan solo con los reportes hechos en Cívika,
 * como dice el mockup, y se recalculan al cambiar el periodo del selector superior.
 */
class PanelIndicadoresViewModel(
    private val repositorio: IncidenteRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelIndicadoresUiState())
    val estado: StateFlow<PanelIndicadoresUiState> = _estado.asStateFlow()

    private var incidentes: List<Incidente> = emptyList()

    init {
        viewModelScope.launch {
            try {
                incidentes = repositorio.listar()
                _estado.value = PanelIndicadoresUiState(
                    cargando = false,
                    periodo = _estado.value.periodo,
                    indicadores = calcularIndicadores(incidentes, _estado.value.periodo),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value = PanelIndicadoresUiState(
                    cargando = false,
                    error = "No se pudieron calcular los indicadores. Recarga la página para intentarlo de nuevo.",
                )
            }
        }
    }

    fun cambiarPeriodo(periodo: PeriodoIndicadores) {
        if (periodo == _estado.value.periodo) return
        _estado.value = _estado.value.copy(
            periodo = periodo,
            indicadores = if (_estado.value.cargando) null else calcularIndicadores(incidentes, periodo),
        )
    }
}
