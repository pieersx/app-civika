package pe.civika.panel.feature.sesion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.panel.ui.LogoCivika
import pe.civika.panel.ui.TarjetaPanel

/**
 * Ingreso al panel (RF-13): una sola tarjeta centrada con correo, contraseña y el botón
 * «Ingresar». Debajo quedan las cuentas de ejemplo y el recordatorio de que Cívika no es un
 * canal de emergencias (105 / 116).
 */
@Composable
fun PanelLoginScreen(viewModel: PanelLoginViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        // La tarjeta se centra en la ventana; el scroll vive dentro, para que al no caber el
        // contenido se pueda leer entero sin que la parte de arriba quede fuera del encuadre.
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LogoCivika(tamano = 56.dp, colorTitulo = MaterialTheme.colorScheme.onBackground)

                TarjetaPanel(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "Iniciar sesión",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            "Entra con tu correo de la municipalidad o de la moderación.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        OutlinedTextField(
                            value = estado.correo,
                            onValueChange = viewModel::escribirCorreo,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Correo") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )

                        OutlinedTextField(
                            value = estado.contrasena,
                            onValueChange = viewModel::escribirContrasena,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Contraseña") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )

                        if (estado.error != null) {
                            Text(
                                estado.error.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Button(
                            onClick = viewModel::ingresar,
                            enabled = estado.puedeIngresar,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Text(
                                if (estado.enviando) "Entrando…" else "Ingresar",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        Text(
                            "Cuentas de ejemplo: luis@civika.pe (entidad) o rosa@civika.pe (moderación), " +
                                "con una contraseña de 6 caracteres o más.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    "Cívika no es un canal de emergencias. Si hay una emergencia, llama al 105 o al 116.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
