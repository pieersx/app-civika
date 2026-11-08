package pe.civika.panel.feature.moderacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.civika.shared.data.DenunciaRepository
import pe.civika.shared.data.SesionRepository
import pe.civika.shared.domain.Denuncia
import pe.civika.shared.domain.Rol

/**
 * Estado de la cola de moderación (RF-16): las denuncias pendientes, el aviso de la última
 * resolución y cuántas quedan para el contador de la barra lateral.
 */
data class PanelModeracionUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val denuncias: List<Denuncia> = emptyList(),
    val aviso: String? = null,
    val avisoError: Boolean = false,
) {
    val pendientes: Int get() = denuncias.size

    val vacia: Boolean get() = !cargando && error == null && denuncias.isEmpty()
}

class PanelModeracionViewModel(
    private val repositorio: DenunciaRepository,
    private val sesion: SesionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelModeracionUiState())
    val estado: StateFlow<PanelModeracionUiState> = _estado.asStateFlow()

    /** Rol de la sesión activa: decide si los botones de resolución están habilitados. */
    val rol: Rol
        get() = sesion.sesion.value?.rol ?: Rol.ENTIDAD

    init {
        viewModelScope.launch {
            _estado.value = try {
                PanelModeracionUiState(
                    cargando = false,
                    denuncias = repositorio.listarPendientes(),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PanelModeracionUiState(
                    cargando = false,
                    error = "No se pudo cargar la cola de moderación. Recarga la página para intentarlo de nuevo.",
                )
            }
        }
    }

    /** Resuelve una denuncia y la saca de la cola. El servidor validará lo mismo en el Sprint 4. */
    fun resolver(id: String, accion: AccionModeracion) {
        val actual = _estado.value
        val denuncia = actual.denuncias.firstOrNull { it.id == id } ?: return
        if (!puedeModerar(rol)) {
            _estado.value = actual.copy(
                aviso = "Tu rol de entidad no resuelve denuncias. Entra con la cuenta de la moderación para atender esta cola.",
                avisoError = true,
            )
            return
        }
        _estado.value = actual.copy(
            denuncias = quitarDenuncia(actual.denuncias, id),
            aviso = accion.aviso(denuncia),
            avisoError = false,
        )
    }
}
