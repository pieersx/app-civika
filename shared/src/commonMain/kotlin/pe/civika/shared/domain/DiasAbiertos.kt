package pe.civika.shared.domain

import kotlin.time.Instant

/**
 * Días de calendario en America/Lima (UTC−5, sin horario de verano) entre dos instantes, igual que en SQL.
 * Un reporte de las 11 p. m. cuenta un día a las 12:30 a. m. Nunca es negativo: el reloj del celular puede estar
 * atrasado.
 */
fun diasCalendarioLima(creadoEn: Instant, ahora: Instant): Int {
    val dias = diaLima(ahora) - diaLima(creadoEn)
    return dias.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
}

private const val SEGUNDOS_POR_DIA = 86_400L
private const val DESFASE_LIMA_SEGUNDOS = 5 * 3_600L

private fun diaLima(instante: Instant): Long =
    (instante.epochSeconds - DESFASE_LIMA_SEGUNDOS).floorDiv(SEGUNDOS_POR_DIA)
