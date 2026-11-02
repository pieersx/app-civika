# Repositorio real de incidentes con supabase-kt: diseño

Estado: **spec aprobada por el tech lead** el 1 de noviembre de 2026: las decisiones 1 a 3, en la lluvia de ideas; de la 4 en adelante, al aceptar los hallazgos de `/code-review` (dos rondas). Tarea: Sprint 2 de Pieers, «repositorio real de incidentes».

Los diagramas están en [2026-11-01-repositorio-supabase-diagrama.drawio](2026-11-01-repositorio-supabase-diagrama.drawio)
(se abre y se edita con la extensión *Draw.io Integration* de VS Code). Tiene 3 páginas:

1. **Hoy:** cómo se llena la lista con los 7 incidentes de ejemplo.
2. **Con el cambio:** el mismo flujo con `SupabaseIncidenteRepository`, supabase-kt y la API de Supabase.
3. **Límites:** qué falla y qué ve el usuario.

Convención de los diagramas del repo: continuo = ya existe; punteado = nuevo o planificado; `?` = sin comprobar.

## 1. Qué cambia

Solo se reemplaza el repositorio. La pantalla, el ViewModel, el caso de uso `ObtenerIncidentesPriorizados` y la interfaz
`IncidenteRepository` no se tocan. `listar()` pasa de devolver 7 incidentes fijos a pedirlos a Supabase.

Petición que sigue el diagrama: el ciudadano abre la lista. El repositorio hace **dos** llamadas con la clave `anon`
(sin sesión): `GET /rest/v1/categorias` y `POST /rest/v1/rpc/listar_incidentes_publicos`. Las dos salen **en cada
`listar()`, en paralelo**; así un cambio de pesos o plazos en SQL llega a la app al refrescar (RNF-08).

## 2. Comprobado el 1 de noviembre de 2026

| Qué | Cómo | Resultado |
|---|---|---|
| La RPC responde con la clave `anon` y sin sesión | `curl` contra `supabase start` | HTTP 200 y `[]` (la base local no tiene incidentes) |
| `categorias` se lee con `anon` | `curl` a `/rest/v1/categorias` | HTTP 200; trae `id`, `service_code`, `nombre`, `peso_prioridad`, `radio_duplicado_m`, `ventana_dias`, `plazo_objetivo_dias`, `activa` |
| CORS en local, con los encabezados que envía supabase-kt | preflight `OPTIONS` desde `http://localhost:8080` (POST a la RPC con `apikey,authorization,content-type,x-client-info`, y GET a `categorias`) | 200 en ambos, con `Access-Control-Allow-Origin: *` y esos encabezados permitidos |
| supabase-kt 3.8.0 y Ktor son compatibles | metadatos de Gradle (`.module`) en Maven Central | supabase-kt 3.8.0 declara `ktor-client-core 3.5.1`, la misma versión que se fija aquí |
| Sintaxis de supabase-kt | context7 (`/supabase-community/supabase-kt`) | `install(Postgrest)`, `supabase.from("tabla").select()`, `postgrest.rpc("funcion")` y `decodeList<T>()` |
| El SQL cuenta días en hora de Lima | `private.calcular_prioridad` | usa `timezone('America/Lima', …)::date` y `+20` si pasa del plazo |
| `lat` y `lon` nunca son nulos | esquema | `incidentes.geom` es `not null` |

Sin comprobar: la forma real del JSON **con datos** (se resuelve en la Tarea 2 del plan con `demo.sql`), los motores de
Ktor por plataforma (context7 no los documentó) y CORS en un navegador real, en Supabase Cloud y desde el panel Wasm.

## 3. Decisiones

Tomadas con el tech lead:

1. **Alcance:** solo lectura pública, sin iniciar sesión (`listar_incidentes_publicos`). Quedan fuera el login,
   `crear_reporte` y la caché sin conexión.
2. **Configuración:** cada app pasa `SupabaseConfig(url, anonKey)` a `shared` por Koin. Android las toma de
   `local.properties` con `buildConfigField`; el panel, de una tarea de Gradle. Sin plugin nuevo. Nunca entra la
   `service_role`.
3. **Errores:** `listar()` sigue lanzando excepciones; los ViewModels ya las atrapan. La interfaz no cambia.

Por defecto, aprobadas con la revisión (`/code-review` del 1 de noviembre):

4. **La prioridad se calcula en el dominio**, no se toma del servidor. `Incidente.prioridad` es un cálculo
   (`calcularPrioridad`) sin campo donde guardar otro valor; así el `Fake`, las pruebas actuales y el `+20` por plazo
   vencido siguen igual. El campo `prioridad` de la RPC se ignora. Queda anotado en `docs/arquitectura.md` §6.
5. **Días abiertos = días de calendario en America/Lima**, como el SQL: se desplaza `ahora` y `creado_en` −5 horas
   (Lima no tiene horario de verano), se toma el día entero de cada uno y se restan, con mínimo 0. Se usa `kotlin.time`
   (sin `kotlinx-datetime`) y no depende de la zona horaria del celular ni del navegador. **Límite conocido:** `ahora`
   sale del reloj del celular y el SQL usa el del servidor; un reloj mal puesto puede dar una prioridad algo distinta
   (el mínimo 0 solo tapa el reloj atrasado). Se acepta en el piloto.
