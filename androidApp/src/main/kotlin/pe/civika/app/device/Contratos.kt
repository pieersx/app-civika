package pe.civika.app.device

import kotlinx.coroutines.flow.Flow
import pe.civika.app.ui.theme.ModoTema

// Lo que los ViewModels necesitan del dispositivo, como interfaces: así se prueban sin Android.

/** Guarda las fotos de un reporte ya recomprimidas (JPEG ≤ 2 MB, sin EXIF) en el almacenamiento interno. */
interface AlmacenFotos {
    /** Re-codifica la imagen de [origen] (un `content://` o `file://`) y devuelve la ruta del JPEG, o `null` si falla. */
    suspend fun guardar(origen: String, clienteId: String): String?

    fun existe(ruta: String): Boolean

    suspend fun borrar(ruta: String)

    /** Borra todas las fotos del reporte. */
    suspend fun borrarCarpeta(clienteId: String)

    suspend fun leer(ruta: String): ByteArray?
}

data class UbicacionObtenida(val latitud: Double, val longitud: Double, val simulada: Boolean)

interface UbicacionProvider {
    /** La posición actual, o `null` si no hay permiso, GPS apagado o no se consiguió a tiempo. */
    suspend fun ubicacionActual(): UbicacionObtenida?
}

/** Token de FCM del dispositivo. Siempre `null` si Firebase no está configurado: nunca lanza. */
interface TokenDispositivo {
    suspend fun actual(): String?
}

/** Preferencias del usuario guardadas en el celular (DataStore). */
interface AjustesApp {
    val tema: Flow<ModoTema>

    suspend fun cambiarTema(modo: ModoTema)

    /** Si ya se le pidió el permiso de notificaciones (se pide una sola vez, tras el primer reporte). */
    val notificacionesPreguntadas: Flow<Boolean>

    suspend fun marcarNotificacionesPreguntadas()
}
