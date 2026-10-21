package pe.civika.app.feature.incidentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.shared.domain.Incidente

@Composable
fun IncidentesScreen(viewModel: IncidentesViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Text(
                text = "Incidentes cerca de ti",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp).semantics { heading() },
            )
            when (val e = estado) {
                IncidentesUiState.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando incidentes" })
                }
                IncidentesUiState.Vacio -> MensajeCentrado(
                    "No hay incidentes por atender cerca de ti. Si ves un problema en la calle, repórtalo.",
                )
                is IncidentesUiState.Error -> MensajeCentrado(e.mensaje)
                is IncidentesUiState.Listo -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(e.incidentes, key = { it.codigo }) { TarjetaIncidente(it) }
                }
            }
        }
    }
}

@Composable
private fun MensajeCentrado(texto: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TarjetaIncidente(incidente: Incidente) {
    // mergeDescendants: TalkBack lee la tarjeta completa de una vez en lugar de texto por texto.
    Card(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(incidente.estado.etiqueta, style = MaterialTheme.typography.labelLarge)
            Text(incidente.titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                "${incidente.categoria.nombre}. Prioridad ${incidente.prioridad}, ${incidente.apoyos} apoyos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
