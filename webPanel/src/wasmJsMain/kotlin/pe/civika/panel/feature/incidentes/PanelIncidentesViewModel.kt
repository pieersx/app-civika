package pe.civika.panel.feature.incidentes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.ObtenerIncidentesPriorizados

sealed interface PanelIncidentesUiState {
    data object Cargando : PanelIncidentesUiState
    data object Vacio : PanelIncidentesUiState
    data class Listo(val incidentes: List<Incidente>) : PanelIncidentesUiState
    data class Error(val mensaje: String) : PanelIncidentesUiState
}

class PanelIncidentesViewModel(
    private val obtenerIncidentes: ObtenerIncidentesPriorizados,
) : ViewModel() {

    private val _estado = MutableStateFlow<PanelIncidentesUiState>(PanelIncidentesUiState.Cargando)
    val estado: StateFlow<PanelIncidentesUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.value = try {
                val incidentes = obtenerIncidentes()
                if (incidentes.isEmpty()) PanelIncidentesUiState.Vacio else PanelIncidentesUiState.Listo(incidentes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PanelIncidentesUiState.Error("No se pudieron cargar los incidentes. Recarga la página para intentarlo de nuevo.")
            }
        }
    }
}
