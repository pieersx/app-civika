package pe.civika.panel.feature.incidentes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.data.SesionRepository
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.Rol

/**
 * Estado de la pantalla de incidentes (RF-13): lista filtrable, búsqueda, selección de la fila
 * y el cambio de estado con comentario para el ciudadano.
 */
data class PanelIncidentesUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    /** Lista completa (se actualiza en memoria al cambiar un estado). */
    val incidentes: List<Incidente> = emptyList(),
    val filtro: FiltroIncidentes = FiltroIncidentes.TODOS,
    val busqueda: String = "",
    val codigoSeleccionado: String? = null,
    /** Estado elegido en el selector de «Cambiar estado»; `null` = el actual. */
    val estadoObjetivo: Estado? = null,
    val comentario: String = "",
    val aviso: String? = null,
    val avisoError: Boolean = false,
) {
    val visibles: List<Incidente>
        get() = filtrarIncidentes(incidentes, filtro, busqueda)

    /** La fila seleccionada, o la primera de la lista: el panel siempre muestra un detalle (mockup). */
    val seleccionado: Incidente?
        get() = visibles.firstOrNull { it.codigo == codigoSeleccionado } ?: visibles.firstOrNull()

    val abiertos: Int get() = incidentes.count { it.estado.familia == Familia.ABIERTO }
    val enAtencion: Int get() = incidentes.count { it.estado.familia == Familia.EN_ATENCION }

    val resumen: String
        get() = when {
            incidentes.isEmpty() -> "Aún no hay incidentes en tu jurisdicción."
            else -> "$abiertos abiertos y $enAtencion en atención en tu jurisdicción. Ordenados por prioridad."
        }
}

class PanelIncidentesViewModel(
    /** El panel ve **todos** los incidentes de la jurisdicción, incluidos resueltos y rechazados:
     *  a diferencia de la app, aquí se filtran con los chips del mockup. */
    private val repositorio: IncidenteRepository,
    private val sesion: SesionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(PanelIncidentesUiState())
    val estado: StateFlow<PanelIncidentesUiState> = _estado.asStateFlow()

    /** Rol de la sesión activa: decide qué transiciones muestra el selector del detalle. */
    val rol: Rol
        get() = sesion.sesion.value?.rol ?: Rol.ENTIDAD

    init {
        viewModelScope.launch {
            _estado.value = try {
                val incidentes = repositorio.listar()
                PanelIncidentesUiState(
                    cargando = false,
                    incidentes = incidentes,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PanelIncidentesUiState(
                    cargando = false,
                    error = "No se pudieron cargar los incidentes. Recarga la página para intentarlo de nuevo.",
                )
            }
        }
    }

    fun cambiarFiltro(filtro: FiltroIncidentes) = actualizar { it.copy(filtro = filtro, aviso = null) }

    fun buscar(texto: String) = actualizar { it.copy(busqueda = texto, aviso = null) }

    fun seleccionar(codigo: String) = actualizar {
        it.copy(codigoSeleccionado = codigo, estadoObjetivo = null, comentario = "", aviso = null)
    }

    fun elegirEstado(destino: Estado) = actualizar { it.copy(estadoObjetivo = destino, aviso = null) }

    fun comentar(texto: String) = actualizar { it.copy(comentario = texto) }

    /** Aplica el cambio en memoria y confirma con un mensaje para la entidad (mockup del panel). */
    fun guardarCambio() {
        val actual = _estado.value.seleccionado ?: return
        val destino = _estado.value.estadoObjetivo ?: return
        val rol = sesion.sesion.value?.rol ?: return
        if (destino == actual.estado) {
            actualizar { it.copy(aviso = "Elige un estado distinto a «${actual.estado.etiqueta}».", avisoError = true) }
            return
        }
        val lista = aplicarCambioEstado(_estado.value.incidentes, actual.codigo, destino, rol)
        if (lista === _estado.value.incidentes) {
            actualizar {
                it.copy(
                    aviso = "Tu rol no puede cambiar «${actual.codigo}» de ${actual.estado.etiqueta} a ${destino.etiqueta}.",
                    avisoError = true,
                )
            }
            return
        }
        actualizar {
            it.copy(
                incidentes = lista,
                estadoObjetivo = null,
                comentario = "",
                aviso = "Cambio guardado: ${actual.codigo} ahora está en «${destino.etiqueta}». Los vecinos verán el cambio.",
                avisoError = false,
            )
        }
    }

    private fun actualizar(transformacion: (PanelIncidentesUiState) -> PanelIncidentesUiState) {
        _estado.value = transformacion(_estado.value)
    }
}
