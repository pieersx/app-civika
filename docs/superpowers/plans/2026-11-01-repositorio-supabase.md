# Repositorio real de incidentes y `apoyar_incidente`: plan de implementación

> **Para quien ejecute:** usa `superpowers:subagent-driven-development` (recomendado) o `superpowers:executing-plans`, tarea por tarea. Los pasos usan casillas (`- [ ]`).

**Objetivo:** que la lista de incidentes de la app y del panel salga de Supabase (`SupabaseIncidenteRepository`) y que exista la RPC `apoyar_incidente` que necesita la rama «es el mismo problema» del flujo de reporte.

**Arquitectura:** solo cambia la capa de datos de `shared`. Un mapeo puro (DTO → `Incidente`) se prueba sin red; el repositorio hace dos llamadas en paralelo con supabase-kt (`categorias` y la RPC `listar_incidentes_publicos`) y Koin elige el real o el `Fake` según haya `SupabaseConfig`. `apoyar_incidente` es una migración SQL con pruebas pgTAP, independiente del lado Kotlin.

**Tecnología:** Kotlin 2.4.21 Multiplatform, supabase-kt 3.8.0, Ktor 3.5.1, kotlinx-serialization-json 1.11.0, Koin 4.2.2, Supabase (PostgreSQL, PostgREST, pgTAP).

**Spec:** [2026-11-01-repositorio-supabase-design.md](../specs/2026-11-01-repositorio-supabase-design.md) y su diagrama [.drawio](../specs/2026-11-01-repositorio-supabase-diagrama.drawio). Léelos antes de empezar.

## Restricciones globales

- Versiones fijas en `gradle/libs.versions.toml`: supabase-kt BOM `3.8.0`, Ktor `3.5.1`, kotlinx-serialization-json `1.11.0`. Antes de añadir o subir una dependencia, consulta context7.
- Solo la clave `anon` en los clientes; la `service_role` jamás entra en `androidApp/` ni en `webPanel/`. Nunca leas ni modifiques `local.properties`, `.env` ni `google-services.json`.
- Las reglas de negocio van en `shared/src/commonMain/.../domain`, sin dependencias de Android ni del navegador. Los ViewModels solo usan `IncidenteRepository`, que no cambia.
- Tests primero. Dominio con `kotlin.test` en `commonTest`; base de datos con pgTAP. El esquema solo cambia con migraciones en `supabase/migrations/`.
- Toda función `security definer` lleva `set search_path = ''` y `revoke execute … from public, anon`.
- El CI ejecuta `:shared:allTests`, `:androidApp:lint :androidApp:testDebugUnitTest :androidApp:assembleDebug`, `:webPanel:wasmJsBrowserTest :webPanel:wasmJsBrowserDistribution` y `supabase test db`, **sin claves de Supabase**: nada de eso puede exigirlas.
- Commits `tipo: descripción` en español, máximo 60 caracteres, sin atribución. Una rama corta por tarea (`tipo/descripcion`), nunca en `main`. Antes de cada commit, `/code-review` sin `--fix`. El push, el PR y la fusión se hacen solo con permiso del tech lead.
- Accesibilidad y diseño no cambian: no se toca ninguna pantalla.

## Revisión de casos límite

La spec no cubre estos casos y cada uno tiene su prueba en la tarea indicada:

1. `creado_en` con 6 decimales y desfase `+00:00` (formato real de PostgREST): Tarea 4, `parseaElFormatoRealDeCreadoEn`.
2. Reloj del celular **anterior** a `creado_en` (días negativos): Tarea 4, `elRelojAtrasadoNuncaDaDiasNegativos`.
3. Doble toque en «Apoyar» o reintento tras perder la red (la misma llamada dos veces): Tarea 8, `apoyarDosVecesNoDuplica`.
4. La RPC devuelve `[]`: Tarea 4, `unaListaVaciaDaUnaListaVacia`.
5. Cambia un `peso_prioridad` entre dos refrescos, o Supabase está caído: Tarea 4, `unCambioDePesoCambiaLaPrioridad`; Tarea 7, paso de `supabase stop` (la pantalla debe mostrar `Error`, no cerrarse).

## Decisiones nuevas para confirmar (`apoyar_incidente`)

La spec no fija estas reglas; el plan usa estas por defecto. Cámbialas antes de la Tarea 8 si no te convencen:

