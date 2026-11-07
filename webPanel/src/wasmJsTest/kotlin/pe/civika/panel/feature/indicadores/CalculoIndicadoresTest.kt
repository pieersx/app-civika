package pe.civika.panel.feature.indicadores

import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Incidente
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Indicadores del distrito (RF-14): qué entra en cada periodo, cómo se reparten los porcentajes
 * por categoría y cómo se escriben los días en los KPI y en las barras del mockup.
 */
class CalculoIndicadoresTest {

    private fun incidente(
        codigo: String,
        categoria: Categoria,
        estado: Estado,
        diasAbierto: Int,
    ) = Incidente(codigo, "Reporte $codigo", categoria, estado, apoyos = 0, diasAbierto, zona = "Zona piloto")

    /** A, B y C son de «este mes»; D y E solo entran en «últimos 3 meses». */
    private val muestra = listOf(
        incidente("CVK-000001", CategoriasPiloto.VIAL, Estado.RESUELTO, diasAbierto = 4),
        incidente("CVK-000002", CategoriasPiloto.VIAL, Estado.RESUELTO, diasAbierto = 8),
        incidente("CVK-000003", CategoriasPiloto.VIAL, Estado.EN_ATENCION, diasAbierto = 2),
        incidente("CVK-000004", CategoriasPiloto.RESIDUOS, Estado.EN_REVISION, diasAbierto = 60),
        incidente("CVK-000005", CategoriasPiloto.ACCESIBILIDAD, Estado.RESUELTO, diasAbierto = 70),
    )

    @Test
    fun elPeriodoDelMockupSeAplicaAntesDeContar() {
        val mes = calcularIndicadores(muestra, PeriodoIndicadores.MES)
        val tresMeses = calcularIndicadores(muestra, PeriodoIndicadores.TRES_MESES)

        assertEquals(3, mes.recibidos)
        assertEquals(5, tresMeses.recibidos)
        assertEquals(listOf("Este mes", "Últimos 3 meses"), PeriodoIndicadores.entries.map { it.etiqueta })
    }

    @Test
    fun losKpiDelMockupSalenDeLosIncidentesDelPeriodo() {
        val mes = calcularIndicadores(muestra, PeriodoIndicadores.MES)

        assertEquals(2, mes.resueltos)
        assertEquals(1, mes.abiertos)
        assertEquals(66, mes.porcentajeResuelto)
        val promedio = assertNotNull(mes.diasPromedioAtencion)
        assertEquals(6.0, promedio)
        assertEquals("6 días", diasLargoTexto(promedio))
    }

    @Test
    fun elPorcentajePorCategoriaSoloCuentaLasCategoriasConIncidentes() {
        val mes = calcularIndicadores(muestra, PeriodoIndicadores.MES)

        assertEquals(listOf("Pistas y veredas"), mes.porCategoria.map { it.categoria })
        assertEquals(66, mes.porCategoria.single().porcentaje)
        assertEquals(3, mes.porCategoria.single().total)
        assertEquals(2, mes.porCategoria.single().resueltos)
    }

    @Test
    fun enTresMesesEntraLaCategoriaQueAunNoSeResuelve() {
        val tresMeses = calcularIndicadores(muestra, PeriodoIndicadores.TRES_MESES)

        assertEquals(60, tresMeses.porcentajeResuelto)
        assertEquals("Residuos", tresMeses.menosResuelta?.categoria)
        assertEquals(0, tresMeses.menosResuelta?.porcentaje)
        assertEquals("Accesibilidad", tresMeses.masLenta?.categoria)
        assertEquals(70.0, tresMeses.masLenta?.dias)
    }

    @Test
    fun sinResueltosNoHayDiasQuePromediar() {
        val soloAbiertos = listOf(incidente("CVK-000006", CategoriasPiloto.VIAL, Estado.EN_ATENCION, diasAbierto = 3))
        val indicadores = calcularIndicadores(soloAbiertos, PeriodoIndicadores.MES)

        assertNull(indicadores.diasPromedioAtencion)
        assertTrue(indicadores.diasPorCategoria.isEmpty())
        assertEquals(0, indicadores.porcentajeResuelto)
    }

    @Test
    fun sinIncidentesElPanelSaleVacio() {
        val indicadores = calcularIndicadores(emptyList(), PeriodoIndicadores.MES)

        assertEquals(0, indicadores.recibidos)
        assertEquals(0, indicadores.porcentajeResuelto)
        assertNull(indicadores.diasPromedioAtencion)
        assertTrue(indicadores.porCategoria.isEmpty())
    }

    @Test
    fun losDiasSeEscribenComoEnElMockup() {
        assertEquals("6.4 d", diasCortoTexto(6.4))
        assertEquals("6 d", diasCortoTexto(6.0))
        assertEquals("4 días", diasLargoTexto(4.0))
        assertEquals("1 día", diasLargoTexto(1.0))
        assertEquals("4.5 días", diasLargoTexto(4.5))
    }
}
