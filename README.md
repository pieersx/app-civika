# Cívika

> Así como Waze convierte a los conductores en sensores del tráfico, Cívika convierte a los ciudadanos en sensores de su ciudad.

**Cívika** es una aplicación móvil Android, desarrollada en Kotlin, para reportar incidencias urbanas
(baches, residuos y barreras de accesibilidad) con foto y GPS. Asigna cada reporte a la entidad responsable,
consolida los reportes repetidos en un único incidente que otros ciudadanos pueden apoyar y permite seguir su atención hasta la solución.

Proyecto del **Grupo 10** del curso *Desarrollo de Sistemas Móviles* (UNMSM, FISI, semestre 2026-II).

## Stack

La app es **Kotlin Multiplatform**; el panel de la entidad es una aplicación web aparte.

| Parte | Tecnología |
|---|---|
| Código común (`shared`) | Dominio y reglas de negocio, repositorios, supabase-kt sobre Ktor, Koin, kotlinx.serialization |
| App móvil (`androidApp`) | Jetpack Compose (Material 3), MVVM (ViewModel + StateFlow), Room, WorkManager, CameraX, ubicación de Android, MapLibre con teselas de OpenFreeMap |
| Panel web (`webPanel`) | React + Vite + TypeScript, Tailwind CSS, TanStack Query, supabase-js, MapLibre GL JS |
| Backend | Supabase: PostgreSQL + PostGIS, Auth, Storage, Edge Functions |
| Notificaciones | Firebase Cloud Messaging |

## Estructura

```
app-civika/
├─ shared/      Kotlin común: dominio y datos
├─ androidApp/  App Android del ciudadano (Compose)
├─ webPanel/    Panel web de la entidad y la moderación (React)
├─ supabase/    Migraciones SQL, datos semilla, pruebas pgTAP y edge functions
└─ docs/        Arquitectura, diseño, plan, guía de herramientas y entregables
```

## Cómo ejecutar

**Requisitos:** JDK 21 y Android SDK 37 (se instala desde *SDK Manager* en Android Studio). No hace falta instalar
Gradle: el proyecto trae `gradlew`. En Android Studio, elige JDK 21 en *Settings → Build Tools → Gradle → Gradle JDK*
(sirve el JBR que trae el programa). Para el panel, Node 24 y pnpm 12 (`mise install` instala las dos herramientas con las versiones del proyecto). Docker y la Supabase CLI solo hacen falta cuando
trabajes en `supabase/` o quieras probar contra un backend local.

Copia `local.properties.example` a `local.properties` en la raíz y pon la ruta de tu SDK (este archivo no se sube;
Android Studio suele crearlo solo):

```properties
sdk.dir=C:/Users/TU_USUARIO/AppData/Local/Android/Sdk
supabase.url=
supabase.anonKey=
```

Con `supabase.url` y `supabase.anonKey` vacíos, la app arranca en **modo demostración** con datos de ejemplo. Para
conectarla, pon la URL y la clave **anon** (pública) de tu proyecto, o las de `supabase status` si usas Supabase
local (desde el emulador, la URL es `http://10.0.2.2:54321`). El panel usa las mismas dos claves en
`webPanel/.env.local` (plantilla: `webPanel/.env.example`). La lista completa de variables y secretos está en
[docs/despliegue.md](docs/despliegue.md). La clave `service_role` no va en ninguno de estos archivos.

| Qué | Comando |
|---|---|
| Pruebas rápidas del dominio | `./gradlew :shared:jvmTest` |
| Todas las pruebas de `shared` | `./gradlew :shared:allTests` |
| Lint y pruebas de la app | `./gradlew :androidApp:lint :androidApp:testDebugUnitTest` |
| Instalar la app en un emulador o celular | `./gradlew :androidApp:installDebug` |
| Panel web en el navegador | `cd webPanel`, `pnpm install` y `pnpm dev` (la terminal muestra la URL) |
| Pruebas del panel | `cd webPanel` y `pnpm typecheck && pnpm lint && pnpm test` |
| Backend local | `supabase start` y `supabase test db` |

**Ver la app en Android:**
- **Emulador:** Android Studio → *Más acciones* → *Virtual Device Manager* → **+** → un Pixel con la imagen más
  reciente de «Google APIs» → ▶. Con el emulador encendido, ejecuta `installDebug`.
- **Celular:** activa *Opciones de desarrollador* (toca 7 veces «Número de compilación») y la *Depuración por USB*,
  conéctalo y ejecuta `installDebug`.
- También puedes abrir la carpeta del proyecto en Android Studio (*Open*) y usar el botón ▶ con `androidApp`.

## Documentación

- [Entregables 1 y 2](docs/entregables-1-y-2.md): problema, ODS, antecedentes, requisitos y arquitectura.
- [Arquitectura](docs/arquitectura.md): contexto, modelo de datos, estados, permisos y decisiones técnicas (ADR).
- [Diagramas](docs/diagramas/README.md): sistema, CI/CD, arquitectura hexagonal, clases del dominio, flujo de un reporte y reglas de duplicados y prioridad, con íconos (draw.io).
- [Dirección visual](docs/diseno/direccion-visual.md): paleta, tipografía y tokens de diseño.
- [Plan de desarrollo](docs/plan-desarrollo.md): roles, sprints y flujo de trabajo del equipo.
- [Primeros pasos](docs/primeros-pasos.md): qué hace cada integrante en la semana 1 del Sprint 1.
- [Riesgos](docs/riesgos.md): qué puede salir mal al desarrollar, operar y desplegar, y cómo lo evitamos.
- [Despliegue](docs/despliegue.md): variables de entorno, checklist de producción (Supabase, Firebase, Vercel, APK) y procedimientos de cuentas.
- [Guía de herramientas](docs/guia-herramientas.md): qué skills, plugins y MCP de Claude Code usar en el proyecto.
- [Cómo contribuir](CONTRIBUTING.md): ramas, commits y pull requests.

## Equipo

Emily Cerna · Valeria Flores · Keyla Sihuacollo · Pieers Pachas (tech lead) · Alex Chavez
