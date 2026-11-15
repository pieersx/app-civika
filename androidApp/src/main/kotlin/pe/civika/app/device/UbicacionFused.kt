package pe.civika.app.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Ubicación con Fused Location. Detecta GPS simulado (`isMock`, API 31+; `isFromMockProvider` antes). */
class UbicacionFused(private val contexto: Context) : UbicacionProvider {

    override suspend fun ubicacionActual(): UbicacionObtenida? {
        if (!tienePermiso()) return null
        val ubicacion = withTimeoutOrNull(TIEMPO_MAXIMO_MS) { actual() }
            ?: ultimaConocida()
            ?: return null
        return UbicacionObtenida(ubicacion.latitude, ubicacion.longitude, esSimulada(ubicacion))
    }

    private fun tienePermiso(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    private suspend fun actual(): Location? = suspendCancellableCoroutine { continuacion ->
        val cancelacion = CancellationTokenSource()
        continuacion.invokeOnCancellation { cancelacion.cancel() }
        try {
            LocationServices.getFusedLocationProviderClient(contexto)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelacion.token)
                .addOnSuccessListener { if (continuacion.isActive) continuacion.resume(it) }
                .addOnFailureListener { if (continuacion.isActive) continuacion.resume(null) }
        } catch (e: Exception) {
            if (continuacion.isActive) continuacion.resume(null)
        }
    }

    /** Sin Google Play Services (o si el GPS tarda) se usa la última posición que conozca el sistema. */
    @SuppressLint("MissingPermission")
    private fun ultimaConocida(): Location? = try {
        val manager = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        manager.getProviders(true)
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
    } catch (e: SecurityException) {
        null
    }

    @Suppress("DEPRECATION")
    private fun esSimulada(ubicacion: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ubicacion.isMock else ubicacion.isFromMockProvider

    private companion object {
        const val TIEMPO_MAXIMO_MS = 10_000L
    }
}
