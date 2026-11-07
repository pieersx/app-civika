package pe.civika.panel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Marca de Cívika: ladrillo (triángulo), adobe (rombo) y verde andino (círculo).
 * Está dibujada con Canvas porque el panel no empaqueta imágenes ni `material-icons-core`.
 */
@Composable
fun LogoCivika(
    modifier: Modifier = Modifier,
    tamano: Dp = 48.dp,
    conTitulo: Boolean = true,
    colorTitulo: Color = Color.White,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(tamano)) {
            val lado = size.minDimension / 3f
            val centroY = size.height / 2f
            // Triángulo (ladrillo)
            val triangulo = Path().apply {
                moveTo(lado * 0.5f, centroY - lado * 0.5f)
                lineTo(lado, centroY + lado * 0.5f)
                lineTo(0f, centroY + lado * 0.5f)
                close()
            }
            drawPath(triangulo, Color(0xFFD64545))
            // Rombo (adobe)
            val rombo = Path().apply {
                moveTo(lado * 1.5f + lado * 0.5f, centroY - lado * 0.5f)
                lineTo(lado * 2f, centroY)
                lineTo(lado * 1.5f + lado * 0.5f, centroY + lado * 0.5f)
                lineTo(lado, centroY)
                close()
            }
            drawPath(rombo, Color(0xFFE0A100))
            // Círculo (verde andino)
            drawCircle(Color(0xFF2E8B57), radius = lado * 0.5f, center = Offset(lado * 2.5f, centroY))
        }
        if (conTitulo) {
            Text("Cívika", style = MaterialTheme.typography.titleLarge, color = colorTitulo)
        }
    }
}
