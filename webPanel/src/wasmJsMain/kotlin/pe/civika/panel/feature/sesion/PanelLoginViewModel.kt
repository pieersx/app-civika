package pe.civika.panel.feature.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.civika.shared.data.InicioSesion
import pe.civika.shared.data.SesionRepository
import pe.civika.shared.domain.Credenciales

/** Estado del formulario de ingreso al panel. */
data class PanelLoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
) {
    /** Mismo criterio que el repositorio, para no dejar pulsar un botón que va a fallar. */
    val puedeIngresar: Boolean
        get() = !enviando &&
            Credenciales.correoValido(correo) &&
            Credenciales.contrasenaValida(contrasena)
}

/**
 * Login del panel. Hoy lo resuelve [pe.civika.shared.data.FakeSesionRepository]; cuando exista el
 * proyecto de Supabase, solo cambia la implementación en el módulo de Koin y esta clase no toca nada.
 */
class PanelLoginViewModel(
    private val sesion: SesionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelLoginUiState())
    val estado: StateFlow<PanelLoginUiState> = _estado.asStateFlow()

    fun escribirCorreo(texto: String) = actualizar { it.copy(correo = texto, error = null) }

    fun escribirContrasena(texto: String) = actualizar { it.copy(contrasena = texto, error = null) }

    fun ingresar() {
        val actual = _estado.value
        if (!actual.puedeIngresar) return
        actualizar { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val respuesta = try {
                sesion.iniciarSesion(actual.correo.trim(), actual.contrasena)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                InicioSesion.Rechazado("No se pudo iniciar sesión. Revisa tu conexión e inténtalo de nuevo.")
            }
            when (respuesta) {
                // La sesión ya cambió: el panel cambia de pantalla y el formulario queda limpio.
                is InicioSesion.Exitoso -> _estado.value = PanelLoginUiState()
                is InicioSesion.Rechazado -> actualizar {
                    it.copy(enviando = false, error = respuesta.motivo)
                }
            }
        }
    }

    private fun actualizar(transformacion: (PanelLoginUiState) -> PanelLoginUiState) {
        _estado.value = transformacion(_estado.value)
    }
}
