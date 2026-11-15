package pe.civika.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.ResultadoRegistro
import pe.civika.shared.domain.Validacion
import pe.civika.shared.domain.Validaciones

/** Formulario de bienvenida: iniciar sesión, registrarse o recuperar la contraseña (RF-01). */
data class AuthUiState(
    val nombre: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val consentimiento: Boolean = false,
    /** Los errores de cada campo solo se muestran después del primer intento de enviar. */
    val intentado: Boolean = false,
    val enviando: Boolean = false,
    val error: ErrorCivika? = null,
    val autenticado: Boolean = false,
    val confirmarCorreo: Boolean = false,
    val recuperacionEnviada: Boolean = false,
) {
    val validacionNombre: Validacion get() = Validaciones.nombre(nombre)
    val validacionCorreo: Validacion get() = Validaciones.correo(correo)
    val validacionContrasenaRegistro: Validacion get() = Validaciones.contrasena(contrasena)

    /** Al iniciar sesión solo se exige que no esté vacía: una contraseña antigua puede ser más corta que la regla actual. */
    val validacionContrasenaLogin: Validacion
        get() = if (contrasena.isEmpty()) Validacion.VACIO else Validacion.OK

    val faltaConsentimiento: Boolean get() = intentado && !consentimiento
}

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    fun cambiarNombre(valor: String) = _estado.update { it.copy(nombre = valor, error = null) }
    fun cambiarCorreo(valor: String) = _estado.update { it.copy(correo = valor, error = null) }
    fun cambiarContrasena(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }
    fun cambiarConsentimiento(valor: Boolean) = _estado.update { it.copy(consentimiento = valor, error = null) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.enviando) return
        _estado.update { it.copy(intentado = true) }
        if (!actual.validacionCorreo.esValida || !actual.validacionContrasenaLogin.esValida) return
        ejecutar {
            auth.iniciarSesion(actual.correo, actual.contrasena)
            _estado.update { it.copy(autenticado = true) }
        }
    }

    fun registrar() {
        val actual = _estado.value
        if (actual.enviando) return
        _estado.update { it.copy(intentado = true) }
        val valido = actual.validacionNombre.esValida &&
            actual.validacionCorreo.esValida &&
            actual.validacionContrasenaRegistro.esValida &&
            actual.consentimiento
        if (!valido) return
        ejecutar {
            when (auth.registrar(actual.nombre, actual.correo, actual.contrasena, Validaciones.VERSION_POLITICA)) {
                ResultadoRegistro.SESION_INICIADA -> _estado.update { it.copy(autenticado = true) }
                ResultadoRegistro.CONFIRMAR_CORREO -> _estado.update { it.copy(confirmarCorreo = true) }
            }
        }
    }

    fun recuperarContrasena() {
        val actual = _estado.value
        if (actual.enviando) return
        _estado.update { it.copy(intentado = true) }
        if (!actual.validacionCorreo.esValida) return
        ejecutar {
            auth.recuperarContrasena(actual.correo)
            _estado.update { it.copy(recuperacionEnviada = true) }
        }
    }

    private fun ejecutar(accion: suspend () -> Unit) {
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                accion()
                _estado.update { it.copy(enviando = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorCivika) {
                _estado.update { it.copy(enviando = false, error = e) }
            } catch (e: Exception) {
                _estado.update { it.copy(enviando = false, error = ErrorCivika.Desconocido(e)) }
            }
        }
    }
}
