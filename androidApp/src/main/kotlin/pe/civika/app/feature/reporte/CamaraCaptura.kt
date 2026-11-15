package pe.civika.app.feature.reporte

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import pe.civika.app.R
import pe.civika.app.ui.components.BotonPrincipal
import pe.civika.app.ui.components.BotonSecundario
import java.io.File

/**
 * Cámara dentro de la app con CameraX (RF-02). El permiso se pide aquí, en el momento de usarla.
 * La foto queda en la caché; el almacén de fotos la recomprime y borra el archivo temporal.
 */
@Composable
fun CamaraCaptura(onFoto: (String) -> Unit, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    val propietario = LocalLifecycleOwner.current
    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var rechazado by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permiso = it
        rechazado = !it
    }
    LaunchedEffect(Unit) { if (!permiso) pedirPermiso.launch(Manifest.permission.CAMERA) }

    if (!permiso) {
        SinPermisoDeCamara(rechazado, onPedir = { pedirPermiso.launch(Manifest.permission.CAMERA) }, onCerrar = onCerrar)
        return
    }

    var fallo by remember { mutableStateOf(false) }
    var capturando by remember { mutableStateOf(false) }
    val capturador = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val vista = remember { PreviewView(contexto).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(vista, propietario) {
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener(
            {
                try {
                    val proveedor = futuro.get()
                    val previa = Preview.Builder().build().also { it.setSurfaceProvider(vista.surfaceProvider) }
                    proveedor.unbindAll()
                    proveedor.bindToLifecycle(propietario, CameraSelector.DEFAULT_BACK_CAMERA, previa, capturador)
                } catch (e: Exception) {
                    fallo = true
                }
            },
            ContextCompat.getMainExecutor(contexto),
        )
        onDispose { runCatching { futuro.get().unbindAll() } }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { vista }, modifier = Modifier.fillMaxSize())

        IconButton(
            onClick = onCerrar,
            modifier = Modifier.statusBarsPadding().padding(8.dp).align(Alignment.TopStart),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f), contentColor = Color.White),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.camara_cerrar))
        }

        if (fallo) {
            Text(
                stringResource(R.string.camara_error),
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
        }

        // Disparador de 72 dp: el área táctil supera de sobra los 48 dp.
        val descripcion = stringResource(R.string.camara_tomar_foto)
        Surface(
            onClick = {
                if (capturando || fallo) return@Surface
                capturando = true
                val archivo = File.createTempFile("captura", ".jpg", contexto.cacheDir)
                capturador.takePicture(
                    ImageCapture.OutputFileOptions.Builder(archivo).build(),
                    ContextCompat.getMainExecutor(contexto),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(resultado: ImageCapture.OutputFileResults) {
                            capturando = false
                            onFoto(Uri.fromFile(archivo).toString())
                        }

                        override fun onError(error: ImageCaptureException) {
                            capturando = false
                            fallo = true
                        }
                    },
                )
            },
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(4.dp, Color.Black.copy(alpha = 0.35f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .size(72.dp)
                .semantics { contentDescription = descripcion },
        ) {}
    }
}

@Composable
private fun SinPermisoDeCamara(rechazado: Boolean, onPedir: () -> Unit, onCerrar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.camara_permiso_titulo), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(if (rechazado) R.string.camara_permiso_rechazado else R.string.camara_permiso_texto),
            style = MaterialTheme.typography.bodyLarge,
        )
        BotonPrincipal(stringResource(R.string.camara_permitir), onPedir)
        BotonSecundario(stringResource(R.string.camara_usar_galeria), onCerrar)
    }
}
