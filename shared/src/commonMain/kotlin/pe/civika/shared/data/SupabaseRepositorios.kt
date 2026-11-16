package pe.civika.shared.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Notificacion
import pe.civika.shared.domain.ReporteCreado
import pe.civika.shared.domain.ReporteNuevo
import pe.civika.shared.domain.ResultadoRegistro
import pe.civika.shared.domain.Usuario
import pe.civika.shared.domain.usuario

/** Cuenta con Supabase Auth. supabase-kt guarda la sesión en el celular y renueva el token solo. */
class SupabaseAuthRepository(
    private val cliente: SupabaseClient,
    alcance: CoroutineScope,
) : AuthRepository {

    private val _sesion = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    override val sesion: StateFlow<EstadoSesion> = _sesion.asStateFlow()

    init {
        alcance.launch {
            cliente.auth.sessionStatus.collectLatest { estado ->
                when (estado) {
                    is SessionStatus.Authenticated -> {
                        val info = estado.session.user ?: cliente.auth.currentUserOrNull()
                        if (info == null) {
                            _sesion.value = EstadoSesion.SinSesion
                        } else {
                            // Primero con lo que trae el token; después con el nombre guardado en `usuarios`.
                            if (_sesion.value.usuario?.id != info.id) _sesion.value = EstadoSesion.Activa(info.aUsuario(null))
                            val nombre = nombreGuardado(info.id)
                            if (nombre != null) _sesion.value = EstadoSesion.Activa(info.aUsuario(nombre))
                        }
                    }
                    is SessionStatus.NotAuthenticated -> _sesion.value = EstadoSesion.SinSesion
                    is SessionStatus.RefreshFailure -> {
                        // Sin red no se pudo renovar el token: la sesión guardada sigue valiendo.
                        if (_sesion.value is EstadoSesion.Cargando) {
                            val info = cliente.auth.currentUserOrNull()
                            _sesion.value = info?.let { EstadoSesion.Activa(it.aUsuario(null)) } ?: EstadoSesion.SinSesion
                        }
                    }
                    SessionStatus.Initializing -> _sesion.value = EstadoSesion.Cargando
                }
            }
        }
    }

    override suspend fun iniciarSesion(correo: String, contrasena: String) = traduciendoErrores {
        cliente.auth.signInWith(Email) {
            email = correo.trim()
            password = contrasena
        }
    }

    override suspend fun registrar(
        nombre: String,
        correo: String,
        contrasena: String,
        versionPolitica: String,
    ): ResultadoRegistro = traduciendoErrores {
        cliente.auth.signUpWith(Email) {
            email = correo.trim()
            password = contrasena
            // El trigger `handle_new_user` lee esta metadata: crea la fila de `usuarios` y guarda el consentimiento.
            data = buildJsonObject {
                put("nombre", nombre.trim())
                put("version_politica", versionPolitica)
            }
        }
        if (cliente.auth.currentSessionOrNull() != null) ResultadoRegistro.SESION_INICIADA else ResultadoRegistro.CONFIRMAR_CORREO
    }

    override suspend fun recuperarContrasena(correo: String) = traduciendoErrores {
        cliente.auth.resetPasswordForEmail(correo.trim())
    }

    override suspend fun cerrarSesion() {
        try {
            cliente.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red no se puede revocar el token en el servidor, pero el celular igual debe quedar sin sesión.
            runCatching { cliente.auth.signOut(SignOutScope.LOCAL) }
        }
        _sesion.value = EstadoSesion.SinSesion
    }

    override suspend fun actualizarNombre(nombre: String) = traduciendoErrores {
        val usuarioId = cliente.exigirUsuario()
        val limpio = nombre.trim()
        cliente.from("usuarios").update({ set("nombre", limpio) }) {
            filter { eq("id", usuarioId) }
        }
        _sesion.update { actual ->
            (actual as? EstadoSesion.Activa)?.let { EstadoSesion.Activa(it.usuario.copy(nombre = limpio)) } ?: actual
        }
    }

    override suspend fun eliminarCuenta() = traduciendoErrores {
        cliente.exigirUsuario()
        cliente.postgrest.rpc("eliminar_mi_cuenta")
        // El usuario ya no existe en el servidor: solo queda borrar la sesión guardada en el celular.
        runCatching { cliente.auth.signOut(SignOutScope.LOCAL) }
        _sesion.value = EstadoSesion.SinSesion
    }

    private suspend fun nombreGuardado(usuarioId: String): String? = try {
        cliente.from("usuarios").select { filter { eq("id", usuarioId) } }
            .decodeList<UsuarioDto>().firstOrNull()?.nombre
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun UserInfo.aUsuario(nombre: String?): Usuario {
        val deMetadata = runCatching { userMetadata?.get("nombre")?.jsonPrimitive?.contentOrNull }.getOrNull()
        return Usuario(
            id = id,
            nombre = nombre ?: deMetadata?.takeIf { it.isNotBlank() } ?: "Ciudadano",
            correo = email.orEmpty(),
        )
    }
}

/** Subida de fotos a Storage y la RPC `crear_reporte`. */
class SupabaseReporteRepository(
    private val cliente: SupabaseClient,
    private val incidentes: SupabaseIncidenteRepository,
) : ReporteRepository {

    override suspend fun subirFoto(clienteId: String, indice: Int, jpeg: ByteArray): String = traduciendoErrores {
        val usuarioId = cliente.exigirUsuario()
        // Un solo nivel dentro de la carpeta del usuario, como exige la política `fotos_subida_propia`.
        val ruta = "$usuarioId/$clienteId-$indice.jpg"
        try {
            cliente.storage.from(SupabaseIncidenteRepository.BUCKET_FOTOS).upload(ruta, jpeg) {
                upsert = false
                contentType = ContentType.Image.JPEG
            }
        } catch (e: RestException) {
            // Reintento de la cola: la foto ya se subió en un intento anterior. No hay upsert, así que se da por buena.
            if (!e.esDuplicado()) throw e
        }
        ruta
    }

    override suspend fun crear(reporte: ReporteNuevo): ReporteCreado = traduciendoErrores {
        cliente.exigirUsuario()
        val fila = cliente.postgrest.rpc(
            "crear_reporte",
            buildJsonObject {
                put("p_cliente_id", reporte.clienteId)
                put("p_lat", reporte.latitud)
                put("p_lon", reporte.longitud)
                put("p_categoria_id", reporte.categoriaId)
                put("p_descripcion", reporte.descripcion)
                put("p_fotos", JsonArray(reporte.fotos.map(::JsonPrimitive)))
                put("p_ubicacion_simulada", reporte.ubicacionSimulada)
                reporte.direccion?.let { put("p_direccion", it) }
            },
        ).decodeList<ReporteCreadoDto>().firstOrNull()
            ?: throw ErrorCivika.Desconocido(IllegalStateException("crear_reporte no devolvió filas"))
        ReporteCreado(
            incidenteId = fila.incidenteId,
            codigo = fila.codigo,
            estado = estadoDesde(fila.estado) ?: Estado.REPORTADO,
            entidad = incidentes.nombreDeEntidad(fila.entidadId),
        )
    }

    private fun RestException.esDuplicado(): Boolean {
        val texto = listOfNotNull(error, description, message).joinToString(" ")
        return statusCode == 409 || texto.contains("already exists", ignoreCase = true) ||
            texto.contains("Duplicate", ignoreCase = true)
    }
}

/** Bandeja de avisos y tokens de FCM. */
class SupabaseNotificacionRepository(private val cliente: SupabaseClient) : NotificacionRepository {

    override suspend fun listar(): List<Notificacion> = traduciendoErrores {
        cliente.exigirUsuario()
        cliente.postgrest.rpc("mis_notificaciones").decodeList<NotificacionDto>().mapNotNull { it.aDominio() }
    }

    override suspend fun marcarLeida(id: String) = traduciendoErrores {
        cliente.exigirUsuario()
        cliente.from("notificaciones").update({ set("leida", true) }) {
            filter { eq("id", id) }
        }
        Unit
    }

    override suspend fun marcarTodasLeidas() = traduciendoErrores {
        val usuarioId = cliente.exigirUsuario()
        cliente.from("notificaciones").update({ set("leida", true) }) {
            filter {
                eq("usuario_id", usuarioId)
                eq("leida", false)
            }
        }
        Unit
    }

    override suspend fun registrarDispositivo(token: String) = traduciendoErrores {
        cliente.exigirUsuario()
        cliente.postgrest.rpc("registrar_dispositivo", buildJsonObject { put("p_token", token) })
        Unit
    }

    override suspend fun eliminarDispositivo(token: String) = traduciendoErrores {
        cliente.exigirUsuario()
        cliente.from("dispositivos").delete {
            filter { eq("fcm_token", token) }
        }
        Unit
    }
}
