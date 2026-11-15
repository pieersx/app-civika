package pe.civika.app.feature.incidente

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.ErrorCivika
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DetalleViewModelTest : PruebaViewModel() {

    private fun vm(id: String, mundo: Mundo) = DetalleViewModel(id, mundo.incidentes, mundo.auth, nuevoId = { "uuid" })

    private fun DetalleViewModel.detalle() = assertIs<DetalleUiState.Listo>(estado.value).detalle

    @Test
    fun cargaElDetalleConHistorialYComentarios() = runTest {
        val detalle = vm("demo-109", Mundo()).detalle()

        assertEquals("CVK-000109", detalle.incidente.codigo)
        assertTrue(detalle.historial.isNotEmpty())
        assertTrue(detalle.comentarios.isNotEmpty())
        assertEquals("Municipalidad del distrito piloto", detalle.entidad)
    }

    @Test
    fun unIncidenteQueNoExisteOUnRechazadoAjenoDaNoEncontrado() = runTest {
        assertIs<DetalleUiState.NoEncontrado>(vm("no-existe", Mundo()).estado.value)
        assertIs<DetalleUiState.NoEncontrado>(vm("demo-090", Mundo()).estado.value)
    }

    @Test
    fun sinSesionApoyarPideIniciarSesion() = runTest {
        val modelo = vm("demo-124", Mundo(sesion = false))

        modelo.eventos.test {
            modelo.alternarApoyo()
            assertEquals(EventoDetalle.RequiereSesion, awaitItem())
        }
        assertFalse(modelo.detalle().yoApoyo)
    }

    @Test
    fun apoyarSumaUnApoyoYTocarDeNuevoLoRetira() = runTest {
        val modelo = vm("demo-124", Mundo())
        val antes = modelo.detalle().incidente.apoyos

        modelo.eventos.test {
            modelo.alternarApoyo()
            assertEquals(EventoDetalle.Aviso(AvisoDetalle.APOYO_AGREGADO), awaitItem())
            assertTrue(modelo.detalle().yoApoyo)
            assertEquals(antes + 1, modelo.detalle().incidente.apoyos)

            modelo.alternarApoyo()
            assertEquals(EventoDetalle.Aviso(AvisoDetalle.APOYO_RETIRADO), awaitItem())
            assertFalse(modelo.detalle().yoApoyo)
            assertEquals(antes, modelo.detalle().incidente.apoyos)
        }
    }

    @Test
    fun elAutorNoPuedeApoyarSuPropioReporte() = runTest {
        val detalle = vm("demo-087", Mundo()).detalle()

        assertTrue(detalle.esMio)
        assertFalse(detalle.puedeApoyar)
    }

    @Test
    fun unResueltoQueSigoPermiteVerificarSiSigueArreglado() = runTest {
        val modelo = vm("demo-087", Mundo())
        assertTrue(modelo.detalle().puedeVerificar)

        modelo.eventos.test {
            modelo.verificar(sigueResuelto = true)
            assertEquals(EventoDetalle.Aviso(AvisoDetalle.VERIFICACION_ENVIADA), awaitItem())
        }
        assertEquals(true, modelo.detalle().miVerificacion)
    }

    @Test
    fun denunciarNecesitaUnMotivoDeAlMenosTresCaracteres() = runTest {
        val modelo = vm("demo-124", Mundo())

        modelo.eventos.test {
            modelo.denunciar("no")
            expectNoEvents()
            modelo.denunciar("Es un reporte falso")
            assertEquals(EventoDetalle.Aviso(AvisoDetalle.DENUNCIA_ENVIADA), awaitItem())
        }
    }

    @Test
    fun unErrorAlApoyarSeAvisaYElDetalleSigueEnPantalla() = runTest {
        val mundo = Mundo()
        val sinRed = object : IncidenteRepository by mundo.incidentes {
            override suspend fun apoyar(clienteId: String, incidenteId: String, fotos: List<String>) =
                throw ErrorCivika.SinConexion()
        }
        val modelo = DetalleViewModel("demo-124", sinRed, mundo.auth)

        modelo.eventos.test {
            modelo.alternarApoyo()
            assertIs<ErrorCivika.SinConexion>(assertIs<EventoDetalle.ErrorAccion>(awaitItem()).error)
        }
        assertIs<DetalleUiState.Listo>(modelo.estado.value)
        assertFalse((modelo.estado.value as DetalleUiState.Listo).ocupado)
    }

    @Test
    fun siFallaLaCargaInicialMuestraErrorYReintentarLoArregla() = runTest {
        val mundo = Mundo()
        var falla = true
        val repo = object : IncidenteRepository by mundo.incidentes {
            override suspend fun detalle(incidenteId: String): DetalleIncidente? =
                if (falla) throw ErrorCivika.SinConexion() else mundo.incidentes.detalle(incidenteId)
        }
        val modelo = DetalleViewModel("demo-124", repo, mundo.auth)
        assertIs<DetalleUiState.Error>(modelo.estado.value)

        falla = false
        modelo.cargar()

        assertIs<DetalleUiState.Listo>(modelo.estado.value)
    }
}
