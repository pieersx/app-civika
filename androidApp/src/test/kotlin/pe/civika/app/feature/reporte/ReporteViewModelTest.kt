package pe.civika.app.feature.reporte

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.runTest
import pe.civika.app.FakeUbicacion
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.app.device.UbicacionObtenida
import pe.civika.shared.domain.CategoriasPiloto
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.EstadoEnvio
import pe.civika.shared.domain.TipoEnvio
import pe.civika.shared.domain.ZonaPiloto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReporteViewModelTest : PruebaViewModel() {

    private val mundo = Mundo()
    private val ubicacion = FakeUbicacion(UbicacionObtenida(ZonaPiloto.centro.latitud, ZonaPiloto.centro.longitud, simulada = false))
    private val manejador = SavedStateHandle()
    private var contador = 0

    private fun crearVm(handle: SavedStateHandle = manejador, mundo: Mundo = this.mundo) = ReporteViewModel(
        estadoGuardado = handle,
        incidentes = mundo.incidentes,
        cola = mundo.cola,
        auth = mundo.auth,
        fotos = mundo.fotos,
        ubicacion = ubicacion,
        nuevoId = { "cliente-${++contador}" },
    )

    private val descripcionValida = "Bache profundo frente a la puerta 3"

    private fun ReporteViewModel.hastaCategoria() {
        agregarFoto("content://foto/1")
        siguiente()
        ubicarme()
        siguiente()
    }

    @Test
    fun empiezaEnElPrimerPasoSinPoderContinuar() = runTest {
        val vm = crearVm()

        assertEquals(PasoReporte.FOTO, vm.estado.value.paso)
        assertFalse(vm.estado.value.puedeContinuar)
        assertEquals(0.2f, vm.estado.value.progreso)
    }

    @Test
    fun sinFotoNoSePasaALaUbicacion() = runTest {
        val vm = crearVm()

        vm.siguiente()

        assertEquals(PasoReporte.FOTO, vm.estado.value.paso)
    }

    @Test
    fun noSeAceptanMasDeTresFotosYSePuedeQuitarUna() = runTest {
        val vm = crearVm()

        repeat(5) { vm.agregarFoto("content://foto/$it") }
        assertEquals(3, vm.estado.value.fotos.size)
        assertFalse(vm.estado.value.puedeAgregarFotos)

        vm.quitarFoto(vm.estado.value.fotos.first())
        assertEquals(2, vm.estado.value.fotos.size)
        assertEquals(2, mundo.fotos.guardadas.size)
    }

    @Test
    fun siLaFotoNoSePuedeProcesarSeAvisaYNoSeAgrega() = runTest {
        mundo.fotos.fallar = true
        val vm = crearVm()

        vm.agregarFoto("content://foto/1")

        assertTrue(vm.estado.value.errorFoto)
        assertTrue(vm.estado.value.fotos.isEmpty())
        assertFalse(vm.estado.value.procesandoFoto)
    }

    @Test
    fun laUbicacionDebeEstarDentroDeLaZonaPiloto() = runTest {
        val vm = crearVm()
        vm.agregarFoto("content://foto/1")
        vm.siguiente()

        vm.moverPunto(-12.0464, -77.0428) // Plaza de Armas: fuera del piloto
        assertTrue(vm.estado.value.fueraDeZona)
        vm.siguiente()
        assertEquals(PasoReporte.UBICACION, vm.estado.value.paso)

        vm.moverPunto(ZonaPiloto.centro.latitud, ZonaPiloto.centro.longitud)
        vm.siguiente()
        assertEquals(PasoReporte.CATEGORIA, vm.estado.value.paso)
    }

    @Test
    fun siNoHayGpsSeAvisaYElMapaArrancaEnElCentroDeLaZona() = runTest {
        ubicacion.resultado = null
        val vm = crearVm()

        vm.ubicarme()

        assertTrue(vm.estado.value.ubicacionNoDisponible)
        assertNull(vm.estado.value.latitud)
        assertEquals(ZonaPiloto.centro, vm.centroInicial())
    }

    @Test
    fun laUbicacionSimuladaSeMarcaYNoSeBorraAlAjustarElPunto() = runTest {
        ubicacion.resultado = UbicacionObtenida(ZonaPiloto.centro.latitud, ZonaPiloto.centro.longitud, simulada = true)
        val vm = crearVm()

        vm.ubicarme()
        vm.moverPunto(-12.0580, -77.0850)

        assertTrue(vm.estado.value.ubicacionSimulada)
    }

    @Test
    fun laDescripcionCortaOSinCategoriaNoDejaContinuar() = runTest {
        val vm = crearVm()
        vm.hastaCategoria()
        assertEquals(PasoReporte.CATEGORIA, vm.estado.value.paso)

        vm.cambiarCategoria(CategoriasPiloto.VIAL.id)
        vm.cambiarDescripcion("bache")
        vm.siguiente()

        assertEquals(PasoReporte.CATEGORIA, vm.estado.value.paso)
        assertTrue(vm.estado.value.intentoCategoria)
    }

    @Test
    fun sinIncidentesParecidosElReporteSaleSoloYQuedaEnLaCola() = runTest {
        val vm = crearVm()
        vm.hastaCategoria()
        // Una categoría sin incidentes en ese punto: residuos (radio de 30 m) no tiene ninguno en el centro del campus.
        vm.cambiarCategoria(CategoriasPiloto.RESIDUOS.id)
        vm.cambiarDescripcion(descripcionValida)

        vm.siguiente()

        assertEquals(PasoReporte.ENVIADO, vm.estado.value.paso)
        val envio = mundo.cola.todos().single()
        assertEquals(TipoEnvio.NUEVO, envio.tipo)
        assertEquals("cliente-1", envio.clienteId)
        assertEquals(mundo.usuario.id, envio.usuarioId)
        assertEquals(EstadoEnvio.PENDIENTE, envio.estado)
        assertEquals(1, envio.fotos.size)
        assertEquals(descripcionValida, envio.descripcion)
        assertNotNull(vm.estado.value.envio)
    }

    @Test
    fun conUnIncidenteCercanoPreguntaSiEsElMismoYApoyarLoEncolaConLasFotos() = runTest {
        val vm = crearVm()
        vm.agregarFoto("content://foto/1")
        vm.siguiente()
        vm.moverPunto(-12.05121, -77.08531) // a metros del bache CVK-000124
        vm.siguiente()
        vm.cambiarCategoria(CategoriasPiloto.VIAL.id)
        vm.cambiarDescripcion(descripcionValida)
        vm.siguiente()

        assertEquals(PasoReporte.DUPLICADOS, vm.estado.value.paso)
        val parecido = vm.estado.value.duplicados.single()
        assertEquals("CVK-000124", parecido.codigo)

        vm.apoyar(parecido)

        val envio = mundo.cola.todos().single()
        assertEquals(TipoEnvio.APOYO, envio.tipo)
        assertEquals("demo-124", envio.incidenteId)
        assertEquals(1, envio.fotos.size)
        assertEquals(PasoReporte.ENVIADO, vm.estado.value.paso)
    }

    @Test
    fun enEsOtroProblemaSeEnviaComoIncidenteNuevo() = runTest {
        val vm = crearVm()
        vm.agregarFoto("content://foto/1")
        vm.siguiente()
        vm.moverPunto(-12.05121, -77.08531)
        vm.siguiente()
        vm.cambiarCategoria(CategoriasPiloto.VIAL.id)
        vm.cambiarDescripcion(descripcionValida)
        vm.siguiente()

        vm.enviarComoNuevo()

        assertEquals(TipoEnvio.NUEVO, mundo.cola.todos().single().tipo)
    }

    @Test
    fun retrocederConservaLoEscrito() = runTest {
        val vm = crearVm()
        vm.hastaCategoria()
        vm.cambiarCategoria(CategoriasPiloto.VIAL.id)
        vm.cambiarDescripcion(descripcionValida)

        assertTrue(vm.atras())
        assertEquals(PasoReporte.UBICACION, vm.estado.value.paso)
        assertNotNull(vm.estado.value.latitud)
        assertTrue(vm.atras())
        assertEquals(PasoReporte.FOTO, vm.estado.value.paso)
        assertEquals(1, vm.estado.value.fotos.size)
        assertFalse(vm.atras()) // en el primer paso la pantalla se cierra

        vm.siguiente()
        vm.siguiente()
        assertEquals(descripcionValida, vm.estado.value.descripcion)
        assertEquals(CategoriasPiloto.VIAL.id, vm.estado.value.categoriaId)
    }

    @Test
    fun elBorradorSobreviveARotacionOMuerteDelProceso() = runTest {
        val primero = crearVm()
        primero.hastaCategoria()
        primero.cambiarCategoria(CategoriasPiloto.VIAL.id)
        primero.cambiarDescripcion(descripcionValida)
        assertTrue(primero.hayBorrador())

        // Un ViewModel nuevo con el mismo SavedStateHandle (lo que pasa tras rotar o recrear el proceso).
        val segundo = crearVm(manejador)

        val e = segundo.estado.value
        assertEquals(PasoReporte.CATEGORIA, e.paso)
        assertEquals(primero.estado.value.clienteId, e.clienteId)
        assertEquals(primero.estado.value.fotos, e.fotos)
        assertEquals(descripcionValida, e.descripcion)
        assertEquals(CategoriasPiloto.VIAL.id, e.categoriaId)
        assertEquals(primero.estado.value.latitud, e.latitud)
    }

    @Test
    fun alRestaurarSeDescartanLasFotosQueYaNoExisten() = runTest {
        val primero = crearVm()
        primero.agregarFoto("content://foto/1")
        mundo.fotos.guardadas.clear()

        val segundo = crearVm(manejador)

        assertTrue(segundo.estado.value.fotos.isEmpty())
    }

    @Test
    fun descartarElBorradorBorraLasFotosGuardadas() = runTest {
        val vm = crearVm()
        vm.agregarFoto("content://foto/1")

        vm.descartarBorrador()

        assertTrue(mundo.fotos.guardadas.isEmpty())
    }

    @Test
    fun sinSesionNoSeEncolaYSePideIniciarSesion() = runTest {
        val sinSesion = Mundo(sesion = false)
        val vm = crearVm(SavedStateHandle(), sinSesion)
        vm.hastaCategoria()
        vm.cambiarCategoria(CategoriasPiloto.RESIDUOS.id)
        vm.cambiarDescripcion(descripcionValida)

        vm.enviarComoNuevo()

        assertIs<ErrorCivika.SesionRequerida>(vm.estado.value.error)
        assertTrue(sinSesion.cola.todos().isEmpty())
    }

    @Test
    fun elEstadoDelEnvioSeActualizaCuandoLaColaLoEnvia() = runTest {
        val vm = crearVm()
        vm.hastaCategoria()
        vm.cambiarCategoria(CategoriasPiloto.RESIDUOS.id)
        vm.cambiarDescripcion(descripcionValida)
        vm.siguiente()

        mundo.cola.actualizar("cliente-1") { it.copy(estado = EstadoEnvio.ENVIADO, codigo = "CVK-000132") }

        assertEquals("CVK-000132", vm.estado.value.envio?.codigo)
        assertEquals(EstadoEnvio.ENVIADO, vm.estado.value.envio?.estado)
    }
}
