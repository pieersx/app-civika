# Cívika

> Así como Waze convierte a los conductores en sensores del tráfico, Cívika convierte a los ciudadanos en sensores de su ciudad.

**Cívika** es una aplicación móvil Android, desarrollada en Kotlin, para reportar incidencias urbanas
(baches, residuos y barreras de accesibilidad) con foto y GPS. Asigna cada reporte a la entidad responsable,
consolida los reportes repetidos en un único incidente que otros ciudadanos pueden apoyar y permite seguir su atención hasta la solución.

Proyecto del **Grupo 10** del curso *Desarrollo de Sistemas Móviles* (UNMSM, FISI, semestre 2026-II).

## Stack

Todo el cliente está en **Kotlin Multiplatform**.

| Parte | Tecnología |
|---|---|
| Código común (`shared`) | Dominio y reglas de negocio, repositorios, supabase-kt sobre Ktor, Koin, kotlinx.serialization |
| App móvil (`androidApp`) | Jetpack Compose (Material 3), MVVM (ViewModel + StateFlow), Room, WorkManager, CameraX, ubicación de Android, MapLibre con teselas de OpenFreeMap, ML Kit |
| Panel web (`webPanel`) | Compose Multiplatform para web (Kotlin/Wasm) |
| Backend | Supabase: PostgreSQL + PostGIS, Auth, Storage, Edge Functions |
| Notificaciones | Firebase Cloud Messaging |

## Estructura

```
app-civika/
├─ shared/      Kotlin común: dominio y datos
├─ androidApp/  App Android del ciudadano (Compose)
├─ webPanel/    Panel web de la entidad (Compose Web, Wasm)
├─ supabase/    Migraciones SQL, datos semilla, pruebas pgTAP y edge functions
└─ docs/        Arquitectura, diseño, plan, guía de herramientas y entregables
```

## Cómo ejecutar

**Requisitos:** JDK 21 y Android SDK (viene con Android Studio). No hace falta instalar Gradle: el proyecto trae
`gradlew`. Crea `local.properties` en la raíz con la ruta de tu SDK (este archivo no se sube):

```properties
sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk
```

| Qué | Comando |
|---|---|
| Pruebas rápidas del dominio | `./gradlew :shared:jvmTest` |
| Todas las pruebas de `shared` (JVM y Wasm) | `./gradlew :shared:allTests` |
| Lint y pruebas de la app | `./gradlew :androidApp:lint :androidApp:testDebugUnitTest` |
| Instalar la app en un emulador o celular | `./gradlew :androidApp:installDebug` |
| Panel web en el navegador | `./gradlew :webPanel:wasmJsBrowserDevelopmentRun` (la terminal muestra la URL, normalmente `http://localhost:8080`; tarda unos segundos en pintar). No recompila solo: tras un cambio, detén con Ctrl+C y vuelve a ejecutar. |

**Ver la app en Android:**
- **Emulador:** Android Studio → *Más acciones* → *Virtual Device Manager* → **+** → un Pixel con la imagen más
  reciente de «Google APIs» → ▶. Con el emulador encendido, ejecuta `installDebug`.
- **Celular:** activa *Opciones de desarrollador* (toca 7 veces «Número de compilación») y la *Depuración por USB*,
  conéctalo y ejecuta `installDebug`.
- También puedes abrir la carpeta del proyecto en Android Studio (*Open*) y usar el botón ▶ con `androidApp`.

## Documentación

- [Entregables 1 y 2](docs/entregables-1-y-2.md): problema, ODS, antecedentes, requisitos y arquitectura.
- [Arquitectura](docs/arquitectura.md): contexto, modelo de datos, estados, permisos y decisiones técnicas (ADR).
- [Diagramas](docs/diagramas/README.md): sistema, CI/CD, arquitectura hexagonal, clases del dominio y flujo de un reporte, con íconos (draw.io).
- [Dirección visual](docs/diseno/direccion-visual.md): paleta, tipografía y tokens de diseño.
- [Plan de desarrollo](docs/plan-desarrollo.md): roles, sprints y flujo de trabajo del equipo.
- [Primeros pasos](docs/primeros-pasos.md): qué hace cada integrante en la semana 1 del Sprint 1.
- [Riesgos](docs/riesgos.md): qué puede salir mal al desarrollar, operar y desplegar, y cómo lo evitamos.
- [Despliegue](docs/despliegue.md): checklist del entorno demo (Supabase, Firebase, Vercel, APK) y procedimientos de cuentas.
- [Guía de herramientas](docs/guia-herramientas.md): qué skills, plugins y MCP de Claude Code usar en el proyecto.
- [Cómo contribuir](CONTRIBUTING.md): ramas, commits y pull requests.

## Equipo

Emily Cerna · Valeria Flores · Keyla Sihuacollo · Pieers Pachas (tech lead) · Alex Chavez
