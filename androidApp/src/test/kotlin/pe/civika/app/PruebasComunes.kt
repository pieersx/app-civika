package pe.civika.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pe.civika.app.device.AjustesApp
import pe.civika.app.device.AlmacenFotos
import pe.civika.app.device.TokenDispositivo
import pe.civika.app.device.UbicacionObtenida
import pe.civika.app.device.UbicacionProvider
import pe.civika.app.ui.theme.ModoTema
import pe.civika.shared.data.DatosDemo
import pe.civika.shared.data.FakeAuthRepository
import pe.civika.shared.data.FakeColaReportesRepository
import pe.civika.shared.data.FakeIncidenteRepository
import pe.civika.shared.data.FakeNotificacionRepository
import pe.civika.shared.domain.Usuario
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/** Los ViewModels usan `viewModelScope` (Main): en las pruebas Main es inmediato y los fakes no esperan. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class PruebaViewModel {
    @BeforeTest
    fun prepararMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restaurarMain() = Dispatchers.resetMain()
}

/** Un mundo de ejemplo completo: repositorios que comparten datos, con o sin sesión iniciada. */
class Mundo(sesion: Boolean = true) {
    val usuario = Usuario("usuario-demo", "Ana Pérez", "ana@unmsm.edu.pe")
    val demo = DatosDemo()
    val auth = FakeAuthRepository(demo, sesionInicial = usuario.takeIf { sesion })
    val incidentes = FakeIncidenteRepository(demo)
    val notificaciones = FakeNotificacionRepository(demo)
    val cola = FakeColaReportesRepository()
    val fotos = FakeAlmacenFotos()
}

suspend fun <T> Flow<T>.primeroQue(condicion: (T) -> Boolean): T = first(condicion)

class FakeAlmacenFotos : AlmacenFotos {
    val guardadas = mutableListOf<String>()
    var fallar = false

    override suspend fun guardar(origen: String, clienteId: String): String? {
        if (fallar) return null
        return "/reportes/$clienteId/${guardadas.size}.jpg".also { guardadas += it }
    }

    override fun existe(ruta: String) = ruta in guardadas

    override suspend fun borrar(ruta: String) {
        guardadas -= ruta
    }

    override suspend fun borrarCarpeta(clienteId: String) {
        guardadas.removeAll { it.startsWith("/reportes/$clienteId/") }
    }

    override suspend fun leer(ruta: String): ByteArray? = if (existe(ruta)) ByteArray(8) else null
}

class FakeUbicacion(var resultado: UbicacionObtenida?) : UbicacionProvider {
    override suspend fun ubicacionActual() = resultado
}

class FakeToken(var token: String? = "token-de-prueba-0123456789") : TokenDispositivo {
    override suspend fun actual() = token
}

class FakeAjustes : AjustesApp {
    private val _tema = MutableStateFlow(ModoTema.SISTEMA)
    private val _preguntadas = MutableStateFlow(false)
    override val tema: Flow<ModoTema> = _tema
    override val notificacionesPreguntadas: Flow<Boolean> = _preguntadas

    override suspend fun cambiarTema(modo: ModoTema) {
        _tema.value = modo
    }

    override suspend fun marcarNotificacionesPreguntadas() {
        _preguntadas.value = true
    }
}
