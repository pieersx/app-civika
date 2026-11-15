package pe.civika.app.feature.perfil

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.civika.app.FakeAjustes
import pe.civika.app.FakeToken
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.app.ui.theme.ModoTema
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.TipoEnvio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerfilViewModelTest : PruebaViewModel() {

    private val mundo = Mundo()
    private val token = FakeToken()
    private val ajustes = FakeAjustes()

    // Se crea dentro de cada prueba, cuando Main ya es el dispatcher de prueba.
    private val vm by lazy { PerfilViewModel(mundo.auth, mundo.notificaciones, mundo.cola, mundo.fotos, token, ajustes) }

    /** `estado` se calcula al recoger, así que cada prueba espera la condición que le interesa. */
    private suspend fun esperar(condicion: (PerfilUiState) -> Boolean) = vm.estado.first(condicion)

    @Test
    fun muestraElNombreYElCorreoDeLaCuenta() = runTest {
        val estado = esperar { it.usuario != null }

        assertEquals("Ana Pérez", estado.usuario?.nombre)
        assertEquals("ana@unmsm.edu.pe", estado.usuario?.correo)
    }

    @Test
    fun elTemaSeCambiaYSeGuarda() = runTest {
        vm.cambiarTema(ModoTema.OSCURO)

        assertEquals(ModoTema.OSCURO, esperar { it.tema == ModoTema.OSCURO }.tema)
        assertEquals(ModoTema.OSCURO, ajustes.tema.first())
    }

    @Test
    fun editarElNombreSoloSeGuardaSiEsValidoYDistinto() = runTest {
        vm.editarNombre()
        assertFalse(esperar { it.nombreEditado != null }.puedeGuardarNombre) // igual al actual

        vm.cambiarNombre("A")
        assertFalse(esperar { it.nombreEditado == "A" }.puedeGuardarNombre) // muy corto

        vm.cambiarNombre("Ana María Pérez")
        assertTrue(esperar { it.nombreEditado == "Ana María Pérez" }.puedeGuardarNombre)
        vm.guardarNombre()

        val estado = esperar { it.usuario?.nombre == "Ana María Pérez" && it.nombreEditado == null && !it.trabajando }
        assertNull(estado.nombreEditado)
    }

    @Test
    fun cerrarSesionBorraElTokenDelDispositivoYTerminaLaSesion() = runTest {
        mundo.notificaciones.registrarDispositivo(token.token!!)
        assertEquals(setOf(token.token), mundo.notificaciones.dispositivos())

        vm.cerrarSesion()
        esperar { it.usuario == null }

        assertTrue(mundo.notificaciones.dispositivos().isEmpty())
        assertEquals(EstadoSesion.SinSesion, mundo.auth.sesion.value)
    }

    @Test
    fun cerrarSesionFuncionaAunqueNoHayaTokenDeFirebase() = runTest {
        token.token = null

        vm.cerrarSesion()
        esperar { it.usuario == null }

        assertEquals(EstadoSesion.SinSesion, mundo.auth.sesion.value)
    }

    @Test
    fun eliminarLaCuentaCierraLaSesionYDescartaLosEnviosPendientes() = runTest {
        mundo.cola.encolar(
            ReportePendiente("c-1", mundo.usuario.id, TipoEnvio.NUEVO, null, 0.0, 0.0, 1, "Vial", "Bache profundo", listOf("/reportes/c-1/0.jpg"), estado = EstadoEnvio.PENDIENTE),
        )

        vm.eliminarCuenta()
        esperar { it.usuario == null && !it.trabajando }

        assertEquals(EstadoSesion.SinSesion, mundo.auth.sesion.value)
        assertTrue(mundo.cola.todos().isEmpty())
    }
}
