package pe.civika.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pe.civika.app.R
import pe.civika.app.feature.auth.BienvenidaScreen
import pe.civika.app.feature.auth.LoginScreen
import pe.civika.app.feature.auth.PoliticaScreen
import pe.civika.app.feature.auth.RecuperarScreen
import pe.civika.app.feature.auth.RegistroScreen
import pe.civika.app.feature.home.HomeScreen
import pe.civika.app.feature.incidente.DetalleScreen
import pe.civika.app.feature.incidente.MisReportesScreen
import pe.civika.app.feature.mapa.MapaScreen
import pe.civika.app.feature.notificaciones.NotificacionesScreen
import pe.civika.app.feature.perfil.PerfilScreen
import pe.civika.app.feature.reporte.ReporteScreen
import pe.civika.shared.domain.EstadoSesion
import kotlin.reflect.KClass

private class Pestana(
    val ruta: Any,
    val clase: KClass<*>,
    @StringRes val etiqueta: Int,
    val activo: ImageVector,
    val inactivo: ImageVector,
)

private val Inicio = Pestana(RutaInicio, RutaInicio::class, R.string.nav_inicio, Icons.Rounded.Home, Icons.Outlined.Home)
private val Mapa = Pestana(RutaMapa, RutaMapa::class, R.string.nav_mapa, Icons.Rounded.Map, Icons.Outlined.Map)
private val MisReportes = Pestana(RutaMisReportes, RutaMisReportes::class, R.string.nav_mis_reportes, Icons.Rounded.Description, Icons.Outlined.Description)
private val Perfil = Pestana(RutaPerfil, RutaPerfil::class, R.string.nav_perfil, Icons.Rounded.Person, Icons.Outlined.Person)
private val PESTANAS = listOf(Inicio, Mapa, MisReportes, Perfil)

/**
 * Navegación de la app. Se puede explorar sin cuenta: reportar, apoyar y «Mis reportes» llevan a iniciar sesión
 * y, al terminar, vuelven a donde estaba el ciudadano.
 */
