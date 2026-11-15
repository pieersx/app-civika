package pe.civika.shared.domain

data class Usuario(val id: String, val nombre: String, val correo: String)

sealed interface EstadoSesion {
    /** Todavía se está leyendo la sesión guardada en el celular. */
    data object Cargando : EstadoSesion

    /** Sin cuenta: se puede explorar el mapa y los incidentes públicos. */
    data object SinSesion : EstadoSesion

    data class Activa(val usuario: Usuario) : EstadoSesion
}

val EstadoSesion.usuario: Usuario? get() = (this as? EstadoSesion.Activa)?.usuario

enum class ResultadoRegistro {
    /** La cuenta quedó creada y con sesión iniciada. */
    SESION_INICIADA,

    /** Falta abrir el enlace de confirmación que llega al correo. */
    CONFIRMAR_CORREO,
}
