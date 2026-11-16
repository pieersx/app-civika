package pe.civika.app.navigation

import kotlinx.serialization.Serializable

// Destinos con tipo (Navigation Compose 2.8+): cada ruta es un objeto serializable y sus argumentos son propiedades.

@Serializable data object RutaBienvenida

/** [requerido] es `true` cuando se llega desde una acción que pide cuenta (reportar, apoyar). */
@Serializable data class RutaLogin(val requerido: Boolean = false)

@Serializable data object RutaRegistro

@Serializable data object RutaRecuperar

@Serializable data object RutaPolitica

@Serializable data object RutaInicio

@Serializable data object RutaMapa

@Serializable data object RutaMisReportes

@Serializable data object RutaPerfil

@Serializable data object RutaReportar

@Serializable data object RutaNotificaciones

@Serializable data class RutaDetalle(val id: String)
