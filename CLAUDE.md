# Cívika — instrucciones para Claude Code

App de reporte de incidencias urbanas (UNMSM, Desarrollo de Sistemas Móviles 2026-II). Responde en español.

## Stack
- **Kotlin Multiplatform**: `shared/` (dominio y datos), `androidApp/` (Compose) y `webPanel/` (Compose Web, Wasm).
- **Backend**: Supabase (PostgreSQL + PostGIS, Auth, Storage, Edge Functions) en `supabase/`.
- DI con **Koin** (no Hilt). Red con **supabase-kt** sobre Ktor (no Retrofit).
- Solo en Android: Room, CameraX, MapLibre Native (teselas de OpenFreeMap), WorkManager, FCM y ML Kit.

## Documentos que mandan
- `docs/arquitectura.md`: modelo de datos, estados, permisos y ADR. Si una decisión cambia, se actualiza aquí primero.
- `docs/diseno/direccion-visual.md`: tokens de diseño. No inventes colores ni tamaños.
- `docs/plan-desarrollo.md`: roles y sprints.
- `docs/guia-herramientas.md`: qué skill usar en cada tarea y cómo se actualizan las skills del proyecto.
- `docs/entregables-1-y-2.md` es histórico: **no** lo uses como fuente de decisiones vigentes.

## Reglas
- Antes de añadir o actualizar una dependencia, **consulta context7** y fija la versión en `gradle/libs.versions.toml`.
- Las reglas de negocio van en `shared/src/commonMain/.../domain`, sin dependencias de Android ni del navegador.
- Los ViewModels solo usan interfaces de repositorio; cada repositorio tiene su implementación `Fake…`.
- Tests primero. Dominio con `kotlin.test` en `commonTest`; base de datos con pgTAP.
- El esquema solo cambia con migraciones en `supabase/migrations/`. Toda función `security definer` lleva
  `set search_path = ''` y `revoke execute … from public, anon`.
- Nunca leas ni modifiques `.env`, `local.properties` ni `google-services.json`. La clave `service_role` jamás va en
  `androidApp/` ni en `webPanel/`.
- Accesibilidad: áreas táctiles de 48 dp o más, contraste AA y el estado nunca se comunica solo con color.

## Git
- Ramas `tipo/descripcion`. Nunca trabajes en `main`. Antes del PR, `git rebase origin/main`. Cada PR necesita 1
  revisión aprobada de otra persona y el CI en verde (si toca código; los PR solo de docs no lo ejecutan). El autor
  nunca aprueba su propio PR: el tech lead revisa los PRs de los demás y su propio PR lo revisa otro colaborador.
  Si no hay otro revisor disponible, el tech lead puede fusionar su PR con CI verde y `--ff-only`, dejando constancia
  en el PR; eso no cuenta como aprobación. La fusión es con `--ff-only` (ver `CONTRIBUTING.md`).
- Commits `tipo: descripción` en español, de 60 caracteres como máximo, sin atribución.
- Antes de cada commit, revisión con el agente `revisor-codigo`. En los PR que tocan `supabase/`, también `/security-review`.
- No asignes reviewers ni assignees sin confirmar el roster del equipo.

## Comandos
- Tests rápidos del dominio: `./gradlew :shared:jvmTest`. Todos (JVM y Wasm): `./gradlew :shared:allTests`
- Versiones: `gradle/libs.versions.toml` (Kotlin, Compose Multiplatform, AGP 9 con el plugin Android-KMP, Koin)
- App: `./gradlew :androidApp:lint :androidApp:testDebugUnitTest`
- Panel: `./gradlew :webPanel:wasmJsBrowserDevelopmentRun`
- Base de datos: `supabase start` y `supabase test db`
