package pe.civika.shared.data

import pe.civika.shared.domain.Categoria
import pe.civika.shared.domain.ComentarioPublico
import pe.civika.shared.domain.DetalleIncidente
import pe.civika.shared.domain.ErrorCivika
import pe.civika.shared.domain.Estado
import pe.civika.shared.domain.EventoHistorial
import pe.civika.shared.domain.Incidente
import pe.civika.shared.domain.MiIncidente
import pe.civika.shared.domain.Notificacion
import pe.civika.shared.domain.PosibleDuplicado
import pe.civika.shared.domain.Relacion
import pe.civika.shared.domain.TipoNotificacion
import pe.civika.shared.domain.diasCalendarioLima
import kotlin.time.Instant

// Mapeo puro de las filas de Supabase al dominio: se prueba sin red (commonTest/data/MapeoTest.kt).

/** Tope de filas de `listar_incidentes_publicos` y `mis_incidentes`. */
internal const val TOPE_FILAS = 200

/** `en_revision` → [Estado.EN_REVISION]. `null` si el servidor manda un estado que esta versión no conoce. */
internal fun estadoDesde(texto: String?): Estado? =
    texto?.let { valor -> Estado.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) } }

/** PostgREST manda `2026-11-01T14:03:27.123456+00:00`. `null` si el texto no es una fecha. */
internal fun instanteDesde(texto: String?): Instant? =
    texto?.let { runCatching { Instant.parse(it) }.getOrNull() }

internal fun CategoriaDto.aDominio() = Categoria(
    id = id,
    serviceCode = serviceCode,
    nombre = nombre,
    pesoPrioridad = pesoPrioridad,
    radioDuplicadoM = radioDuplicadoM,
    ventanaDias = ventanaDias,
    plazoObjetivoDias = plazoObjetivoDias,
    activa = activa,
)

/**
 * Arma los incidentes con su categoría. Una fila con un estado, una categoría o una fecha desconocidos se omite con
 * un aviso en vez de cerrar la app. Las categorías inactivas sí se incluyen: sus incidentes siguen existiendo.
 */
internal fun construirIncidentes(
    filas: List<IncidenteDto>,
    categorias: List<CategoriaDto>,
    ahora: Instant,
    aviso: (String) -> Unit,
): List<Incidente> {
    if (filas.size >= TOPE_FILAS) {
        aviso("La consulta devolvió $TOPE_FILAS incidentes (el tope): puede haber más que no se muestran.")
    }
    val porId = categorias.associate { it.id to it.aDominio() }
    return filas.mapNotNull { it.aIncidente(porId, ahora, aviso) }
}

internal fun construirMisIncidentes(
    filas: List<IncidenteDto>,
    categorias: List<CategoriaDto>,
    ahora: Instant,
    aviso: (String) -> Unit,
): List<MiIncidente> {
    val porId = categorias.associate { it.id to it.aDominio() }
    return filas.mapNotNull { fila ->
        val incidente = fila.aIncidente(porId, ahora, aviso) ?: return@mapNotNull null
        MiIncidente(
            incidente = incidente,
            relacion = if (fila.relacion == "autor") Relacion.AUTOR else Relacion.APOYO,
            desde = instanteDesde(fila.desde),
        )
    }
}

private fun IncidenteDto.aIncidente(
    categorias: Map<Int, Categoria>,
    ahora: Instant,
    aviso: (String) -> Unit,
): Incidente? {
    val estado = estadoDesde(estado) ?: return omitir(aviso, codigo, "estado desconocido «$estado»")
    val categoria = categorias[categoriaId] ?: return omitir(aviso, codigo, "categoría desconocida $categoriaId")
    val creado = instanteDesde(creadoEn) ?: return omitir(aviso, codigo, "fecha inválida «$creadoEn»")
    return Incidente(
        id = id,
        codigo = codigo,
        titulo = titulo?.takeIf { it.isNotBlank() } ?: categoria.nombre,
        categoria = categoria,
        estado = estado,
        apoyos = apoyosCount,
        diasAbierto = diasCalendarioLima(creado, ahora),
        zona = zona,
        latitud = lat,
        longitud = lon,
        creadoEn = creado,
    )
}

private fun omitir(aviso: (String) -> Unit, codigo: String, motivo: String): Incidente? {
    aviso("Se omite el incidente $codigo: $motivo.")
    return null
}

