package pe.civika.panel.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mismos roles de Material 3 que la app (docs/diseno/direccion-visual.md). El panel solo usa el tema claro.
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

@Composable
fun PanelTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Claro, content = content)
}
