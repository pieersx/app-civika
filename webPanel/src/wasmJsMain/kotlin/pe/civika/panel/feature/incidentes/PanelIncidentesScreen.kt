package pe.civika.panel.feature.incidentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.panel.ui.ChipPanel
import pe.civika.panel.ui.IconoBusqueda
import pe.civika.panel.ui.PanelColores
import pe.civika.panel.ui.SenalEstado
import pe.civika.panel.ui.TarjetaPanel
import pe.civika.shared.domain.Incidente

/** Ancho a partir del cual la columna de detalle se pone al lado de la tabla (mockup a 1920 px). */
private val ANCHO_CON_DETALLE = 1100.dp

/** Por debajo de este ancho el título y el buscador de la cabecera van apilados. */
private val ANCHO_CABECERA_EN_FILA = 760.dp

/**
 * Lista de incidentes con filtros, búsqueda y la columna de detalle.
 *
 * En pantallas angostas el detalle se abre a pantalla completa (`mostrarDetalleEnAngosto`) y se
 * vuelve con `alVolverDeDetalle`; en anchos chicos la selección de una fila avisa con
 * `alAbrirDetalle`. En el ancho del mockup (1920 px) la columna de detalle va al lado de la tabla.
 */
@Composable
fun PanelIncidentesScreen(
    viewModel: PanelIncidentesViewModel = koinViewModel(),
    mostrarDetalleEnAngosto: Boolean = false,
    alAbrirDetalle: () -> Unit = {},
    alVolverDeDetalle: () -> Unit = {},
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val conDetalleLateral = maxWidth >= ANCHO_CON_DETALLE
            when {
                estado.cargando -> ContenidoCentrado {
                    CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando incidentes" })
                }
                estado.error != null -> ContenidoCentrado {
                    Text(
                        estado.error.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                estado.incidentes.isEmpty() -> ContenidoCentrado {
                    Text("No hay incidentes por atender en la zona piloto.", style = MaterialTheme.typography.bodyLarge)
                }
                conDetalleLateral -> Row(Modifier.fillMaxSize()) {
                    TablaYFiltros(estado, viewModel, Modifier.weight(1f).fillMaxHeight())
                    Column(Modifier.width(380.dp).fillMaxHeight().padding(top = 32.dp, end = 32.dp, bottom = 32.dp)) {
                        DetalleEnPanel(estado, viewModel, Modifier.fillMaxHeight(), alVolver = null)
                    }
                }
                mostrarDetalleEnAngosto -> Column(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxWidth().padding(start = 32.dp, top = 32.dp, end = 32.dp)) {
                        Text(
                            "Detalle del incidente",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    DetalleEnPanel(
                        estado,
                        viewModel,
                        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
                        alVolver = alVolverDeDetalle,
                    )
                }
                else -> TablaYFiltros(estado, viewModel, Modifier.fillMaxSize(), alAbrirDetalle)
            }
        }
    }
}

@Composable
private fun DetalleEnPanel(
    estado: PanelIncidentesUiState,
    viewModel: PanelIncidentesViewModel,
    modifier: Modifier = Modifier,
    alVolver: (() -> Unit)?,
) {
    val incidente = estado.seleccionado
    if (incidente == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "Selecciona un incidente para ver su detalle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    DetalleIncidente(
        incidente = incidente,
        rol = viewModel.rol,
        estadoObjetivo = estado.estadoObjetivo,
        comentario = estado.comentario,
        aviso = estado.aviso,
        avisoError = estado.avisoError,
        alElegirEstado = viewModel::elegirEstado,
        alComentar = viewModel::comentar,
        alGuardar = viewModel::guardarCambio,
        modifier = modifier,
        alVolver = alVolver,
    )
}

@Composable
private fun ContenidoCentrado(contenido: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun TablaYFiltros(
    estado: PanelIncidentesUiState,
    viewModel: PanelIncidentesViewModel,
    modifier: Modifier = Modifier,
    alAbrirDetalle: () -> Unit = {},
) {
    BoxWithConstraints(modifier) {
        // Con menos de este ancho no caben ni los chips en fila ni las columnas fijas de la
        // tabla: los chips se envuelven y la tabla pasa a fichas apiladas con los mismos datos.
        val angosto = maxWidth < ANCHO_CABECERA_EN_FILA
        Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // En pantallas angostas el buscador no cabe al lado del título (se lo tragaba y el
            // título quedaba de una letra por línea); ahí va debajo, a todo el ancho.
            if (angosto) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TituloYResumen(estado.resumen, Modifier.fillMaxWidth())
                    BuscadorIncidentes(estado.busqueda, viewModel::buscar, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    TituloYResumen(estado.resumen, Modifier.weight(1f))
                    BuscadorIncidentes(estado.busqueda, viewModel::buscar, Modifier.width(320.dp))
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroIncidentes.entries.forEach { filtro ->
                    ChipPanel(
                        texto = filtro.etiqueta,
                        seleccionado = estado.filtro == filtro,
                        onClick = { viewModel.cambiarFiltro(filtro) },
                    )
                }
            }

            TarjetaPanel(Modifier.weight(1f)) {
                Column(Modifier.fillMaxSize()) {
                    if (!angosto) EncabezadoTabla()
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (estado.visibles.isEmpty()) {
                        Text(
                            "Ningún incidente coincide con este filtro o búsqueda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                            items(estado.visibles, key = { it.codigo }) { incidente ->
                                FilaIncidente(
                                    incidente = incidente,
                                    seleccionada = estado.seleccionado?.codigo == incidente.codigo,
                                    alSeleccionar = {
                                        viewModel.seleccionar(incidente.codigo)
                                        alAbrirDetalle()
                                    },
                                    angosta = angosto,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TituloYResumen(resumen: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Incidentes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            resumen,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BuscadorIncidentes(texto: String, alBuscar: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = texto,
        onValueChange = alBuscar,
        modifier = modifier,
        singleLine = true,
        leadingIcon = { IconoBusqueda(tamano = 20.dp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        placeholder = { Text("Buscar por código o dirección") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun EncabezadoTabla() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Estado", Modifier.width(150.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Incidente", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Categoría", Modifier.width(150.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Apoyos", Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Prioridad", Modifier.width(90.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Antigüedad", Modifier.width(90.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilaIncidente(incidente: Incidente, seleccionada: Boolean, alSeleccionar: () -> Unit, angosta: Boolean) {
    val fondo = if (seleccionada) PanelColores.FilaSeleccionada else MaterialTheme.colorScheme.surface
    Surface(
        color = fondo,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alSeleccionar)
            .semantics(mergeDescendants = true) {},
    ) {
        if (angosta) {
            FilaIncidenteAngosta(incidente)
        } else {
            FilaIncidenteTabla(incidente)
        }
    }
}

/** Fila de la tabla en el ancho del mockup (1920 px): columnas con medidas fijas. */
@Composable
private fun FilaIncidenteTabla(incidente: Incidente) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(150.dp)) { SenalEstado(incidente.estado) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(incidente.titulo, style = MaterialTheme.typography.titleMedium)
            Text(
                "${incidente.codigo}, ${incidente.zona}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(incidente.categoria.nombre, Modifier.width(150.dp), style = MaterialTheme.typography.bodyMedium)
        Text("${incidente.apoyos}", Modifier.width(70.dp), style = MaterialTheme.typography.bodyMedium)
        Text(etiquetaPrioridad(incidente.prioridad), Modifier.width(90.dp), style = MaterialTheme.typography.labelLarge)
        Text(antiguedadTexto(incidente.diasAbierto), Modifier.width(90.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Fila apilada para pantallas angostas: misma información que la tabla (estado, incidente,
 * categoría, apoyos, prioridad y antigüedad) pero en tres líneas, porque las columnas de
 * ancho fijo no caben en 390 px.
 */
@Composable
private fun FilaIncidenteAngosta(incidente: Incidente) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SenalEstado(incidente.estado)
        Text(incidente.titulo, style = MaterialTheme.typography.titleMedium)
        Text(
            "${incidente.codigo}, ${incidente.zona}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${incidente.categoria.nombre} · ${incidente.apoyos} apoyos · " +
                "${etiquetaPrioridad(incidente.prioridad)} · ${antiguedadTexto(incidente.diasAbierto)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
