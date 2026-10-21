package pe.civika.app.feature.incidentes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.ObtenerIncidentesPriorizados

sealed interface IncidentesUiState {
    data object Cargando : IncidentesUiState
    data object Vacio : IncidentesUiState
    data class Listo(val incidentes: List<Incidente>) : IncidentesUiState
    data class Error(val mensaje: String) : IncidentesUiState
}

class IncidentesViewModel(
    private val obtenerIncidentes: ObtenerIncidentesPriorizados,
) : ViewModel() {

    private val _estado = MutableStateFlow<IncidentesUiState>(IncidentesUiState.Cargando)
    val estado: StateFlow<IncidentesUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            _estado.value = try {
                val incidentes = obtenerIncidentes()
                if (incidentes.isEmpty()) IncidentesUiState.Vacio else IncidentesUiState.Listo(incidentes)
            } catch (e: CancellationException) {
                throw e // cancelar la corrutina no es un error que haya que mostrar
            } catch (e: Exception) {
                IncidentesUiState.Error("No pudimos cargar los incidentes. Revisa tu conexión e inténtalo de nuevo.")
            }
        }
    }
}
