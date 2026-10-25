# androidApp/

App Android del ciudadano, en Kotlin con Jetpack Compose.

- Usa el dominio y los repositorios de `shared/`. Aquí solo van las pantallas, los ViewModels y lo que depende del
  dispositivo: Room, CameraX, MapLibre, WorkManager, FCM y ML Kit.
- DI con Koin. Versión mínima: Android 8.0, API 26 (RNF-06).
- La URL y la clave `anon` de Supabase van en `local.properties`, y `google-services.json` en esta carpeta.
  **Ninguno se sube al repo.**
- Estructura y reglas: [docs/arquitectura.md](../docs/arquitectura.md#8-estructura-de-módulos-kotlin-multiplatform).