@Composable
fun CivikaNavegacion(
    sesion: EstadoSesion,
    incidentePendiente: String?,
    onIncidenteAbierto: () -> Unit,
) {
    val nav = rememberNavController()
    val inicial: Any = remember { if (sesion is EstadoSesion.Activa) RutaInicio else RutaBienvenida }
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val enPestana = PESTANAS.any { destino?.hasRoute(it.clase) == true }
    val conSesion = sesion is EstadoSesion.Activa

    // La bienvenida solo existe al principio: al iniciar sesión o explorar se quita de la pila.
    var bienvenidaEnPila by remember { mutableStateOf(inicial == RutaBienvenida) }

    fun pedirSesion() = nav.navigate(RutaLogin(requerido = true))

    fun reportar() = if (conSesion) nav.navigate(RutaReportar) else pedirSesion()

    fun irAPestana(ruta: Any) {
        nav.navigate(ruta) {
            popUpTo<RutaInicio> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun terminarAutenticacion() {
        if (bienvenidaEnPila) {
            bienvenidaEnPila = false
            nav.navigate(RutaInicio) { popUpTo<RutaBienvenida> { inclusive = true } }
        } else {
            // Se llegó desde una acción que pedía cuenta: se vuelve a ella quitando las pantallas de cuenta.
            while (nav.currentDestination.esDeCuenta() && nav.popBackStack()) Unit
        }
    }

    // Si la sesión se pierde (se eliminó la cuenta, venció), se vuelve a la bienvenida.
    var anterior by remember { mutableStateOf(sesion) }
    LaunchedEffect(sesion) {
        if (anterior is EstadoSesion.Activa && sesion is EstadoSesion.SinSesion) {
            bienvenidaEnPila = true
            nav.navigate(RutaBienvenida) { popUpTo(nav.graph.id) { inclusive = true } }
        }
        anterior = sesion
    }
    // Un aviso push abre el detalle del incidente.
    LaunchedEffect(incidentePendiente) {
        if (incidentePendiente != null) {
            nav.navigate(RutaDetalle(incidentePendiente))
            onIncidenteAbierto()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (enPestana) {
                BarraInferior(
                    destino = destino,
                    onPestana = ::irAPestana,
                    onReportar = ::reportar,
                    conSesion = conSesion,
                    onSinSesion = ::pedirSesion,
                )
            }
        },
    ) { relleno ->
        NavHost(
            navController = nav,
            startDestination = inicial,
            modifier = Modifier.padding(relleno),
        ) {
            composable<RutaBienvenida> {
                BienvenidaScreen(
                    onIniciarSesion = { nav.navigate(RutaLogin()) },
                    onRegistrarse = { nav.navigate(RutaRegistro) },
                    onExplorar = {
                        bienvenidaEnPila = false
                        nav.navigate(RutaInicio) { popUpTo<RutaBienvenida> { inclusive = true } }
                    },
                )
            }
            composable<RutaLogin> { e ->
                LoginScreen(
                    onExito = ::terminarAutenticacion,
                    onRegistrarse = { nav.navigate(RutaRegistro) },
                    onRecuperar = { nav.navigate(RutaRecuperar) },
                    onVolver = { nav.popBackStack() },
                    motivoSesion = e.toRoute<RutaLogin>().requerido,
                )
            }
            composable<RutaRegistro> {
                RegistroScreen(
                    onExito = ::terminarAutenticacion,
                    onIniciarSesion = { nav.navigate(RutaLogin()) { popUpTo<RutaRegistro> { inclusive = true } } },
                    onLeerPolitica = { nav.navigate(RutaPolitica) },
                    onVolver = { nav.popBackStack() },
                )
            }
            composable<RutaRecuperar> { RecuperarScreen(onVolver = { nav.popBackStack() }) }
            composable<RutaPolitica> { PoliticaScreen(onVolver = { nav.popBackStack() }) }

            composable<RutaInicio> {
                HomeScreen(
                    onAbrirIncidente = { nav.navigate(RutaDetalle(it)) },
                    onNotificaciones = { nav.navigate(RutaNotificaciones) },
                    onIniciarSesion = { nav.navigate(RutaLogin()) },
                    onReportar = ::reportar,
                )
            }
            composable<RutaMapa> { MapaScreen(onAbrirIncidente = { nav.navigate(RutaDetalle(it)) }) }
            composable<RutaMisReportes> {
                MisReportesScreen(
                    onAbrirIncidente = { nav.navigate(RutaDetalle(it)) },
                    onReportar = ::reportar,
                    onIniciarSesion = { nav.navigate(RutaLogin()) },
                )
            }
            composable<RutaPerfil> {
                PerfilScreen(
                    onIniciarSesion = { nav.navigate(RutaLogin()) },
                    onCrearCuenta = { nav.navigate(RutaRegistro) },
                    onPolitica = { nav.navigate(RutaPolitica) },
                )
            }

            composable<RutaReportar> {
                ReporteScreen(
                    onCerrar = { nav.popBackStack() },
                    onVerIncidente = { id -> nav.navigate(RutaDetalle(id)) { popUpTo<RutaReportar> { inclusive = true } } },
                    onVerMisReportes = {
                        nav.navigate(RutaMisReportes) {
                            popUpTo<RutaReportar> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<RutaDetalle> { e ->
                DetalleScreen(
                    incidenteId = e.toRoute<RutaDetalle>().id,
                    onVolver = { nav.popBackStack() },
                    onIniciarSesion = { nav.navigate(RutaLogin(requerido = true)) },
                )
            }
            composable<RutaNotificaciones> {
                NotificacionesScreen(
                    onVolver = { nav.popBackStack() },
                    onAbrirIncidente = { nav.navigate(RutaDetalle(it)) },
                    onIniciarSesion = { nav.navigate(RutaLogin(requerido = true)) },
                )
            }
        }
    }
}

private fun androidx.navigation.NavDestination?.esDeCuenta(): Boolean =
    this != null && (hasRoute<RutaLogin>() || hasRoute<RutaRegistro>() || hasRoute<RutaRecuperar>() || hasRoute<RutaPolitica>())

/** Inicio, Mapa, [Reportar], Mis reportes y Perfil. El botón central es el FAB de la acción principal. */
@Composable
private fun BarraInferior(
    destino: androidx.navigation.NavDestination?,
    onPestana: (Any) -> Unit,
    onReportar: () -> Unit,
    conSesion: Boolean,
    onSinSesion: () -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Elemento(Inicio, destino, onPestana)
        Elemento(Mapa, destino, onPestana)
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                FloatingActionButton(
                    onClick = onReportar,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_reportar_problema))
                }
                Text(stringResource(R.string.nav_reportar), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        // «Mis reportes» sin cuenta lleva a una pantalla que invita a iniciar sesión, no directo al formulario.
        Elemento(MisReportes, destino, onPestana)
        Elemento(Perfil, destino, onPestana)
    }
}

@Composable
private fun RowScope.Elemento(pestana: Pestana, destino: androidx.navigation.NavDestination?, onPestana: (Any) -> Unit) {
    val seleccionada = destino?.hasRoute(pestana.clase) == true
    NavigationBarItem(
        selected = seleccionada,
        onClick = { onPestana(pestana.ruta) },
        icon = { Icon(if (seleccionada) pestana.activo else pestana.inactivo, contentDescription = null) },
        label = { Text(stringResource(pestana.etiqueta), style = MaterialTheme.typography.labelMedium) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.secondary,
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
