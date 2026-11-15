package pe.civika.shared.domain

/** Resultado de validar un campo. La app traduce cada motivo a un texto; el servidor valida lo mismo en SQL. */
enum class Validacion {
    OK, VACIO, MUY_CORTO, MUY_LARGO, FORMATO;

    val esValida: Boolean get() = this == OK
}

object Validaciones {
    const val DESCRIPCION_MIN = 10
    const val DESCRIPCION_MAX = 2000
    const val NOMBRE_MIN = 2
    const val NOMBRE_MAX = 80
    const val CONTRASENA_MIN = 8
    const val CONTRASENA_MAX = 72
    const val MOTIVO_MIN = 3
    const val MOTIVO_MAX = 500
    const val MAX_FOTOS = 3
    const val MAX_BYTES_FOTO = 2 * 1024 * 1024

    /** Versión de la política de privacidad que acepta quien se registra (tabla `consentimientos`). */
    const val VERSION_POLITICA = "2026-10"

    /** Igual que `crear_reporte`: 10 caracteres como mínimo sin contar los espacios de los extremos y 2000 como máximo. */
    fun descripcion(texto: String): Validacion = longitud(texto, DESCRIPCION_MIN, DESCRIPCION_MAX)

    /** El trigger de registro recorta el nombre a 80 caracteres. */
    fun nombre(texto: String): Validacion = longitud(texto, NOMBRE_MIN, NOMBRE_MAX)

    /** Igual que `denunciar_incidente`: de 3 a 500 caracteres. */
    fun motivoDenuncia(texto: String): Validacion = longitud(texto, MOTIVO_MIN, MOTIVO_MAX)

    fun correo(texto: String): Validacion {
        val limpio = texto.trim()
        return when {
            limpio.isEmpty() -> Validacion.VACIO
            limpio.length > 254 -> Validacion.MUY_LARGO
            !CORREO.matches(limpio) -> Validacion.FORMATO
            else -> Validacion.OK
        }
    }

    /** Supabase acepta 6 caracteres; la app pide 8 para que la clave no sea trivial. bcrypt ignora más de 72. */
    fun contrasena(texto: String): Validacion = when {
        texto.isEmpty() -> Validacion.VACIO
        texto.length < CONTRASENA_MIN -> Validacion.MUY_CORTO
        texto.length > CONTRASENA_MAX -> Validacion.MUY_LARGO
        else -> Validacion.OK
    }

    /** De 1 a 3 fotos por reporte (`private.validar_fotos`). */
    fun cantidadDeFotos(cantidad: Int): Validacion = when {
        cantidad <= 0 -> Validacion.VACIO
        cantidad > MAX_FOTOS -> Validacion.MUY_LARGO
        else -> Validacion.OK
    }

    private fun longitud(texto: String, minimo: Int, maximo: Int): Validacion {
        val limpio = texto.trim()
        return when {
            limpio.isEmpty() -> Validacion.VACIO
            limpio.length < minimo -> Validacion.MUY_CORTO
            texto.length > maximo -> Validacion.MUY_LARGO
            else -> Validacion.OK
        }
    }

    private val CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
}
