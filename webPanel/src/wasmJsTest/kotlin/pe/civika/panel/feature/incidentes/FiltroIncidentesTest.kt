package pe.civika.panel.feature.incidentes

import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.Rol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reglas de la pantalla de incidentes (RF-13): chips del mockup, búsqueda, orden por prioridad
 * y quién puede cambiar un estado. Lógica pura, sin Compose.
 */
class FiltroIncidentesTest {

    private fun incidente(
        codigo: String,
        estado: Estado,
        categoria: Categoria = CategoriasPiloto.VIAL,
        apoyos: Int = 0,
        diasAbierto: Int = 0,
        titulo: String = "Reporte $codigo",
        zona: String = "Zona piloto",
    ) = Incidente(codigo, titulo, categoria, estado, apoyos, diasAbierto, zona)

    /** Prioridades: 124 = 100 (Alta), 109 y 087 = 30 (Media), 118 = 20 (Baja). */
    private val muestra = listOf(
        incidente("CVK-000124", Estado.DERIVADO, apoyos = 50, diasAbierto = 10, titulo = "Bache profundo", zona = "Ciudad Universitaria"),
        incidente("CVK-000118", Estado.EN_REVISION, categoria = CategoriasPiloto.RESIDUOS, titulo = "Basura acumulada", zona = "Mercado"),
        incidente("CVK-000109", Estado.EN_ATENCION, titulo = "Pista hundida", zona = "Mercado"),
        incidente("CVK-000087", Estado.RESUELTO, titulo = "Rampa despejada", zona = "Ciudad Universitaria"),
    )

    @Test
    fun losChipsDelMockupFiltranPorFamiliaDelEstado() {
        assertEquals(4, filtrarIncidentes(muestra, FiltroIncidentes.TODOS, "").size)
        assertEquals(2, filtrarIncidentes(muestra, FiltroIncidentes.ABIERTOS, "").size)
        assertEquals(1, filtrarIncidentes(muestra, FiltroIncidentes.EN_ATENCION, "").size)
        assertEquals(1, filtrarIncidentes(muestra, FiltroIncidentes.RESUELTOS, "").size)
    }

    @Test
    fun laBusquedaCubreCodigoTituloYZona() {
        assertEquals(listOf("CVK-000124"), filtrarIncidentes(muestra, FiltroIncidentes.TODOS, "cvk-000124").map { it.codigo })
        assertEquals(listOf("CVK-000118"), filtrarIncidentes(muestra, FiltroIncidentes.TODOS, "BASURA").map { it.codigo })
        assertEquals(2, filtrarIncidentes(muestra, FiltroIncidentes.TODOS, "mercado").size)
        assertEquals(4, filtrarIncidentes(muestra, FiltroIncidentes.TODOS, "   ").size)
    }

    @Test
    fun laBusquedaYElFiltroSeAplicanJuntos() {
        val resultado = filtrarIncidentes(muestra, FiltroIncidentes.ABIERTOS, "mercado")

        assertEquals(listOf("CVK-000118"), resultado.map { it.codigo })
    }

    @Test
    fun seOrdenaDeLaMasUrgenteALaMenosUrgente() {
        val resultado = filtrarIncidentes(muestra, FiltroIncidentes.TODOS, "")

        assertEquals("CVK-000124", resultado.first().codigo)
        val prioridades = resultado.map { it.prioridad }
        assertEquals(prioridades.sortedDescending(), prioridades)
    }

    @Test
    fun laPrioridadSeTraduceEnLaEtiquetaDelMockup() {
        assertEquals("Alta", etiquetaPrioridad(100))
        assertEquals("Media", etiquetaPrioridad(30))
        assertEquals("Baja", etiquetaPrioridad(29))
    }

    @Test
    fun laAntiguedadSeEscribeComoEnElMockup() {
        assertEquals("Hoy", antiguedadTexto(0))
        assertEquals("1 día", antiguedadTexto(1))
        assertEquals("4 días", antiguedadTexto(4))
    }

    @Test
    fun laTransicionSoloSeAplicaSiElRolLaPuedeHacer() {
        val conPermiso = aplicarCambioEstado(muestra, "CVK-000109", Estado.RESUELTO, Rol.ENTIDAD)
        assertEquals(Estado.RESUELTO, conPermiso.first { it.codigo == "CVK-000109" }.estado)

        val sinPermiso = aplicarCambioEstado(muestra, "CVK-000109", Estado.RESUELTO, Rol.CIUDADANO)
        assertTrue(sinPermiso === muestra, "sin permiso debe devolver la misma lista")
        assertEquals(Estado.EN_ATENCION, sinPermiso.first { it.codigo == "CVK-000109" }.estado)
    }

    @Test
    fun elSelectorDeEstadoSoloOfreceLasAccionesDelRol() {
        assertEquals(listOf(Estado.EN_REVISION, Estado.RESUELTO), accionesDisponibles(Estado.EN_ATENCION, Rol.ENTIDAD))
        assertTrue(accionesDisponibles(Estado.EN_ATENCION, Rol.CIUDADANO).isEmpty())
        assertEquals(listOf(Estado.DERIVADO, Estado.RECHAZADO), accionesDisponibles(Estado.EN_REVISION, Rol.MODERADOR))
    }
}
