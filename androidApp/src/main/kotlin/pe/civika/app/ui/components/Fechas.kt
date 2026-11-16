package pe.civika.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Instant

private val ZONA_LIMA: ZoneId = ZoneId.of("America/Lima")
private val LOCALE_PERU = Locale.forLanguageTag("es-PE")
private val FORMATO = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", LOCALE_PERU)

/** «13 nov 2026, 14:03», siempre en hora de Lima: es la hora que cuentan los plazos y los días abiertos. */
fun formatearFecha(fecha: Instant): String =
    FORMATO.format(java.time.Instant.ofEpochSecond(fecha.epochSeconds).atZone(ZONA_LIMA))

@Composable
fun rememberFecha(fecha: Instant): String = remember(fecha) { formatearFecha(fecha) }
