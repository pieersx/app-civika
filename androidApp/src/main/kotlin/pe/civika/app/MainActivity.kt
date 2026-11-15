package pe.civika.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.inject
import pe.civika.app.device.AjustesApp
import pe.civika.app.device.Notificador
import pe.civika.app.navigation.CivikaNavegacion
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.app.ui.theme.ModoTema
import pe.civika.app.ui.theme.esOscuro
import pe.civika.shared.data.AuthRepository
import pe.civika.shared.domain.EstadoSesion

class MainActivity : ComponentActivity() {

    private val auth: AuthRepository by inject()
    private val ajustes: AjustesApp by inject()

    /** Incidente que pidió abrir un aviso push; la navegación lo consume. */
    private val incidentePendiente = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // El splash se queda hasta saber si hay una sesión guardada: así no se ve un destello de «sin sesión».
        splash.setKeepOnScreenCondition { auth.sesion.value is EstadoSesion.Cargando }
        incidentePendiente.value = idDeIncidente(intent)

        setContent {
            val tema by ajustes.tema.collectAsStateWithLifecycle(initialValue = ModoTema.SISTEMA)
            val sesion by auth.sesion.collectAsStateWithLifecycle()
            val oscuro = tema.esOscuro()
            // Los íconos de las barras del sistema siguen al tema elegido, no solo al del teléfono.
            DisposableEffect(oscuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
                )
                onDispose {}
            }
            CivikaTheme(tema) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (sesion !is EstadoSesion.Cargando) {
                        CivikaNavegacion(
                            sesion = sesion,
                            incidentePendiente = incidentePendiente.value,
                            onIncidenteAbierto = { incidentePendiente.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incidentePendiente.value = idDeIncidente(intent)
    }

    private fun idDeIncidente(intent: Intent?): String? =
        intent?.getStringExtra(Notificador.EXTRA_INCIDENTE_ID)?.takeIf { it.isNotBlank() }
}
