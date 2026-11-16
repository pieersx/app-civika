package pe.civika.app.feature.incidente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Validaciones
import java.util.UUID

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState

    /** No existe o no es visible (por ejemplo, un rechazado que no es tuyo). */
    data object NoEncontrado : DetalleUiState

    data class Error(val error: ErrorCivika) : DetalleUiState

    data class Listo(val detalle: DetalleIncidente, val ocupado: Boolean = false) : DetalleUiState
}

/** Avisos de una sola vez: la pantalla los muestra en un snackbar o abre el inicio de sesión. */
sealed interface EventoDetalle {
    /** Apoyar, verificar o denunciar piden una cuenta. */
    data object RequiereSesion : EventoDetalle

    data class Aviso(val aviso: AvisoDetalle) : EventoDetalle

    data class ErrorAccion(val error: ErrorCivika) : EventoDetalle
}

enum class AvisoDetalle { APOYO_AGREGADO, APOYO_RETIRADO, VERIFICACION_ENVIADA, REABIERTO, DENUNCIA_ENVIADA, DENUNCIA_A_REVISION }

class DetalleViewModel(
    private val incidenteId: String,
    private val incidentes: IncidenteRepository,
    private val auth: AuthRepository,
    private val nuevoId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val _estado = MutableStateFlow<DetalleUiState>(DetalleUiState.Cargando)
    val estado: StateFlow<DetalleUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EventoDetalle>(Channel.BUFFERED)
    val eventos: Flow<EventoDetalle> = _eventos.receiveAsFlow()

    init {
        cargar()
    }

    fun cargar() {
        if (_estado.value !is DetalleUiState.Listo) _estado.value = DetalleUiState.Cargando
        viewModelScope.launch { recargar() }
    }

    private suspend fun recargar() {
        try {
            val detalle = incidentes.detalle(incidenteId)
            _estado.value = if (detalle == null) DetalleUiState.NoEncontrado else DetalleUiState.Listo(detalle)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorCivika) {
            // Si ya había datos en pantalla se conservan: un fallo al refrescar no debe vaciarla.
            if (_estado.value !is DetalleUiState.Listo) _estado.value = DetalleUiState.Error(e)
        } catch (e: Exception) {
            if (_estado.value !is DetalleUiState.Listo) _estado.value = DetalleUiState.Error(ErrorCivika.Desconocido(e))
        }
    }

    /** «Apoyar» / «Ya apoyas»: tocarlo otra vez retira el apoyo. */
    fun alternarApoyo() {
        val actual = _estado.value as? DetalleUiState.Listo ?: return
        if (actual.ocupado) return
        if (!haySesion()) return avisar(EventoDetalle.RequiereSesion)
        accion {
            if (actual.detalle.yoApoyo) {
                incidentes.retirarApoyo(incidenteId)
                EventoDetalle.Aviso(AvisoDetalle.APOYO_RETIRADO)
            } else {
                incidentes.apoyar(nuevoId(), incidenteId)
                EventoDetalle.Aviso(AvisoDetalle.APOYO_AGREGADO)
            }
        }
    }

    /** «¿Sigue arreglado?». Con tres «sigue igual» el servidor reabre el incidente. */
    fun verificar(sigueResuelto: Boolean) {
        val actual = _estado.value as? DetalleUiState.Listo ?: return
        if (actual.ocupado) return
        if (!haySesion()) return avisar(EventoDetalle.RequiereSesion)
        accion {
            val resultado = incidentes.verificarResolucion(incidenteId, sigueResuelto)
            EventoDetalle.Aviso(if (resultado == Estado.RESUELTO) AvisoDetalle.VERIFICACION_ENVIADA else AvisoDetalle.REABIERTO)
        }
    }

    fun denunciar(motivo: String) {
        val actual = _estado.value as? DetalleUiState.Listo ?: return
        if (actual.ocupado || !Validaciones.motivoDenuncia(motivo).esValida) return
        if (!haySesion()) return avisar(EventoDetalle.RequiereSesion)
        accion {
            val aRevision = incidentes.denunciar(incidenteId, motivo)
            EventoDetalle.Aviso(if (aRevision) AvisoDetalle.DENUNCIA_A_REVISION else AvisoDetalle.DENUNCIA_ENVIADA)
        }
    }

    private fun haySesion() = auth.sesion.value is EstadoSesion.Activa

    private fun avisar(evento: EventoDetalle) {
        _eventos.trySend(evento)
    }

    private fun accion(bloque: suspend () -> EventoDetalle) {
        _estado.update { if (it is DetalleUiState.Listo) it.copy(ocupado = true) else it }
        viewModelScope.launch {
            try {
                val evento = bloque()
                recargar()
                avisar(evento)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorCivika) {
                avisar(EventoDetalle.ErrorAccion(e))
            } catch (e: Exception) {
                avisar(EventoDetalle.ErrorAccion(ErrorCivika.Desconocido(e)))
            } finally {
                _estado.update { if (it is DetalleUiState.Listo) it.copy(ocupado = false) else it }
            }
        }
    }
}
