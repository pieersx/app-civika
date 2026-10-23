# Guía de herramientas con Claude Code

Esta guía es para quien use **Claude Code** en el proyecto. Explica qué skills, plugins y MCP sirven para Cívika y
cuáles no. No es obligatoria: el flujo de Git y los PR de [CONTRIBUTING](../CONTRIBUTING.md) aplican a todos, uses o
no Claude Code.

> Una **skill** es un conjunto de instrucciones que Claude carga para un tipo de tarea; se invoca con `/nombre`.
> Un **MCP** es un conector que le da a Claude una herramienta externa, como la documentación de librerías o un navegador.

## Reglas de oro

1. **Antes de añadir o actualizar una dependencia, consulta context7.** Pídele a Claude: «revisa con context7 la versión
   actual de supabase-kt y cómo se instala en KMP». Así se evita usar APIs viejas que Claude recuerde de memoria.
2. **Tarea nueva = plan primero.** Usa `brainstorming` y después `writing-plans`. No se
   escribe código sin un plan aprobado.
3. **Tests primero.** `test-driven-development`: primero la prueba que falla, después el código.
4. **Nada se da por terminado sin evidencia.** `verification-before-completion`: hay que ejecutar las
   pruebas y mostrar la salida.
5. **Revisión antes de cada commit.** Usa el agente `revisor-codigo`. En un PR que toque `supabase/`, ejecuta también
   `/security-review`.

## Qué usar

| Herramienta | Para qué en Cívika | Quién la usa más |
|---|---|---|
| **context7** (MCP) | Documentación vigente de KMP, Compose Multiplatform, Koin, supabase-kt, MapLibre, CameraX, Room y ML Kit | Todos |
| `supabase-postgres-best-practices` (skill) | Reglas de Supabase/Postgres para esquema, RLS, índices y RPC; se carga antes de tocar SQL | Pieers |
| `supabase` (skill) | CLI, Auth, Storage, Edge Functions y diagnóstico; su auditoría Snyk está en riesgo medio, revisar antes de aplicar | Pieers y Alex |
| `brainstorming` + `writing-plans` (skills) | Convertir un issue en un plan con pasos y pruebas | Todos |
| `test-driven-development` (skill) | Reglas de `shared/domain`, ViewModels y RPC | Todos |
| `systematic-debugging` (skill) | Cuando algo falla y no es obvio por qué | Todos |
| `using-git-worktrees` (skill) | Trabajar en dos ramas a la vez sin mezclar archivos | Pieers |
| `finishing-a-development-branch` + `verification-before-completion` | Cerrar una rama con tests y evidencia | Todos |
| Agente `revisor-codigo` y `/code-review` (skill integrada) | Revisión con contexto limpio antes del commit y en el PR | Todos; Pieers revisa los PRs ajenos; su PR lo revisa otro colaborador o aplica la excepción del tech lead |
| `/security-review` | RLS, funciones `security definer`, secretos | Pieers |
| `mockup-ui` (personal de Pieers) + `frontend-design` (plugin) | Mockup clicable, dirección visual y especificaciones para Compose | Emily |
| Checklist de accesibilidad de `CLAUDE.md` | Contraste, TalkBack, foco y áreas táctiles antes de cerrar una pantalla | Emily y quien haga UI |
| `dataviz` | Gráficos de los indicadores del panel | Alex |
| Playwright MCP y Chrome DevTools MCP (`.mcp.json`) | Probar el panel web: flujos E2E, Lighthouse y accesibilidad | Alex |
| `archify` (instalar aparte) | Diagramas de arquitectura, secuencia y estados para el informe; `npx skills add tt-a1i/archify -y` | Todos |
| `docx` / `pptx` | Informe final (Entregable 3) y diapositivas; se usan desde la cuenta de cada uno y **no se copian al repo** por su licencia | Todos |
| `documentation-writer` (skill) | Documentación técnica con estructura Diátaxis | Todos |
| `commit` y `mejorar-skill` (personales de Pieers, fuera del repo) | Commits guiados y mejora de skills | Pieers |
| `entorno-windows` | Instalar Android Studio, Docker Desktop y la Supabase CLI en Windows | Todos, si está disponible |
| `explicar` (personal de Pieers, fuera del repo) | Entender un error o un archivo paso a paso | Quien esté aprendiendo |

**De dónde sale cada una:**
- **Integradas en Claude Code** (no hay que instalar nada): `dataviz`, `/security-review`, `/simplify` y el MCP de
  Claude in Chrome.
