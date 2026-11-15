package pe.civika.shared.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import pe.civika.shared.domain.ErrorCivika

/** URL del proyecto y clave pública `anon`. La `service_role` jamás llega a un cliente. */
data class SupabaseConfig(val url: String, val anonKey: String)

internal val JsonSupabase = Json {
    ignoreUnknownKeys = true // una columna nueva en una RPC no rompe un APK ya instalado
    explicitNulls = false
    encodeDefaults = true
}

fun crearClienteSupabase(config: SupabaseConfig): SupabaseClient =
    createSupabaseClient(supabaseUrl = config.url, supabaseKey = config.anonKey) {
        defaultSerializer = KotlinXSerializer(JsonSupabase)
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

/** Id del usuario con sesión, o [ErrorCivika.SesionRequerida] antes de gastar una llamada que va a fallar. */
internal fun SupabaseClient.exigirUsuario(): String =
    auth.currentUserOrNull()?.id ?: throw ErrorCivika.SesionRequerida()

/** Ejecuta una llamada a Supabase y traduce lo que falle a [ErrorCivika]. */
internal suspend fun <T> traduciendoErrores(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (e: ErrorCivika) {
    throw e
} catch (e: Exception) {
    // Queda en el log (etiqueta System.out) para saber qué falló de verdad detrás del mensaje que ve el ciudadano.
    println("Cívika: falló una llamada a Supabase (${e::class.simpleName}): ${e.message}")
    throw e.aErrorCivika()
}

internal fun Exception.aErrorCivika(): ErrorCivika = when (this) {
    is PostgrestRestException -> clasificarErrorPostgres(code, error, statusCode).conCausa(this)
    is AuthRestException -> clasificarErrorAuth(error, statusCode).conCausa(this)
    is RestException -> when (statusCode) {
        401 -> ErrorCivika.SesionRequerida(this)
        403 -> ErrorCivika.SinPermiso(error, this)
        429 -> ErrorCivika.DemasiadosIntentos(this)
        in 500..599 -> ErrorCivika.SinConexion(this)
        else -> ErrorCivika.Desconocido(this)
    }
    is HttpRequestTimeoutException, is IOException -> ErrorCivika.SinConexion(this)
    else -> ErrorCivika.Desconocido(this)
}

private fun ErrorCivika.conCausa(causa: Throwable): ErrorCivika = apply {
    if (cause == null) runCatching { initCause(causa) }
}
