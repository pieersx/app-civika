# android/

Aquí irá la app Android de Cívika (Kotlin + Jetpack Compose). Se crea en el **Sprint 1** con Android Studio.

- Arquitectura: MVVM por capas (`ui` → `ViewModel` → `repository` → Room / Retrofit).
- Versión mínima: Android 8.0 (API 26) (RNF-06).
- Los secretos (URL y clave anon de Supabase) van en `local.properties`, que **nunca** se sube al repo.
