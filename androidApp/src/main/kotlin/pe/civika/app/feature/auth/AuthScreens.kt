package pe.civika.app.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pe.civika.app.R
import pe.civika.app.ui.components.BarraSuperior
import pe.civika.app.ui.components.BotonPrincipal
import pe.civika.app.ui.components.BotonSecundario
import pe.civika.app.ui.components.CampoTexto
import pe.civika.app.ui.components.CampoUi
import pe.civika.app.ui.components.CivikaTextButton
import pe.civika.app.ui.components.MarcadorSenal
import pe.civika.app.ui.components.encabezado
import pe.civika.app.ui.components.mensaje
import pe.civika.app.ui.components.mensajeDeCampo
import pe.civika.shared.domain.Familia
import pe.civika.shared.domain.Validaciones

/** Marco común de las pantallas de cuenta: barra superior, desplazamiento y teclado que no tapa los campos. */
@Composable
private fun MarcoAuth(titulo: String, onVolver: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = { BarraSuperior(titulo, onVolver) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            contenido()
        }
    }
}

/** Primera pantalla: qué es Cívika, el aviso de que no es para emergencias y las tres salidas. */
@Composable
fun BienvenidaScreen(onIniciarSesion: () -> Unit, onRegistrarse: () -> Unit, onExplorar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MarcadorSenal(Familia.ABIERTO, 40.dp)
            MarcadorSenal(Familia.EN_ATENCION, 40.dp)
            MarcadorSenal(Familia.RESUELTO, 40.dp)
        }
        Text(stringResource(R.string.bienvenida_titulo), style = MaterialTheme.typography.displaySmall, modifier = Modifier.encabezado())
        Text(
            stringResource(R.string.bienvenida_texto),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                stringResource(R.string.sin_emergencias),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        BotonPrincipal(stringResource(R.string.iniciar_sesion), onIniciarSesion)
        BotonSecundario(stringResource(R.string.crear_cuenta), onRegistrarse)
        CivikaTextButton(stringResource(R.string.explorar_sin_cuenta), onExplorar, Modifier.fillMaxWidth())
    }
}

