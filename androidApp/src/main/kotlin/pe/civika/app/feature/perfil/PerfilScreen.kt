package pe.civika.app.feature.perfil

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.app.BuildConfig
import pe.civika.app.R
import pe.civika.app.ui.components.AvisoDemostracion
import pe.civika.app.ui.components.BotonPrincipal
import pe.civika.app.ui.components.BotonSecundario
import pe.civika.app.ui.components.CampoTexto
import pe.civika.app.ui.components.CampoUi
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.encabezado
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.mensajeDeCampo
import pe.civika.app.ui.theme.CivikaTheme
import pe.civika.app.ui.theme.ModoTema
import pe.civika.shared.domain.Validaciones

@Composable
fun PerfilScreen(
    onIniciarSesion: () -> Unit,
    onCrearCuenta: () -> Unit,
    onPolitica: () -> Unit,
    viewModel: PerfilViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var confirmarEliminar by remember { mutableStateOf(false) }
    var confirmarCerrar by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.nav_perfil), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.encabezado())
        AvisoDemostracion(detalle = true)

        val usuario = estado.usuario
        if (usuario == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, CivikaTheme.colores.borde)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.perfil_sin_sesion_titulo), style = MaterialTheme.typography.titleLarge, modifier = Modifier.encabezado())
                    Text(stringResource(R.string.perfil_sin_sesion_texto), style = MaterialTheme.typography.bodyLarge)
                    BotonPrincipal(stringResource(R.string.iniciar_sesion), onIniciarSesion)
                    BotonSecundario(stringResource(R.string.crear_cuenta), onCrearCuenta)
                }
            }
        } else {
            Cuenta(estado, viewModel)
        }

        Tema(estado.tema, viewModel::cambiarTema)

        FilaAccion(Icons.Rounded.PrivacyTip, stringResource(R.string.perfil_politica), onPolitica)

        if (usuario != null) {
            BotonSecundario(stringResource(R.string.perfil_cerrar_sesion), { confirmarCerrar = true }, habilitado = !estado.trabajando)
            CivikaTextButton(stringResource(R.string.perfil_eliminar), { confirmarEliminar = true }, Modifier.fillMaxWidth(), habilitado = !estado.trabajando)
        }
        Text(
            stringResource(R.string.perfil_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (confirmarCerrar) {
        AlertDialog(
            onDismissRequest = { confirmarCerrar = false },
            title = { Text(stringResource(R.string.perfil_cerrar_titulo)) },
            text = { Text(stringResource(R.string.perfil_cerrar_texto)) },
            confirmButton = { CivikaTextButton(stringResource(R.string.perfil_cerrar_sesion), { confirmarCerrar = false; viewModel.cerrarSesion() }) },
            dismissButton = { CivikaTextButton(stringResource(R.string.cancelar), { confirmarCerrar = false }) },
        )
    }
    if (confirmarEliminar) {
        DialogoEliminar(
            onCancelar = { confirmarEliminar = false },
            onConfirmar = {
                confirmarEliminar = false
                viewModel.eliminarCuenta()
            },
        )
    }
}

@Composable
private fun Cuenta(estado: PerfilUiState, viewModel: PerfilViewModel) {
    val usuario = estado.usuario ?: return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, CivikaTheme.colores.borde)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val editado = estado.nombreEditado
            if (editado == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(usuario.nombre.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(usuario.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(usuario.correo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = viewModel::editarNombre) {
                        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.perfil_editar_nombre))
                    }
                }
            } else {
                CampoTexto(
                    valor = editado,
                    onCambio = viewModel::cambiarNombre,
                    etiqueta = stringResource(R.string.nombre),
                    error = Validaciones.nombre(editado).mensajeDeCampo(CampoUi.NOMBRE),
                    habilitado = !estado.trabajando,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonPrincipal(stringResource(R.string.guardar), viewModel::guardarNombre, Modifier.weight(1f), habilitado = estado.puedeGuardarNombre, cargando = estado.trabajando)
                    BotonSecundario(stringResource(R.string.cancelar), viewModel::cancelarEdicion, Modifier.weight(1f))
                }
            }
            estado.error?.let { Text(it.mensaje(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun Tema(actual: ModoTema, onCambiar: (ModoTema) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, CivikaTheme.colores.borde)) {
        Column(Modifier.padding(vertical = 8.dp).selectableGroup()) {
            Text(
                stringResource(R.string.perfil_tema),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).encabezado(),
            )
            ModoTema.entries.forEach { modo ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = actual == modo, role = Role.RadioButton, onClick = { onCambiar(modo) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(selected = actual == modo, onClick = null)
                    Text(
                        stringResource(
                            when (modo) {
                                ModoTema.SISTEMA -> R.string.tema_sistema
                                ModoTema.CLARO -> R.string.tema_claro
                                ModoTema.OSCURO -> R.string.tema_oscuro
                            },
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaAccion(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, CivikaTheme.colores.borde),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** Confirmación fuerte: hay que escribir la palabra ELIMINAR. Borrar la cuenta no se puede deshacer. */
@Composable
private fun DialogoEliminar(onCancelar: () -> Unit, onConfirmar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    val palabra = stringResource(R.string.eliminar_palabra)
    val habilitado = texto.trim().equals(palabra, ignoreCase = true)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.eliminar_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.eliminar_texto), style = MaterialTheme.typography.bodyMedium)
                CampoTexto(
                    valor = texto,
                    onCambio = { texto = it },
                    etiqueta = stringResource(R.string.eliminar_campo, palabra),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                enabled = habilitado,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.eliminar_confirmar), style = MaterialTheme.typography.labelLarge) }
        },
        dismissButton = { CivikaTextButton(stringResource(R.string.cancelar), onCancelar) },
    )
}
