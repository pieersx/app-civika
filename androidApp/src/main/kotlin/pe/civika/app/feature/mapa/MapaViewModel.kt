package pe.civika.app.feature.mapa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.FiltroMapa
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.filtrar

data class MapaUiState(
    val cargando: Boolean = true,
    val error: ErrorCivika? = null,
    val todos: List<Incidente> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val filtro: FiltroMapa = FiltroMapa(),
    val seleccionadoId: String? = null,
    /** Alternativa accesible al mapa: los mismos incidentes como lista. */
    val verComoLista: Boolean = false,
) {
    val visibles: List<Incidente> get() = filtrar(todos, filtro)
    val seleccionado: Incidente? get() = visibles.firstOrNull { it.id == seleccionadoId }
}

class MapaViewModel(private val incidentes: IncidenteRepository) : ViewModel() {

    private val _estado = MutableStateFlow(MapaUiState())
    val estado: StateFlow<MapaUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val lista = incidentes.listar()
                val categorias = runCatching { incidentes.categorias() }.getOrDefault(emptyList())
                _estado.update { it.copy(cargando = false, todos = lista, categorias = categorias) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorCivika) {
                _estado.update { it.copy(cargando = false, error = e) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = ErrorCivika.Desconocido(e)) }
            }
        }
    }

    fun alternarCategoria(serviceCode: String) = _estado.update { it.copy(filtro = it.filtro.conCategoria(serviceCode)) }

    fun alternarFamilia(familia: Familia) = _estado.update { it.copy(filtro = it.filtro.conFamilia(familia)) }

    fun limpiarFiltros() = _estado.update { it.copy(filtro = FiltroMapa()) }

    fun seleccionar(incidenteId: String?) = _estado.update { it.copy(seleccionadoId = incidenteId) }

    fun alternarLista() = _estado.update { it.copy(verComoLista = !it.verComoLista) }
}
