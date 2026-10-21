# webPanel/

Panel web de la entidad y la moderación (RF-13, RF-14 y RF-16), hecho con **Compose Multiplatform para web**
(Kotlin/Wasm, en Beta). Se crea en el **Sprint 1**.

- Reutiliza el dominio, los repositorios y supabase-kt de `shared/`. Aquí solo van las pantallas y los ViewModels.
- Pantallas: lista de incidentes con cambio de estado y comentarios, indicadores por zona y cola de moderación
  (ver el mockup en `docs/diseno/direccion-visual.md`).
- Para desarrollar: `./gradlew :webPanel:wasmJsBrowserDevelopmentRun`.
- Solo usa la clave `anon`. **Nunca** la `service_role`.
