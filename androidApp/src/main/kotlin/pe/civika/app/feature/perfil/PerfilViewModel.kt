package pe.civika.app.feature.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.app.device.AjustesApp
import pe.civika.app.device.AlmacenFotos
import pe.civika.app.device.TokenDispositivo
import pe.civika.app.ui.theme.ModoTema
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.ColaReportesRepository
import pe.civika.shared.data.NotificacionRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Usuario
import pe.civika.shared.domain.Validaciones
import pe.civika.shared.domain.usuario

data class PerfilUiState(
    val usuario: Usuario?,
    val tema: ModoTema,
    /** `null` mientras no se edita; si no, el texto que se está escribiendo. */
    val nombreEditado: String? = null,
    val trabajando: Boolean = false,
    val error: ErrorCivika? = null,
) {
    val puedeGuardarNombre: Boolean
        get() = nombreEditado != null && Validaciones.nombre(nombreEditado).esValida && nombreEditado.trim() != usuario?.nombre
}

class PerfilViewModel(
    private val auth: AuthRepository,
    private val notificaciones: NotificacionRepository,
    private val cola: ColaReportesRepository,
    private val fotos: AlmacenFotos,
    private val tokens: TokenDispositivo,
    private val ajustes: AjustesApp,
) : ViewModel() {

    private data class Local(val nombreEditado: String? = null, val trabajando: Boolean = false, val error: ErrorCivika? = null)

    private val local = MutableStateFlow(Local())

    val estado: StateFlow<PerfilUiState> = combine(auth.sesion, ajustes.tema, local) { sesion, tema, l ->
        PerfilUiState(sesion.usuario, tema, l.nombreEditado, l.trabajando, l.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilUiState(auth.sesion.value.usuario, ModoTema.SISTEMA))

    fun cambiarTema(modo: ModoTema) {
        viewModelScope.launch { ajustes.cambiarTema(modo) }
    }

    fun editarNombre() = local.update { it.copy(nombreEditado = auth.sesion.value.usuario?.nombre.orEmpty(), error = null) }

    fun cambiarNombre(texto: String) = local.update { it.copy(nombreEditado = texto) }

    fun cancelarEdicion() = local.update { it.copy(nombreEditado = null, error = null) }

    fun guardarNombre() {
        val texto = local.value.nombreEditado ?: return
        if (!Validaciones.nombre(texto).esValida) return
        trabajar {
            auth.actualizarNombre(texto)
            local.update { it.copy(nombreEditado = null) }
        }
    }

    /** Cierra la sesión y le quita a este celular el aviso de la cuenta (borra el token de `dispositivos`). */
    fun cerrarSesion() {
        trabajar {
            quitarToken()
            auth.cerrarSesion()
        }
    }

    /** Ley 29733: la cuenta se anonimiza y los envíos pendientes de este celular se descartan. */
    fun eliminarCuenta() {
        trabajar {
            val usuario = auth.sesion.value.usuario
            quitarToken()
            auth.eliminarCuenta()
            if (usuario != null) {
                runCatchingSinCancelar {
                    cola.observar(usuario.id).first().forEach {
                        cola.descartar(it.clienteId)
                        fotos.borrarCarpeta(it.clienteId)
                    }
                }
            }
        }
    }

    fun errorMostrado() = local.update { it.copy(error = null) }

    private suspend fun quitarToken() {
        runCatchingSinCancelar {
            val token = tokens.actual() ?: return@runCatchingSinCancelar
            notificaciones.eliminarDispositivo(token)
        }
    }

    private fun trabajar(accion: suspend () -> Unit) {
        if (local.value.trabajando) return
        local.update { it.copy(trabajando = true, error = null) }
        viewModelScope.launch {
            try {
                accion()
                local.update { it.copy(trabajando = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorCivika) {
                local.update { it.copy(trabajando = false, error = e) }
            } catch (e: Exception) {
                local.update { it.copy(trabajando = false, error = ErrorCivika.Desconocido(e)) }
            }
        }
    }

    private suspend fun runCatchingSinCancelar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Quitar el token o limpiar la cola es de mejor esfuerzo: no debe impedir cerrar sesión ni eliminar la cuenta.
        }
    }
}