6. **`Incidente` gana solo `latitud: Double?` y `longitud: Double?`** al final, con valor por defecto `null`, para
   no romper las pruebas ni el `Fake` ni inventar un punto en 0.0 (las necesita el mapa, issue #4). **No** se añade
   `id`: ninguna pantalla lo usa todavía y se agrega cuando una tarea lo necesite (detalle o apoyo). El DTO usa
   `Double` no nulo porque la base garantiza la ubicación, e ignora los campos que no usa (la configuración de `Json`
   se comprueba en la Tarea 3, paso 1, del plan).
7. **Estado:** el DTO lee `estado` como texto y lo convierte a mano (`reportado` → `REPORTADO`, `en_revision` →
   `EN_REVISION`, etc.). Un valor desconocido **omite esa fila con un aviso** (`println`, que aparece en Logcat y en la
   consola del navegador; un logger propio queda para después) y no tumba la lista. Hay una prueba que lo cubre, para
   que no se pierdan incidentes en silencio.
8. **Categorías:** el repositorio lee **todas** las filas de `categorias` (también las inactivas, para no descartar
   incidentes de una categoría desactivada) en **cada** `listar()`, en paralelo con la RPC (son 3 filas). No hay
   caché, ni `Mutex`, ni relectura: un cambio de `peso_prioridad` o `plazo_objetivo_dias` se ve al refrescar. El `id`
   queda dentro del repositorio; `Categoria` no cambia. Un `categoria_id` que no está **omite ese incidente con un
   aviso** (`println`). Si la lectura falla, `listar()` lanza una excepción y el ViewModel muestra `Error`.
9. **Límite de 200:** la RPC corta en 200 **antes** de que el caso de uso quite los resueltos, y los resueltos conservan
   su prioridad (que además puede estar vieja mientras no exista `pg_cron`), así que se acumulan. Se acepta en el
   piloto con una alarma medible: si la RPC devuelve **exactamente 200 filas**, se llegó al tope; el repositorio lo
   avisa con `println` y hay que arreglarlo ya con una migración (filtro de estados en la RPC), junto con las
   funciones versionadas pendientes.
10. **Sin configuración, solo en debug:** el `Fake` se usa en debug y en pruebas. El build **release** de Android
    (`assembleRelease` y `bundleRelease`) **falla** si faltan `supabase.url` o `anonKey`, para no publicar datos
    falsos. En el panel, la falla es opcional (`-Pcivika.exigirSupabase=true`) y la activará el workflow de
    despliegue (issue #14): el CI construye `wasmJsBrowserDistribution` sin claves y no puede exigirlas. **Límite
    conocido:** hasta que exista ese workflow, un build manual de distribución del panel sin claves publica el `Fake`;
    no se debe publicar a mano.

## 4. Diseño

- **Dependencias** (`gradle/libs.versions.toml`): supabase-kt BOM `3.8.0` con `postgrest-kt`, Ktor `3.5.1` y
  `kotlinx-serialization-json` `1.11.0`, con el plugin de serialización a la versión de Kotlin. Motores de Ktor:
  OkHttp en `androidMain`, **OkHttp también en `jvmMain`** (el target `jvm()` de `shared` corre `:shared:jvmTest` y el
  CI) y `ktor-client-js` en `wasmJsMain`. Las versiones existen en Maven Central; los motores por plataforma y la
  configuración de `Json` se confirman con context7 en la Tarea 3, paso 1, del plan, antes de tocar `libs.versions.toml`.
- **Código nuevo en `shared/data`:** `SupabaseConfig`, `IncidentePublicoDto`, `CategoriaDto` y
  `SupabaseIncidenteRepository`.
- **Koin:** `SharedModule` ofrece el repositorio real solo si hay `SupabaseConfig`; si no, el `Fake`. El CI sigue sin
  necesitar claves.
- **Documentación:** nota en `docs/arquitectura.md` §6 sobre qué prioridad usa la app (ya escrita en esta rama).

## 5. Plan y pruebas

El plan, con tareas, rutas y comandos, está en [2026-11-01-repositorio-supabase.md](../plans/2026-11-01-repositorio-supabase.md). Orden: Tarea 1 (PR #33 de dependabot), 2 (`demo.sql`), 3 (dependencias, con la consulta a context7), 4 (dominio y mapeo), 5 (repositorio y Koin), 6 (configuración en Android y el panel), 7 (prueba real contra Supabase local) y 8 (`apoyar_incidente`).

Pruebas que fija esta spec (se escriben antes del código):

- mapeo `IncidentePublicoDto` + `CategoriaDto` → `Incidente` con JSON copiado de la respuesta real del demo;
- estado conocido y estado desconocido (se omite la fila con aviso; este JSON es inventado, porque la RPC real no devuelve estados que la app no conoce);
- categoría desconocida (se omite el incidente con aviso y los demás pasan) y categoría inactiva (se incluye);
- tope: con exactamente 200 filas se emite el aviso de límite alcanzado;
- días de calendario en Lima: creado ayer a las 23:00 (Lima) y listado hoy a las 00:30 = 1 día; nunca negativo;
- Koin con y sin configuración: con ella resuelve el repositorio real; sin ella, el `Fake`;
- las pruebas actuales (`PrioridadTest`, `TransicionesEstadoTest`, `ObtenerIncidentesPriorizadosTest`) pasan sin cambios.

## 6. Fuera de alcance

Login, `crear_reporte`, subida de fotos, caché sin conexión, reintentos, `Result` en la interfaz, el filtro de estados
en la RPC (ver decisión 9) y un `demo.sql` con apoyos, fotos o historial.

## 7. Preguntas abiertas

- `?` ¿CORS en un navegador real, en Supabase Cloud y desde el panel Wasm? Se comprueba al conectar el panel.
- `?` Forma real del JSON con datos (por ejemplo, el formato de `creado_en`): se resuelve en la Tarea 2 del plan.
