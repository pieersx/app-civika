package pe.civika.shared.data

import kotlinx.serialization.builtins.ListSerializer
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Relacion
import pe.civika.shared.domain.TipoNotificacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** El mapeo de las filas de Supabase se prueba sin red, con el formato real de PostgREST. */
class MapeoTest {

    private val ahora = Instant.parse("2026-11-13T17:00:00Z")

    private val categorias = listOf(
        CategoriaDto(1, "vial", "Pistas y veredas", 3, 50, 30, 15, true),
        CategoriaDto(2, "residuos", "Residuos", 2, 30, 30, 5, true),
        CategoriaDto(3, "accesibilidad", "Accesibilidad", 3, 25, 30, 10, false),
    )

    // Formato de `listar_incidentes_publicos`: incluye columnas que la app no usa (`categoria`, `prioridad`).
    private val json = """
        [
          {"id":"0b9d6f4e-2a53-4c0e-9d57-0a1b2c3d4e5f","codigo":"DEMO-001","titulo":"Bache profundo frente a la puerta 3",
           "categoria":"Pistas y veredas","categoria_id":1,"estado":"en_revision","prioridad":44,"apoyos_count":5,
           "zona":"Ciudad Universitaria UNMSM","lat":-12.0575,"lon":-77.0845,"creado_en":"2026-11-10T14:03:27.123456+00:00"},
          {"id":"1c0e7a5f-3b64-4d1f-8e68-1b2c3d4e5f60","codigo":"DEMO-002","titulo":"Residuos",
           "categoria":"Residuos","categoria_id":2,"estado":"derivado","prioridad":20,"apoyos_count":0,
           "zona":"Alrededores de la Ciudad Universitaria","lat":-12.05,"lon":-77.08,"creado_en":"2026-11-13T03:00:00+00:00"}
        ]
    """.trimIndent()

    private fun filas(texto: String = json) =
        JsonSupabase.decodeFromString(ListSerializer(IncidenteDto.serializer()), texto)

    private fun fila(estado: String = "derivado", categoriaId: Int = 1, creadoEn: String = "2026-11-13T03:00:00+00:00") =
        IncidenteDto("id", "CVK-000001", "Bache", categoriaId, estado, 0, "zona", -12.05, -77.08, creadoEn)

    @Test
    fun parseaElFormatoRealDeCreadoEn() {
        assertEquals(Instant.parse("2026-11-10T14:03:27.123456Z"), instanteDesde("2026-11-10T14:03:27.123456+00:00"))
        assertNull(instanteDesde("ayer"))
    }

    @Test
    fun armaElIncidenteConSuCategoriaPorId() {
        val incidentes = construirIncidentes(filas(), categorias, ahora) {}

        assertEquals(2, incidentes.size)
        val primero = incidentes.first()
        assertEquals("DEMO-001", primero.codigo)
        assertEquals("vial", primero.categoria.serviceCode)
        assertEquals(5, primero.apoyos)
        assertEquals(3, primero.diasAbierto) // del 10 al 13 de noviembre, en Lima
        // La prioridad se calcula en el celular (3×10 + 5 + 3×2 = 41), no se copia el 44 del servidor.
        assertEquals(41, primero.prioridad)
    }

    @Test
    fun elEstadoEnSnakeCaseSeConvierte() {
        assertEquals(Estado.EN_REVISION, estadoDesde("en_revision"))
        assertEquals(Estado.EN_ATENCION, estadoDesde("en_atencion"))
        assertNull(estadoDesde("archivado"))
        assertNull(estadoDesde(null))
    }

    @Test
    fun unEstadoDesconocidoOmiteLaFilaYAvisa() {
        val avisos = mutableListOf<String>()

        val incidentes = construirIncidentes(listOf(fila(estado = "archivado"), fila()), categorias, ahora, avisos::add)

        assertEquals(1, incidentes.size)
        assertTrue(avisos.single().contains("archivado"))
    }

