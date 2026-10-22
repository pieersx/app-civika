---
name: mockup-ui
description: Crea y valida un mockup clicable de UI (dirección visual, Artifact de diseño, copia local y pruebas con Playwright). Úsala solo cuando el usuario escriba /mockup-ui.
disable-model-invocation: true
---

Tarea: $ARGUMENTS

1. Busca sistema de diseño o componentes en el proyecto (`package.json`, carpeta de componentes, tokens). Si existen, reutilízalos y no preguntes la dirección visual.
2. Si no existen, usa `frontend-design` y pregunta con AskUserQuestion, una a una y con tu recomendación: referencias (2-3 webs y qué le gusta de cada una), paleta y un solo acento, estilo, tipografía, densidad y layout. Evita iconos sobre cuadros pastel y el look genérico de IA.
3. Haz el mockup clicable en un Artifact tipo diseño (escritorio 1920x1080 y móvil 390x844) con datos de ejemplo. Placeholders de foto con proporción real y el nombre del archivo futuro; sin fotos de marcas ajenas. Incluye estados: vacío, error, desactivado, cargando y foco visible con teclado.
4. Pásalo por `frontend-design` y revisa contraste, TalkBack, foco visible y áreas táctiles de 48 dp según `CLAUDE.md`; da los 5 problemas más graves.
5. Genera la copia local en `<proyecto>/mockup/` y sírvela con `node .agents/skills/mockup-ui/scripts/servidor.mjs <carpeta> 4173` (solo localhost). Si no es viable, explica por qué.
6. Prueba con Playwright en 3 tandas:
   - Flujos: reportar, apoyar, cambiar de estado, errores y éxito.
   - Solo teclado: orden de Tab, Escape y dónde queda el foco.
   - Tamaños en píxeles CSS (lo que debe valer `window.innerWidth`). Obligatorios (mínimos de CLAUDE.md): 1920x1080 (monitor), 1366x768 (laptop antigua) y 390x844 (iPhone y Android grande). Adicionales: 1536x864 (laptop con zoom de Windows 125 %) y 360x800 (Android pequeño). Comprueba sin scroll horizontal y elementos fijos.
   - Objetivos táctiles de al menos 44 px (alto y ancho) en todas las vistas, no solo en el listado: migas de pan, pie de página, formularios, panel y páginas nuevas.
   - Solo en Windows con escala distinta de 100 %: `setViewportSize` va en píxeles físicos: pide `ancho × devicePixelRatio`, espera a que `window.innerWidth` valga lo pedido y registra esa comprobación ("viewport exacto"). No fuerces el tamaño por CDP (el protocolo interno de Chrome): Playwright lo ignora y deja el navegador contaminado; si pasa, cierra el navegador y empieza de cero.
   - La escala puede cambiar a mitad de una ejecución (por ejemplo, al mover la ventana de monitor): relee `devicePixelRatio` en cada carga, reintenta si `innerWidth` no coincide y cierra las pestañas sobrantes del navegador de pruebas.
   - Si el mockup tiene un marco móvil (por ejemplo, `Movil.dc.html`), ábrelo también en una ventana ancha: no debe depender del ancho de la ventana.
7. Auditoría con Chrome DevTools sobre la copia local: `lighthouse_audit` en escritorio y móvil, y una traza de rendimiento (móvil con CPU 4x y red 4G rápida). Informa de LCP, CLS y fallos. No cuentes contra el mockup lo que causa el servidor local (favicon, caché, compresión). Tras corregir, repite la auditoría: un arreglo no está hecho hasta que Lighthouse deja de avisar. Si `take_screenshot` se cuelga con emulación móvil, usa las capturas de Playwright.
8. Antes de contar fallos, mira la consola: un solo error de ciclo de vida puede romper muchas pruebas. Playwright espera para siempre en botones con `aria-disabled`; fuerza el clic o usa `evaluate`.
   - Comprueba que cada prueba mide lo correcto (muestra el elemento medido en el dato): un OK no prueba nada si el selector es el equivocado. Mira también las capturas clave antes de dar algo por bueno.
   - Valida la sintaxis de los scripts antes de ejecutarlos: guárdalos en un archivo y usa `node --check ruta/script.js` (si usan `await` suelto, guárdalos como `.mjs`); no te fíes de un "OK" que viene tras un error.
9. Guarda capturas en `mockup/evidencia/` y un `RESULTADOS.md` (Id, prueba, resultado, dato). En la primera pasada no arregles nada; tras corregir, repite y añade una tabla "antes / después".
10. Termina con un resumen de 5 viñetas y espera mi aprobación antes del brainstorming técnico. Sin commit; añade `.playwright-mcp/` al `.gitignore` del proyecto y detén el servidor local.
11. Haz esto antes del resumen del paso 10: quita la emulación (simular otro tamaño de pantalla, CPU 4x y red 4G del paso 7) del Chrome de DevTools, restableciéndola a su valor por defecto, y verifica con `evaluate_script` que `innerWidth` es el real; si no, el usuario ve la página cortada (por ejemplo, un botón fijo fuera de la ventana).
