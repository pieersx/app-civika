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
| `supabase-postgres-best-practices` (skill del proyecto) | Reglas de Supabase/Postgres para esquema, RLS, índices y RPC; se carga antes de tocar SQL | Pieers |
| `supabase` (skill del proyecto) | CLI, Auth, Storage, Edge Functions y diagnóstico; su auditoría Snyk está en riesgo medio, revisar antes de aplicar | Pieers y Alex |
| `brainstorming` + `writing-plans` (skills del proyecto) | Convertir un issue en un plan con pasos y pruebas | Todos |
| `test-driven-development` (skill del proyecto) | Reglas de `shared/domain`, ViewModels y RPC | Todos |
| `systematic-debugging` (skill del proyecto) | Cuando algo falla y no es obvio por qué | Todos |
| `using-git-worktrees` (skill del proyecto) | Trabajar en dos ramas a la vez sin mezclar archivos | Pieers |
| `finishing-a-development-branch` + `verification-before-completion` | Cerrar una rama con tests y evidencia | Todos |
| Agente `revisor-codigo` y `/code-review` | Revisión con contexto limpio antes del commit y en el PR | Todos; Pieers en los PR |
| `/security-review` | RLS, funciones `security definer`, secretos | Pieers |
| `mockup-ui` + `frontend-design` (skills del proyecto) | Mockup clicable, dirección visual y especificaciones para Compose | Emily |
| Checklist de accesibilidad de `CLAUDE.md` | Contraste, TalkBack, foco y áreas táctiles antes de cerrar una pantalla | Emily y quien haga UI |
| `dataviz` | Gráficos de los indicadores del panel | Alex |
| Playwright MCP y Chrome DevTools MCP (`.mcp.json`) | Probar el panel web: flujos E2E, Lighthouse y accesibilidad | Alex |
| `archify` (instalar aparte) | Diagramas de arquitectura, secuencia y estados para el informe; `npx skills add tt-a1i/archify -y` | Todos |
| `docx` / `pptx` | Informe final (Entregable 3) y diapositivas; se usan desde la cuenta de cada uno y **no se copian al repo** por su licencia | Todos |
| `documentation-writer` (skill del proyecto) | Documentación técnica con estructura Diátaxis | Todos |
| `commit` y `mejorar-skill` (skills del proyecto) | Commits guiados y mejora de skills | Pieers |
| `entorno-windows` | Instalar Android Studio, Docker Desktop y la Supabase CLI en Windows | Todos, si está disponible |
| `explicar` (skill del proyecto) | Entender un error o un archivo paso a paso | Quien esté aprendiendo |

**De dónde sale cada una:**
- **Integradas en Claude Code** (no hay que instalar nada): `dataviz`, `/security-review`, `/simplify` y el MCP de
  Claude in Chrome.
- **Skills del proyecto** (en `.agents/skills` y `.claude/skills`): `brainstorming`, `commit`,
  `documentation-writer`, `explicar`, `find-skills`, `finishing-a-development-branch`, `frontend-design`,
  `mejorar-skill`, `mockup-ui`, `supabase`, `supabase-postgres-best-practices`, `systematic-debugging`,
  `test-driven-development`, `using-git-worktrees`, `verification-before-completion` y `writing-plans`.
- **Documentos**: `docx` y `pptx` se usan desde la cuenta de cada uno; su licencia no permite copiarlos al repo.
- **Opcionales**: `archify` (diagramas) se instala con `npx skills add tt-a1i/archify -y`; los plugins `design` y
  `excalidraw` se instalan con `/plugin`. El flujo actual no depende de ellos.
- **MCP del proyecto** (en `.mcp.json`): `context7`, `playwright` y `chrome-devtools`. Claude Code pedirá aprobarlos
  la primera vez que abras el proyecto; `context7` además necesita iniciar sesión con `/mcp`.
- **Personales de Pieers**: el agente `revisor-codigo` ya está versionado en `.claude/agents/`; la skill
  `entorno-windows` no está en este repo y se comparte aparte.

Las skills instaladas con `npx` se actualizan con `npx skills update`; después sincroniza la copia de Claude Code:

```bash
cp -a .agents/skills/. .claude/skills/
```

En Windows PowerShell:

```powershell
Copy-Item .agents\skills\* .claude\skills -Recurse -Force
```

`skills-lock.json` fija las versiones. Las skills de terceros no se editan a mano; las personales se mantienen en
`.agents/skills` y se copian a `.claude/skills`.

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
3. `revisor-codigo` → commit → PR → `/security-review`.

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
