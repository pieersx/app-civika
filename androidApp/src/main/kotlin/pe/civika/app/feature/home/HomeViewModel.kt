package pe.civika.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.data.NotificacionRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.ResumenPublico
import pe.civika.shared.domain.priorizar
import pe.civika.shared.domain.usuario

sealed interface HomeUiState {
    data object Cargando : HomeUiState

    data class Error(val error: ErrorCivika) : HomeUiState

    data class Listo(
        /** `null` si no hay sesión: el saludo es genérico. */
        val nombre: String?,
        val resumen: ResumenPublico?,
        /** Los más urgentes según la fórmula de `shared/domain`, no según el campo `prioridad` del servidor. */
        val prioritarios: List<Incidente>,
        val noLeidas: Int,
        val refrescando: Boolean = false,
    ) : HomeUiState
}

class HomeViewModel(
    private val incidentes: IncidenteRepository,
    private val notificaciones: NotificacionRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<HomeUiState>(HomeUiState.Cargando)
    val estado: StateFlow<HomeUiState> = _estado.asStateFlow()

    init {
        // Se vuelve a cargar al entrar o salir de una cuenta: cambian el saludo y el contador de no leídas.
        viewModelScope.launch {
            auth.sesion
                .filter { it !is EstadoSesion.Cargando }
                .map { it.usuario?.id }
                .distinctUntilChanged()
                .collectLatest { cargar(mostrarEsqueleto = true) }
        }
    }

    fun refrescar() {
        _estado.update { if (it is HomeUiState.Listo) it.copy(refrescando = true) else it }
        viewModelScope.launch { cargar(mostrarEsqueleto = _estado.value !is HomeUiState.Listo) }
    }

    private suspend fun cargar(mostrarEsqueleto: Boolean) {
        if (mostrarEsqueleto) _estado.value = HomeUiState.Cargando
        val usuario = auth.sesion.value.usuario
        try {
            coroutineScope {
                val lista = async { incidentes.listar() }
                // El resumen y el contador son un extra: si fallan, la pantalla se muestra igual.
                val resumen = async { runCatchingSinCancelar { incidentes.resumen() } }
                val noLeidas = async {
                    if (usuario == null) 0 else runCatchingSinCancelar { notificaciones.listar().count { !it.leida } } ?: 0
                }
                _estado.value = HomeUiState.Listo(
                    nombre = usuario?.nombre,
                    resumen = resumen.await(),
                    prioritarios = priorizar(lista.await()).take(MAX_PRIORITARIOS),
                    noLeidas = noLeidas.await(),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorCivika) {
            _estado.value = HomeUiState.Error(e)
        } catch (e: Exception) {
            _estado.value = HomeUiState.Error(ErrorCivika.Desconocido(e))
        }
    }

    private suspend fun <T> runCatchingSinCancelar(bloque: suspend () -> T): T? = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val MAX_PRIORITARIOS = 8
    }
}