@Composable
fun LoginScreen(
    onExito: () -> Unit,
    onRegistrarse: () -> Unit,
    onRecuperar: () -> Unit,
    onVolver: () -> Unit,
    motivoSesion: Boolean = false,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.autenticado) { if (estado.autenticado) onExito() }

    MarcoAuth(stringResource(R.string.iniciar_sesion), onVolver) {
        if (motivoSesion) {
            Text(stringResource(R.string.login_motivo), style = MaterialTheme.typography.bodyLarge)
        }
        CampoTexto(
            valor = estado.correo,
            onCambio = viewModel::cambiarCorreo,
            etiqueta = stringResource(R.string.correo),
            error = if (estado.intentado) estado.validacionCorreo.mensajeDeCampo(CampoUi.CORREO) else null,
            teclado = KeyboardType.Email,
            habilitado = !estado.enviando,
        )
        CampoTexto(
            valor = estado.contrasena,
            onCambio = viewModel::cambiarContrasena,
            etiqueta = stringResource(R.string.contrasena),
            error = if (estado.intentado) estado.validacionContrasenaLogin.mensajeDeCampo(CampoUi.CONTRASENA) else null,
            esContrasena = true,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::iniciarSesion,
            habilitado = !estado.enviando,
        )
        estado.error?.let { ErrorEnFormulario(it.mensaje()) }
        BotonPrincipal(stringResource(R.string.iniciar_sesion), viewModel::iniciarSesion, cargando = estado.enviando)
        CivikaTextButton(stringResource(R.string.olvide_contrasena), onRecuperar, Modifier.align(Alignment.CenterHorizontally))
        CivikaTextButton(stringResource(R.string.no_tengo_cuenta), onRegistrarse, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun RegistroScreen(
    onExito: () -> Unit,
    onIniciarSesion: () -> Unit,
    onLeerPolitica: () -> Unit,
    onVolver: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LaunchedEffect(estado.autenticado) { if (estado.autenticado) onExito() }

    MarcoAuth(stringResource(R.string.crear_cuenta), onVolver) {
        if (estado.confirmarCorreo) {
            RevisaTuCorreo(estado.correo.trim(), onIniciarSesion)
            return@MarcoAuth
        }
        CampoTexto(
            valor = estado.nombre,
            onCambio = viewModel::cambiarNombre,
            etiqueta = stringResource(R.string.nombre),
            error = if (estado.intentado) estado.validacionNombre.mensajeDeCampo(CampoUi.NOMBRE) else null,
            habilitado = !estado.enviando,
        )
        CampoTexto(
            valor = estado.correo,
            onCambio = viewModel::cambiarCorreo,
            etiqueta = stringResource(R.string.correo),
            error = if (estado.intentado) estado.validacionCorreo.mensajeDeCampo(CampoUi.CORREO) else null,
            teclado = KeyboardType.Email,
            habilitado = !estado.enviando,
        )
        CampoTexto(
            valor = estado.contrasena,
            onCambio = viewModel::cambiarContrasena,
            etiqueta = stringResource(R.string.contrasena),
            ayuda = stringResource(R.string.contrasena_ayuda, Validaciones.CONTRASENA_MIN),
            error = if (estado.intentado) estado.validacionContrasenaRegistro.mensajeDeCampo(CampoUi.CONTRASENA) else null,
            esContrasena = true,
            imeAction = ImeAction.Done,
            habilitado = !estado.enviando,
        )
        // Toda la fila es el área táctil de la casilla (48 dp o más) y TalkBack la lee como una sola casilla.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(
                    value = estado.consentimiento,
                    role = Role.Checkbox,
                    enabled = !estado.enviando,
                    onValueChange = viewModel::cambiarConsentimiento,
                ),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = estado.consentimiento, onCheckedChange = null, modifier = Modifier.padding(top = 4.dp))
            Text(
                stringResource(R.string.consentimiento_texto, Validaciones.VERSION_POLITICA),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (estado.faltaConsentimiento) ErrorEnFormulario(stringResource(R.string.consentimiento_obligatorio))
        CivikaTextButton(stringResource(R.string.leer_politica), onLeerPolitica)
        estado.error?.let { ErrorEnFormulario(it.mensaje()) }
        BotonPrincipal(stringResource(R.string.crear_cuenta), viewModel::registrar, cargando = estado.enviando)
        CivikaTextButton(stringResource(R.string.ya_tengo_cuenta), onIniciarSesion, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun RevisaTuCorreo(correo: String, onIniciarSesion: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Rounded.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(56.dp))
        Text(stringResource(R.string.revisa_correo_titulo), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.encabezado())
        Text(stringResource(R.string.revisa_correo_texto, correo), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        BotonPrincipal(stringResource(R.string.ya_confirme_iniciar_sesion), onIniciarSesion)
    }
}

@Composable
fun RecuperarScreen(onVolver: () -> Unit, viewModel: AuthViewModel = koinViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    MarcoAuth(stringResource(R.string.recuperar_titulo), onVolver) {
        if (estado.recuperacionEnviada) {
            Text(stringResource(R.string.recuperar_enviado, estado.correo.trim()), style = MaterialTheme.typography.bodyLarge)
            BotonPrincipal(stringResource(R.string.volver_a_iniciar_sesion), onVolver)
            return@MarcoAuth
        }
        Text(stringResource(R.string.recuperar_texto), style = MaterialTheme.typography.bodyLarge)
        CampoTexto(
            valor = estado.correo,
            onCambio = viewModel::cambiarCorreo,
            etiqueta = stringResource(R.string.correo),
            error = if (estado.intentado) estado.validacionCorreo.mensajeDeCampo(CampoUi.CORREO) else null,
            teclado = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::recuperarContrasena,
            habilitado = !estado.enviando,
        )
        estado.error?.let { ErrorEnFormulario(it.mensaje()) }
        BotonPrincipal(stringResource(R.string.enviar_enlace), viewModel::recuperarContrasena, cargando = estado.enviando)
    }
}

@Composable
private fun ErrorEnFormulario(texto: String) {
    // El texto del error va completo y con el color de error: no depende solo del color para entenderse.
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
}

/** Política de privacidad (versión 2026-10). El texto es el que acepta quien se registra. */
@Composable
fun PoliticaScreen(onVolver: () -> Unit) {
    MarcoAuth(stringResource(R.string.politica_titulo), onVolver) {
        Text(stringResource(R.string.politica_version, Validaciones.VERSION_POLITICA), style = MaterialTheme.typography.labelLarge)
        listOf(
            R.string.politica_p1_titulo to R.string.politica_p1,
            R.string.politica_p2_titulo to R.string.politica_p2,
            R.string.politica_p3_titulo to R.string.politica_p3,
            R.string.politica_p4_titulo to R.string.politica_p4,
            R.string.politica_p5_titulo to R.string.politica_p5,
            R.string.politica_p6_titulo to R.string.politica_p6,
        ).forEach { (titulo, texto) ->
            Text(stringResource(titulo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.encabezado())
            Text(stringResource(texto), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))
    }
}