- Solo apoya un **ciudadano activo** (igual que la política de subida de fotos); moderación y entidad no.
- Solo se apoya un incidente **abierto** (`reportado`, `en_revision`, `derivado`, `en_atencion`); uno `resuelto` o `rechazado` da error 22023.
- El **autor** del incidente no puede apoyarlo (ya cuenta como un voto; evita inflar la prioridad). Error 22023.
- Apoyar de nuevo el mismo incidente **no falla**: devuelve el estado actual y no crea nada (idempotente).
- Las fotos son **opcionales** (0 a 3). Con fotos se crea además un reporte de tipo `apoyo`, con la ubicación del incidente.
- Sin límite propio por hora para apoyar (YAGNI; la clave primaria ya limita a un apoyo por persona e incidente). Pero el tope de 10 reportes por hora de `crear_reporte` pasa a contar solo los **originales**, para que apoyar con fotos no impida reportar.

## Estructura de archivos

| Archivo | Responsabilidad |
|---|---|
| `supabase/demo.sql` (nuevo) | Incidentes de ejemplo para local y el proyecto demo |
| `shared/src/commonMain/.../domain/DiasAbiertos.kt` (nuevo) | Regla: días de calendario en Lima |
| `shared/src/commonMain/.../domain/Incidente.kt` (modifica) | Añade `latitud` y `longitud` |
| `shared/src/commonMain/.../data/SupabaseDtos.kt` (nuevo) | `IncidentePublicoDto`, `CategoriaDto` |
| `shared/src/commonMain/.../data/IncidenteMapper.kt` (nuevo) | DTO → `Incidente`, omite lo desconocido con aviso |
| `shared/src/commonMain/.../data/SupabaseConfig.kt` y `SupabaseIncidenteRepository.kt` (nuevos) | Configuración y repositorio real |
| `shared/src/commonMain/.../di/SharedModule.kt` (modifica) | Real si hay config, `Fake` si no |
| `gradle/supabase-config.gradle.kts` (nuevo) | Lee `supabase.url` y `supabase.anonKey` de `local.properties` o del entorno |
| `androidApp/build.gradle.kts`, `CivikaApplication.kt` (modifican) | `BuildConfig`, módulo Koin con la config, guarda de release |
| `webPanel/build.gradle.kts`, `Main.kt` (modifican) | Config generada, módulo Koin, guarda opcional |
| `supabase/migrations/<fecha>_apoyar_incidente.sql` y `supabase/tests/04_apoyar.sql` (nuevos) | RPC, ajuste del tope de `crear_reporte` y sus pruebas |

---

## Tarea 1: Dejar `main` listo (PR #33 de dependabot)

**Rama:** ninguna propia. **Entra antes de tocar `libs.versions.toml`**, para no generar conflictos.

- [ ] **Paso 1:** consulta context7 por las notas de AGP 9.5.0 y su compatibilidad con Kotlin 2.4.21 y Gradle 9.8.2.
- [ ] **Paso 2:** comprueba `gh pr checks 33` (hoy: «Pruebas y build» y «Base de datos (pgTAP)» en verde).
- [ ] **Paso 3:** si es compatible, el tech lead lo aprueba como revisor (`gh pr review 33 --approve`, con su permiso) y se fusiona con rebase; la excepción del tech lead **no** se usa, porque el PR no es suyo. Si no es compatible, se cierra con el motivo. Esperado: `main` local = remoto y PR #33 cerrado.

## Tarea 2: `supabase/demo.sql`

**Rama:** `feat/demo-incidentes`.

**Interfaces:**
- Produce: al menos 7 incidentes con códigos `DEMO-001`…, repartidos en las 3 categorías y los 6 estados, con un `resuelto` y un `rechazado`. `apoyos_count` **no se escribe a mano**: `private.recalcular_prioridad` lo reescribe contando filas de `public.apoyos`. Por eso el demo crea 5 o 6 usuarios demo en `auth.users` y filas reales de `apoyos` (conteos de 0 a 5); `creado_en` y categoría distintos hacen que la prioridad ordene. Se puede volver a ejecutar sin duplicar.

