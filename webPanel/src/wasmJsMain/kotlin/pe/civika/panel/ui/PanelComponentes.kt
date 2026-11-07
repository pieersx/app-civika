package pe.civika.panel.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tarjeta blanca del panel: se separa del fondo arena por el color, no por sombra (guía de formas). */
@Composable
fun TarjetaPanel(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { contenido() }
}

/** Chip de filtro: seleccionado en verde andino, sin seleccionar en blanco con borde. */
@Composable
fun ChipPanel(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fondo = if (seleccionado) PanelColores.VerdeAndino else MaterialTheme.colorScheme.surface
    val textoColor = if (seleccionado) Color.White else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier.semantics { role = Role.Button },
        shape = MaterialTheme.shapes.small,
        color = fondo,
        border = if (seleccionado) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = textoColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

/** Enlace de acción en terracotaTexto: el terracota de relleno no sirve como texto sobre arena. */
@Composable
fun EnlacePanel(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clickable(onClick = onClick), color = Color.Transparent) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = PanelColores.TerracotaTexto,
            modifier = Modifier.padding(4.dp),
        )
    }
}

/* Íconos del panel, dibujados a mano: `material-icons-core` no está en el classpath del módulo y no
   se añade una librería entera por cinco glifos (el Wasm ya pesa 8 MB). */

@Composable
fun IconoIndicadores(tamano: Dp = 20.dp, color: Color = Color.White) {
    Canvas(Modifier.size(tamano)) {
        val grosor = size.width * 0.16f
        val baseY = size.height * 0.88f
        val barras = listOf(0.12f to 0.40f, 0.44f to 0.78f, 0.76f to 0.55f)
        barras.forEach { (x, alto) ->
            drawLine(
                color = color,
                start = Offset(size.width * x, baseY),
                end = Offset(size.width * x, baseY - size.height * alto),
                strokeWidth = grosor,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun IconoModeracion(tamano: Dp = 20.dp, color: Color = Color.White) {
    Canvas(Modifier.size(tamano)) {
        val grosor = size.width * 0.11f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.94f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.24f, size.height * 0.06f),
            strokeWidth = grosor,
            cap = StrokeCap.Round,
        )
        val bandera = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.12f)
            lineTo(size.width * 0.92f, size.height * 0.28f)
            lineTo(size.width * 0.28f, size.height * 0.48f)
            close()
        }
        drawPath(bandera, color)
    }
}

/** Lupa del buscador de incidentes. */
@Composable
fun IconoBusqueda(tamano: Dp = 20.dp, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Canvas(Modifier.size(tamano)) {
        val grosor = size.width * 0.12f
        val radio = size.minDimension * 0.30f
        val centro = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(color, radius = radio, center = centro, style = Stroke(width = grosor))
        drawLine(
            color = color,
            start = Offset(centro.x + radio * 0.74f, centro.y + radio * 0.74f),
            end = Offset(size.width * 0.90f, size.height * 0.90f),
            strokeWidth = grosor,
            cap = StrokeCap.Round,
        )
    }
}

/** Flecha del selector de estados del detalle. */
@Composable
fun IconoDesplegar(tamano: Dp = 20.dp, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(Modifier.size(tamano)) {
        val flecha = Path().apply {
            moveTo(size.width * 0.24f, size.height * 0.40f)
            lineTo(size.width * 0.50f, size.height * 0.66f)
            lineTo(size.width * 0.76f, size.height * 0.40f)
        }
        drawPath(
            flecha,
            color,
            style = Stroke(width = size.width * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Lista de reportes, para la navegación lateral. */
@Composable
fun IconoIncidentes(tamano: Dp = 20.dp, color: Color = Color.White) {
    Canvas(Modifier.size(tamano)) {
        val grosor = size.width * 0.12f
        listOf(0.24f, 0.50f, 0.76f).forEach { y ->
            drawCircle(color, radius = grosor * 0.72f, center = Offset(size.width * 0.14f, size.height * y))
            drawLine(
                color = color,
                start = Offset(size.width * 0.34f, size.height * y),
                end = Offset(size.width * 0.90f, size.height * y),
                strokeWidth = grosor,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Fila de ícono + texto para la navegación lateral. */
@Composable
fun FilaNavegacion(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    fin: (@Composable () -> Unit)? = null,
    icono: @Composable () -> Unit,
) {
    val colorTexto = if (seleccionado) Color.White else PanelColores.TextoSobreVerde.copy(alpha = 0.92f)
    Surface(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.small,
        color = if (seleccionado) PanelColores.VerdeAndino else Color.Transparent,
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icono()
            Text(texto, style = MaterialTheme.typography.labelLarge, color = colorTexto, modifier = Modifier.weight(1f))
            fin?.invoke()
        }
    }
}
