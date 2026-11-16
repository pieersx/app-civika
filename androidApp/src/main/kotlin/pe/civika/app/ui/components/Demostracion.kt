package pe.civika.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import pe.civika.app.R
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.shared.di.ModoApp

/**
 * Aviso discreto del «modo demostración»: la app no tiene Supabase configurado y muestra datos de ejemplo.
 * No dibuja nada cuando la app está conectada.
 */
@Composable
fun AvisoDemostracion(modifier: Modifier = Modifier, detalle: Boolean = false) {
    val modo: ModoApp = koinInject()
    if (modo != ModoApp.DEMOSTRACION) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CivikaTheme.colores.borde),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.Science, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(
                stringResource(if (detalle) R.string.modo_demostracion_detalle else R.string.modo_demostracion),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
