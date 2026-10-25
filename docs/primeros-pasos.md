# Primeros pasos: semana 1 del Sprint 1

Guía corta para saber **qué hace cada quien** del 26 de octubre al 1 de noviembre de 2026. No reemplaza el
[plan de desarrollo](plan-desarrollo.md): lo baja a pasos concretos. Cada persona tiene un issue en GitHub con su
lista de tareas. Si el issue y esta guía no coinciden, avisa al tech lead antes de seguir.

> **Issue** = una tarea en GitHub con casillas por marcar. **PR** (*pull request*) = pedir que tu rama se una a `main`.

## Para todos (haz esto primero)

1. Acepta la invitación al repositorio en GitHub (te llega por correo).
2. Clona el proyecto y ábrelo en Android Studio siguiendo el [README](../README.md#cómo-ejecutar): JDK 21, Android SDK 37
   y un `local.properties` con la ruta de tu SDK.
3. Comprueba que todo funciona: `./gradlew :shared:jvmTest` (las pruebas del dominio deben pasar).
4. Lee [CONTRIBUTING.md](../CONTRIBUTING.md) y crea tu rama: `git switch -c tipo/descripcion` (nunca trabajes en `main`).
5. Abre tu issue (tabla de abajo) y trabaja sus casillas de una en una.
6. Antes de añadir una librería, revisa su documentación vigente con context7 y fija la versión en
   `gradle/libs.versions.toml`.
7. Nunca subas `local.properties`, `google-services.json` ni claves de Supabase. El repositorio es público.

Una tarea está **terminada** cuando cumple la [definición de «terminado»](plan-desarrollo.md#definición-de-terminado):
PR revisado por otra persona, pruebas y CI en verde, y probada en emulador o navegador.

## Qué hace cada quien

| Quién | Issue | Esta semana (en orden) |
|---|---|---|
| **Pieers** (tech lead) | [#1](https://github.com/pieersx/app-civika/issues/1) (ya cerrado) | 1. Fusionar el PR de README (#27) y el de esta guía. 2. Compartir el mockup desde «Compartir» para que el grupo lo abra, y acordar con Emily cómo corregir los bloqueos. 3. Revisar los PR del equipo. 4. Crear un issue para la política de subida de fotos en `storage.objects` (con la prueba pgTAP primero), que Valeria necesita en el Sprint 2. |
| **Emily** | [#2](https://github.com/pieersx/app-civika/issues/2) | 1. Corregir los 8 bloqueos de la revisión del mockup del 22 de octubre (están en un comentario del issue) y actualizar el artifact. 2. Revisión de las pantallas cambiadas y aprobación del grupo antes del 1 de noviembre. 3. Solo después: completar el tema en `androidApp/.../ui/theme` (la dirección visual y los tokens ya están en [direccion-visual.md](diseno/direccion-visual.md)) y las pantallas Splash, Login y Registro. |
| **Valeria** | [#3](https://github.com/pieersx/app-civika/issues/3) | 1. Prototipo de CameraX con permisos (hasta 3 fotos; el permiso se pide al usarla). 2. Ubicación con Fused Location Provider. 3. Entidades de Room para los reportes pendientes, con prueba en memoria. |
| **Keyla** | [#4](https://github.com/pieersx/app-civika/issues/4) | 1. Mapa con MapLibre y el estilo de OpenFreeMap, con su atribución visible ([ADR-3](arquitectura.md#adr-3-mapa-con-maplibre-y-teselas-de-openfreemap); el issue dice «OpenStreetMap», pero la decisión vigente es OpenFreeMap). 2. Marcadores por categoría y estado con los datos de `FakeIncidenteRepository`. |
| **Alex** | [#5](https://github.com/pieersx/app-civika/issues/5) | 1. Login con supabase-kt (solo clave `anon`) y proyecto de Firebase con el paquete `pe.civika.app`, con una notificación push de prueba: no dependen del diseño. 2. Las pantallas base de `webPanel` (con `PanelTheme` y datos de ejemplo) **solo después** de que el grupo apruebe el mockup. 3. Prueba de humo con Playwright. |

## Cómo pedir ayuda

- Si algo falla dos veces igual, no insistas: pega el **error completo** en el grupo o en tu issue.
- Si una decisión cambia (por ejemplo, un color o una regla de negocio), se actualiza primero el documento que manda:
  [arquitectura.md](arquitectura.md) o [direccion-visual.md](diseno/direccion-visual.md).
- Qué herramientas de Claude Code usar en cada tarea: [guía de herramientas](guia-herramientas.md).

## Al cerrar la semana 1

Se revisa en la sincronización: mockup aprobado, cada issue con sus casillas marcadas y los PR abiertos con el CI en verde.
