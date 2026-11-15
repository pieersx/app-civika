package pe.civika.app.device

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Fotos de los reportes en el almacenamiento interno. Cada foto se **re-codifica** como JPEG: al volver a comprimir el
 * bitmap no se copia ningún metadato, así que el EXIF (incluido el GPS de la cámara, que delataría un domicilio) se borra.
 */
class AlmacenFotosAndroid(private val contexto: Context) : AlmacenFotos {

    private companion object {
        const val ETIQUETA = "AlmacenFotos"
    }

    private fun carpeta(clienteId: String) = File(contexto.filesDir, "reportes/$clienteId")

    override suspend fun guardar(origen: String, clienteId: String): String? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(origen)
            val bitmap = decodificar(uri) ?: return@withContext null
            try {
                val bytes = CompresionJpeg.ajustar { calidad, escala -> codificar(bitmap, calidad, escala) }
                    ?: return@withContext null
                val destino = File(carpeta(clienteId), "foto-${System.nanoTime()}.jpg")
                destino.parentFile?.mkdirs()
                destino.writeBytes(bytes)
                destino.absolutePath
            } finally {
                bitmap.recycle()
                // La foto de la cámara es un temporal en la caché: ya no hace falta (la del selector no se toca).
                if (uri.scheme == "file") uri.path?.let { File(it).takeIf { f -> f.parentFile == contexto.cacheDir }?.delete() }
            }
        } catch (e: Exception) {
            Log.w(ETIQUETA, "No se pudo procesar la foto", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(ETIQUETA, "Sin memoria al procesar la foto", e)
            null
        }
    }

    private fun decodificar(uri: Uri): Bitmap? {
        val resolver = contexto.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Con inJustDecodeBounds, decodeStream devuelve siempre null: el resultado está en `limites`.
        (resolver.openInputStream(uri) ?: return null).use { BitmapFactory.decodeStream(it, null, limites) }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val opciones = BitmapFactory.Options().apply { inSampleSize = CompresionJpeg.muestreo(limites.outWidth, limites.outHeight) }
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null

        val orientacion = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val matriz = Matrix().apply {
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        if (matriz.isIdentity) return original
        val girado = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
        if (girado !== original) original.recycle()
        return girado
    }

    private fun codificar(bitmap: Bitmap, calidad: Int, escala: Float): ByteArray {
        val lado = maxOf(bitmap.width, bitmap.height)
        val factor = minOf(escala, CompresionJpeg.LADO_MAXIMO_PX.toFloat() / lado).coerceAtMost(1f)
        val aCodificar = if (factor < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt().coerceAtLeast(1), (bitmap.height * factor).toInt().coerceAtLeast(1), true)
        } else {
            bitmap
        }
        return ByteArrayOutputStream().use { salida ->
            aCodificar.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
            if (aCodificar !== bitmap) aCodificar.recycle()
            salida.toByteArray()
        }
    }

    override fun existe(ruta: String): Boolean = File(ruta).isFile

    override suspend fun borrar(ruta: String) {
        withContext(Dispatchers.IO) { File(ruta).delete() }
    }

    override suspend fun borrarCarpeta(clienteId: String) {
        withContext(Dispatchers.IO) { carpeta(clienteId).deleteRecursively() }
    }

    override suspend fun leer(ruta: String): ByteArray? = withContext(Dispatchers.IO) {
        File(ruta).takeIf { it.isFile }?.readBytes()
    }
}