- [ ] **Paso 1:** escribe `supabase/demo.sql` con `insert … on conflict do nothing` sobre `public.incidentes` (el trigger calcula `prioridad`) y sobre `public.apoyos`. Ubicación: `extensions.ST_PointOnSurface` de la primera zona del seed, desplazada unos metros; entidad: la de `reglas_ruteo` para estados derivados. Añade 2 reportes con descripción para que `titulo` no sea siempre el nombre de la categoría. Los usuarios demo se insertan en `auth.users` como en `03_storage.sql`; las pruebas de la Tarea 8 usan sus propios datos, no el demo.
- [ ] **Paso 2:** carga y comprueba. Comando: `docker exec -i supabase_db_app-civika psql -U postgres -v ON_ERROR_STOP=1 < supabase/demo.sql`. Esperado: sin errores; al repetirlo, sin filas nuevas.
- [ ] **Paso 3:** pide la RPC con la clave `anon`: `POST /rest/v1/rpc/listar_incidentes_publicos` devuelve 200 con N−1 filas (**el rechazado no aparece**, ese es su único propósito) y ordenadas por `prioridad`. Guarda dos filas del JSON real: se pegan en la Tarea 4.
- [ ] **Paso 4:** documenta en `supabase/README.md` cómo cargar el demo (local y proyecto demo; nunca en producción).
- [ ] **Paso 5:** `/code-review` y `/security-review` (toca `supabase/` e inserta en `auth.users`); enséñale el mensaje al tech lead y espera su sí; commit `feat: incidentes de demo para el piloto` (39 caracteres). PR y fusión solo con su permiso.

## Tarea 3: Dependencias de supabase-kt y Ktor

**Rama:** `feat/repositorio-supabase` (las tareas 3 a 7 van en esta rama, un commit por tarea).

**Interfaces:**
- Produce: alias en el catálogo `supabase-bom`, `supabase-postgrest`, `ktor-client-okhttp`, `ktor-client-js`, `kotlinx-serialization-json` y el plugin `kotlinSerialization`; `shared` compila en Android, JVM y Wasm con ellos.

- [ ] **Paso 1:** consulta context7 para confirmar: el motor de Ktor de cada plataforma (OkHttp en `androidMain` y `jvmMain`, `ktor-client-js` en `wasmJsMain`), si el `Json` por defecto de supabase-kt ignora claves desconocidas, y las versiones. Anota el resultado en el mensaje del PR. Si algo contradice la spec, **para** y actualiza la spec primero.
- [ ] **Paso 2:** añade las versiones y alias a `gradle/libs.versions.toml` y las dependencias a `shared/build.gradle.kts` (`commonMain`: BOM, postgrest, serialization-json; los motores en su conjunto de fuentes; plugin `kotlinSerialization` con la versión de Kotlin).
- [ ] **Paso 3:** verifica que nada se rompe. Comando: `./gradlew :shared:allTests --console=plain`. Esperado: `BUILD SUCCESSFUL` (JVM y Wasm).
- [ ] **Paso 4:** `/code-review`; enséñale el mensaje al tech lead y espera su sí; commit `chore: dependencias de supabase-kt y Ktor` (41 caracteres).

## Tarea 4: Dominio y mapeo (tests primero)

**Archivos:** crea `domain/DiasAbiertos.kt`, `data/SupabaseDtos.kt`, `data/IncidenteMapper.kt`; modifica `domain/Incidente.kt`. Tests en `commonTest`: `domain/DiasAbiertosTest.kt` y `data/IncidenteMapperTest.kt`.

**Interfaces:**
- Produce `fun diasCalendarioLima(creadoEn: Instant, ahora: Instant): Int` (`kotlin.time.Instant`). Desplaza ambos −5 horas, toma el día entero de cada uno y resta; mínimo 0.
- Produce `@Serializable data class IncidentePublicoDto(codigo, titulo, @SerialName("categoria_id") categoriaId: Int, estado: String, @SerialName("apoyos_count") apoyosCount: Int, zona: String, lat: Double, lon: Double, @SerialName("creado_en") creadoEn: String)` y `CategoriaDto(id: Int, @SerialName("service_code") serviceCode, nombre, @SerialName("peso_prioridad") pesoPrioridad, @SerialName("radio_duplicado_m") radioDuplicadoM, @SerialName("ventana_dias") ventanaDias, @SerialName("plazo_objetivo_dias") plazoObjetivoDias, activa: Boolean)`.
- Produce `internal fun construirIncidentes(filas: List<IncidentePublicoDto>, categorias: List<CategoriaDto>, ahora: Instant, aviso: (String) -> Unit): List<Incidente>`. Omite con `aviso(...)` una fila de estado o categoría desconocidos y emite un aviso si `filas.size == 200` (tope de la RPC). Incluye categorías inactivas.
- `Incidente` gana `latitud: Double? = null` y `longitud: Double? = null` al final.

