package pe.civika.panel.feature.indicadores

import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Cálculo de los indicadores del distrito (RF-14) a partir de los incidentes del repositorio.
 * Regla de `direccion-visual.md`: los colores de estado nunca se usan como colores de datos.
 *
 * Lógica pura, sin Compose ni ViewModels, para probarla en `wasmJsTest`.
 */

/** Ventana de tiempo del selector «Este mes / Últimos 3 meses» del mockup. */
enum class PeriodoIndicadores(val etiqueta: String, val dias: Int) {
    MES("Este mes", 30),
    TRES_MESES("Últimos 3 meses", 90),
    ;

    fun incluye(diasAbierto: Int) = diasAbierto in 0..dias
}

data class IndicadorCategoria(
    val categoria: String,
    val total: Int,
    val resueltos: Int,
    val porcentaje: Int,
)

data class DiasPromedio(val categoria: String, val dias: Double)

data class Indicadores(
    val recibidos: Int,
    val abiertos: Int,
    val resueltos: Int,
    val porcentajeResuelto: Int,
    /** `null` cuando todavía no se resolvió nada en el periodo. */
    val diasPromedioAtencion: Double?,
    val porCategoria: List<IndicadorCategoria>,
    /** Solo las categorías con incidentes resueltos: sin eso no hay días que promediar. */
    val diasPorCategoria: List<DiasPromedio>,
) {
    val masLenta: DiasPromedio? get() = diasPorCategoria.maxByOrNull { it.dias }
    val menosResuelta: IndicadorCategoria? get() = porCategoria.minByOrNull { it.porcentaje }
}

fun calcularIndicadores(incidentes: List<Incidente>, periodo: PeriodoIndicadores): Indicadores {
    val delPeriodo = incidentes.filter { periodo.incluye(it.diasAbierto) }
    val resueltos = delPeriodo.filter { it.estado == Estado.RESUELTO }

    val porCategoria = CategoriasPiloto.todas.mapNotNull { categoria ->
        val delCat = delPeriodo.filter { it.categoria.serviceCode == categoria.serviceCode }
        if (delCat.isEmpty()) return@mapNotNull null
        val resueltosDelCat = delCat.count { it.estado == Estado.RESUELTO }
        IndicadorCategoria(
            categoria = categoria.nombre,
            total = delCat.size,
            resueltos = resueltosDelCat,
            porcentaje = resueltosDelCat * 100 / delCat.size,
        )
    }

    val diasPorCategoria = porCategoria.mapNotNull { indicador ->
        val categoria = CategoriasPiloto.todas.first { it.nombre == indicador.categoria }
        val dias = resueltos
            .filter { it.categoria.serviceCode == categoria.serviceCode }
            .map { it.diasAbierto }
        if (dias.isEmpty()) null else DiasPromedio(indicador.categoria, dias.average())
    }

    return Indicadores(
        recibidos = delPeriodo.size,
        abiertos = delPeriodo.count { it.estado.familia == Familia.ABIERTO || it.estado.familia == Familia.EN_ATENCION },
        resueltos = resueltos.size,
        porcentajeResuelto = if (delPeriodo.isEmpty()) 0 else resueltos.size * 100 / delPeriodo.size,
        diasPromedioAtencion = if (resueltos.isEmpty()) null else resueltos.map { it.diasAbierto }.average(),
        porCategoria = porCategoria,
        diasPorCategoria = diasPorCategoria,
    )
}

/** «6.4 d» (barras del mockup). Un decimal, con punto: es el formato del mockup, sin localizar. */
fun diasCortoTexto(dias: Double): String {
    val decenas = (dias * 10).roundToInt()
    val entero = decenas / 10
    val decimal = abs(decenas % 10)
    return if (decimal == 0) "$entero d" else "$entero.$decimal d"
}

/** «4 días» (KPI del mockup). */
fun diasLargoTexto(dias: Double): String {
    val decenas = (dias * 10).roundToInt()
    val entero = decenas / 10
    val decimal = abs(decenas % 10)
    val diasTexto = if (entero == 1) "día" else "días"
    return if (decimal == 0) "$entero $diasTexto" else "$entero.$decimal $diasTexto"
}
