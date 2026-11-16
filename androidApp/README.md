# androidApp/

App Android del ciudadano, en Kotlin con Jetpack Compose y Material 3.

- Usa el dominio y los repositorios de `shared/`. Aquí van las pantallas, los ViewModels y lo que depende del
  dispositivo: Room, CameraX, MapLibre, WorkManager, FCM y ubicación.
- DI con Koin. Versión mínima: Android 8.0, API 26 (RNF-06). Solo español.
- Estructura y reglas: [docs/arquitectura.md](../docs/arquitectura.md#8-estructura-de-módulos-kotlin-multiplatform).

## Configuración

| Qué | Dónde | Sin él |
|---|---|---|
| URL y clave **anon** de Supabase | `local.properties` (`supabase.url`, `supabase.anonKey`) o variables `SUPABASE_URL` y `SUPABASE_ANON_KEY` (la variable gana) | La app arranca en **modo demostración** con datos de ejemplo y un aviso discreto |
| Firebase (push) | `androidApp/google-services.json` | El plugin de Google Services no se aplica; la bandeja de avisos funciona igual |
| Firma del release | `androidApp/keystore.properties` o `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | El release se genera **sin firmar** |

Plantillas: `local.properties.example` (raíz) y `keystore.properties.example`. Ningún archivo con secretos se sube al repo y
la clave `service_role` nunca va en la app.

- `assembleRelease` y `bundleRelease` **fallan** con un mensaje claro si falta la URL o la clave, o si la URL no es `https`.
- `-Pcivika.demo=true` fuerza el modo demostración aunque haya claves.
- `-Pcivika.r8Debug=true` pasa el debug por R8 con las reglas del release, para probar en el emulador que la
  serialización y Ktor sobreviven a la minificación.

## Probar contra Supabase local

```powershell
supabase start
$env:SUPABASE_URL = "http://10.0.2.2:54321"          # el emulador ve la máquina como 10.0.2.2
$env:SUPABASE_ANON_KEY = "<ANON_KEY de supabase status -o env>"
.\gradlew.bat :androidApp:installDebug
```

Solo el debug permite HTTP, y solo hacia `10.0.2.2`, `localhost` y `127.0.0.1` (`src/debug/res/xml/network_security_config.xml`).
Si `10.0.2.2` no responde (pasó con Docker Desktop en Windows), usa `adb reverse tcp:54321 tcp:54321` y la URL
`http://127.0.0.1:54321`.

## Estructura

```
src/main/kotlin/pe/civika/app/
├─ ui/theme, ui/components   tokens de la dirección visual, SenalEstado, botones, estados Cargando/Vacío/Error
├─ navigation/               rutas con tipo y barra inferior con el botón central «Reportar»
├─ feature/                  auth, home, mapa, reporte, incidente (detalle y Mis reportes), notificaciones, perfil
├─ local/                    Room: cola de reportes pendientes (ADR-6)
├─ sync/                     ReporteSyncWorker (WorkManager, con red y backoff)
├─ device/                   fotos, ubicación, FCM, DataStore
└─ di/                       módulo de Koin de Android
```

El esquema de Room se exporta a `schemas/` para poder escribir y probar migraciones.

## Pruebas

```powershell
.\gradlew.bat :shared:allTests :androidApp:lint :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

Las pruebas de ViewModel usan los repositorios `Fake…` de `shared` y Turbine. Las capturas del recorrido en emulador
están en `docs/capturas/`.
