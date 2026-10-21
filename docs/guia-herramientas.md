# Guía de herramientas con Claude Code

Esta guía es para quien use **Claude Code** en el proyecto. Explica qué skills, plugins y MCP sirven para Cívika y
cuáles no. No es obligatoria: el flujo de Git y los PR de [CONTRIBUTING](../CONTRIBUTING.md) aplican a todos, uses o
no Claude Code.

> Una **skill** es un conjunto de instrucciones que Claude carga para un tipo de tarea; se invoca con `/nombre`.
> Un **MCP** es un conector que le da a Claude una herramienta externa, como la documentación de librerías o un navegador.

## Reglas de oro

1. **Antes de añadir o actualizar una dependencia, consulta context7.** Pídele a Claude: «revisa con context7 la versión
   actual de supabase-kt y cómo se instala en KMP». Así se evita usar APIs viejas que Claude recuerde de memoria.
2. **Tarea nueva = plan primero.** Usa `superpowers:brainstorming` y después `superpowers:writing-plans`. No se
   escribe código sin un plan aprobado.
3. **Tests primero.** `superpowers:test-driven-development`: primero la prueba que falla, después el código.
4. **Nada se da por terminado sin evidencia.** `superpowers:verification-before-completion`: hay que ejecutar las
   pruebas y mostrar la salida.
5. **Revisión antes de cada commit.** Usa el agente `revisor-codigo`. En un PR que toque `supabase/`, ejecuta también
   `/security-review`.

## Qué usar

| Herramienta | Para qué en Cívika | Quién la usa más |
|---|---|---|
| **context7** (MCP) | Documentación vigente de KMP, Compose Multiplatform, Koin, supabase-kt, MapLibre, CameraX, Room y ML Kit | Todos |
| `superpowers:brainstorming` + `writing-plans` | Convertir un issue en un plan con pasos y pruebas | Todos |
| `superpowers:test-driven-development` | Reglas de `shared/domain`, ViewModels y RPC | Todos |
| `superpowers:systematic-debugging` | Cuando algo falla y no es obvio por qué | Todos |
| `superpowers:using-git-worktrees` | Trabajar en dos ramas a la vez sin mezclar archivos | Pieers |
| `superpowers:finishing-a-development-branch` | Cerrar una rama: tests, PR y fusión | Todos |
| Agente `revisor-codigo` y `/code-review` | Revisión con contexto limpio antes del commit y en el PR | Todos; Pieers en los PR |
| `/security-review` | RLS, funciones `security definer`, secretos | Pieers |
| `design:design-handoff` | Pasar del mockup a especificaciones de Compose (medidas, estados, tokens) | Emily |
| `design:accessibility-review` | Contraste, TalkBack y áreas táctiles antes de cerrar una pantalla | Emily y quien haga UI |
| `design:ux-copy` | Textos de botones, errores y pantallas vacías | Emily |
| `design:user-research` + `design:research-synthesis` | Guion y análisis de las pruebas de usabilidad del Sprint 4 | Emily |
| `dataviz` | Gráficos de los indicadores del panel | Alex |
| `frontend-design` | Diseñar una pantalla nueva que no esté en el mockup | Emily, Alex |
| Playwright MCP y Chrome DevTools MCP | Probar el panel web: flujos E2E, Lighthouse y accesibilidad | Alex |
| `excalidraw` | Diagramas para el informe y la exposición | Todos |
| `anthropic-skills:docx` / `pptx` | Informe final (Entregable 3) y diapositivas | Todos |
| `entorno-windows` | Instalar Android Studio, Docker Desktop y la Supabase CLI en Windows | Todos |
| `explicar` | Entender un error o un archivo paso a paso | Quien esté aprendiendo |

**De dónde sale cada una:**
- **Integradas en Claude Code** (no hay que instalar nada): `dataviz`, `/security-review`, `/simplify` y el MCP de
  Claude in Chrome.
- **Plugins** que cada uno instala con `/plugin`: `superpowers`, `design`, `frontend-design`, `code-review`
  (para `/code-review`), `excalidraw`, `context7`, `playwright` y `chrome-devtools-mcp`.
- **Personales de Pieers**: el agente `revisor-codigo` y las skills `entorno-windows` y `explicar` están en su
  configuración. Si alguien más las quiere, Pieers puede compartir los archivos.

En Android no hay MCP para controlar el emulador. Las pantallas se verifican con Compose UI Test, y las capturas se
sacan con `adb exec-out screencap -p > captura.png`.

## Qué no usar (y por qué)

| Herramienta | Por qué no |
|---|---|
| Asana, Linear, Notion, monday, HubSpot, Intercom, Box | La gestión ya está en GitHub Issues y Projects; otra herramienta duplica la información |
| Gmail, Google Drive, Canva | No forman parte del flujo de desarrollo |
| `claude-api` | La IA del proyecto es ML Kit en el celular; no se llama a ningún LLM desde la app |
| `Workflow` (multiagente), `schedule`, `loop` | Gastan muchos tokens y el proyecto no los necesita |
| Figma MCP | Solo si el grupo decide editar el diseño a mano; el mockup vigente está en Claude Design |

## Flujo por rol (ejemplos)

**Pieers: esquema de Supabase (issue #1)**
1. «Usa context7 para revisar la sintaxis de `geography` y `ST_DWithin` en PostGIS» → `superpowers:writing-plans`.
2. Escribe la prueba pgTAP de `buscar_duplicados` (que falle) → la migración → `supabase test db`.
3. `revisor-codigo` → commit → PR → `/security-review`.

**Emily: tema de Compose (issue #2)**
1. `design:design-handoff` sobre el mockup → tabla de tokens de `Color.kt` y `Type.kt`.
2. «Usa context7: ¿cómo se cargan fuentes con Compose Resources en KMP?».
3. Pantalla de login → `design:accessibility-review` → `revisor-codigo` → PR.

**Valeria: cámara y cola sin conexión (issue #3)**
1. «Usa context7 para CameraX con Compose y WorkManager con restricciones de red».
2. Prueba de `ReportePendienteDao` con Room en memoria → implementación.
3. `superpowers:systematic-debugging` si el worker no se dispara.

**Keyla: mapa (issue #4)**
1. «Usa context7 para MapLibre Native Android: cargar el estilo de OpenFreeMap y agrupar marcadores (clustering)».
2. Marcadores con forma de señal desde el repositorio fake → prueba de UI.
3. `revisor-codigo` → PR.

**Alex: panel web (issue #5)**
1. «Usa context7 para Compose Multiplatform wasmJs: configuración del módulo y navegación».
2. Pantalla de incidentes con los datos fake de `shared` → prueba de humo con Playwright MCP.
3. Indicadores con `dataviz` → `revisor-codigo` → PR.
