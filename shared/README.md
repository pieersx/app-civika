# shared/

Código Kotlin común a la app Android y al panel web (Kotlin Multiplatform). Se crea en el **Sprint 1**.

- `commonMain/domain`: reglas de negocio puras, sin Android ni navegador: estados y transiciones permitidas, fórmula
  de prioridad, plazo vencido, validaciones y traducción de etiquetas de ML Kit a categorías.
- `commonMain/data`: interfaces de repositorio, implementación con supabase-kt e implementaciones `Fake…`.
- `commonTest`: pruebas con `kotlin.test`, que corren en JVM y en Wasm.
- `androidMain` / `wasmJsMain`: solo lo que cambia por plataforma, como el motor de Ktor.

Si una regla se puede escribir aquí, **se escribe aquí**: así la app y el panel nunca se contradicen.
