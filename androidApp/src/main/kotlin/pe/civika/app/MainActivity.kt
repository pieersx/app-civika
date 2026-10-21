package pe.civika.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.civika.app.feature.incidentes.IncidentesScreen
import pe.civika.app.ui.theme.CivikaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CivikaTheme {
                IncidentesScreen()
            }
        }
    }
}