    @Test
    fun unaCategoriaDesconocidaOmiteElIncidenteYAvisa() {
        val avisos = mutableListOf<String>()

        val incidentes = construirIncidentes(listOf(fila(categoriaId = 99)), categorias, ahora, avisos::add)

        assertTrue(incidentes.isEmpty())
        assertTrue(avisos.single().contains("99"))
    }

    @Test
    fun unaCategoriaInactivaSeIncluye() {
        val incidentes = construirIncidentes(listOf(fila(categoriaId = 3)), categorias, ahora) {}

        assertEquals("accesibilidad", incidentes.single().categoria.serviceCode)
    }

    @Test
    fun exactamente200FilasEmiteElAvisoDeTope() {
        val avisos = mutableListOf<String>()

        construirIncidentes(List(200) { fila() }, categorias, ahora, avisos::add)

        assertTrue(avisos.single().contains("200"))
    }

    @Test
    fun unaListaVaciaDaUnaListaVacia() {
        assertTrue(construirIncidentes(emptyList(), categorias, ahora) { error("no debe avisar") }.isEmpty())
    }

    @Test
    fun unCambioDePesoCambiaLaPrioridad() {
        val antes = construirIncidentes(listOf(fila()), categorias, ahora) {}.single().prioridad
        val conMasPeso = categorias.map { if (it.id == 1) it.copy(pesoPrioridad = 5) else it }

        val despues = construirIncidentes(listOf(fila()), conMasPeso, ahora) {}.single().prioridad

        assertEquals(antes + 20, despues)
    }

    @Test
    fun copiaLatYLonComoNullable() {
        val sinCoordenadas = fila().copy(lat = null, lon = null)

        val incidente = construirIncidentes(listOf(sinCoordenadas), categorias, ahora) {}.single()

        assertNull(incidente.latitud)
        assertNull(incidente.longitud)
        assertEquals(-12.0575, construirIncidentes(filas(), categorias, ahora) {}.first().latitud)
    }

    @Test
    fun elRelojAtrasadoNoDaDiasNegativos() {
        val incidente = construirIncidentes(listOf(fila(creadoEn = "2026-12-01T00:00:00+00:00")), categorias, ahora) {}.single()

        assertEquals(0, incidente.diasAbierto)
    }

    @Test
    fun misIncidentesDistingueAutorDeApoyo() {
        val mios = construirMisIncidentes(
            listOf(
                fila().copy(relacion = "autor", desde = "2026-11-12T10:00:00+00:00"),
                fila().copy(id = "otro", relacion = "apoyo"),
            ),
            categorias,
            ahora,
        ) {}

        assertEquals(listOf(Relacion.AUTOR, Relacion.APOYO), mios.map { it.relacion })
        assertNotNull(mios.first().desde)
    }

    @Test
    fun elDetalleTraeHistorialComentariosYLoQueElUsuarioHizo() {
        val dto = JsonSupabase.decodeFromString(
            DetalleDto.serializer(),
            """{"id":"x","codigo":"CVK-000007","titulo":"Bache","descripcion":"Bache profundo","categoria":"Pistas y veredas",
               "categoria_id":1,"estado":"resuelto","prioridad":30,"apoyos_count":2,"zona":"Campus","entidad":null,
               "lat":-12.05,"lon":-77.08,"creado_en":"2026-11-01T12:00:00+00:00","plazo_objetivo_dias":15,
               "yo_apoyo":true,"es_mio":false,"mi_verificacion":null,"motivo_rechazo":null}""",
        )

        val detalle = dto.aDominio(
            categorias,
            ahora,
            fotos = listOf("https://ejemplo/foto.jpg"),
            historial = listOf(
                HistorialDto("derivado", "2026-11-01T12:00:00+00:00"),
                HistorialDto("desconocido", "2026-11-02T12:00:00+00:00"),
                HistorialDto("resuelto", "2026-11-05T12:00:00+00:00"),
            ),
            comentarios = listOf(ComentarioDto("Ya se reparó", "2026-11-05T12:10:00+00:00")),
        )

        assertNotNull(detalle)
        assertEquals(listOf(Estado.DERIVADO, Estado.RESUELTO), detalle.historial.map { it.estado })
        assertEquals("Ya se reparó", detalle.comentarios.single().texto)
        assertTrue(detalle.puedeVerificar)
        assertNull(detalle.entidad)
    }

