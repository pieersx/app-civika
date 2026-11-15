package pe.civika.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.maplibre.android.MapLibre
import pe.civika.app.device.Notificador
import pe.civika.app.device.RegistroDispositivo
import pe.civika.app.di.appModule
import pe.civika.app.sync.ProgramadorSync
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.data.SupabaseConfig
import pe.civika.shared.di.ALCANCE_APP
import pe.civika.shared.di.crearSharedModule
import pe.civika.shared.domain.EstadoSesion

class CivikaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        Notificador.crearCanal(this)

        // Sin URL y clave `anon` (local.properties o variables de entorno) arranca en modo demostración.
        val configuracion = if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        } else {
            null
        }
        val koin = startKoin {
            androidContext(this@CivikaApplication)
            modules(crearSharedModule(configuracion), appModule)
        }.koin

        koin.get<RegistroDispositivo>().iniciar()
        // Al iniciar sesión se envía lo que haya quedado en la cola (y al abrir la app con sesión ya iniciada).
        val programador: ProgramadorSync = koin.get()
        koin.get<CoroutineScope>(ALCANCE_APP).launch {
            koin.get<AuthRepository>().sesion
                .filterIsInstance<EstadoSesion.Activa>()
                .map { it.usuario.id }
                .distinctUntilChanged()
                .collect { programador.programar() }
        }
    }
}