- **Skills de terceros**: ya **no se copian al repo**; cada integrante las instala en su equipo (ver «Instalar» abajo).
  Son `brainstorming`, `writing-plans`, `test-driven-development`, `systematic-debugging`, `using-git-worktrees`,
  `finishing-a-development-branch`, `verification-before-completion` (plugin `superpowers`), `frontend-design`
  (plugin), `supabase` y `supabase-postgres-best-practices` (`supabase/agent-skills`).
- **Documentos**: `docx` y `pptx` se usan desde la cuenta de cada uno; su licencia no permite copiarlos al repo.
- **Opcionales**: `archify` (diagramas) se instala con `npx skills add tt-a1i/archify -y`; los plugins `design` y
  `excalidraw` se instalan con `/plugin`. El flujo actual no depende de ellos.
- **MCP del proyecto** (en `.mcp.json`): `context7`, `playwright` y `chrome-devtools`. Claude Code pedirá aprobarlos
  la primera vez que abras el proyecto; `context7` además necesita iniciar sesión con `/mcp`. Las versiones de
  `playwright` y `chrome-devtools` están **fijadas** (nada de `@latest`): así nadie ejecuta en su PC una versión nueva
  sin revisar. Para subirlas, se revisa el changelog del paquete y se cambia la versión en `.mcp.json` en un PR.
- **Personales de Pieers**: el agente `revisor-codigo` sí está versionado en `.claude/agents/`. Las skills `commit`,
  `explicar`, `mejorar-skill`, `mockup-ui` y `entorno-windows` viven en su equipo (`~/.claude/skills`) y **no** están
  en este repo; se comparten aparte si hacen falta.

### Instalar (por comandos, en tu equipo)

Las instalaciones son por usuario: nada de esto se sube al repo. Dentro de Claude Code:

```text
/plugin install superpowers@anthropic-plugin-directory
/plugin install frontend-design@anthropic-plugin-directory
```

En la terminal, para las skills de Supabase (`-g` = para tu usuario, no para el proyecto):

```bash
npx skills add supabase/agent-skills -g -a claude-code -s supabase -s supabase-postgres-best-practices -y
```

Las skills quedan en `~/.claude/skills` (comprobado el 2026-10-22). Para ver qué hay antes de instalar: `npx skills add <repositorio> --list`. Para actualizar: `npx skills update -g`.
Para añadir un MCP al proyecto (queda en `.mcp.json`, con la versión fijada): `claude mcp add --scope project <nombre> -- npx -y <paquete>@<versión>`.
Un MCP solo tuyo se añade con `--scope user`.

Nunca se suben al repo: `.claude/settings.local.json`, `.claude/worktrees/`, `.claude/skills/`, `.claude/commands/`,
`.agents/` ni `skills-lock.json` (están en `.gitignore`). Sí se suben: `CLAUDE.md`, `.claude/agents/` y `.mcp.json`.

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
1. «Usa context7 para revisar la sintaxis de `geography` y `ST_DWithin` en PostGIS» → `writing-plans`.
2. Escribe la prueba pgTAP de `buscar_duplicados` (que falle) → la migración → `supabase test db`.
3. `revisor-codigo` → `/security-review` → commit → PR. Como el PR es tuyo, pide revisión a otro colaborador;
   tú revisas los PRs del equipo. Si no hay otro revisor, aplica la excepción del tech lead y deja constancia en el PR.

**Emily: tema de Compose (issue #2)**
1. `mockup-ui` sobre el mockup → tabla de tokens de `Color.kt` y `Type.kt`; `frontend-design` para la dirección visual.
2. «Usa context7: ¿cómo se cargan fuentes con Compose Resources en KMP?».
3. Pantalla de login → checklist de accesibilidad de `CLAUDE.md` → `revisor-codigo` → PR.

**Valeria: cámara y cola sin conexión (issue #3)**
1. «Usa context7 para CameraX con Compose y WorkManager con restricciones de red».
2. Prueba de `ReportePendienteDao` con Room en memoria → implementación.
3. `systematic-debugging` si el worker no se dispara.

**Keyla: mapa (issue #4)**
1. «Usa context7 para MapLibre Native Android: cargar el estilo de OpenFreeMap y agrupar marcadores (clustering)».
2. Marcadores con forma de señal desde el repositorio fake → prueba de UI.
3. `revisor-codigo` → PR.

**Alex: panel web (issue #5)**
1. «Usa context7 para Compose Multiplatform wasmJs: configuración del módulo y navegación».
2. Pantalla de incidentes con los datos fake de `shared` → prueba de humo con Playwright MCP.
3. Indicadores con `dataviz` → `revisor-codigo` → PR.
