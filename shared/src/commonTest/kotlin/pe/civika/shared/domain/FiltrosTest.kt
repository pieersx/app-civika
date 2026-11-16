package pe.civika.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FiltrosTest {

    private fun incidente(id: String, categoria: Categoria, estado: Estado) =
        Incidente(id, id, id, categoria, estado, 0, 0, "zona")

    private val todos = listOf(
        incidente("a", CategoriasPiloto.VIAL, Estado.DERIVADO),
        incidente("b", CategoriasPiloto.RESIDUOS, Estado.EN_ATENCION),
        incidente("c", CategoriasPiloto.RESIDUOS, Estado.RESUELTO),
        incidente("d", CategoriasPiloto.ACCESIBILIDAD, Estado.EN_REVISION),
    )

    @Test
    fun sinFiltrosPasanTodos() {
        assertEquals(4, filtrar(todos, FiltroMapa()).size)
        assertFalse(FiltroMapa().activo)
    }

    @Test
    fun filtraPorCategoria() {
        val ids = filtrar(todos, FiltroMapa().conCategoria("residuos")).map { it.id }
        assertEquals(listOf("b", "c"), ids)
    }

    @Test
    fun lasFamiliasAbiertoAgrupanTresEstados() {
        val ids = filtrar(todos, FiltroMapa().conFamilia(Familia.ABIERTO)).map { it.id }
        assertEquals(listOf("a", "d"), ids)
    }

    @Test
    fun categoriaYFamiliaSeCombinanConY() {
        val filtro = FiltroMapa().conCategoria("residuos").conFamilia(Familia.RESUELTO)
        assertEquals(listOf("c"), filtrar(todos, filtro).map { it.id })
    }

    @Test
    fun tocarDosVecesUnChipLoQuita() {
        val filtro = FiltroMapa().conCategoria("vial").conCategoria("vial")
        assertTrue(filtro.categorias.isEmpty())
        assertFalse(filtro.activo)
    }

    @Test
    fun misReportesFiltraPorEstadoYRelacion() {
        fun mi(estado: Estado, relacion: Relacion) = MiIncidente(incidente("x", CategoriasPiloto.VIAL, estado), relacion, null)
        assertTrue(FiltroMisReportes.ABIERTOS.aplica(mi(Estado.DERIVADO, Relacion.AUTOR)))
        assertFalse(FiltroMisReportes.ABIERTOS.aplica(mi(Estado.RESUELTO, Relacion.AUTOR)))
        assertTrue(FiltroMisReportes.RESUELTOS.aplica(mi(Estado.RESUELTO, Relacion.APOYO)))
        assertTrue(FiltroMisReportes.MIS_APOYOS.aplica(mi(Estado.DERIVADO, Relacion.APOYO)))
        assertFalse(FiltroMisReportes.MIS_APOYOS.aplica(mi(Estado.DERIVADO, Relacion.AUTOR)))
    }

    @Test
    fun unEnvioPendienteCuentaComoAbiertoYNuncaComoResuelto() {
        val pendiente = ReportePendiente("c", "u", TipoEnvio.NUEVO, null, 0.0, 0.0, 1, "Vial", "Bache profundo", emptyList())
        assertTrue(FiltroMisReportes.ABIERTOS.aplica(pendiente))
        assertFalse(FiltroMisReportes.RESUELTOS.aplica(pendiente))
        assertFalse(FiltroMisReportes.MIS_APOYOS.aplica(pendiente))
        assertTrue(FiltroMisReportes.MIS_APOYOS.aplica(pendiente.copy(tipo = TipoEnvio.APOYO)))
    }
}
