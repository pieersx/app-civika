package pe.civika.panel.feature.incidentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.shared.domain.Incidente

@Composable
fun PanelIncidentesScreen(viewModel: PanelIncidentesViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(32.dp).widthIn(max = 1200.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Incidentes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Zona piloto: Ciudad Universitaria UNMSM y alrededores. Ordenados por prioridad.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (val e = estado) {
                PanelIncidentesUiState.Cargando ->
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando incidentes" })
                PanelIncidentesUiState.Vacio ->
                    Text("No hay incidentes por atender en la zona.", style = MaterialTheme.typography.bodyLarge)
                is PanelIncidentesUiState.Error ->
                    Text(e.mensaje, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                is PanelIncidentesUiState.Listo ->
                    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                        LazyColumn {
                            items(e.incidentes, key = { it.codigo }) { incidente ->
                                FilaIncidente(incidente)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
            }
        }
    }
}

/** Por debajo de este ancho la fila se apila en vertical, para que el panel también se pueda revisar en un celular. */
private val ANCHO_MINIMO_TABLA = 720.dp

@Composable
private fun FilaIncidente(incidente: Incidente) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
    ) {
        if (maxWidth >= ANCHO_MINIMO_TABLA) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(incidente.estado.etiqueta, Modifier.width(120.dp), style = MaterialTheme.typography.labelLarge)
                TituloYCodigo(incidente, Modifier.weight(1f))
                Text(incidente.categoria.nombre, Modifier.width(140.dp))
                Text("${incidente.apoyos} apoyos", Modifier.width(90.dp))
                Text("Prioridad ${incidente.prioridad}", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${incidente.estado.etiqueta}. Prioridad ${incidente.prioridad}",
                    style = MaterialTheme.typography.labelLarge,
                )
                TituloYCodigo(incidente)
                Text(
                    "${incidente.categoria.nombre}, ${incidente.apoyos} apoyos",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TituloYCodigo(incidente: Incidente, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(incidente.titulo, style = MaterialTheme.typography.titleMedium)
        Text(
            "${incidente.codigo}, ${incidente.zona}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
