# shared/

Código Kotlin común a la app Android y al panel web (Kotlin Multiplatform).

- `commonMain/domain`: reglas de negocio puras, sin Android ni navegador: estados y transiciones permitidas, fórmula
  de prioridad, plazo vencido, validaciones y traducción de etiquetas de ML Kit a categorías.
- `commonMain/data`: interfaces de repositorio, implementación con supabase-kt e implementaciones `Fake…`.
- `commonTest`: pruebas con `kotlin.test`, que corren en la JVM.
- `androidMain`: solo lo específico de Android, como el motor de Ktor. El panel web ya no usa `shared` (ADR-17).

Si una regla se puede escribir aquí, **se escribe aquí**: así la app y el panel nunca se contradicen.
