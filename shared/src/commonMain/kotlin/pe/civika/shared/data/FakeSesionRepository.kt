package pe.civika.shared.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.civika.shared.domain.Credenciales
import pe.civika.shared.domain.Rol
import pe.civika.shared.domain.Sesion

/**
 * Sesiones de ejemplo para desarrollar el panel sin backend y para las pruebas.
 *
 * Cualquier correo con formato válido y una contraseña de 6 caracteres o más entra en modo demo:
 * el correo decide el rol (`rosa` → moderación, cualquier otro → entidad), así se pueden probar las
 * dos cuentas del mockup. Cuando llegue Supabase Auth, esta clase se va del módulo de Koin y nada más.
 */
class FakeSesionRepository : SesionRepository {

    private val ES_CORREO_DE_MODERACION = Regex("rosa|moderador")

    private val _sesion = MutableStateFlow<Sesion?>(null)
    override val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    override suspend fun iniciarSesion(correo: String, contrasena: String): InicioSesion {
        val limpio = correo.trim().lowercase()
        if (!Credenciales.correoValido(limpio)) {
            return InicioSesion.Rechazado("Revisa el formato de tu correo.")
        }
        if (!Credenciales.contrasenaValida(contrasena)) {
            return InicioSesion.Rechazado("Correo o contraseña incorrectos.")
        }
        val nueva = if (ES_CORREO_DE_MODERACION.containsMatchIn(limpio)) {
            Sesion("demo-rosa", "Rosa T.", "moderadora", Rol.MODERADOR)
        } else {
            Sesion("demo-luis", "Luis M.", "Gerencia de Obras", Rol.ENTIDAD)
        }
        _sesion.value = nueva
        return InicioSesion.Exitoso(nueva)
    }

    override fun cerrarSesion() {
        _sesion.value = null
    }
}
