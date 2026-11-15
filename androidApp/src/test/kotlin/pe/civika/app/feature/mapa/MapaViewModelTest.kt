package pe.civika.app.feature.mapa

import kotlinx.coroutines.test.runTest
import pe.civika.app.Mundo
import pe.civika.app.PruebaViewModel
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapaViewModelTest : PruebaViewModel() {

    private val mundo = Mundo(sesion = false)
    private val vm = MapaViewModel(mundo.incidentes)

    @Test
    fun cargaLosIncidentesPublicosYLasCategorias() = runTest {
        val estado = vm.estado.value

        assertEquals(false, estado.cargando)
        assertTrue(estado.todos.isNotEmpty())
        assertEquals(3, estado.categorias.size)
        assertTrue(estado.visibles.size == estado.todos.size)
    }

    @Test
    fun elFiltroPorCategoriaYPorFamiliaSeCombinan() = runTest {
        vm.alternarCategoria("residuos")
        assertTrue(vm.estado.value.visibles.all { it.categoria.serviceCode == "residuos" })

        vm.alternarFamilia(Familia.EN_ATENCION)
        assertTrue(vm.estado.value.visibles.all { it.estado.familia == Familia.EN_ATENCION })

        vm.limpiarFiltros()
        assertEquals(vm.estado.value.todos.size, vm.estado.value.visibles.size)
    }

    @Test
    fun seleccionarUnIncidenteFiltradoLoQuitaDeLaSeleccion() = runTest {
        val vial = vm.estado.value.todos.first { it.categoria.serviceCode == "vial" }
        vm.seleccionar(vial.id)
        assertEquals(vial.id, vm.estado.value.seleccionado?.id)

        vm.alternarCategoria("residuos")

        assertNull(vm.estado.value.seleccionado)
    }

    @Test
    fun verComoListaEsUnaAlternativaAccesibleQueSeAlterna() = runTest {
        vm.alternarLista()
        assertTrue(vm.estado.value.verComoLista)
        vm.alternarLista()
        assertEquals(false, vm.estado.value.verComoLista)
    }

    @Test
    fun siFallaLaRedMuestraElErrorYPermiteReintentar() = runTest {
        var falla = true
        val repo = object : IncidenteRepository by mundo.incidentes {
            override suspend fun listar(): List<Incidente> =
                if (falla) throw ErrorCivika.SinConexion() else mundo.incidentes.listar()
        }
        val vmConFallo = MapaViewModel(repo)
        assertIs<ErrorCivika.SinConexion>(vmConFallo.estado.value.error)

        falla = false
        vmConFallo.cargar()

        assertNull(vmConFallo.estado.value.error)
        assertTrue(vmConFallo.estado.value.todos.isNotEmpty())
    }
}
