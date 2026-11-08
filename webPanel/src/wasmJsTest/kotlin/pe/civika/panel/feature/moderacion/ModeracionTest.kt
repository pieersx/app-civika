package pe.civika.panel.feature.moderacion

import pe.civika.shared.domain.Denuncia
import pe.civika.shared.domain.MotivoDenuncia
import pe.civika.shared.domain.Rol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Reglas de la cola de moderación (RF-16): qué botón lleva cada motivo, qué se quita de la cola
 * al resolver y quién puede resolver.
 */
class ModeracionTest {

    private fun denuncia(
        id: String = "DEN-000003",
        motivo: MotivoDenuncia = MotivoDenuncia.ROSTROS,
        codigo: String = "CVK-000127",
    ) = Denuncia(
        id = id,
        motivo = motivo,
        titulo = "Basura en la puerta del colegio",
        codigoIncidente = codigo,
        meta = "denunciado por 2 vecinos, hace 1 hora",
        detalle = "En la foto se ven dos niños con uniforme.",
    )

    private val cola = listOf(
        denuncia(id = "DEN-000003", motivo = MotivoDenuncia.ROSTROS, codigo = "CVK-000127"),
        denuncia(id = "DEN-000002", motivo = MotivoDenuncia.DUPLICADO, codigo = "CVK-000129"),
        denuncia(id = "DEN-000001", motivo = MotivoDenuncia.LENGUAJE, codigo = "CVK-000130"),
    )

    @Test
    fun laAccionPrincipalCambiaSegunElMotivo() {
        cola.forEach { denuncia ->
            assertEquals(denuncia.motivo.accionPrincipal, AccionModeracion.PRINCIPAL.etiqueta(denuncia))
        }
        assertEquals("Foto con rostros visibles", cola[0].motivo.etiqueta)
    }

    @Test
    fun lasDosAccionesSecundariasSonLasDelMockup() {
        val denuncia = cola.first()

        assertEquals("Marcar como duplicado", AccionModeracion.DUPLICADO.etiqueta(denuncia))
        assertEquals("Rechazar el reporte", AccionModeracion.RECHAZAR.etiqueta(denuncia))
    }

    @Test
    fun alResolverSeQuitaDeLaCola() {
        val restantes = quitarDenuncia(cola, "DEN-000002")

        assertEquals(listOf("DEN-000003", "DEN-000001"), restantes.map { it.id })
        assertEquals(2, PanelModeracionUiState(denuncias = restantes).pendientes)
        assertFalse(PanelModeracionUiState(denuncias = restantes).vacia)
    }

    @Test
    fun unIdDesconocidoNoTocaLaCola() {
        val mismaLista = quitarDenuncia(cola, "DEN-999999")

        assertTrue(mismaLista === cola, "sin coincidencia debe devolver la misma lista")
        assertTrue(PanelModeracionUiState(denuncias = mismaLista).pendientes == 3)
        assertTrue(PanelModeracionUiState(cargando = false).vacia)
    }

    @Test
    fun elAvisoDiceQueSeHizoYConQueReporte() {
        val rostros = AccionModeracion.PRINCIPAL.aviso(cola[0])
        assertTrue(rostros.contains("ocultar la foto y aprobar"), rostros)
        assertTrue(rostros.contains("CVK-000127"), rostros)

        val duplicado = AccionModeracion.DUPLICADO.aviso(cola[1])
        assertTrue(duplicado.contains("duplicado"), duplicado)
        assertTrue(duplicado.contains("CVK-000129"), duplicado)
    }

    @Test
    fun soloLaModeracionResuelveDenuncias() {
        assertTrue(puedeModerar(Rol.MODERADOR))
        assertFalse(puedeModerar(Rol.ENTIDAD))
        assertFalse(puedeModerar(Rol.CIUDADANO))
        assertFalse(puedeModerar(Rol.SISTEMA))
    }
}
