package pe.civika.app.feature.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.shared.data.FakeAuthRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.Validacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AuthViewModelTest : PruebaViewModel() {

    private val mundo = Mundo(sesion = false)
    private val vm = AuthViewModel(mundo.auth)

    private fun llenarRegistro(consentimiento: Boolean = true, correo: String = "ana@unmsm.edu.pe", clave: String = "secreto123") {
        vm.cambiarNombre("Ana Pérez")
        vm.cambiarCorreo(correo)
        vm.cambiarContrasena(clave)
        vm.cambiarConsentimiento(consentimiento)
    }

    @Test
    fun iniciarSesionConDatosValidosAutentica() = runTest {
        vm.cambiarCorreo("ana@unmsm.edu.pe")
        vm.cambiarContrasena("secreto123")

        vm.estado.test {
            vm.iniciarSesion()
            assertTrue(expectMostRecentItem().autenticado)
        }
        assertIs<EstadoSesion.Activa>(mundo.auth.sesion.value)
    }

    @Test
    fun conUnCorreoMalEscritoNoLlamaAlServidorYMuestraElErrorDelCampo() = runTest {
        vm.cambiarCorreo("ana@unmsm")
        vm.cambiarContrasena("secreto123")

        vm.iniciarSesion()

        val estado = vm.estado.value
        assertTrue(estado.intentado)
        assertEquals(Validacion.FORMATO, estado.validacionCorreo)
        assertFalse(estado.autenticado)
        assertEquals(EstadoSesion.SinSesion, mundo.auth.sesion.value)
    }

    @Test
    fun credencialesIncorrectasMuestranUnErrorQueSeBorraAlEscribir() = runTest {
        vm.cambiarCorreo("ana@unmsm.edu.pe")
        vm.cambiarContrasena(FakeAuthRepository.CONTRASENA_INCORRECTA)

        vm.iniciarSesion()
        assertIs<ErrorCivika.CredencialesInvalidas>(vm.estado.value.error)
        assertFalse(vm.estado.value.enviando)

        vm.cambiarContrasena("otra")
        assertEquals(null, vm.estado.value.error)
    }

    @Test
    fun alRegistrarseElConsentimientoEsObligatorio() = runTest {
        llenarRegistro(consentimiento = false)

        vm.registrar()

        assertTrue(vm.estado.value.faltaConsentimiento)
        assertFalse(vm.estado.value.autenticado)
        assertEquals(EstadoSesion.SinSesion, mundo.auth.sesion.value)
    }

    @Test
    fun alRegistrarseLaContrasenaPideOchoCaracteres() = runTest {
        llenarRegistro(clave = "corta")

        vm.registrar()

        assertEquals(Validacion.MUY_CORTO, vm.estado.value.validacionContrasenaRegistro)
        assertFalse(vm.estado.value.autenticado)
    }

    @Test
    fun registrarseConTodoValidoIniciaSesion() = runTest {
        llenarRegistro()

        vm.registrar()

        assertTrue(vm.estado.value.autenticado)
        assertEquals("Ana Pérez", (mundo.auth.sesion.value as EstadoSesion.Activa).usuario.nombre)
    }

    @Test
    fun siHaceFaltaConfirmarElCorreoSeAvisaYNoSeInicia() = runTest {
        llenarRegistro(correo = "confirmar@unmsm.edu.pe")

        vm.registrar()

        assertTrue(vm.estado.value.confirmarCorreo)
        assertFalse(vm.estado.value.autenticado)
    }

    @Test
    fun unCorreoYaRegistradoSeExplica() = runTest {
        llenarRegistro(correo = FakeAuthRepository.CORREO_YA_REGISTRADO)

        vm.registrar()

        assertIs<ErrorCivika.CorreoYaRegistrado>(vm.estado.value.error)
    }

    @Test
    fun recuperarLaContrasenaNecesitaUnCorreoValido() = runTest {
        vm.recuperarContrasena()
        assertFalse(vm.estado.value.recuperacionEnviada)

        vm.cambiarCorreo("ana@unmsm.edu.pe")
        vm.recuperarContrasena()
        assertTrue(vm.estado.value.recuperacionEnviada)
    }
}
