package pe.civika.panel.feature.incidentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pe.civika.panel.ui.IconoDesplegar
import pe.civika.panel.ui.PanelColores
import pe.civika.panel.ui.TarjetaPanel
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.Rol

/**
 * Columna derecha del mockup de incidentes: foto, código, título, selector de estado,
 * comentario para el ciudadano y el botón «Guardar cambio» (RF-13).
 */
@Composable
fun DetalleIncidente(
    incidente: Incidente,
    rol: Rol,
    estadoObjetivo: Estado?,
    comentario: String,
    aviso: String?,
    avisoError: Boolean,
    alElegirEstado: (Estado) -> Unit,
    alComentar: (String) -> Unit,
    alGuardar: () -> Unit,
    modifier: Modifier = Modifier,
    alVolver: (() -> Unit)? = null,
) {
    TarjetaPanel(modifier) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (alVolver != null) {
                Text(
                    "← Volver a la lista",
                    style = MaterialTheme.typography.labelLarge,
                    color = PanelColores.TerracotaTexto,
                    modifier = Modifier.padding(bottom = 4.dp).clickable(onClick = alVolver),
                )
            }

            MarcoDeFoto()

            Text(incidente.codigo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                incidente.titulo,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "${incidente.zona}, ${incidente.apoyos} apoyos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Cambiar estado", style = MaterialTheme.typography.labelLarge)
            SelectorEstado(
                actual = incidente.estado,
                elegido = estadoObjetivo,
                rol = rol,
                alElegir = alElegirEstado,
            )
            val acciones = accionesDisponibles(incidente.estado, rol)
            if (acciones.isEmpty()) {
                Text(
                    "Tu rol todavía no puede cambiar este estado; sigue el curso normal del reporte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("Comentario para el ciudadano", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = comentario,
                onValueChange = alComentar,
                modifier = Modifier.fillMaxWidth().height(104.dp),
                placeholder = { Text("Por ejemplo: programado para la cuadrilla del jueves.") },
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = alGuardar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = estadoObjetivo != null && acciones.isNotEmpty(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Guardar cambio", style = MaterialTheme.typography.labelLarge)
            }

            if (aviso != null) {
                Text(
                    aviso,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (avisoError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/** Marco de la foto del reporte: hoy es un placeholder, las fotos llegan con Storage (Sprint 2). */
@Composable
private fun MarcoDeFoto() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(PanelColores.Neutro, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Foto del reporte",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Selector con el estado actual y las transiciones que el rol puede hacer (arquitectura §5).
 * Es un menú simple sobre un campo de solo lectura: `ExposedDropdownMenuBox` cambió de API
 * entre versiones y este camino es estable en Compose Multiplatform.
 */
@Composable
private fun SelectorEstado(
    actual: Estado,
    elegido: Estado?,
    rol: Rol,
    alElegir: (Estado) -> Unit,
) {
    var abierto by remember { mutableStateOf(false) }
    val opciones = listOf(actual) + accionesDisponibles(actual, rol)
    val mostrado = elegido ?: actual

    Box {
        Surface(
            onClick = { abierto = true },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(mostrado.etiqueta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconoDesplegar(tamano = 24.dp)
            }
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.etiqueta) },
                    onClick = {
                        abierto = false
                        alElegir(opcion)
                    },
                )
            }
        }
    }
}
