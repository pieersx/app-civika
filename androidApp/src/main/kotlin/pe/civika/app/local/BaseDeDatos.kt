package pe.civika.app.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Un envío en la cola (ADR-6). Los enums se guardan como texto para que el esquema se lea y migre fácil. */
@Entity(tableName = "reportes_pendientes")
data class ReporteEntity(
    @PrimaryKey val clienteId: String,
    val usuarioId: String,
    val tipo: String,
    val incidenteId: String?,
    val latitud: Double,
    val longitud: Double,
    val categoriaId: Int,
    val categoriaNombre: String,
    val descripcion: String,
    /** Rutas de los JPEG separadas por salto de línea. */
    val fotos: String,
    val ubicacionSimulada: Boolean,
    val direccion: String?,
    val estado: String,
    val motivoError: String?,
    val detalleError: String?,
    val intentos: Int,
    val creadoEnMs: Long,
    val incidenteCreadoId: String?,
    val codigo: String?,
    val estadoIncidente: String?,
    val entidad: String?,
)

@Dao
interface ReporteDao {
    @Query("SELECT * FROM reportes_pendientes WHERE usuarioId = :usuarioId ORDER BY creadoEnMs DESC")
    fun observar(usuarioId: String): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes_pendientes WHERE clienteId = :clienteId")
    fun observarUno(clienteId: String): Flow<ReporteEntity?>

    @Upsert
    suspend fun guardar(reporte: ReporteEntity)

    @Query("SELECT * FROM reportes_pendientes WHERE usuarioId = :usuarioId AND estado = 'PENDIENTE' ORDER BY creadoEnMs ASC")
    suspend fun pendientes(usuarioId: String): List<ReporteEntity>

    @Query("SELECT COUNT(*) FROM reportes_pendientes WHERE estado = 'PENDIENTE'")
    suspend fun cuantosPendientes(): Int

    /** Si el proceso murió a mitad de un envío, ese envío vuelve a la cola. */
    @Query("UPDATE reportes_pendientes SET estado = 'PENDIENTE' WHERE usuarioId = :usuarioId AND estado = 'ENVIANDO'")
    suspend fun reanudarEnviando(usuarioId: String)

    @Query("UPDATE reportes_pendientes SET estado = 'ENVIANDO', intentos = intentos + 1 WHERE clienteId = :clienteId")
    suspend fun marcarEnviando(clienteId: String)

    @Query("UPDATE reportes_pendientes SET estado = 'PENDIENTE', motivoError = :motivo WHERE clienteId = :clienteId")
    suspend fun marcarReintento(clienteId: String, motivo: String)

    @Query("UPDATE reportes_pendientes SET estado = 'ERROR', motivoError = :motivo, detalleError = :detalle WHERE clienteId = :clienteId")
    suspend fun marcarError(clienteId: String, motivo: String, detalle: String?)

    @Query(
        "UPDATE reportes_pendientes SET estado = 'ENVIADO', motivoError = NULL, detalleError = NULL, " +
            "incidenteCreadoId = :incidenteId, codigo = :codigo, estadoIncidente = :estado, entidad = :entidad, " +
            "direccion = :direccion WHERE clienteId = :clienteId",
    )
    suspend fun marcarEnviado(clienteId: String, incidenteId: String, codigo: String, estado: String, entidad: String?, direccion: String?)

    @Query("UPDATE reportes_pendientes SET estado = 'PENDIENTE', motivoError = NULL, detalleError = NULL WHERE clienteId = :clienteId")
    suspend fun reintentar(clienteId: String)

    @Query("DELETE FROM reportes_pendientes WHERE clienteId = :clienteId")
    suspend fun borrar(clienteId: String)

    @Query("DELETE FROM reportes_pendientes WHERE estado = 'ENVIADO' AND creadoEnMs < :limiteMs")
    suspend fun purgarEnviados(limiteMs: Long)
}

@Database(entities = [ReporteEntity::class], version = 1, exportSchema = true)
abstract class CivikaDatabase : RoomDatabase() {
    abstract fun reportes(): ReporteDao

    companion object {
        fun crear(contexto: Context): CivikaDatabase =
            Room.databaseBuilder(contexto.applicationContext, CivikaDatabase::class.java, "civika.db").build()
    }
}
