package pe.civika.app.feature.mapa

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import pe.civika.app.ui.components.dibujarSenal
import pe.civika.app.ui.components.iconoDeCategoria
import pe.civika.app.ui.theme.CivikaColores
import pe.civika.shared.domain.Familia

/** Categorías con ícono propio en el mapa. Cualquier otra se dibuja con el de «vial». */
val CATEGORIAS_CON_ICONO = listOf("vial", "residuos", "accesibilidad")

fun categoriaDeMarcador(serviceCode: String): String = if (serviceCode in CATEGORIAS_CON_ICONO) serviceCode else "vial"

fun idDeMarcador(familia: Familia, categoria: String) = "m-${familia.name}-$categoria"

/**
 * Marcador del mapa: la señal de la familia de estado (triángulo, rombo, círculo o cuadrado) con el ícono de la
 * categoría dentro. Se dibuja como bitmap para añadirlo al estilo de MapLibre como imagen.
 */
fun crearMarcador(familia: Familia, categoria: String, colores: CivikaColores, densidad: Density, ladoDp: Float = 44f): Bitmap {
    val lado = (ladoDp * densidad.density).toInt().coerceAtLeast(1)
    val imagen = ImageBitmap(lado, lado)
    val lienzo = Canvas(imagen)
    val color = colores.estado(familia)
    val tamano = Size(lado.toFloat(), lado.toFloat())

    CanvasDrawScope().draw(densidad, LayoutDirection.Ltr, lienzo, tamano) {
        dibujarSenal(familia, color.relleno, color.contorno, grosorContorno = 2f * densidad.density)
    }

    val icono = iconoDeCategoria(categoria)
    val trazos = icono.trazos()
    val ladoIcono = lado * if (familia == Familia.ABIERTO) 0.42f else 0.52f
    val escala = ladoIcono / icono.viewportWidth
    val origenX = (lado - ladoIcono) / 2f
    // El triángulo tiene más cuerpo abajo: el ícono baja un poco para quedar en su centro visual.
    val origenY = (lado - ladoIcono) / 2f + if (familia == Familia.ABIERTO) lado * 0.08f else 0f
    val pincel = Paint().apply {
        this.color = color.icono
        style = PaintingStyle.Fill
        isAntiAlias = true
    }
    lienzo.save()
    lienzo.translate(origenX, origenY)
    lienzo.scale(escala, escala)
    trazos.forEach { lienzo.drawPath(it, pincel) }
    lienzo.restore()
    return imagen.asAndroidBitmap()
}

private fun ImageVector.trazos(): List<Path> {
    val trazos = mutableListOf<Path>()
    fun visitar(nodo: VectorNode) {
        when (nodo) {
            is VectorGroup -> nodo.forEach { visitar(it) }
            is VectorPath -> trazos += nodo.pathData.toPath().also { it.fillType = nodo.pathFillType }
        }
    }
    visitar(root)
    return trazos
}
