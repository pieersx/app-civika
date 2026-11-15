package pe.civika.app.device

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import pe.civika.app.MainActivity
import pe.civika.app.R
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.NotificacionRepository
import pe.civika.shared.di.ALCANCE_APP
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.usuario
import kotlin.coroutines.resume

/**
 * Token de FCM. Sin `google-services.json` Firebase no se inicializa: en ese caso (o ante cualquier fallo)
 * devuelve `null` y la app sigue funcionando solo con la bandeja de avisos.
 */
@Suppress("DEPRECATION")
class TokenFcm(private val contexto: Context) : TokenDispositivo {

    override suspend fun actual(): String? = try {
        if (FirebaseApp.getApps(contexto).isEmpty()) {
            null
        } else {
            suspendCancellableCoroutine { continuacion ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { tarea ->
                    if (continuacion.isActive) continuacion.resume(if (tarea.isSuccessful) tarea.result else null)
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }
}

/** Registra el token en `dispositivos` (RPC `registrar_dispositivo`) cada vez que se inicia sesión. */
class RegistroDispositivo(
    private val auth: AuthRepository,
    private val notificaciones: NotificacionRepository,
    private val tokens: TokenDispositivo,
    private val alcance: CoroutineScope,
) {
    fun iniciar() {
        alcance.launch {
            auth.sesion
                .filter { it !is EstadoSesion.Cargando }
                .map { it.usuario?.id }
                .distinctUntilChanged()
                .collect { usuarioId ->
                    if (usuarioId != null) registrarActual()
                }
        }
    }

    suspend fun registrarActual() = registrar(tokens.actual())

    suspend fun registrar(token: String?) {
        if (token == null) return
        try {
            notificaciones.registrarDispositivo(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // El push es un extra: si falla el registro, la bandeja funciona igual y se reintenta en el próximo inicio.
        }
    }
}

object Notificador {
    const val CANAL_ESTADO = "estado_reportes"
    const val EXTRA_INCIDENTE_ID = "incidente_id"

    fun crearCanal(contexto: Context) {
        val canal = NotificationChannel(
            CANAL_ESTADO,
            contexto.getString(R.string.canal_estado_nombre),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = contexto.getString(R.string.canal_estado_descripcion) }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun mostrar(contexto: Context, titulo: String?, cuerpo: String?, incidenteId: String?) {
        val concedido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!concedido) return
        val intent = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            incidenteId?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_INCIDENTE_ID, it) }
        }
        val pendiente = PendingIntent.getActivity(
            contexto,
            incidenteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL_ESTADO)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo ?: contexto.getString(R.string.app_name))
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        contexto.getSystemService(NotificationManager::class.java).notify(incidenteId.hashCode(), notificacion)
    }
}

/** Solo recibe mensajes si Firebase está configurado. Con la app abierta FCM no muestra nada solo: se muestra aquí. */
class CivikaMessagingService : FirebaseMessagingService(), KoinComponent {

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        val alcance: CoroutineScope = get(ALCANCE_APP)
        val registro: RegistroDispositivo = get()
        alcance.launch { registro.registrar(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val aviso = message.notification ?: return
        Notificador.mostrar(this, aviso.title, aviso.body, message.data[Notificador.EXTRA_INCIDENTE_ID])
    }
}
