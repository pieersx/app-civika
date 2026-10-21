package pe.civika.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Roles de Material 3 de docs/diseno/direccion-visual.md. Tipografía y formas llegan con el issue #2 (Emily).
private val Claro = lightColorScheme(
    primary = Color(0xFFC4532D),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2F6B4F),
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

private val Oscuro = darkColorScheme(
    primary = Color(0xFFF0A07E),
    onPrimary = Color(0xFF3B1205),
    secondary = Color(0xFF7FC8A0),
    onSecondary = Color(0xFF0F2A1D),
    background = Color(0xFF171310),
    onBackground = Color(0xFFEDE6DC),
    surface = Color(0xFF221C18),
    onSurface = Color(0xFFEDE6DC),
    onSurfaceVariant = Color(0xFFB5ADA4),
    outline = Color(0xFF7F756B),
    outlineVariant = Color(0xFF4A3F37),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0F0B),
)

@Composable
fun CivikaTheme(oscuro: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (oscuro) Oscuro else Claro, content = content)
}
