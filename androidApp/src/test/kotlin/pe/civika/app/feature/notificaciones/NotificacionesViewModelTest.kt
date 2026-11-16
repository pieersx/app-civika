package pe.civika.app.feature.notificaciones

import kotlinx.coroutines.test.runTest
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.shared.data.NotificacionRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Notificacion
import pe.civika.shared.domain.TipoNotificacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NotificacionesViewModelTest : PruebaViewModel() {

    @Test
    fun sinSesionPideIniciarSesion() = runTest {
        val mundo = Mundo(sesion = false)

        assertIs<NotificacionesUiState.SinSesion>(NotificacionesViewModel(mundo.notificaciones, mundo.auth).estado.value)
    }

    @Test
    fun cargaLaBandejaMasRecientePrimeroConSuContadorDeNoLeidas() = runTest {
        val mundo = Mundo()

        val listo = assertIs<NotificacionesUiState.Listo>(NotificacionesViewModel(mundo.notificaciones, mundo.auth).estado.value)

        assertEquals(3, listo.items.size)
        assertEquals(2, listo.noLeidas)
        assertEquals(listo.items.sortedByDescending { it.fecha }, listo.items)
        assertEquals(setOf(TipoNotificacion.ESTADO, TipoNotificacion.COMENTARIO), listo.items.map { it.tipo }.toSet())
    }

    @Test
    fun alTocarUnAvisoSeMarcaLeidoAlInstanteYEnElServidor() = runTest {
        val mundo = Mundo()
        val vm = NotificacionesViewModel(mundo.notificaciones, mundo.auth)

        vm.marcarLeida("n-1")

        assertEquals(1, assertIs<NotificacionesUiState.Listo>(vm.estado.value).noLeidas)
        assertEquals(1, mundo.notificaciones.listar().count { !it.leida })
    }

    @Test
    fun marcarTodasLeidasDejaElContadorEnCero() = runTest {
        val mundo = Mundo()
        val vm = NotificacionesViewModel(mundo.notificaciones, mundo.auth)

        vm.marcarTodasLeidas()

        assertEquals(0, assertIs<NotificacionesUiState.Listo>(vm.estado.value).noLeidas)
    }

    @Test
    fun siFallaLaRedMuestraErrorYSiYaHabiaListaLaConserva() = runTest {
        val mundo = Mundo()
        var falla = true
        val repo = object : NotificacionRepository by mundo.notificaciones {
            override suspend fun listar(): List<Notificacion> =
                if (falla) throw ErrorCivika.SinConexion() else mundo.notificaciones.listar()
        }
        val vm = NotificacionesViewModel(repo, mundo.auth)
        assertIs<NotificacionesUiState.Error>(vm.estado.value)

        falla = false
        vm.cargar()
        assertIs<NotificacionesUiState.Listo>(vm.estado.value)

        falla = true
        vm.refrescar()
        val conservada = assertIs<NotificacionesUiState.Listo>(vm.estado.value)
        assertEquals(3, conservada.items.size)
        assertEquals(false, conservada.refrescando)
    }
}
