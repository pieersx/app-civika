package pe.civika.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.civika.app.R

/** Esqueleto con brillo suave mientras carga. TalkBack solo oye «Cargando». */
@Composable
fun CargandoEsqueleto(modifier: Modifier = Modifier, filas: Int = 4) {
    val transicion = rememberInfiniteTransition(label = "esqueleto")
    val brillo by transicion.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "brillo",
    )
    val descripcion = stringResource(R.string.cargando)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(filas) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(brillo)
                    .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Bloque(Modifier.width(96.dp).height(16.dp))
                Bloque(Modifier.fillMaxWidth().height(18.dp))
                Bloque(Modifier.fillMaxWidth(0.6f).height(14.dp))
            }
        }
    }
}

@Composable
private fun Bloque(modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f), RoundedCornerShape(6.dp)))
}

/** Pantalla vacía: invita a actuar en vez de solo decir «no hay nada». */
@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.encabezado())
        Text(texto, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (accion != null && onAccion != null) {
            BotonPrincipal(accion, onAccion, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Error con salida: dice qué pasó y deja reintentar. */
@Composable
fun EstadoError(mensaje: String, onReintentar: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onReintentar != null) BotonSecundario(stringResource(R.string.reintentar), onReintentar)
    }
}
