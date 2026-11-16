package pe.civika.app.feature.incidente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.app.device.AlmacenFotos
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.ColaReportesRepository
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.FiltroMisReportes
import pe.civika.shared.domain.MiIncidente
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.aplica
import pe.civika.shared.domain.usuario

/** Una fila de «Mis reportes»: un envío que aún no llegó (cola local) o un incidente del servidor. */
sealed interface ItemMiReporte {
    data class Local(val envio: ReportePendiente) : ItemMiReporte

    data class Remoto(val mi: MiIncidente) : ItemMiReporte
}

sealed interface MisReportesUiState {
    data object Cargando : MisReportesUiState

    /** Hay que iniciar sesión para ver «Mis reportes». */
    data object SinSesion : MisReportesUiState

    data class Error(val error: ErrorCivika) : MisReportesUiState

    data class Listo(
        val items: List<ItemMiReporte>,
        val filtro: FiltroMisReportes,
        /** Cuántos hay en total sin filtrar: distingue «no tienes reportes» de «ninguno cumple el filtro». */
        val total: Int,
        val refrescando: Boolean = false,
    ) : MisReportesUiState
}

private sealed interface Remotos {
    data object Cargando : Remotos
    data class Error(val error: ErrorCivika) : Remotos
    data class Listo(val lista: List<MiIncidente>) : Remotos
}

@OptIn(ExperimentalCoroutinesApi::class)
class MisReportesViewModel(
    private val incidentes: IncidenteRepository,
    private val cola: ColaReportesRepository,
    private val auth: AuthRepository,
    private val fotos: AlmacenFotos,
) : ViewModel() {

    private val remotos = MutableStateFlow<Remotos>(Remotos.Cargando)
    private val filtro = MutableStateFlow(FiltroMisReportes.TODOS)
    private val refrescando = MutableStateFlow(false)

    val estado: StateFlow<MisReportesUiState> = auth.sesion
        .flatMapLatest { sesion ->
            when (sesion) {
                EstadoSesion.Cargando -> flowOf(MisReportesUiState.Cargando)
                EstadoSesion.SinSesion -> flowOf(MisReportesUiState.SinSesion)
                is EstadoSesion.Activa -> combine(cola.observar(sesion.usuario.id), remotos, filtro, refrescando) { locales, r, f, ref ->
                    armar(locales, r, f, ref)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisReportesUiState.Cargando)

    init {
        viewModelScope.launch {
            auth.sesion
                .filter { it !is EstadoSesion.Cargando }
                .map { it.usuario?.id }
                .distinctUntilChanged()
                .collectLatest { id ->
                    if (id == null) return@collectLatest
                    remotos.value = Remotos.Cargando
                    cargar()
                    // Cuando un envío de la cola llega al servidor, la lista del servidor se actualiza sola.
                    cola.observar(id)
                        .map { lista -> lista.filter { it.estado == EstadoEnvio.ENVIADO }.map { it.clienteId }.toSet() }
                        .distinctUntilChanged()
                        .drop(1)
                        .collect { cargar() }
                }
        }
    }

    fun refrescar() {
        refrescando.value = true
        viewModelScope.launch {
            cargar()
            refrescando.value = false
        }
    }

    fun cambiarFiltro(nuevo: FiltroMisReportes) {
        filtro.value = nuevo
    }

    fun reintentar(clienteId: String) {
        viewModelScope.launch { cola.reintentar(clienteId) }
    }

    /** Descarta un envío que falló y borra sus fotos. */
    fun descartar(clienteId: String) {
        viewModelScope.launch {
            cola.descartar(clienteId)
            fotos.borrarCarpeta(clienteId)
        }
    }

    private suspend fun cargar() {
        try {
            remotos.value = Remotos.Listo(incidentes.misIncidentes())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ErrorCivika) {
            // Si ya había una lista se conserva; solo se muestra el error cuando no hay nada que enseñar.
            if (remotos.value !is Remotos.Listo) remotos.value = Remotos.Error(e)
        } catch (e: Exception) {
            if (remotos.value !is Remotos.Listo) remotos.value = Remotos.Error(ErrorCivika.Desconocido(e))
        }
    }

    private fun armar(
        locales: List<ReportePendiente>,
        remotos: Remotos,
        filtro: FiltroMisReportes,
        refrescando: Boolean,
    ): MisReportesUiState {
        val lista = when (remotos) {
            Remotos.Cargando -> if (locales.isEmpty()) return MisReportesUiState.Cargando else emptyList()
            is Remotos.Error -> if (locales.isEmpty()) return MisReportesUiState.Error(remotos.error) else emptyList()
            is Remotos.Listo -> remotos.lista
        }
        val idsRemotos = lista.map { it.incidente.id }.toSet()
        // Un envío ya entregado se oculta cuando su incidente aparece en la lista del servidor.
        val visiblesLocales = locales
            .filterNot { it.estado == EstadoEnvio.ENVIADO && (it.incidenteCreadoId in idsRemotos) }
            .sortedByDescending { it.creadoEnMs }
        val total = visiblesLocales.size + lista.size
        val items = visiblesLocales.filter { filtro.aplica(it) }.map { ItemMiReporte.Local(it) } +
            lista.filter { filtro.aplica(it) }.map { ItemMiReporte.Remoto(it) }
        return MisReportesUiState.Listo(items, filtro, total, refrescando)
    }
}
