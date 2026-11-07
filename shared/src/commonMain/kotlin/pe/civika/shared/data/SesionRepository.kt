package pe.civika.shared.data

import kotlinx.coroutines.flow.StateFlow
import pe.civika.shared.domain.Sesion

/** Resultado de intentar iniciar sesión. No se lanzan excepciones por credenciales malas. */
sealed interface InicioSesion {
    data class Exitoso(val sesion: Sesion) : InicioSesion
    data class Rechazado(val motivo: String) : InicioSesion
}

/**
 * Fuente de sesión. Hoy la implementa [FakeSesionRepository]; cuando exista el proyecto de Supabase,
 * una implementación con supabase-kt (Auth) reemplaza a esta en el módulo de Koin.
 * Solo se usa la clave `anon`: la `service_role` nunca entra en el cliente (arquitectura §11).
 */
interface SesionRepository {
    /** Sesión activa, o `null` si nadie ha iniciado. El panel la observa para decidir si muestra el login. */
    val sesion: StateFlow<Sesion?>

    suspend fun iniciarSesion(correo: String, contrasena: String): InicioSesion

    fun cerrarSesion()
}
