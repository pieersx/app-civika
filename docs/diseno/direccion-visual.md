# Dirección visual de Cívika

Este documento es la fuente de verdad del diseño. El tema de Compose (`ui/theme`) y el panel web toman sus valores de aquí.
Si algo cambia, se actualiza primero este documento y después el código.

- **Mockup clicable:** [Cívika — Mockup MVP](https://claude.ai/artifact/M3L43Rf8e9fwZMN8wuCZSL). Tiene 18 pantallas: 12 de la
  app en tema claro, 3 en tema oscuro y 3 del panel web. Es privado: hay que compartirlo desde «Compartir» para que el
  grupo pueda abrirlo.
- **Issue:** #2

## Idea

**Cívico, cálido y peruano.** Cívika tiene que sentirse cercana, como un vecino que te ayuda. No debe parecer un trámite
del Estado ni una app genérica. La calidez sale del **terracota del ladrillo y del adobe** y del **verde andino**, que
comparten protagonismo. La claridad sale de la **señalética vial**, de donde vienen las formas de los estados.

Principios:

1. **Una acción principal por pantalla.** El ciudadano usa la app de pie, en la calle y con una sola mano.
2. **El estado se entiende sin color.** Cada estado tiene su forma, su ícono y su texto, además del color.
3. **Lo memorable está en un solo lugar:** los marcadores con forma de señal de tránsito. Todo lo demás es sobrio.
4. **El texto explica, no vende.** Frases cortas, de «tú», sin tecnicismos.

## Color

### Base (tema claro)

| Token | Hex | Uso |
|---|---|---|
| `terracota` | `#C4532D` | Relleno de botones principales y FAB. El texto encima va en blanco (4,54:1) |
| `terracotaTexto` | `#A8431F` | Enlaces y texto de acento sobre arena (5,59:1) |
| `verdeAndino` | `#2F6B4F` | Navegación, chips seleccionados y encabezados de sección (5,85:1 sobre arena) |
| `arena` | `#FAF6F0` | Fondo de la app |
| `superficie` | `#FFFFFF` | Tarjetas, bottom sheets y diálogos |
| `tinta` | `#1F1B16` | Texto principal (15,9:1) |
| `tintaSuave` | `#5C534A` | Texto secundario (6,99:1) |
| `borde` | `#E4DCD0` | Solo separadores y tarjetas (decorativo, 1,26:1) |
| `bordeCampo` | `#8C8278` | Contorno de campos de texto y selectores: 3,5:1 sobre arena y 3,76:1 sobre blanco (WCAG 1.4.11 pide 3:1) |

### Base (tema oscuro)

| Token | Hex | Uso |
|---|---|---|
| `terracota` | `#F0A07E` | Relleno de botones principales. El texto encima va en `#3B1205` (7,87:1) |
| `verdeAndino` | `#7FC8A0` | Navegación y chips (9,39:1 sobre el fondo) |
| `fondo` | `#171310` | Fondo de la app. Es marrón muy oscuro, no negro: así conserva la calidez |
| `superficie` | `#221C18` | Tarjetas y sheets |
| `tinta` | `#EDE6DC` | Texto principal (14,9:1) |
| `tintaSuave` | `#B5ADA4` | Texto secundario (8,34:1) |
| `borde` | `#4A3F37` | Solo separadores (decorativo) |
| `bordeCampo` | `#7F756B` | Contorno de campos: 4,1:1 sobre el fondo y 3,74:1 sobre la superficie |

En el tema oscuro, el `terracota` `#F0A07E` sí sirve como color de texto sobre el fondo (8,83:1), así que no hace
falta un `terracotaTexto` aparte.

### Roles de Material 3

| Rol M3 | Claro | Oscuro |
|---|---|---|
| `primary` / `onPrimary` | `#C4532D` / `#FFFFFF` | `#F0A07E` / `#3B1205` |
| `secondary` / `onSecondary` | `#2F6B4F` / `#FFFFFF` | `#7FC8A0` / `#0F2A1D` |
| `background` / `onBackground` | `#FAF6F0` / `#1F1B16` | `#171310` / `#EDE6DC` |
| `surface` / `onSurface` | `#FFFFFF` / `#1F1B16` | `#221C18` / `#EDE6DC` |
| `onSurfaceVariant` | `#5C534A` | `#B5ADA4` |
| `outline` | `#8C8278` | `#7F756B` |
| `outlineVariant` | `#E4DCD0` | `#4A3F37` |
| `error` / `onError` | `#B83232` / `#FFFFFF` (5,93:1) | `#FF8A80` / `#3B0F0B` (7,31:1) |

> El terracota `#C4532D` **no se usa como color de texto sobre arena**: da 4,22:1 y no llega al mínimo AA de 4,5:1.
> Para texto se usa `terracotaTexto`. Ojo: en Material 3, `TextButton` y los enlaces pintan el texto con `primary`.
> En el tema claro de Compose hay que darles `terracotaTexto`, por ejemplo con
> `ButtonDefaults.textButtonColors(contentColor = TerracotaTexto)`, o con un componente propio `CivikaTextButton`.

### Estados del incidente

Los 6 estados del RF-10 se agrupan en 4 familias visuales. Cada familia tiene un **relleno** (para marcadores y
etiquetas) y un **color de texto** (para escribir el estado sobre arena).

| Familia | Estados | Forma del marcador | Relleno + ícono | Texto sobre arena | Oscuro (relleno + ícono) |
|---|---|---|---|---|---|
| Abierto | Reportado, En revisión, Derivado | **Triángulo** (señal de peligro) | `#D64545` + ícono blanco (4,38:1, solo para íconos) | `#B83232` (5,51:1) | `#FF8A80` + `#3B0F0B` |
| En atención | En atención | **Rombo** (señal de obra) | `#E0A100` + ícono `#1F1B16` (7,56:1) | `#8A6100` (5,15:1) | `#F2C14E` + `#2A1F00` |
| Resuelto | Resuelto | **Círculo** | `#2E8B57` + ícono blanco (4,25:1, solo para íconos) | `#1F7044` (5,64:1) | `#6FD39A` + `#0F2A1D` |
| Rechazado | Rechazado | **Cuadrado** | `#6B6560` + ícono blanco | `#6B6560` (5,34:1) | `#B5ADA4` + `#171310` |

En el tema oscuro, los rellenos de estado son claros, así que el ícono va en oscuro y el contorno del marcador va en
el color del fondo (`#171310`).

### Datos (gráficos del panel)

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| `datos` | `#1E7A55` | `#3BA878` | Barras de los gráficos con una sola serie |

El verde andino `#2F6B4F` no sirve para los gráficos: el validador de paletas lo marca porque tiene poca saturación
(«se lee como gris»). `datos` es el mismo tono, pero más saturado, y pasa los controles de luminosidad, croma y contraste.
Los colores de estado **nunca** se usan como colores de una serie de datos.

> El ámbar `#E0A100` da solo 2,1:1 sobre arena, así que **nunca** se usa como texto ni como borde suelto. Va siempre
> como relleno con ícono oscuro y con contorno `#1F1B16` en el marcador.

**Regla:** el estado nunca se comunica solo con color. Siempre van juntos la forma, el ícono y la palabra del estado
(«En atención»). Así lo entiende una persona daltónica y también TalkBack.

## Tipografía

- **Títulos:** [Bricolage Grotesque](https://fonts.google.com/specimen/Bricolage+Grotesque). Tiene carácter y ancho
  variable. Se usa en peso 600-700 y solo en `display`, `headline` y `titleLarge`. Desde `titleMedium` hacia abajo
  todo va en Atkinson.
- **Texto e interfaz:** [Atkinson Hyperlegible Next](https://fonts.google.com/specimen/Atkinson+Hyperlegible+Next),
  diseñada para personas con baja visión. Se usa en peso 400-700 en `body` y `label`.
- Las dos fuentes van **empaquetadas** como archivos `.ttf` con Compose Resources (`composeResources/font/`), para
  que la app Android y el panel web (Wasm) usen exactamente las mismas. Las fuentes descargables de Google Fonts solo
  existen en Android. Se empaquetan los `.ttf` **estáticos** de cada peso (Bricolage 600 y 700; Atkinson 400 y 700),
  no la versión variable, porque los ejes de una fuente variable pueden no aplicarse en Wasm. Las dos tienen licencia
  SIL Open Font License, que permite incluirlas en la app.

| Estilo M3 | Familia | Tamaño / interlineado | Peso |
|---|---|---|---|
| `displaySmall` | Bricolage | 36 / 44 sp | 700 |
| `headlineMedium` | Bricolage | 28 / 36 sp | 700 |
| `headlineSmall` | Bricolage | 24 / 32 sp | 600 |
| `titleLarge` | Bricolage | 22 / 28 sp | 600 |
| `titleMedium` | Atkinson | 16 / 24 sp | 700 |
| `bodyLarge` | Atkinson | 16 / 24 sp | 400 |
| `bodyMedium` | Atkinson | 14 / 20 sp | 400 |
| `labelLarge` | Atkinson | 14 / 20 sp | 700 |
| `labelMedium` | Atkinson | 12 / 16 sp | 700 |

Reglas: siempre **mayúscula inicial y el resto en minúscula** (nada de etiquetas en MAYÚSCULAS), líneas de texto de
menos de 70 caracteres y sin resaltar una sola palabra con otro color.

## Medidas y formas

- **Espaciado:** múltiplos de 4 dp. Lo habitual es 8, 12, 16, 24 y 32 dp. El margen lateral de las pantallas es de 16 dp.
- **Área táctil:** 48 x 48 dp como mínimo en todo lo que se pueda tocar (RNF-07). El botón principal mide 56 dp de alto.
- **Radios:** 8 dp en chips y campos, 12 dp en tarjetas y 28 dp en bottom sheets y FAB. La jerarquía se marca con el
  radio: no todo lleva el mismo.
- **Elevación:** casi plana. Las tarjetas se separan del fondo arena por el color de `superficie`, no por la sombra.
  Solo el FAB y el bottom sheet llevan sombra.

## Iconografía

- [Material Symbols Rounded](https://fonts.google.com/icons), con relleno para los íconos activos y contorno para los inactivos.
- Categorías del MVP:

| Categoría | Ícono | `service_code` |
|---|---|---|
| Infraestructura vial (baches) | `road` / `warning` | `vial` |
| Residuos | `delete` | `residuos` |
| Accesibilidad | `accessible` | `accesibilidad` |

## Tono de los textos

- Se habla de **tú**, con verbos claros: «Reportar problema», «Apoyar», «Ver en el mapa».
- Una acción mantiene el mismo nombre en todo el flujo. Si el botón dice «Enviar reporte», el mensaje final dice
  «Reporte enviado».
- Los errores dicen qué pasó y qué hacer, sin disculpas. Ejemplo: «No hay conexión. Tu reporte se enviará cuando vuelva
  la señal.»
- Las pantallas vacías invitan a actuar. Ejemplo: «Aún no tienes reportes. Cuando veas un problema, repórtalo aquí.»
- Cívika **no es un canal de emergencias**. Cuando un texto pueda prestarse a confusión, se recuerda llamar al 105 o al 116.