    @Test
    fun unaNotificacionDeTipoNuevoNoRompeLaBandeja() {
        val dto = NotificacionDto("n", "encuesta", null, false, "2026-11-05T12:10:00+00:00")

        assertEquals(TipoNotificacion.OTRO, dto.aDominio()?.tipo)
        assertEquals(TipoNotificacion.REABIERTO, dto.copy(tipo = "reabierto").aDominio()?.tipo)
    }

    @Test
    fun elTopeDeReportesPorHoraSeReintentaYNoEsUnErrorDeDatos() {
        val error = clasificarErrorPostgres("22023", "Demasiados reportes en la última hora", 400)

        assertIs<ErrorCivika.DemasiadosIntentos>(error)
        assertFalse(error.esDefinitivo)
    }

    @Test
    fun losCodigosDeErrorDeLasRpcSeTraducen() {
        assertIs<ErrorCivika.FueraDeZona>(clasificarErrorPostgres("P0001", "Fuera de la zona piloto", 400))
        val datos = assertIs<ErrorCivika.DatosInvalidos>(clasificarErrorPostgres("22023", "Máximo 3 fotos por reporte", 400))
        assertEquals("Máximo 3 fotos por reporte", datos.detalle)
        assertIs<ErrorCivika.SesionRequerida>(clasificarErrorPostgres("42501", "Se requiere una sesión", 403))
        assertIs<ErrorCivika.SinPermiso>(clasificarErrorPostgres("42501", "Solo un ciudadano con la cuenta activa puede apoyar", 403))
        assertIs<ErrorCivika.SesionRequerida>(clasificarErrorPostgres("PGRST301", "JWT expired", 401))
        assertIs<ErrorCivika.SinConexion>(clasificarErrorPostgres(null, "Bad gateway", 502))
        assertIs<ErrorCivika.Desconocido>(clasificarErrorPostgres("P0001", "otro error", 400))
    }

    @Test
    fun losErroresDeAuthSeTraducen() {
        assertIs<ErrorCivika.CredencialesInvalidas>(clasificarErrorAuth("invalid_credentials", 400))
        assertIs<ErrorCivika.CorreoSinConfirmar>(clasificarErrorAuth("email_not_confirmed", 400))
        assertIs<ErrorCivika.CorreoYaRegistrado>(clasificarErrorAuth("user_already_exists", 422))
        assertIs<ErrorCivika.ContrasenaDebil>(clasificarErrorAuth("weak_password", 422))
        assertIs<ErrorCivika.DemasiadosIntentos>(clasificarErrorAuth("over_email_send_rate_limit", 429))
        assertIs<ErrorCivika.DemasiadosIntentos>(clasificarErrorAuth(null, 429))
    }

    @Test
    fun nominatimDevuelveCalleYBarrioSinNumeroDePuerta() {
        val respuesta = """{"display_name":"123, Avenida Venezuela, Lima, Perú",
            "address":{"house_number":"123","road":"Avenida Venezuela","suburb":"Cercado de Lima","city":"Lima"}}"""

        assertEquals("Avenida Venezuela, Cercado de Lima", direccionDesdeNominatim(respuesta))
        assertEquals("Lima, Perú", direccionDesdeNominatim("""{"display_name":"Lima, Perú, América"}"""))
        assertNull(direccionDesdeNominatim("""{"error":"Unable to geocode"}"""))
        assertNull(direccionDesdeNominatim("<html>"))
    }
}
