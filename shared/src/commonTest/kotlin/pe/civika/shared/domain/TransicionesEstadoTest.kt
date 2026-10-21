package pe.civika.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tabla de la sección 5 de docs/arquitectura.md. */
class TransicionesEstadoTest {

    @Test
    fun elSistemaDerivaUnReporteCuandoHayReglaDeRuteo() {
        assertTrue(TransicionesEstado.permitida(Estado.REPORTADO, Estado.DERIVADO, Rol.SISTEMA))
    }

    @Test
    fun elSistemaMandaARevisionUnReporteSinRegla() {
        assertTrue(TransicionesEstado.permitida(Estado.REPORTADO, Estado.EN_REVISION, Rol.SISTEMA))
    }

    @Test
    fun soloElModeradorResuelveLoQueEstaEnRevision() {
        assertTrue(TransicionesEstado.permitida(Estado.EN_REVISION, Estado.DERIVADO, Rol.MODERADOR))
        assertTrue(TransicionesEstado.permitida(Estado.EN_REVISION, Estado.RECHAZADO, Rol.MODERADOR))
        assertFalse(TransicionesEstado.permitida(Estado.EN_REVISION, Estado.RECHAZADO, Rol.ENTIDAD))
        assertFalse(TransicionesEstado.permitida(Estado.EN_REVISION, Estado.DERIVADO, Rol.CIUDADANO))
    }

    @Test
    fun laEntidadAtiendeYResuelve() {
        assertTrue(TransicionesEstado.permitida(Estado.DERIVADO, Estado.EN_ATENCION, Rol.ENTIDAD))
        assertTrue(TransicionesEstado.permitida(Estado.EN_ATENCION, Estado.RESUELTO, Rol.ENTIDAD))
    }

    @Test
    fun laEntidadPuedeDecirQueNoLeCorresponde() {
        assertTrue(TransicionesEstado.permitida(Estado.DERIVADO, Estado.EN_REVISION, Rol.ENTIDAD))
    }

    @Test
    fun unaDenunciaDevuelveARevisionDesdeDerivadoOEnAtencion() {
        assertTrue(TransicionesEstado.permitida(Estado.DERIVADO, Estado.EN_REVISION, Rol.SISTEMA))
        assertTrue(TransicionesEstado.permitida(Estado.EN_ATENCION, Estado.EN_REVISION, Rol.SISTEMA))
    }

    @Test
    fun losVecinosReabrenUnResueltoQueSigueIgual() {
        assertTrue(TransicionesEstado.permitida(Estado.RESUELTO, Estado.EN_ATENCION, Rol.SISTEMA))
        assertFalse(TransicionesEstado.permitida(Estado.RESUELTO, Estado.EN_ATENCION, Rol.CIUDADANO))
    }

    @Test
    fun elCiudadanoNuncaCambiaEstadosDirectamente() {
        Estado.entries.forEach { desde ->
            assertTrue(TransicionesEstado.siguientes(desde, Rol.CIUDADANO).isEmpty(), "desde $desde")
        }
    }

    @Test
    fun rechazadoEsFinal() {
        Rol.entries.forEach { rol ->
            assertTrue(TransicionesEstado.siguientes(Estado.RECHAZADO, rol).isEmpty(), "rol $rol")
        }
    }

    @Test
    fun noSePuedeSaltarDeReportadoAResuelto() {
        Rol.entries.forEach { rol ->
            assertFalse(TransicionesEstado.permitida(Estado.REPORTADO, Estado.RESUELTO, rol), "rol $rol")
        }
    }

    @Test
    fun cadaEstadoPerteneceAUnaFamiliaVisual() {
        assertEquals(Familia.ABIERTO, Estado.DERIVADO.familia)
        assertEquals(Familia.EN_ATENCION, Estado.EN_ATENCION.familia)
        assertEquals(Familia.RESUELTO, Estado.RESUELTO.familia)
        assertEquals(Familia.RECHAZADO, Estado.RECHAZADO.familia)
    }
}