- [ ] **Paso 1: escribe los tests que fallan.** `DiasAbiertosTest`: `creadoAyerALas23DeLimaYListadoHoyALas0030DaUnDia`, `elMismoDiaDaCero`, `elRelojAtrasadoNuncaDaDiasNegativos`. `IncidenteMapperTest` (JSON copiado del paso 3 de la Tarea 2, decodificado con `Json { ignoreUnknownKeys = true }`): `parseaElFormatoRealDeCreadoEn`, `armaElIncidenteConSuCategoriaPorId`, `elEstadoEnSnakeCaseSeConvierte` (`en_revision` → `Estado.EN_REVISION`), `unEstadoDesconocidoOmiteLaFilaYAvisa`, `unaCategoriaDesconocidaOmiteElIncidenteYAvisa`, `unaCategoriaInactivaSeIncluye`, `exactamente200FilasEmiteElAvisoDeTope`, `unaListaVaciaDaUnaListaVacia`, `unCambioDePesoCambiaLaPrioridad`, `copiaLatYLonComoNullable`.
- [ ] **Paso 2:** ejecuta `./gradlew :shared:jvmTest --console=plain`. Esperado: FALLA al compilar (no existen las funciones).
- [ ] **Paso 3:** implementa las firmas de arriba. `construirIncidentes` convierte `estado` con `Estado.entries.firstOrNull { it.name.equals(texto, ignoreCase = true) }`.
- [ ] **Paso 4:** ejecuta `./gradlew :shared:allTests --console=plain`. Esperado: `BUILD SUCCESSFUL`; los tests viejos (`PrioridadTest`, `TransicionesEstadoTest`, `ObtenerIncidentesPriorizadosTest`) siguen en verde sin cambios.
- [ ] **Paso 5:** `/code-review`; enséñale el mensaje al tech lead y espera su sí; commit `feat: mapeo de incidentes de Supabase al dominio` (48 caracteres).

## Tarea 5: Repositorio real y Koin (tests primero)

**Archivos:** crea `data/SupabaseConfig.kt`, `data/SupabaseIncidenteRepository.kt`; modifica `di/SharedModule.kt` y `androidApp/src/test/.../KoinModulesTest.kt`. Test: `commonTest/.../di/SharedModuleTest.kt`.

**Interfaces:**
- Produce `data class SupabaseConfig(val url: String, val anonKey: String)`.
- Produce `class SupabaseIncidenteRepository(config: SupabaseConfig) : IncidenteRepository`. `listar()` lanza `categorias` (`from("categorias").select()`) y `rpc("listar_incidentes_publicos")` en paralelo con `coroutineScope { async … }`, y devuelve `construirIncidentes(…, ahora = kotlin.time.Clock.System.now(), aviso = ::println)`. Cualquier fallo de red se propaga como excepción (los ViewModels ya la atrapan).
- `sharedModule` ofrece `SupabaseIncidenteRepository` si `getOrNull<SupabaseConfig>()` no es nulo; si no, `FakeIncidenteRepository`.

- [ ] **Paso 1: escribe el test que falla.** `SharedModuleTest` con `koinApplication { modules(sharedModule) }` (sin `koin-test`): `sinConfiguracionResuelveElFake` y `conConfiguracionResuelveElRepositorioReal` (declara un módulo con `SupabaseConfig("http://127.0.0.1:54321", "clave")`; solo comprueba el tipo, sin red).
- [ ] **Paso 2:** ejecuta `./gradlew :shared:jvmTest --console=plain`. Esperado: FALLA (no existe el repositorio).
- [ ] **Paso 3:** implementa las firmas. En `KoinModulesTest` usa `verify(extraTypes = listOf(SupabaseConfig::class))` si `verify()` reclama el tipo.
- [ ] **Paso 4:** ejecuta `./gradlew :shared:allTests :androidApp:testDebugUnitTest --console=plain`. Esperado: `BUILD SUCCESSFUL`.
- [ ] **Paso 5:** `/code-review`; enséñale el mensaje al tech lead y espera su sí; commit `feat: repositorio de incidentes con supabase-kt` (47 caracteres).

## Tarea 6: Configuración en Android y en el panel

