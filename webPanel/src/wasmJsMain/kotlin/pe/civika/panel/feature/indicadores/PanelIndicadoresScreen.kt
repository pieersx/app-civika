package pe.civika.panel.feature.indicadores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.panel.ui.EnlacePanel
import pe.civika.panel.ui.PanelColores
import pe.civika.panel.ui.TarjetaPanel

/** Por debajo de este ancho las tarjetas y las barras van apiladas en lugar de en fila. */
private val ANCHO_EN_FILA = 900.dp

@Composable
fun PanelIndicadoresScreen(viewModel: PanelIndicadoresViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when {
            estado.cargando -> Centrado { CircularProgressIndicator(Modifier.semantics { contentDescription = "Cargando indicadores" }) }
            estado.error != null -> Centrado {
                Text(
                    estado.error.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            else -> Contenido(estado, viewModel)
        }
    }
}

@Composable
private fun Centrado(contenido: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun Contenido(estado: PanelIndicadoresUiState, viewModel: PanelIndicadoresViewModel) {
    val indicadores = estado.indicadores
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // En pantallas angostas no caben tres tarjetas en fila ni el título junto al selector
        // de período: todo se apila, con los mismos datos del mockup.
        val angosto = maxWidth < ANCHO_EN_FILA
        Column(
            Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (angosto) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TituloIndicadores(Modifier.fillMaxWidth())
                    SelectorPeriodo(estado.periodo, viewModel::cambiarPeriodo)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    TituloIndicadores(Modifier.weight(1f))
                    SelectorPeriodo(estado.periodo, viewModel::cambiarPeriodo)
                }
            }

            if (indicadores == null) {
                Text("Sin datos suficientes.", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            if (angosto) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TarjetaRecibidos(indicadores, Modifier.fillMaxWidth())
                    TarjetaResueltos(indicadores, Modifier.fillMaxWidth())
                    TarjetaTiempo(indicadores, Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TarjetaRecibidos(indicadores, Modifier.weight(1f))
                    TarjetaResueltos(indicadores, Modifier.weight(1f))
                    TarjetaTiempo(indicadores, Modifier.weight(1f))
                }
            }

            if (estado.sinDatos) {
                TarjetaPanel {
                    Text(
                        "Aún no hay reportes en este periodo. Cuando la vecindad empiece a reportar, aquí verás los indicadores.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else if (angosto) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TarjetaPorcentajes(indicadores, Modifier.fillMaxWidth(), angosto)
                    TarjetaDias(indicadores, Modifier.fillMaxWidth(), angosto)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TarjetaPorcentajes(indicadores, Modifier.weight(1f), angosto = false)
                    TarjetaDias(indicadores, Modifier.weight(1f), angosto = false)
                }
            }
        }
    }
}

@Composable
private fun TituloIndicadores(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Indicadores del distrito",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Calculados solo con los reportes hechos en Cívika. Son referenciales, no una evaluación oficial.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TarjetaRecibidos(indicadores: Indicadores, modifier: Modifier = Modifier) = TarjetaKpi(
    etiqueta = "Reportes recibidos",
    valor = "${indicadores.recibidos}",
    nota = if (indicadores.recibidos == 0) "En el periodo seleccionado" else "${indicadores.abiertos} siguen abiertos",
    modifier = modifier,
)

@Composable
private fun TarjetaResueltos(indicadores: Indicadores, modifier: Modifier = Modifier) = TarjetaKpi(
    etiqueta = "Resueltos",
    valor = "${indicadores.resueltos}",
    nota = if (indicadores.recibidos == 0) "En el periodo seleccionado" else "${indicadores.porcentajeResuelto} % de lo recibido",
    modifier = modifier,
)

@Composable
private fun TarjetaTiempo(indicadores: Indicadores, modifier: Modifier = Modifier) = TarjetaKpi(
    etiqueta = "Tiempo promedio de atención",
    valor = indicadores.diasPromedioAtencion?.let { diasLargoTexto(it) } ?: "—",
    nota = "Desde que se reporta hasta que se resuelve",
    modifier = modifier,
)

@Composable
private fun SelectorPeriodo(actual: PeriodoIndicadores, alCambiar: (PeriodoIndicadores) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PeriodoIndicadores.entries.forEach { periodo ->
            val seleccionado = periodo == actual
            Surface(
                onClick = { alCambiar(periodo) },
                shape = MaterialTheme.shapes.small,
                color = if (seleccionado) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                border = if (seleccionado) {
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                } else {
                    null
                },
            ) {
                Text(
                    periodo.etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (seleccionado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaKpi(etiqueta: String, valor: String, nota: String, modifier: Modifier = Modifier) {
    TarjetaPanel(modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.displaySmall)
            Text(nota, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TarjetaPorcentajes(indicadores: Indicadores, modifier: Modifier = Modifier, angosto: Boolean) {
    var verComoTabla by remember { mutableStateOf(false) }
    TarjetaPanel(modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Porcentaje resuelto por categoría",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (indicadores.porCategoria.isNotEmpty()) {
                    EnlacePanel(
                        texto = if (verComoTabla) "Ver como barras" else "Ver como tabla",
                        onClick = { verComoTabla = !verComoTabla },
                    )
                }
            }

            if (indicadores.porCategoria.isEmpty()) {
                Text(
                    "No hay reportes en las categorías del piloto en este periodo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (verComoTabla) {
                TablaPorcentajes(indicadores.porCategoria, angosto)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    indicadores.porCategoria.forEach { indicador ->
                        FilaBarraPorcentaje(indicador, angosto)
                    }
                }
            }

            Text(
                "Se cuentan los incidentes resueltos del periodo. Con datos reales la meta es superar el 70 %.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaBarraPorcentaje(indicador: IndicadorCategoria, angosto: Boolean) {
    if (angosto) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(indicador.categoria, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BarraDeDatos(indicador.porcentaje / 100f, Modifier.weight(1f))
                Text("${indicador.porcentaje} %", Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge)
            }
            TextaSinResolver(indicador)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(indicador.categoria, Modifier.width(150.dp), style = MaterialTheme.typography.bodyMedium)
            BarraDeDatos(indicador.porcentaje / 100f, Modifier.weight(1f))
            Text("${indicador.porcentaje} %", Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge)
        }
        TextaSinResolver(indicador)
    }
}

@Composable
private fun TextaSinResolver(indicador: IndicadorCategoria) {
    if (indicador.total > 0 && indicador.resueltos == 0) {
        Text(
            "Sin incidentes resueltos todavía (${indicador.total} en curso).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TablaPorcentajes(indicadores: List<IndicadorCategoria>, angosto: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (angosto) {
            // Sin las columnas de ancho fijo: cada categoría queda en dos líneas.
            indicadores.forEach { indicador ->
                Text(indicador.categoria, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${indicador.resueltos} de ${indicador.total} resueltos · ${indicador.porcentaje} %",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Categoría", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Resueltos", Modifier.width(100.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Total", Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("%", Modifier.width(60.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        indicadores.forEach { indicador ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(indicador.categoria, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("${indicador.resueltos}", Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
                Text("${indicador.total}", Modifier.width(80.dp), style = MaterialTheme.typography.bodyMedium)
                Text("${indicador.porcentaje} %", Modifier.width(60.dp), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TarjetaDias(indicadores: Indicadores, modifier: Modifier = Modifier, angosto: Boolean) {
    TarjetaPanel(modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Días promedio hasta resolver", style = MaterialTheme.typography.titleLarge)

            if (indicadores.diasPorCategoria.isEmpty()) {
                Text(
                    "Todavía no se resolvió ningún incidente en este periodo, así que no hay días que promediar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val maximo = indicadores.diasPorCategoria.maxOf { it.dias }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    indicadores.diasPorCategoria.forEach { diasPromedio ->
                        if (angosto) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(diasPromedio.categoria, style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    BarraDeDatos(
                                        if (maximo <= 0.0) 0f else (diasPromedio.dias / maximo).toFloat(),
                                        Modifier.weight(1f),
                                    )
                                    Text(diasCortoTexto(diasPromedio.dias), Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(diasPromedio.categoria, Modifier.width(150.dp), style = MaterialTheme.typography.bodyMedium)
                                BarraDeDatos(
                                    if (maximo <= 0.0) 0f else (diasPromedio.dias / maximo).toFloat(),
                                    Modifier.weight(1f),
                                )
                                Text(diasCortoTexto(diasPromedio.dias), Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                val masLenta = indicadores.masLenta
                if (masLenta != null) {
                    Text(
                        "Meta interna: resolver en 5 días o menos. ${masLenta.categoria} es la categoría que más tarda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Barra de una sola serie: siempre el color `datos`, nunca un color de estado (guía de diseño). */
@Composable
private fun BarraDeDatos(valor: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(14.dp)
            .background(PanelColores.Neutro, MaterialTheme.shapes.small),
    ) {
        Box(
            Modifier
                .fillMaxWidth(valor.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(PanelColores.Datos, MaterialTheme.shapes.small),
        )
    }
}
