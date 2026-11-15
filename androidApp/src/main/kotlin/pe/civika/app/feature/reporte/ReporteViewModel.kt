package pe.civika.app.feature.reporte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.civika.app.device.AlmacenFotos
import pe.civika.app.device.UbicacionProvider
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.ColaReportesRepository
import pe.civika.shared.data.IncidenteRepository
import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.Coordenada
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.PosibleDuplicado
import pe.civika.shared.domain.ReportePendiente
import pe.civika.shared.domain.TipoEnvio
import pe.civika.shared.domain.Validacion
import pe.civika.shared.domain.Validaciones
import pe.civika.shared.domain.ZonaPiloto
import pe.civika.shared.domain.usuario
import java.util.UUID

/** Los 5 pasos del flujo de reporte (RNF-01). */
enum class PasoReporte { FOTO, UBICACION, CATEGORIA, DUPLICADOS, ENVIADO }

data class ReporteUiState(
    val clienteId: String,
    val paso: PasoReporte = PasoReporte.FOTO,
    val fotos: List<String> = emptyList(),
    val procesandoFoto: Boolean = false,
    val errorFoto: Boolean = false,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val ubicacionSimulada: Boolean = false,
    val buscandoUbicacion: Boolean = false,
    val ubicacionNoDisponible: Boolean = false,
    val categorias: List<Categoria> = emptyList(),
    val categoriaId: Int? = null,
    val descripcion: String = "",
    val intentoCategoria: Boolean = false,
    val duplicados: List<PosibleDuplicado> = emptyList(),
    val buscandoDuplicados: Boolean = false,
    val enviando: Boolean = false,
    val error: ErrorCivika? = null,
    /** El envío ya guardado en la cola: su estado cambia solo (pendiente, enviando, enviado, error). */
    val envio: ReportePendiente? = null,
) {
    val fueraDeZona: Boolean
        get() = latitud != null && longitud != null && !ZonaPiloto.contiene(latitud, longitud)

    val ubicacionValida: Boolean get() = latitud != null && longitud != null && !fueraDeZona

    val validacionDescripcion: Validacion get() = Validaciones.descripcion(descripcion)

    val categoria: Categoria? get() = categorias.firstOrNull { it.id == categoriaId }

    val categoriaValida: Boolean get() = categoriaId != null && validacionDescripcion.esValida

    val puedeAgregarFotos: Boolean get() = fotos.size < Validaciones.MAX_FOTOS && !procesandoFoto

    val puedeContinuar: Boolean
        get() = when (paso) {
            PasoReporte.FOTO -> fotos.isNotEmpty() && !procesandoFoto
            PasoReporte.UBICACION -> ubicacionValida
            PasoReporte.CATEGORIA -> categoriaId != null
            PasoReporte.DUPLICADOS, PasoReporte.ENVIADO -> false
        }

    /** Pasos completados sobre 5, para la barra de progreso. */
    val progreso: Float get() = (paso.ordinal + 1) / PasoReporte.entries.size.toFloat()
}

/**
 * Estado del flujo de reporte. Lo que el ciudadano ya escribió sobrevive a la rotación y a que el sistema mate el
 * proceso: se guarda en [SavedStateHandle] y las fotos están en el almacenamiento interno.
 */
