package pe.civika.shared.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.civika.shared.domain.Rol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SesionFakeTest {

    @Test
    fun iniciaSesionConCredencialesValidasYSepublicaLaSesion() = runTest {
        val repositorio = FakeSesionRepository()

        assertNull(repositorio.sesion.first())

        val respuesta = repositorio.iniciarSesion("luis@civika.pe", "clave123")

        val exito = assertIs<InicioSesion.Exitoso>(respuesta)
        assertEquals(Rol.ENTIDAD, exito.sesion.rol)
        assertEquals(exito.sesion, repositorio.sesion.first())
    }

    @Test
    fun elCorreoDeModeracionDevuelveElRolDeModeracion() = runTest {
        val repositorio = FakeSesionRepository()

        val respuesta = repositorio.iniciarSesion("rosa@civika.pe", "clave123")

        assertEquals(Rol.MODERADOR, assertIs<InicioSesion.Exitoso>(respuesta).sesion.rol)
    }

    @Test
    fun rechazaElCorreoSinFormatoYSinSesion() = runTest {
        val repositorio = FakeSesionRepository()

        val respuesta = repositorio.iniciarSesion("luis@civika", "clave123")

        assertIs<InicioSesion.Rechazado>(respuesta)
        assertNull(repositorio.sesion.first())
    }

    @Test
    fun rechazaLaContrasenaCorta() = runTest {
        val repositorio = FakeSesionRepository()

        val respuesta = repositorio.iniciarSesion("luis@civika.pe", "123")

        assertIs<InicioSesion.Rechazado>(respuesta)
        assertNull(repositorio.sesion.first())
    }

    @Test
    fun cerrarSesionQuitaALaSesionActiva() = runTest {
        val repositorio = FakeSesionRepository()
        repositorio.iniciarSesion("luis@civika.pe", "clave123")

        repositorio.cerrarSesion()

        assertNull(repositorio.sesion.first())
    }

    @Test
    fun laColaDeEjemplosTraeLosTresMotivosDelMockup() = runTest {
        val denuncias = FakeDenunciaRepository().listarPendientes()

        assertEquals(3, denuncias.size)
        assertTrue(denuncias.any { it.motivo == pe.civika.shared.domain.MotivoDenuncia.ROSTROS })
        assertTrue(denuncias.any { it.motivo == pe.civika.shared.domain.MotivoDenuncia.DUPLICADO })
        assertTrue(denuncias.any { it.motivo == pe.civika.shared.domain.MotivoDenuncia.LENGUAJE })
    }
}
