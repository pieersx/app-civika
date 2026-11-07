package pe.civika.shared.domain

/**
 * Quién está usando el panel o la app en esta sesión.
 *
 * `nombre` y `cargo` son los que se leen en la tarjeta de la barra lateral del panel
 * («Sesión de Luis M., Gerencia de Obras»); el [Rol] decide qué transiciones de estado puede hacer.
 */
data class Sesion(
    val usuarioId: String,
    val nombre: String,
    val cargo: String,
    val rol: Rol,
)

/**
 * Reglas mínimas de credenciales. Viven en un solo lugar para que el repositorio de sesiones y el
 * formulario del panel no pongan de acuerdo dos validaciones distintas.
 */
object Credenciales {
    const val LONGITUD_MINIMA_CONTRASENA = 6

    private val CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun correoValido(correo: String) = CORREO.containsMatchIn(correo.trim().lowercase())

    fun contrasenaValida(contrasena: String) = contrasena.length >= LONGITUD_MINIMA_CONTRASENA
}