internal fun DetalleDto.aDominio(
    categorias: List<CategoriaDto>,
    ahora: Instant,
    fotos: List<String>,
    historial: List<HistorialDto>,
    comentarios: List<ComentarioDto>,
): DetalleIncidente? {
    val estado = estadoDesde(estado) ?: return null
    val categoria = categorias.firstOrNull { it.id == categoriaId }?.aDominio() ?: return null
    val creado = instanteDesde(creadoEn) ?: return null
    return DetalleIncidente(
        incidente = Incidente(
            id = id,
            codigo = codigo,
            titulo = titulo?.takeIf { it.isNotBlank() } ?: categoria.nombre,
            categoria = categoria,
            estado = estado,
            apoyos = apoyosCount,
            diasAbierto = diasCalendarioLima(creado, ahora),
            zona = zona,
            latitud = lat,
            longitud = lon,
            creadoEn = creado,
        ),
        descripcion = descripcion?.takeIf { it.isNotBlank() },
        entidad = entidad,
        yoApoyo = yoApoyo,
        esMio = esMio,
        miVerificacion = miVerificacion,
        motivoRechazo = motivoRechazo?.takeIf { it.isNotBlank() },
        fotos = fotos,
        historial = historial.mapNotNull { fila ->
            val e = estadoDesde(fila.estado) ?: return@mapNotNull null
            val fecha = instanteDesde(fila.creadoEn) ?: return@mapNotNull null
            EventoHistorial(e, fecha)
        },
        comentarios = comentarios.mapNotNull { fila ->
            instanteDesde(fila.creadoEn)?.let { ComentarioPublico(fila.texto, it) }
        },
    )
}

internal fun DuplicadoDto.aDominio(ahora: Instant) = PosibleDuplicado(
    incidenteId = incidenteId,
    codigo = codigo,
    distanciaM = distanciaM,
    apoyos = apoyosCount,
    diasAbierto = instanteDesde(creadoEn)?.let { diasCalendarioLima(it, ahora) } ?: 0,
)

internal fun NotificacionDto.aDominio(): Notificacion? {
    val fecha = instanteDesde(creadoEn) ?: return null
    return Notificacion(
        id = id,
        tipo = TipoNotificacion.desde(tipo),
        estado = estadoDesde(estado),
        leida = leida,
        fecha = fecha,
        incidenteId = incidenteId,
        codigo = codigo,
        titulo = titulo,
    )
}

/**
 * Traduce un error de PostgREST o de una RPC. Los códigos salen de las migraciones:
 * `P0001` fuera de zona, `22023` datos inválidos, `42501` sin sesión o sin permiso.
 */
internal fun clasificarErrorPostgres(codigo: String?, mensaje: String?, estadoHttp: Int): ErrorCivika {
    val texto = mensaje.orEmpty()
    return when {
        codigo == "P0001" && texto.contains("zona", ignoreCase = true) -> ErrorCivika.FueraDeZona()
        // El tope de reportes por hora comparte el código 22023 con los datos inválidos, pero se
        // resuelve esperando: no debe tratarse como un error definitivo.
        codigo == "22023" && texto.contains("demasiados reportes", ignoreCase = true) -> ErrorCivika.DemasiadosIntentos()
        codigo == "22023" -> ErrorCivika.DatosInvalidos(texto)
        codigo == "42501" && texto.contains("sesión", ignoreCase = true) -> ErrorCivika.SesionRequerida()
        codigo == "42501" -> ErrorCivika.SinPermiso(texto)
        // JWT vencido o inválido
        codigo == "PGRST301" || codigo == "PGRST302" || estadoHttp == 401 -> ErrorCivika.SesionRequerida()
        estadoHttp == 403 -> ErrorCivika.SinPermiso(texto)
        estadoHttp == 429 -> ErrorCivika.DemasiadosIntentos()
        estadoHttp >= 500 -> ErrorCivika.SinConexion()
        else -> ErrorCivika.Desconocido(IllegalStateException("$codigo: $texto"))
    }
}

/** Traduce un código de error de Supabase Auth (`error_code` de GoTrue). */
internal fun clasificarErrorAuth(codigo: String?, estadoHttp: Int): ErrorCivika = when (codigo) {
    "invalid_credentials", "invalid_grant" -> ErrorCivika.CredencialesInvalidas()
    "email_not_confirmed" -> ErrorCivika.CorreoSinConfirmar()
    "user_already_exists", "email_exists" -> ErrorCivika.CorreoYaRegistrado()
    "weak_password" -> ErrorCivika.ContrasenaDebil()
    "over_request_rate_limit", "over_email_send_rate_limit" -> ErrorCivika.DemasiadosIntentos()
    "session_not_found", "session_expired", "refresh_token_not_found", "user_not_found", "bad_jwt" ->
        ErrorCivika.SesionRequerida()
    else -> when {
        estadoHttp == 429 -> ErrorCivika.DemasiadosIntentos()
        estadoHttp >= 500 -> ErrorCivika.SinConexion()
        else -> ErrorCivika.Desconocido(IllegalStateException("auth: $codigo"))
    }
}
