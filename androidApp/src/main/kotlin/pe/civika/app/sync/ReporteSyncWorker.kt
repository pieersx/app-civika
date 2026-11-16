package pe.civika.app.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pe.civika.app.device.AlmacenFotos
import pe.civika.app.local.ReporteDao
import pe.civika.app.local.aDominio
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.domain.EstadoSesion
import pe.civika.shared.domain.MotivoError
import pe.civika.shared.domain.ResultadoSync
import pe.civika.shared.domain.SincronizarReporte
import java.util.concurrent.TimeUnit

/** Pide a WorkManager que envíe la cola. Interfaz para poder sustituirla en pruebas. */
fun interface ProgramadorSync {
    fun programar()
}

class ProgramadorWorkManager(private val contexto: Context) : ProgramadorSync {
    override fun programar() {
        val pedido = OneTimeWorkRequestBuilder<ReporteSyncWorker>()
            // Solo con red: sin conexión el trabajo espera y arranca cuando vuelve la señal.
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: un envío nuevo no interrumpe al que va en curso, y uno fallido no bloquea a los demás.
        WorkManager.getInstance(contexto).enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.APPEND_OR_REPLACE, pedido)
    }

    private companion object {
        const val NOMBRE_TRABAJO = "sincronizar-reportes"
    }
}

/**
 * Envía los reportes de la cola (RF-15, ADR-6). Es idempotente: el `cliente_id` evita duplicados si el sistema lo
 * repite. Los errores pasajeros se reintentan con backoff; los definitivos (fuera de zona, validación) no.
 */
class ReporteSyncWorker(contexto: Context, parametros: WorkerParameters) :
    CoroutineWorker(contexto, parametros), KoinComponent {

    private val dao: ReporteDao by inject()
    private val auth: AuthRepository by inject()
    private val sincronizar: SincronizarReporte by inject()
    private val fotos: AlmacenFotos by inject()

    override suspend fun doWork(): Result {
        val sesion = withTimeoutOrNull(ESPERA_SESION_MS) { auth.sesion.first { it !is EstadoSesion.Cargando } }
        // Sin sesión no se envía nada: los envíos siguen en la cola y se reprograman al iniciar sesión.
        val usuario = (sesion as? EstadoSesion.Activa)?.usuario ?: return Result.success()

        dao.reanudarEnviando(usuario.id)
        var hayQueReintentar = false
        for (fila in dao.pendientes(usuario.id)) {
            dao.marcarEnviando(fila.clienteId)
            when (val resultado = sincronizar(fila.aDominio()) { fotos.leer(it) }) {
                is ResultadoSync.Enviado -> {
                    val creado = resultado.creado
                    dao.marcarEnviado(
                        clienteId = fila.clienteId,
                        incidenteId = creado.incidenteId,
                        codigo = creado.codigo,
                        estado = creado.estado.name,
                        entidad = creado.entidad,
                        direccion = resultado.direccion,
                    )
                    // Las fotos ya están en el servidor: la copia local deja de hacer falta.
                    fotos.borrarCarpeta(fila.clienteId)
                }
                is ResultadoSync.Reintentar -> {
                    dao.marcarReintento(fila.clienteId, resultado.motivo.name)
                    if (resultado.motivo != MotivoError.SESION) hayQueReintentar = true
                }
                is ResultadoSync.Definitivo -> dao.marcarError(fila.clienteId, resultado.motivo.name, resultado.detalle)
            }
        }
        dao.purgarEnviados(System.currentTimeMillis() - SIETE_DIAS_MS)
        return if (hayQueReintentar) Result.retry() else Result.success()
    }

    private companion object {
        const val ESPERA_SESION_MS = 15_000L
        const val SIETE_DIAS_MS = 7L * 24 * 60 * 60 * 1000
    }
}
