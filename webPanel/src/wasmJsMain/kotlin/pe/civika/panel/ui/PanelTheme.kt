package pe.civika.panel.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.civika.shared.domain.Familia

/**
 * Tokens del panel, tomados de `docs/diseno/direccion-visual.md` (la fuente de verdad del diseño).
 * El tema solo es claro: la entidad y la moderación trabajan de día, en escritorio.
 */
object PanelColores {
    /** Verde muy oscuro de la barra lateral y de la tarjeta de sesión (mockup del panel). */
    val VerdeProfundo = Color(0xFF153A2A)
    val VerdeProfundoSuperficie = Color(0xFF1E4A38)
    val TextoSobreVerde = Color(0xFFFFFFFF)
    val TextoSobreVerdeSuave = Color(0xFF9DBFAE)

    /** Verde andino: navegación, chips seleccionados y botones secundarios. */
    val VerdeAndino = Color(0xFF2F6B4F)
    /** Terracota de texto sobre arena (el terracota de relleno no llega a 4,5:1 como texto). */
    val TerracotaTexto = Color(0xFFA8431F)

    /** Relleno de datos de los gráficos (direccion-visual.md §Datos). */
    val Datos = Color(0xFF1E7A55)
    /** Relleno neutro de barras y marcos de foto. */
    val Neutro = Color(0xFFEFE9DF)
    /** Fondo de la fila seleccionada en la tabla de incidentes. */
    val FilaSeleccionada = Color(0xFFFDF3E3)
    /** Ámbar del contador de moderación: relleno con texto oscuro (nunca texto ni borde suelto). */
    val Amarillo = Color(0xFFE0A100)

    /** Relleno y texto por familia de estado (direccion-visual.md §Estados del incidente). */
    fun rellenoDe(familia: Familia) = when (familia) {
        Familia.ABIERTO -> Color(0xFFD64545)
        Familia.EN_ATENCION -> Amarillo
        Familia.RESUELTO -> Color(0xFF2E8B57)
        Familia.RECHAZADO -> Color(0xFF6B6560)
    }

    fun textoDe(familia: Familia) = when (familia) {
        Familia.ABIERTO -> Color(0xFFB83232)
        Familia.EN_ATENCION -> Color(0xFF8A6100)
        Familia.RESUELTO -> Color(0xFF1F7044)
        Familia.RECHAZADO -> Color(0xFF6B6560)
    }
}

private val Claro = lightColorScheme(
    primary = Color(0xFFC4532D),
    onPrimary = Color(0xFFFFFFFF),
    secondary = PanelColores.VerdeAndino,
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF6F0),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1B16),
    onSurfaceVariant = Color(0xFF5C534A),
    outline = Color(0xFF8C8278),
    outlineVariant = Color(0xFFE4DCD0),
    error = Color(0xFFB83232),
    onError = Color(0xFFFFFFFF),
)

/**
 * Escala del panel (direccion-visual.md §Tipografía): Bricolage Grotesque para títulos y Atkinson
 * Hyperlegible para el resto. Las familias se empaquetan como `.ttf` en `composeResources/font/`;
 * mientras Emily las incorpora al tema de la app, aquí se usa la familia del sistema con los mismos
 * tamaños, pesos e interlineados, para que el ritmo tipográfico ya sea el correcto.
 */
private val TipografiaPanel = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
)

/** Radios de la guía: 8 en chips y campos, 12 en tarjetas, 28 en botones y sheets. */
private val FormasPanel = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun PanelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Claro,
        typography = TipografiaPanel,
        shapes = FormasPanel,
        content = content,
    )
}
