package pe.civika.app.feature.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.NotificacionRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Notificacion

sealed interface NotificacionesUiState {
    data object Cargando : NotificacionesUiState

    data object SinSesion : NotificacionesUiState

    data class Error(val error: ErrorCivika) : NotificacionesUiState

    data class Listo(val items: List<Notificacion>, val refrescando: Boolean = false) : NotificacionesUiState {
        val noLeidas: Int get() = items.count { !it.leida }
    }
}

class NotificacionesViewModel(
    private val notificaciones: NotificacionRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow<NotificacionesUiState>(NotificacionesUiState.Cargando)
    val estado: StateFlow<NotificacionesUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        if (auth.sesion.value !is EstadoSesion.Activa) {
            _estado.value = NotificacionesUiState.SinSesion
            return
        }
        if (_estado.value !is NotificacionesUiState.Listo) _estado.value = NotificacionesUiState.Cargando
        viewModelScope.launch { recargar() }
    }

    fun refrescar() {
        _estado.update { if (it is NotificacionesUiState.Listo) it.copy(refrescando = true) else it }
        viewModelScope.launch { recargar() }
    }

    private suspend fun recargar() {
        try {
            _estado.value = NotificacionesUiState.Listo(notificaciones.listar())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorCivika) {
            _estado.update { if (it is NotificacionesUiState.Listo) it.copy(refrescando = false) else NotificacionesUiState.Error(e) }
        } catch (e: Exception) {
            _estado.update {
                if (it is NotificacionesUiState.Listo) it.copy(refrescando = false) else NotificacionesUiState.Error(ErrorCivika.Desconocido(e))
            }
        }
    }

    /** Al tocar un aviso se marca como leído (se ve al instante) y la pantalla abre su incidente. */
    fun marcarLeida(id: String) {
        _estado.update { actual ->
            if (actual is NotificacionesUiState.Listo) {
                actual.copy(items = actual.items.map { if (it.id == id) it.copy(leida = true) else it })
            } else {
                actual
            }
        }
        viewModelScope.launch { runCatchingSinCancelar { notificaciones.marcarLeida(id) } }
    }

    fun marcarTodasLeidas() {
        _estado.update { actual ->
            if (actual is NotificacionesUiState.Listo) actual.copy(items = actual.items.map { it.copy(leida = true) }) else actual
        }
        viewModelScope.launch { runCatchingSinCancelar { notificaciones.marcarTodasLeidas() } }
    }

    private suspend fun runCatchingSinCancelar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Marcar como leído es cosmético: si falla, el aviso sigue sin leer en el servidor y reaparece al refrescar.
        }
    }
}
