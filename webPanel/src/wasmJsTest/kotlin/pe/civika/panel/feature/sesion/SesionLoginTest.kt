package pe.civika.panel.feature.sesion

import pe.civika.shared.domain.Credenciales
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Formulario de ingreso del panel: el botón solo se habilita con las mismas reglas que usa el
 * repositorio de sesiones, para no dejar pulsar un botón que va a fallar.
 */
class SesionLoginTest {

    @Test
    fun elBotonEsperaCorreoYContrasenaValidos() {
        assertFalse(PanelLoginUiState().puedeIngresar)

        val listo = PanelLoginUiState(correo = "luis@civika.pe", contrasena = "clave123")
        assertTrue(listo.puedeIngresar)
    }

    @Test
    fun unCorreoSinFormatoONoCompletadoNoHabilitaElBoton() {
        assertFalse(PanelLoginUiState(correo = "luis@civika", contrasena = "clave123").puedeIngresar)
        assertFalse(PanelLoginUiState(correo = "luis@", contrasena = "clave123").puedeIngresar)
        assertFalse(PanelLoginUiState(correo = "luis@civika.pe").puedeIngresar)
        assertFalse(PanelLoginUiState(correo = "luis@civika.pe", contrasena = "").puedeIngresar)
    }

    @Test
    fun laContrasenaCortaNoHabilitaElBoton() {
        assertFalse(PanelLoginUiState(correo = "rosa@civika.pe", contrasena = "12345").puedeIngresar)
        assertTrue(PanelLoginUiState(correo = "rosa@civika.pe", contrasena = "123456").puedeIngresar)
    }

    @Test
    fun mientrasEnviaNoSePuedeVolverAPulsar() {
        val enviando = PanelLoginUiState(correo = "luis@civika.pe", contrasena = "clave123", enviando = true)

        assertFalse(enviando.puedeIngresar)
    }

    @Test
    fun elCorreoDeLaModeracionYElDeLaEntidadPasanElMismoFiltro() {
        assertTrue(Credenciales.correoValido("rosa@civika.pe"))
        assertTrue(Credenciales.correoValido("  Luis@Civika.pe  "))
        assertFalse(Credenciales.correoValido("luis@civika.pe sin formato"))
        assertEquals(6, Credenciales.LONGITUD_MINIMA_CONTRASENA)
    }
}
