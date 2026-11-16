package pe.civika.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pe.civika.app.R
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.MotivoError
import pe.civika.shared.domain.Validacion

/** Texto en español de cada error. Dice qué pasó y qué hacer, sin disculpas (tono de la dirección visual). */
@Composable
fun ErrorCivika.mensaje(): String = when (this) {
    is ErrorCivika.SinConexion -> stringResource(R.string.error_sin_conexion)
    is ErrorCivika.SesionRequerida -> stringResource(R.string.error_sesion)
    is ErrorCivika.FueraDeZona -> stringResource(R.string.error_fuera_de_zona)
    is ErrorCivika.DatosInvalidos -> detalle.ifBlank { stringResource(R.string.error_generico) }
    is ErrorCivika.SinPermiso -> stringResource(R.string.error_permiso)
    is ErrorCivika.CredencialesInvalidas -> stringResource(R.string.error_credenciales)
    is ErrorCivika.CorreoSinConfirmar -> stringResource(R.string.error_correo_sin_confirmar)
    is ErrorCivika.CorreoYaRegistrado -> stringResource(R.string.error_correo_registrado)
    is ErrorCivika.ContrasenaDebil -> stringResource(R.string.error_contrasena_debil)
    is ErrorCivika.DemasiadosIntentos -> stringResource(R.string.error_demasiados_intentos)
    is ErrorCivika.Desconocido -> stringResource(R.string.error_generico)
}

/** Mensaje de un campo inválido; `null` si es válido. */
@Composable
fun Validacion.mensajeDeCampo(campo: CampoUi): String? = when (this) {
    Validacion.OK -> null
    Validacion.VACIO -> stringResource(R.string.campo_obligatorio)
    Validacion.MUY_CORTO -> when (campo) {
        CampoUi.CONTRASENA -> stringResource(R.string.contrasena_muy_corta)
        CampoUi.DESCRIPCION -> stringResource(R.string.descripcion_muy_corta)
        CampoUi.NOMBRE -> stringResource(R.string.nombre_muy_corto)
        else -> stringResource(R.string.campo_muy_corto)
    }
    Validacion.MUY_LARGO -> stringResource(R.string.campo_muy_largo)
    Validacion.FORMATO -> stringResource(R.string.correo_invalido)
}

enum class CampoUi { NOMBRE, CORREO, CONTRASENA, DESCRIPCION, MOTIVO }

/** Por qué un envío de la cola no salió, en palabras: qué pasó y qué hacer. */
@Composable
fun textoDeMotivo(motivo: MotivoError?, detalle: String?): String = when (motivo) {
    MotivoError.SIN_CONEXION -> stringResource(R.string.envio_motivo_sin_conexion)
    MotivoError.SESION -> stringResource(R.string.envio_motivo_sesion)
    MotivoError.FUERA_DE_ZONA -> stringResource(R.string.error_fuera_de_zona)
    MotivoError.DATOS -> detalle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.error_generico)
    MotivoError.PERMISO -> stringResource(R.string.error_permiso)
    MotivoError.FOTO_PERDIDA -> stringResource(R.string.envio_motivo_foto_perdida)
    MotivoError.DESCONOCIDO, null -> stringResource(R.string.error_generico)
}
