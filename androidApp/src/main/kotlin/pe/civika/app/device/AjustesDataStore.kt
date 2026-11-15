package pe.civika.app.device

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import pe.civika.app.ui.theme.ModoTema

private val Context.almacenAjustes by preferencesDataStore(name = "ajustes")

class AjustesDataStore(contexto: Context) : AjustesApp {

    private val almacen = contexto.applicationContext.almacenAjustes

    override val tema: Flow<ModoTema> = almacen.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferencias ->
            ModoTema.entries.firstOrNull { it.name == preferencias[CLAVE_TEMA] } ?: ModoTema.SISTEMA
        }

    override suspend fun cambiarTema(modo: ModoTema) {
        almacen.edit { it[CLAVE_TEMA] = modo.name }
    }

    override val notificacionesPreguntadas: Flow<Boolean> = almacen.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { it[CLAVE_NOTIFICACIONES] ?: false }

    override suspend fun marcarNotificacionesPreguntadas() {
        almacen.edit { it[CLAVE_NOTIFICACIONES] = true }
    }

    private companion object {
        val CLAVE_TEMA = stringPreferencesKey("tema")
        val CLAVE_NOTIFICACIONES = booleanPreferencesKey("notificaciones_preguntadas")
    }
}
