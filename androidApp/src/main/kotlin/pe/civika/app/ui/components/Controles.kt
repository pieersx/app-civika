package pe.civika.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pe.civika.app.R
import pe.civika.app.ui.theme.CivikaTheme

fun Modifier.encabezado(): Modifier = semantics { heading() }

/** Acción principal de la pantalla: terracota, 56 dp de alto, el texto encima en blanco (o `onPrimary` en oscuro). */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(texto, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    icono: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Enlaces y acciones de texto. Material 3 pinta el texto con `primary`, que en claro (terracota) da 4,22:1 sobre
 * arena; por eso aquí se usa `terracotaTexto` (5,59:1).
 */
@Composable
fun CivikaTextButton(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = CivikaTheme.colores.terracotaTexto),
    ) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Chip de filtro: seleccionado en verde andino, con ícono de visto además del color. */
@Composable
fun ChipFiltro(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        modifier = modifier,
        label = { Text(texto, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = icono?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) } },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = seleccionado,
            borderColor = CivikaTheme.colores.bordeCampo,
            selectedBorderColor = MaterialTheme.colorScheme.secondary,
        ),
    )
}

/** Campo de texto con el contorno `bordeCampo` (3:1 mínimo, WCAG 1.4.11) y el error escrito, no solo en rojo. */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    esContrasena: Boolean = false,
    minLineas: Int = 1,
    maxLineas: Int = if (minLineas > 1) 8 else 1,
    habilitado: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        modifier = modifier.fillMaxWidth(),
        enabled = habilitado,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = maxLineas == 1,
        minLines = minLineas,
        maxLines = maxLineas,
        shape = MaterialTheme.shapes.small,
        visualTransformation = if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esContrasena) KeyboardType.Password else teclado, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        trailingIcon = if (esContrasena) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = stringResource(if (visible) R.string.ocultar_contrasena else R.string.mostrar_contrasena),
                    )
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = CivikaTheme.colores.bordeCampo,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Barra superior con título como encabezado y botón de volver de 48 dp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(titulo: String, onVolver: (() -> Unit)?, modifier: Modifier = Modifier, acciones: @Composable () -> Unit = {}) {
    TopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.encabezado()) },
        modifier = modifier,
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.volver))
                }
            }
        },
        actions = { acciones() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}
