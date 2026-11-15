package pe.civika.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Accessible
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.civika.app.R
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Familia

/** Ícono del interior de la señal de cada familia. La forma y el ícono acompañan al color: nunca solo color. */
fun Familia.glifo(): ImageVector = when (this) {
    Familia.ABIERTO -> Icons.Rounded.PriorityHigh
    Familia.EN_ATENCION -> Icons.Rounded.Construction
    Familia.RESUELTO -> Icons.Rounded.Check
    Familia.RECHAZADO -> Icons.Rounded.Close
}

fun iconoDeCategoria(serviceCode: String): ImageVector = when (serviceCode) {
    "residuos" -> Icons.Rounded.Delete
    "accesibilidad" -> Icons.AutoMirrored.Rounded.Accessible
    else -> Icons.Rounded.Warning
}

fun Categoria.icono(): ImageVector = iconoDeCategoria(serviceCode)

/** Forma de la señal: triángulo (peligro), rombo (obra), círculo y cuadrado. */
fun formaDeSenal(familia: Familia, lado: Float): Path = Path().apply {
    when (familia) {
        Familia.ABIERTO -> {
            moveTo(lado * 0.5f, lado * 0.05f)
            lineTo(lado * 0.97f, lado * 0.93f)
            lineTo(lado * 0.03f, lado * 0.93f)
            close()
        }
        Familia.EN_ATENCION -> {
            moveTo(lado * 0.5f, 0f)
            lineTo(lado, lado * 0.5f)
            lineTo(lado * 0.5f, lado)
            lineTo(0f, lado * 0.5f)
            close()
        }
        Familia.RESUELTO -> addOval(Rect(0f, 0f, lado, lado))
        Familia.RECHAZADO -> addRoundRect(RoundRect(Rect(0f, 0f, lado, lado), CornerRadius(lado * 0.14f)))
    }
}

fun DrawScope.dibujarSenal(familia: Familia, relleno: Color, contorno: Color, grosorContorno: Float) {
    val lado = size.minDimension - grosorContorno
    val forma = formaDeSenal(familia, lado)
    translateSeguro(grosorContorno / 2f) {
        drawPath(forma, relleno)
        drawPath(forma, contorno, style = Stroke(width = grosorContorno, join = StrokeJoin.Round))
    }
}

private fun DrawScope.translateSeguro(desplazamiento: Float, bloque: DrawScope.() -> Unit) {
    drawContext.transform.translate(desplazamiento, desplazamiento)
    bloque()
    drawContext.transform.translate(-desplazamiento, -desplazamiento)
}

/** Solo la señal (forma + ícono), sin palabra. Quien la use debe aportar la palabra o la descripción. */
@Composable
fun MarcadorSenal(familia: Familia, tamano: Dp, modifier: Modifier = Modifier) {
    val colores = CivikaTheme.colores.estado(familia)
    Box(
        modifier = modifier
            .size(tamano)
            .drawBehind { dibujarSenal(familia, colores.relleno, colores.contorno, 1.5.dp.toPx()) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = familia.glifo(),
            contentDescription = null,
            tint = colores.icono,
            modifier = Modifier
                .size(tamano * 0.56f)
                .offset(y = if (familia == Familia.ABIERTO) tamano * 0.08f else 0.dp),
        )
    }
}

/** Estado del incidente: señal con su forma, su ícono y su palabra. TalkBack lo lee como una sola cosa. */
@Composable
fun SenalEstado(estado: Estado, modifier: Modifier = Modifier) {
    val colores = CivikaTheme.colores.estado(estado.familia)
    val descripcion = stringResource(R.string.cd_estado, estado.etiqueta)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = descripcion },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MarcadorSenal(estado.familia, 28.dp)
        Text(estado.etiqueta, style = MaterialTheme.typography.labelLarge, color = colores.texto)
    }
}

val FormaChipEstado = RoundedCornerShape(8.dp)