**Archivos:** crea `gradle/supabase-config.gradle.kts`; modifica `androidApp/build.gradle.kts`, `CivikaApplication.kt`, `webPanel/build.gradle.kts`, `webPanel/.../Main.kt`.

**Interfaces:**
- Produce el script de Gradle que expone `supabaseUrl` y `supabaseAnonKey` leyendo `supabase.url` y `supabase.anonKey` de `local.properties` y, si faltan, de las variables de entorno `SUPABASE_URL` y `SUPABASE_ANON_KEY` (el CI y el despliegue las usan), con `providers.environmentVariable(...)` para que Gradle las trate como entradas.
- Android: `buildFeatures { buildConfig = true }`, `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_ANON_KEY`; `CivikaApplication` añade un módulo Koin con `SupabaseConfig` solo si ambos tienen valor. Las tareas `assembleRelease` y `bundleRelease` dependen de una tarea que **falla** si faltan; `lint` y `assembleDebug` no.
- Panel: una tarea genera `PanelConfig.kt` en `build/generated` (agregado al conjunto de fuentes `wasmJsMain`) y **declara la URL y la clave como entradas** (`inputs.property`), para que un cambio solo de variables de entorno la vuelva a ejecutar y nunca quede UP-TO-DATE con claves viejas; `Main.kt` añade el módulo con `SupabaseConfig` si hay valores. `wasmJsBrowserDistribution` solo falla si se pasa `-Pcivika.exigirSupabase=true` (lo usará el workflow de despliegue, issue #14); el CI no lo pasa.

- [ ] **Paso 1:** implementa lo anterior. Documenta en `README.md` (sección «Cómo ejecutar») las dos claves de `local.properties` y que son claves **públicas** (`anon`), nunca la `service_role`.
- [ ] **Paso 2:** comprueba que el CI sigue sin claves. Comando: `./gradlew :androidApp:lint :androidApp:testDebugUnitTest :androidApp:assembleDebug :webPanel:wasmJsBrowserTest :webPanel:wasmJsBrowserDistribution --console=plain`. Esperado: `BUILD SUCCESSFUL`.
- [ ] **Paso 3:** comprueba la guarda. Comando: `./gradlew :androidApp:assembleRelease --console=plain`. Esperado: FALLA con un mensaje que nombra `supabase.url` y `supabase.anonKey`. Después `./gradlew :webPanel:wasmJsBrowserDistribution -Pcivika.exigirSupabase=true` también debe fallar.
- [ ] **Paso 4:** `/code-review`; enséñale el mensaje al tech lead y espera su sí; commit `feat: configuración de Supabase en la app y el panel` (52 caracteres).

## Tarea 7: Prueba real contra Supabase local

**Sin código nuevo.** Resuelve los `?` de la spec.

- [ ] **Paso 1:** con `supabase start` y el demo de la Tarea 2, arranca el panel pasando las claves por entorno (sin tocar `local.properties`): `SUPABASE_URL=http://127.0.0.1:54321 SUPABASE_ANON_KEY=<clave de supabase status -o env> ./gradlew :webPanel:wasmJsBrowserDevelopmentRun`.
- [ ] **Paso 2:** con Playwright abre el panel y comprueba que la lista muestra los incidentes del demo (sin el rechazado ni el resuelto), ordenados por prioridad, con captura de pantalla. Esto prueba CORS en un navegador real.
- [ ] **Paso 3:** `supabase stop`, recarga el panel y comprueba que muestra el estado `Error` sin cerrarse. Vuelve a levantar con `supabase start`.
- [ ] **Paso 4:** 🧑 el tech lead prueba la app en un emulador o celular (`./gradlew :androidApp:installDebug` con las claves en su `local.properties`) y confirma que ve la misma lista.
- [ ] **Paso 5:** actualiza la spec (sección «Comprobado» y «Preguntas abiertas») con lo observado, `/code-review` y PR de la rama `feat/repositorio-supabase`. El PR dice qué pasó en cada paso.

## Tarea 8: RPC `apoyar_incidente` y ajuste de `crear_reporte` (tests primero)

**Rama:** `feat/apoyar-incidente`. **Archivos:** `supabase/migrations/<timestamp>_apoyar_incidente.sql` (con `supabase migration new apoyar_incidente`), `supabase/tests/04_apoyar.sql`, `supabase/README.md`.

**Interfaces:**
- Produce `public.apoyar_incidente(p_cliente_id uuid, p_incidente_id uuid, p_fotos text[] default null) returns table (incidente_id uuid, apoyos integer, prioridad integer)`, `security definer`, `set search_path = ''`, `revoke execute … from public, anon`, `grant execute … to authenticated`.
- Comportamiento (igual de estricto que `crear_reporte`): exige sesión y cuenta activa con rol `ciudadano` (`private.rol_actual()`), `p_cliente_id` no nulo, bloqueo con `pg_advisory_xact_lock` por usuario y cliente, incidente abierto y no propio. **Autor** = quien tiene el reporte `tipo = 'original'` del incidente (`incidentes` no guarda autor). Inserta en `public.apoyos` con `on conflict do nothing` (el trigger `private.on_apoyo_cambiado` recalcula `apoyos_count` y `prioridad`). **Solo si ese insert fue nuevo** (`get diagnostics … row_count = 1`) y hay fotos, crea un `reportes` de tipo `apoyo` con la `geom` del incidente y sus `fotos`; si el apoyo ya existía, devuelve el estado actual sin crear nada, aunque llegue otro `cliente_id`. Hay fotos solo si `p_fotos is not null and cardinality(p_fotos) > 0` (`{}` equivale a `null`); entonces llama a `private.validar_fotos(p_fotos)`.
- La misma migración reemplaza `public.crear_reporte` (misma firma y mismos `revoke`/`grant`) cambiando solo el tope de 10 por hora: cuenta `reportes` con `tipo = 'original'`, para que apoyar con fotos no impida reportar.

- [ ] **Paso 1: escribe `04_apoyar.sql` (falla).** Usuarios y objetos de Storage como en `03_storage.sql`, con datos propios (no el demo). Pruebas, con el código de error esperado:
  1. `el ciudadano apoya un incidente abierto` (`apoyos` pasa de 0 a 1 y `prioridad` sube 1);
  2. `apoyarDosVecesNoDuplica` (misma llamada dos veces: sin error, sigue en 1);
  3. `con fotos crea un reporte de tipo apoyo y sus fotos`;
  4. `repetir el mismo cliente_id con fotos no duplica el reporte`;
  5. `repetir con otro cliente_id y fotos nuevas no crea un segundo reporte apoyo` (sin error);
  6. `fotos vacías ({}) apoyan sin error y sin reporte`;
  7. `una foto de otra carpeta falla` (`42501`) y `más de 3 fotos falla` (`22023`);
  8. `un incidente resuelto no admite apoyos`, `uno rechazado` y `uno inexistente` (`22023`);
  9. `el autor no puede apoyar su propio incidente` (`22023`), pero quien ya apoyó con foto sigue sin ser autor;
  10. `un anónimo no puede ejecutarla` (`42501`), `moderación y entidad no pueden apoyar` (`42501`) y `una cuenta desactivada no puede apoyar` (`42501`);
  11. `diez reportes de tipo apoyo no bloquean a crear_reporte`, y `diez originales sí lo bloquean` (como hoy).
- [ ] **Paso 2:** ejecuta `supabase test db`. Esperado: `04_apoyar.sql` FALLA (la función no existe); los otros 4 archivos siguen en PASS.
- [ ] **Paso 3:** escribe la migración con la firma y el comportamiento de arriba.
- [ ] **Paso 4:** ejecuta `supabase db reset && supabase test db`. Esperado: `Result: PASS`, 5 archivos. El reset borra el demo: vuelve a cargarlo con el comando de la Tarea 2.
- [ ] **Paso 5:** actualiza `supabase/README.md` (mueve `apoyar_incidente` a «Ya existen», añade la migración 7 y la línea de pruebas de apoyos) y `docs/arquitectura.md` si cambió algo del contrato. Ejecuta `/code-review` y `/security-review` (toca `supabase/`).
- [ ] **Paso 6:** enséñale el mensaje al tech lead y espera su sí; commit `feat: RPC apoyar_incidente con sus pruebas` (42 caracteres). PR y fusión solo con su permiso.

---

## Orden y dependencias

`1 → 2 → 3 → 4 → 5 → 6 → 7` (la 4 usa el JSON real de la 2). La Tarea 8 no depende de las otras: va **después de la 7**, como se acordó, y no bloquea el flujo de Valeria si la 7 se retrasa. Cada tarea termina con tests en verde y un commit; la 7 termina con un PR de la rama `feat/repositorio-supabase`.
