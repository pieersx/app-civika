package pe.civika.shared.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.MiIncidente
import pe.civika.shared.domain.PosibleDuplicado
import pe.civika.shared.domain.ReporteCreado
import pe.civika.shared.domain.ResumenPublico
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/** Incidentes desde Supabase: lecturas públicas con RPC `security definer` y escrituras con RPC que validan. */
class SupabaseIncidenteRepository(
    private val cliente: SupabaseClient,
    private val config: SupabaseConfig,
    private val aviso: (String) -> Unit = ::println,
) : IncidenteRepository {

    @Volatile
    private var categoriasEnCache: List<CategoriaDto>? = null

    override suspend fun listar(): List<Incidente> = traduciendoErrores {
        coroutineScope {
            // En paralelo: si cambia un peso entre dos refrescos, la prioridad se recalcula con el nuevo.
            val categorias = async { leerCategorias() }
            val filas = async { cliente.postgrest.rpc("listar_incidentes_publicos").decodeList<IncidenteDto>() }
            construirIncidentes(filas.await(), categorias.await(), Clock.System.now(), aviso)
        }
    }

    override suspend fun categorias(): List<Categoria> = traduciendoErrores {
        leerCategorias().filter { it.activa }.map { it.aDominio() }
    }

    override suspend fun resumen(): ResumenPublico = traduciendoErrores {
        val fila = cliente.postgrest.rpc("resumen_publico").decodeList<ResumenDto>().firstOrNull() ?: ResumenDto()
        ResumenPublico(fila.abiertos.toInt(), fila.enAtencion.toInt(), fila.resueltos30d.toInt())
    }

    override suspend fun detalle(incidenteId: String): DetalleIncidente? = traduciendoErrores {
        val parametros = buildJsonObject { put("p_incidente_id", incidenteId) }
        coroutineScope {
            val fila = async { cliente.postgrest.rpc("detalle_incidente", parametros).decodeList<DetalleDto>().firstOrNull() }
            val historial = async { cliente.postgrest.rpc("historial_publico", parametros).decodeList<HistorialDto>() }
            val comentarios = async { cliente.postgrest.rpc("comentarios_publicos", parametros).decodeList<ComentarioDto>() }
            val fotos = async { urlsDeFotos(incidenteId) }
            val categorias = categoriasEnCache ?: leerCategorias()
            fila.await()?.aDominio(categorias, Clock.System.now(), fotos.await(), historial.await(), comentarios.await())
        }
    }

    /** Las fotos son privadas: se leen con URL firmadas de 10 minutos. Si fallan, el detalle se muestra sin fotos. */
    private suspend fun urlsDeFotos(incidenteId: String): List<String> = try {
        val rutas = cliente.postgrest
            .rpc("fotos_incidente", buildJsonObject { put("p_incidente_id", incidenteId) })
            .decodeList<FotoDto>()
            .map { it.ruta }
        if (rutas.isEmpty()) {
            emptyList()
        } else {
            cliente.storage.from(BUCKET_FOTOS)
                .createSignedUrls(DURACION_URL_FIRMADA, rutas)
                .mapNotNull { it.signedURL.takeIf { url -> it.error == null && url.isNotBlank() } }
                .map { url -> if (url.startsWith("http")) url else config.url.trimEnd('/') + "/storage/v1" + url }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        aviso("No se pudieron firmar las fotos del incidente $incidenteId: ${e.message}")
        emptyList()
    }

    override suspend fun misIncidentes(): List<MiIncidente> = traduciendoErrores {
        cliente.exigirUsuario()
        coroutineScope {
            val categorias = async { categoriasEnCache ?: leerCategorias() }
            val filas = async { cliente.postgrest.rpc("mis_incidentes").decodeList<IncidenteDto>() }
            construirMisIncidentes(filas.await(), categorias.await(), Clock.System.now(), aviso)
        }
    }

    override suspend fun buscarDuplicados(latitud: Double, longitud: Double, categoriaId: Int): List<PosibleDuplicado> =
        traduciendoErrores {
            cliente.exigirUsuario()
            val ahora = Clock.System.now()
            cliente.postgrest.rpc(
                "buscar_duplicados",
                buildJsonObject {
                    put("p_lat", latitud)
                    put("p_lon", longitud)
                    put("p_categoria_id", categoriaId)
                },
            ).decodeList<DuplicadoDto>().map { it.aDominio(ahora) }
        }

    override suspend fun apoyar(clienteId: String, incidenteId: String, fotos: List<String>): ReporteCreado =
        traduciendoErrores {
            cliente.exigirUsuario()
            cliente.postgrest.rpc(
                "apoyar_incidente",
                buildJsonObject {
                    put("p_cliente_id", clienteId)
                    put("p_incidente_id", incidenteId)
                    if (fotos.isNotEmpty()) put("p_fotos", JsonArray(fotos.map(::JsonPrimitive)))
                },
            ).decodeList<ApoyoDto>()
            // `apoyar_incidente` no devuelve el código: se lee del detalle para la pantalla de confirmación.
            val detalle = detalle(incidenteId)
                ?: throw ErrorCivika.DatosInvalidos("Incidente no encontrado")
            ReporteCreado(incidenteId, detalle.incidente.codigo, detalle.incidente.estado, detalle.entidad)
        }

    override suspend fun retirarApoyo(incidenteId: String) = traduciendoErrores {
        val usuarioId = cliente.exigirUsuario()
        cliente.from("apoyos").delete {
            filter {
                eq("incidente_id", incidenteId)
                eq("usuario_id", usuarioId)
            }
        }
        Unit
    }

    override suspend fun denunciar(incidenteId: String, motivo: String): Boolean = traduciendoErrores {
        cliente.exigirUsuario()
        cliente.postgrest.rpc(
            "denunciar_incidente",
            buildJsonObject {
                put("p_incidente_id", incidenteId)
                put("p_motivo", motivo.trim())
            },
        ).decodeAs<Boolean>()
    }

    override suspend fun verificarResolucion(incidenteId: String, sigueResuelto: Boolean): Estado = traduciendoErrores {
        cliente.exigirUsuario()
        val texto = cliente.postgrest.rpc(
            "verificar_resolucion",
            buildJsonObject {
                put("p_incidente_id", incidenteId)
                put("p_sigue_resuelto", sigueResuelto)
            },
        ).decodeAs<String>()
        estadoDesde(texto) ?: Estado.RESUELTO
    }

    private suspend fun leerCategorias(): List<CategoriaDto> =
        cliente.from("categorias").select().decodeList<CategoriaDto>().also { categoriasEnCache = it }

    internal suspend fun nombreDeEntidad(entidadId: Int?): String? {
        if (entidadId == null) return null
        return try {
            cliente.from("entidades").select { filter { eq("id", entidadId) } }
                .decodeList<EntidadDto>().firstOrNull()?.nombre
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    internal companion object {
        const val BUCKET_FOTOS = "fotos"
        val DURACION_URL_FIRMADA = 600.seconds
    }
}
