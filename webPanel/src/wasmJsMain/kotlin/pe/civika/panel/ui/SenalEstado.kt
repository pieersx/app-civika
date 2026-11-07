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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Familia

/**
 * Señalética de estados (direccion-visual.md §Estados del incidente): **forma + ícono + palabra**,
 * nunca solo color. La forma recuerda la señalética vial: triángulo (peligro), rombo (obra),
 * círculo (hecho) y cuadrado (detenido).
 *
 * El estado se comunica entero en el texto accesible; el Canvas no aporta lectura a lectores de pantalla.
 */
@Composable
fun SenalEstado(
    estado: Estado,
    modifier: Modifier = Modifier,
    tamano: Dp = 18.dp,
) {
    Row(
        modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(tamano)) { dibujarSenal(estado.familia) }
        Text(
            estado.etiqueta,
            style = MaterialTheme.typography.labelLarge,
            color = PanelColores.textoDe(estado.familia),
        )
    }
}

private fun DrawScope.dibujarSenal(familia: Familia) {
    val ancho = size.width
    val alto = size.height
    val relleno = PanelColores.rellenoDe(familia)
    when (familia) {
        Familia.ABIERTO -> {
            val triangulo = Path().apply {
                moveTo(ancho / 2f, 0f)
                lineTo(ancho, alto)
                lineTo(0f, alto)
                close()
            }
            drawPath(triangulo, relleno)
            dibujarExclamacion(color = Color.White, centroX = ancho / 2f, desde = alto * 0.42f, hasta = alto * 0.68f, grosor = ancho * 0.09f)
        }
        Familia.EN_ATENCION -> {
            val rombo = Path().apply {
                moveTo(ancho / 2f, 0f)
                lineTo(ancho, alto / 2f)
                lineTo(ancho / 2f, alto)
                lineTo(0f, alto / 2f)
                close()
            }
            drawPath(rombo, relleno)
            dibujarExclamacion(color = Color(0xFF1F1B16), centroX = ancho / 2f, desde = alto * 0.30f, hasta = alto * 0.62f, grosor = ancho * 0.10f)
        }
        Familia.RESUELTO -> {
            drawCircle(relleno, radius = ancho / 2f, center = Offset(ancho / 2f, alto / 2f))
            val grosor = ancho * 0.11f
            drawLine(Color.White, Offset(ancho * 0.28f, alto * 0.52f), Offset(ancho * 0.45f, alto * 0.70f), grosor)
            drawLine(Color.White, Offset(ancho * 0.45f, alto * 0.70f), Offset(ancho * 0.74f, alto * 0.32f), grosor)
        }
        Familia.RECHAZADO -> {
            drawRect(relleno, size = size)
            val grosor = ancho * 0.11f
            drawLine(Color.White, Offset(ancho * 0.30f, alto * 0.30f), Offset(ancho * 0.70f, alto * 0.70f), grosor)
            drawLine(Color.White, Offset(ancho * 0.70f, alto * 0.30f), Offset(ancho * 0.30f, alto * 0.70f), grosor)
        }
    }
}

private fun DrawScope.dibujarExclamacion(color: Color, centroX: Float, desde: Float, hasta: Float, grosor: Float) {
    drawLine(color, Offset(centroX, desde), Offset(centroX, hasta), grosor)
    drawCircle(color, radius = grosor / 2f, center = Offset(centroX, hasta + grosor * 1.4f))
}