class ReporteViewModel(
    private val estadoGuardado: SavedStateHandle,
    private val incidentes: IncidenteRepository,
    private val cola: ColaReportesRepository,
    private val auth: AuthRepository,
    private val fotos: AlmacenFotos,
    private val ubicacion: UbicacionProvider,
    private val nuevoId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val _estado = MutableStateFlow(restaurar())
    val estado: StateFlow<ReporteUiState> = _estado.asStateFlow()

    private var observandoEnvio: Job? = null

    init {
        viewModelScope.launch {
            try {
                val categorias = incidentes.categorias()
                actualizar { it.copy(categorias = categorias) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin categorías no se puede continuar del paso 3: se reintenta al llegar allí.
            }
        }
        if (_estado.value.paso == PasoReporte.ENVIADO) observarEnvio(_estado.value.clienteId)
    }

    // ---- Paso 1: fotos ----

    fun agregarFoto(origen: String) {
        if (!_estado.value.puedeAgregarFotos) return
        actualizar { it.copy(procesandoFoto = true, errorFoto = false) }
        viewModelScope.launch {
            val ruta = try {
                fotos.guardar(origen, _estado.value.clienteId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            actualizar {
                if (ruta == null) {
                    it.copy(procesandoFoto = false, errorFoto = true)
                } else {
                    it.copy(procesandoFoto = false, fotos = (it.fotos + ruta).take(Validaciones.MAX_FOTOS))
                }
            }
        }
    }

    fun quitarFoto(ruta: String) {
        actualizar { it.copy(fotos = it.fotos - ruta) }
        viewModelScope.launch { fotos.borrar(ruta) }
    }

    // ---- Paso 2: ubicación ----

    fun ubicarme() {
        if (_estado.value.buscandoUbicacion) return
        actualizar { it.copy(buscandoUbicacion = true, ubicacionNoDisponible = false) }
        viewModelScope.launch {
            val obtenida = try {
                ubicacion.ubicacionActual()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            actualizar {
                if (obtenida == null) {
                    it.copy(buscandoUbicacion = false, ubicacionNoDisponible = true)
                } else {
                    it.copy(
                        buscandoUbicacion = false,
                        latitud = obtenida.latitud,
                        longitud = obtenida.longitud,
                        // Una vez marcada como simulada sigue así aunque luego se ajuste el punto a mano.
                        ubicacionSimulada = it.ubicacionSimulada || obtenida.simulada,
                    )
                }
            }
        }
    }

    /** El ciudadano arrastró el mapa: el punto elegido es el centro. */
    fun moverPunto(latitud: Double, longitud: Double) {
        actualizar { it.copy(latitud = latitud, longitud = longitud) }
    }

    /** Centro inicial del mapa de ajuste: lo ya elegido o, si no hay nada, el centro de la zona piloto. */
    fun centroInicial(): Coordenada {
        val e = _estado.value
        return if (e.latitud != null && e.longitud != null) Coordenada(e.latitud, e.longitud) else ZonaPiloto.centro
    }

    // ---- Paso 3: categoría y descripción ----

    fun cambiarCategoria(id: Int) = actualizar { it.copy(categoriaId = id) }

    fun cambiarDescripcion(texto: String) = actualizar { it.copy(descripcion = texto.take(Validaciones.DESCRIPCION_MAX + 1)) }

    // ---- Navegación entre pasos ----

    fun siguiente() {
        val e = _estado.value
        when (e.paso) {
            PasoReporte.FOTO -> if (e.puedeContinuar) irA(PasoReporte.UBICACION)
            PasoReporte.UBICACION -> if (e.puedeContinuar) irA(PasoReporte.CATEGORIA)
            PasoReporte.CATEGORIA -> {
                if (!e.categoriaValida) {
                    actualizar { it.copy(intentoCategoria = true) }
                } else {
                    irA(PasoReporte.DUPLICADOS)
                    buscarDuplicados()
                }
            }
            PasoReporte.DUPLICADOS, PasoReporte.ENVIADO -> Unit
        }
    }

    /** Vuelve un paso sin perder lo escrito. `false` si no hay paso anterior y la pantalla debe cerrarse. */
    fun atras(): Boolean {
        val e = _estado.value
        val anterior = when (e.paso) {
            PasoReporte.FOTO, PasoReporte.ENVIADO -> null
            PasoReporte.UBICACION -> PasoReporte.FOTO
            PasoReporte.CATEGORIA -> PasoReporte.UBICACION
            PasoReporte.DUPLICADOS -> PasoReporte.CATEGORIA
        }
        if (anterior == null) return false
        if (e.enviando) return true
        irA(anterior)
        return true
    }

    /** ¿Hay algo escrito que se perdería al salir? */
    fun hayBorrador(): Boolean {
        val e = _estado.value
        return e.paso != PasoReporte.ENVIADO && (e.fotos.isNotEmpty() || e.descripcion.isNotBlank())
    }

    // ---- Paso 4: «¿Es el mismo problema?» ----

    private fun buscarDuplicados() {
        val e = _estado.value
        val lat = e.latitud ?: return
        val lon = e.longitud ?: return
        val categoriaId = e.categoriaId ?: return
        actualizar { it.copy(buscandoDuplicados = true, duplicados = emptyList(), error = null) }
        viewModelScope.launch {
            val encontrados = try {
                incidentes.buscarDuplicados(lat, lon, categoriaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin conexión también se puede reportar (RF-15): se envía como incidente nuevo y el servidor decide.
                emptyList()
            }
            actualizar { it.copy(buscandoDuplicados = false, duplicados = encontrados) }
            // Si no hay nada parecido cerca no se le pregunta nada: el reporte sale en el mismo paso.
            if (encontrados.isEmpty()) enviarComoNuevo()
        }
    }

    fun enviarComoNuevo() = encolar(TipoEnvio.NUEVO, incidenteId = null)

    fun apoyar(duplicado: PosibleDuplicado) = encolar(TipoEnvio.APOYO, incidenteId = duplicado.incidenteId)

    private fun encolar(tipo: TipoEnvio, incidenteId: String?) {
        val e = _estado.value
        if (e.enviando || e.paso == PasoReporte.ENVIADO) return
        val usuario = auth.sesion.value.usuario
        val lat = e.latitud
        val lon = e.longitud
        val categoria = e.categoria
        if (usuario == null) {
            actualizar { it.copy(error = ErrorCivika.SesionRequerida()) }
            return
        }
        if (lat == null || lon == null || categoria == null || e.fotos.isEmpty()) return
        actualizar { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            try {
                cola.encolar(
                    ReportePendiente(
                        clienteId = e.clienteId,
                        usuarioId = usuario.id,
                        tipo = tipo,
                        incidenteId = incidenteId,
                        latitud = lat,
                        longitud = lon,
                        categoriaId = categoria.id,
                        categoriaNombre = categoria.nombre,
                        descripcion = e.descripcion.trim(),
                        fotos = e.fotos,
                        ubicacionSimulada = e.ubicacionSimulada,
                        creadoEnMs = System.currentTimeMillis(),
                    ),
                )
                actualizar { it.copy(enviando = false, paso = PasoReporte.ENVIADO) }
                observarEnvio(e.clienteId)
            } catch (c: CancellationException) {
                throw c
            } catch (x: Exception) {
                actualizar { it.copy(enviando = false, error = ErrorCivika.Desconocido(x)) }
            }
        }
    }

    private fun observarEnvio(clienteId: String) {
        observandoEnvio?.cancel()
        observandoEnvio = viewModelScope.launch {
            cola.observarUno(clienteId).collect { envio -> _estado.update { it.copy(envio = envio) } }
        }
    }

    fun reintentarEnvio() {
        viewModelScope.launch { cola.reintentar(_estado.value.clienteId) }
    }

    /** El ciudadano salió sin enviar: se borran las fotos que ya estaban guardadas. */
    fun descartarBorrador() {
        val e = _estado.value
        if (e.paso == PasoReporte.ENVIADO) return
        viewModelScope.launch { fotos.borrarCarpeta(e.clienteId) }
    }

    fun errorMostrado() = actualizar { it.copy(error = null) }

    // ---- Estado guardado ----

    private fun irA(paso: PasoReporte) = actualizar { it.copy(paso = paso, error = null) }

    private fun actualizar(cambio: (ReporteUiState) -> ReporteUiState) {
        _estado.update(cambio)
        guardar(_estado.value)
    }

    private fun guardar(e: ReporteUiState) {
        estadoGuardado[PASO] = e.paso.name
        estadoGuardado[FOTOS] = ArrayList(e.fotos)
        estadoGuardado[LATITUD] = e.latitud
        estadoGuardado[LONGITUD] = e.longitud
        estadoGuardado[SIMULADA] = e.ubicacionSimulada
        estadoGuardado[CATEGORIA] = e.categoriaId
        estadoGuardado[DESCRIPCION] = e.descripcion
        estadoGuardado[CLIENTE_ID] = e.clienteId
    }

    private fun restaurar(): ReporteUiState {
        val clienteId = estadoGuardado.get<String>(CLIENTE_ID) ?: nuevoId()
        val paso = estadoGuardado.get<String>(PASO)?.let { n -> PasoReporte.entries.firstOrNull { it.name == n } }
            ?: PasoReporte.FOTO
        val rutas = estadoGuardado.get<ArrayList<String>>(FOTOS).orEmpty().filter(fotos::existe)
        return ReporteUiState(
            clienteId = clienteId,
            // Los pasos que dependen de la red se rehacen: nunca se restaura en mitad de «buscando duplicados».
            paso = if (paso == PasoReporte.DUPLICADOS) PasoReporte.CATEGORIA else paso,
            fotos = rutas,
            latitud = estadoGuardado.get<Double>(LATITUD),
            longitud = estadoGuardado.get<Double>(LONGITUD),
            ubicacionSimulada = estadoGuardado.get<Boolean>(SIMULADA) ?: false,
            categoriaId = estadoGuardado.get<Int>(CATEGORIA),
            descripcion = estadoGuardado.get<String>(DESCRIPCION).orEmpty(),
        )
    }

    private companion object {
        const val PASO = "paso"
        const val FOTOS = "fotos"
        const val LATITUD = "latitud"
        const val LONGITUD = "longitud"
        const val SIMULADA = "simulada"
        const val CATEGORIA = "categoria"
        const val DESCRIPCION = "descripcion"
        const val CLIENTE_ID = "cliente_id"
    }
}
