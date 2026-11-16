package pe.civika.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.civika.shared.domain.Familia

/** Preferencia de tema del usuario (Perfil). */
enum class ModoTema { SISTEMA, CLARO, OSCURO }

/** Colores de una familia de estado: relleno del marcador, ícono encima, contorno y texto legible sobre el fondo. */
@Immutable
class ColorEstado(val relleno: Color, val icono: Color, val contorno: Color, val texto: Color)

/** Tokens de docs/diseno/direccion-visual.md que Material 3 no tiene como rol propio. */
@Immutable
class CivikaColores(
    val terracota: Color,
    val terracotaTexto: Color,
    val verdeAndino: Color,
    val borde: Color,
    val bordeCampo: Color,
    val tintaSuave: Color,
    val esOscuro: Boolean,
    private val estados: Map<Familia, ColorEstado>,
) {
    fun estado(familia: Familia): ColorEstado = estados.getValue(familia)
}

private val ColoresClaro = CivikaColores(
    terracota = Color(0xFFC4532D),
    terracotaTexto = Color(0xFFA8431F),
    verdeAndino = Color(0xFF2F6B4F),
    borde = Color(0xFFE4DCD0),
    bordeCampo = Color(0xFF8C8278),
    tintaSuave = Color(0xFF5C534A),
    esOscuro = false,
    estados = mapOf(
        Familia.ABIERTO to ColorEstado(Color(0xFFD64545), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFB83232)),
        Familia.EN_ATENCION to ColorEstado(Color(0xFFE0A100), Color(0xFF1F1B16), Color(0xFF1F1B16), Color(0xFF8A6100)),
        Familia.RESUELTO to ColorEstado(Color(0xFF2E8B57), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF1F7044)),
        Familia.RECHAZADO to ColorEstado(Color(0xFF6B6560), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF6B6560)),
    ),
)

private val ColoresOscuro = CivikaColores(
    terracota = Color(0xFFF0A07E),
    // En oscuro el terracota claro ya sirve como texto (8,83:1): no hace falta uno aparte.
    terracotaTexto = Color(0xFFF0A07E),
    verdeAndino = Color(0xFF7FC8A0),
    borde = Color(0xFF4A3F37),
    bordeCampo = Color(0xFF7F756B),
    tintaSuave = Color(0xFFB5ADA4),
    esOscuro = true,
    estados = mapOf(
        Familia.ABIERTO to ColorEstado(Color(0xFFFF8A80), Color(0xFF3B0F0B), Color(0xFF171310), Color(0xFFFF8A80)),
        Familia.EN_ATENCION to ColorEstado(Color(0xFFF2C14E), Color(0xFF2A1F00), Color(0xFF171310), Color(0xFFF2C14E)),
        Familia.RESUELTO to ColorEstado(Color(0xFF6FD39A), Color(0xFF0F2A1D), Color(0xFF171310), Color(0xFF6FD39A)),
        Familia.RECHAZADO to ColorEstado(Color(0xFFB5ADA4), Color(0xFF171310), Color(0xFF171310), Color(0xFFB5ADA4)),
    ),
)

// Roles de Material 3 de la tabla de la dirección visual. Las tarjetas se separan del fondo por el color de
// `superficie`, no por la sombra, así que todos los contenedores de superficie valen lo mismo.
private val Claro = lightColorScheme(
    primary = Color(0xFFC4532D),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2F6B4F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2F6B4F),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFFAF6F0),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF5C534A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    outline = Color(0xFF8C8278),
    outlineVariant = Color(0xFFE4DCD0),
    error = Color(0xFFB83232),
    onError = Color(0xFFFFFFFF),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFF0A07E),
    onPrimary = Color(0xFF3B1205),
    secondary = Color(0xFF7FC8A0),
    onSecondary = Color(0xFF0F2A1D),
    secondaryContainer = Color(0xFF7FC8A0),
    onSecondaryContainer = Color(0xFF0F2A1D),
    background = Color(0xFF171310),
    onBackground = Color(0xFFEDE6DC),
    surface = Color(0xFF221C18),
    onSurface = Color(0xFFEDE6DC),
    surfaceVariant = Color(0xFF221C18),
    onSurfaceVariant = Color(0xFFB5ADA4),
    surfaceContainerLowest = Color(0xFF221C18),
    surfaceContainerLow = Color(0xFF221C18),
    surfaceContainer = Color(0xFF221C18),
    surfaceContainerHigh = Color(0xFF221C18),
    surfaceContainerHighest = Color(0xFF221C18),
    outline = Color(0xFF7F756B),
    outlineVariant = Color(0xFF4A3F37),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0F0B),
)

/** Radios: 8 dp en chips y campos, 12 dp en tarjetas y 28 dp en bottom sheets, diálogos y FAB. */
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LocalCivikaColores = staticCompositionLocalOf { ColoresClaro }

/** Única regla que decide si la app se dibuja en oscuro: el tema de Compose y las barras del sistema la comparten. */
@Composable
fun ModoTema.esOscuro(): Boolean = when (this) {
    ModoTema.SISTEMA -> isSystemInDarkTheme()
    ModoTema.CLARO -> false
    ModoTema.OSCURO -> true
}

@Composable
fun CivikaTheme(modo: ModoTema = ModoTema.SISTEMA, content: @Composable () -> Unit) {
    val oscuro = modo.esOscuro()
    MaterialTheme(
        colorScheme = if (oscuro) Oscuro else Claro,
        typography = CivikaTipografia,
        shapes = Formas,
    ) {
        CompositionLocalProvider(LocalCivikaColores provides if (oscuro) ColoresOscuro else ColoresClaro) {
            content()
        }
    }
}

object CivikaTheme {
    val colores: CivikaColores
        @Composable @ReadOnlyComposable get() = LocalCivikaColores.current
}
