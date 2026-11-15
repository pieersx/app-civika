package pe.civika.shared.domain

/**
 * Errores que la app sabe explicar. Los repositorios traducen a estos tipos lo que devuelven Supabase y la red,
 * para que los ViewModels no dependan de supabase-kt.
 */
sealed class ErrorCivika(mensaje: String? = null, causa: Throwable? = null) : Exception(mensaje, causa) {

    /** No hay red o el servidor no responde. Se puede reintentar más tarde. */
    class SinConexion(causa: Throwable? = null) : ErrorCivika("Sin conexión", causa)

    /** La acción necesita una cuenta con sesión iniciada. */
    class SesionRequerida(causa: Throwable? = null) : ErrorCivika("Se requiere una sesión", causa)

    /** El punto cae fuera de la zona piloto (`crear_reporte`, código P0001). */
    class FueraDeZona(causa: Throwable? = null) : ErrorCivika("Fuera de la zona piloto", causa)

    /** El servidor rechazó los datos (código 22023). El mensaje ya viene en español y se puede mostrar. */
    class DatosInvalidos(val detalle: String, causa: Throwable? = null) : ErrorCivika(detalle, causa)

    /** La cuenta no tiene permiso para esta acción (código 42501). */
    class SinPermiso(val detalle: String, causa: Throwable? = null) : ErrorCivika(detalle, causa)

    class CredencialesInvalidas(causa: Throwable? = null) : ErrorCivika("Correo o contraseña incorrectos", causa)

    class CorreoSinConfirmar(causa: Throwable? = null) : ErrorCivika("Falta confirmar el correo", causa)

    class CorreoYaRegistrado(causa: Throwable? = null) : ErrorCivika("El correo ya tiene una cuenta", causa)

    class ContrasenaDebil(causa: Throwable? = null) : ErrorCivika("La contraseña es muy débil", causa)

    /** Supabase limita los intentos (registro, correos, inicio de sesión). */
    class DemasiadosIntentos(causa: Throwable? = null) : ErrorCivika("Demasiados intentos", causa)

    class Desconocido(causa: Throwable? = null) : ErrorCivika(causa?.message, causa)

    /**
     * Un error definitivo no se arregla reintentando: la cola sin conexión lo marca y se lo explica al ciudadano
     * en vez de volver a enviarlo (RF-15).
     */
    val esDefinitivo: Boolean
        get() = this is FueraDeZona || this is DatosInvalidos || this is SinPermiso
}
