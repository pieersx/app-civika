# Plan de desarrollo del MVP — Cívika

Este documento cubre la **fase 3 (Desarrollo del MVP)** y las **fases 4 y 5 (pruebas y cierre)** de la
[Tabla 4 de los entregables](entregables-1-y-2.md#31-fases-del-proyecto). Los requisitos (RF y RNF) son los de las
[Tablas 5 y 6](entregables-1-y-2.md#311-análisis-de-requisitos).

- **Duración:** 8 semanas, en 4 sprints de 2 semanas.
- **Equipo:** 5 integrantes de nivel intermedio en Kotlin y Android. Pieers es el tech lead.
- **Fechas tentativas:** del 26 de octubre al 20 de diciembre de 2026. Se ajustan al calendario del curso.

## 1. ¿Por dónde empezamos?

**En la semana 1 avanzan en paralelo el diseño de la interfaz y el contrato de datos.** Las pantallas se programan
cuando el mockup ya esté aprobado.

- El backend **no depende del diseño**, sino de los RF, que ya están cerrados en el Entregable 2. Por eso Pieers puede
  definir el esquema de Supabase desde el día 1.
- La interfaz **sí depende del mockup**. Si alguien programa pantallas antes de aprobarlo, lo más probable es que tenga
  que rehacerlas.

**Herramienta de diseño: Claude Design** (Artifact de tipo diseño). Con ella se hace un mockup clicable del móvil
(390x844) y otro del panel web (1920x1080), con datos de ejemplo de Lima. Se itera rápido y el link se comparte con
todo el grupo. *Alternativa: Figma, si el grupo prefiere editar a mano.*

Orden de trabajo de la interfaz:

1. **Dirección visual:** paleta, tipografía, estilo y densidad. Se buscan referencias cívicas con identidad peruana,
   para que la app no tenga el look genérico de las apps hechas con IA.
2. **Mockup** de las pantallas clave: Login, Home, Mapa, flujo de reporte (5 pasos como máximo, RNF-01), pantalla
   «¿Es el mismo problema?», detalle con historial y Perfil.
3. **Aprobación del grupo**, como fecha límite el final de la semana 1.
4. **Design system en Compose:** `ui/theme` (colores, tipografía, formas) y componentes reutilizables.
5. **Pantallas.**

## 2. Roles

| Integrante | Rol | Responsable de | Requisitos |
|---|---|---|---|
| **Pieers** | Tech lead y backend geoespacial | Arquitectura y esqueleto de la app (paquetes por capa, Hilt, Navigation y DI); esquema de Supabase + PostGIS + RLS; RPC de **duplicados**, **ruteo** y **prioridad**; CI; revisión de todos los PR | RF-05, RF-06, RF-09; RNF-03, RNF-04, RNF-05 (RLS, no exponer datos del reportante), RNF-08, RNF-09, RNF-10 |
| **Emily** | UI/UX y design system | Dirección visual y mockup; tema Material 3 y componentes; Splash, Login/Registro (con consentimiento de datos), Home, Perfil y «Mis reportes»; accesibilidad | RF-01, RF-12; RNF-01, RNF-05 (consentimiento), RNF-07 |
| **Valeria** | Flujo de reporte | CameraX (hasta 3 fotos); ubicación con Fused Location y ajuste en el mapa; categoría y descripción; pantalla de duplicados; subida a Storage; trabajo sin conexión con Room + WorkManager | RF-02, RF-03, RF-04, RF-15; RNF-11 |
| **Keyla** | Mapa e incidentes | Mapa (MapLibre con teselas de OpenStreetMap) con marcadores por categoría y estado y filtros; detalle del incidente; historial de estados; botón «Apoyar» | RF-07, RF-08, RF-10; RNF-02 |
| **Alex** | Panel web y notificaciones | Panel Vite + TypeScript (login, lista y mapa, cambio de estado y comentarios, moderación); Edge Function + FCM; indicadores por distrito | RF-11, RF-13, RF-14, RF-16 |

**¿Por qué este reparto?** A Pieers le toca la lógica geoespacial (duplicados, ruteo y prioridad), que es la parte más
difícil y la que el Entregable 1 presenta como innovación. Cada uno de los demás tiene un módulo vertical, es decir,
una funcionalidad completa de principio a fin, y así se pisan lo menos posible.

**Pruebas:** cada integrante escribe las pruebas unitarias de su módulo (RNF-10). En el Sprint 4 se cruzan las pruebas:
cada uno prueba el módulo de otro compañero.

## 3. Contrato compartido (lo fija Pieers en la semana 1)

Este contrato es lo que permite trabajar en paralelo. Hasta que esté publicado nadie programa contra el backend real.

**Tablas (PostgreSQL + PostGIS)**

| Tabla | Para qué sirve |
|---|---|
| `usuarios` | Perfil y rol (`ciudadano`, `moderador`, `entidad`). Se enlaza con Supabase Auth |
| `categorias` | `service_code` (Open311), nombre, ícono y peso para la prioridad |
| `distritos` | Nombre y polígono (`geometry`) del distrito |
| `entidades` | Municipalidad o empresa responsable |
| `reglas_ruteo` | Regla (categoría + distrito) → entidad |
| `incidentes` | Punto (`geometry`), categoría, `status`, prioridad, distrito, entidad y número de apoyos |
| `reportes` | Cada envío de un ciudadano, enlazado con su incidente |
| `fotos` | Ruta en Storage y relación con el reporte |
| `apoyos` | Único por (usuario, incidente) |
| `historial_estados` | Estado anterior, estado nuevo, comentario, autor y fecha |
| `notificaciones` | Token FCM y notificaciones enviadas |

**Funciones RPC**

- `buscar_duplicados(lat, lon, categoria)`: devuelve los incidentes abiertos de la misma categoría a menos de 50 m y
  dentro de la ventana de tiempo. Usa `ST_DWithin`.
- `crear_reporte(...)`: crea el incidente o se une a uno existente, asigna el distrito con `ST_Contains`, busca la
  entidad en `reglas_ruteo`, calcula la prioridad y registra el estado «Reportado».

> La Figura 1 del Entregable 2 habla de «funciones de borde» en sentido general. En la implementación, los
> duplicados, el ruteo y la prioridad se programan como **funciones RPC en PostgreSQL**, porque necesitan PostGIS y
> transacciones. Las **Edge Functions** quedan solo para las integraciones externas, como FCM.

**En Android:** cada fuente de datos se usa a través de una interfaz (por ejemplo, `IncidenteRepository`) que tiene
una implementación *fake* con datos de ejemplo. Así Emily, Valeria y Keyla pueden avanzar desde la semana 1, y
cuando el backend esté listo solo cambia la implementación que se inyecta con Hilt.

## 4. Sprints

### Sprint 1 — Base (semanas 1 y 2)

| Integrante | Tareas |
|---|---|
| Pieers | Proyecto Android (Compose, Hilt, Navigation, Retrofit, Room); contrato de datos; migraciones de Supabase con RLS; seed del distrito piloto; CI en GitHub Actions (build + tests + lint) |
| Emily | Dirección visual y mockup en Claude Design; tema y componentes en Compose; Splash y Login/Registro con Supabase Auth (RF-01) |
| Valeria | Prototipo de CameraX y permisos; obtención de la ubicación con Fused Location; entidades de Room para los reportes pendientes |
| Keyla | Mapa con MapLibre y teselas de OSM; marcadores con datos del repositorio fake; elección del proveedor de teselas |
| Alex | Proyecto del panel en Vite + TS; login con supabase-js; proyecto de Firebase y prueba de una notificación push |

**Al cerrar el Sprint 1:** mockup aprobado, CI en verde, login real funcionando y mapa con datos fake.

### Sprint 2 — Flujo principal (semanas 3 y 4)

| Integrante | Tareas |
|---|---|
| Pieers | `buscar_duplicados` y `crear_reporte` con sus pruebas SQL; repositorio real de incidentes |
| Emily | Home y navegación principal; estados de carga, vacío y error |
| Valeria | Flujo completo de reporte (foto → GPS → categoría → duplicados → enviar) y subida a Storage |
| Keyla | Mapa conectado a Supabase; filtros por categoría y estado (RF-08) |
| Alex | Lista y mapa de incidentes en el panel; cambio de estado con registro en el historial |

**Al cerrar el Sprint 2:** un reporte hecho desde el celular aparece en el mapa con la entidad asignada.

### Sprint 3 — Comunidad y entidad (semanas 5 y 6)

| Integrante | Tareas |
|---|---|
| Pieers | Cálculo de prioridad (RF-09); trigger que notifica cada cambio de estado; endurecimiento de RLS |
| Emily | Perfil y «Mis reportes» (RF-12); revisión de accesibilidad con TalkBack |
| Valeria | Modo sin conexión con WorkManager (RF-15) |
| Keyla | Detalle e historial del incidente (RF-10); botón «Apoyar» con foto opcional (RF-07) |
| Alex | Edge Function + FCM (RF-11); comentarios de la entidad (RF-13) |

**Al cerrar el Sprint 3:** la entidad cambia un estado en el panel y llega la notificación push al celular.

### Sprint 4 — Cierre (semanas 7 y 8)

| Integrante | Tareas |
|---|---|
| Todos | Pruebas cruzadas y corrección de bugs |
| Pieers | Revisión de seguridad (RLS y secretos); APK firmado; guion de la demo |
| Emily | Pruebas de usabilidad con 5 o más usuarios (RNF-01); ajustes de UX |
| Valeria | Pruebas de los casos sin conexión y con GPS impreciso |
| Keyla | Rendimiento del mapa con 500 incidentes (RNF-02); pruebas de interfaz |
| Alex | Indicadores por distrito (RF-14); moderación (RF-16) |

**Al cerrar el Sprint 4:** APK de demo, informe de pruebas y presentación final.

**Si falta tiempo, se recorta en este orden:** RF-14 → RF-16 → RF-15. Son de prioridad Media y no rompen el flujo
principal. *Riesgo:* si se recorta RF-16, la moderación queda reducida a ocultar reportes a mano desde el panel, y
los reportes falsos se mitigan menos (ver «Limitaciones del sistema»).

## 5. Flujo de trabajo

- **Tablero:** GitHub Projects, con 1 issue por tarea. Cada issue indica el RF que cubre.
- **Ramas:** una rama corta por tarea, con el formato `tipo/descripcion` (por ejemplo, `feat/flujo-reporte`). Nadie trabaja directamente en `main`.
- **Pull requests:** cada PR necesita 1 revisión (de Pieers o de otro compañero) y el CI en verde. La fusión es
  fast-forward (ver [CONTRIBUTING](../CONTRIBUTING.md#4-pull-request)) y luego se borra la rama.
- **Commits:** `tipo: descripción`, en español, con un máximo de 60 caracteres. Más detalle en [CONTRIBUTING](../CONTRIBUTING.md).
- **Reuniones:** sincronización corta 2 veces por semana y demo al final de cada sprint.
- **Secretos:** la URL y la clave `anon` de Supabase van en `local.properties` (Android) o en `.env` (panel), y
  `google-services.json` va en `android/app/`. Ninguno de estos archivos se sube. La clave **`service_role`** solo se
  guarda en los secretos de las Edge Functions y **nunca** entra en `android/` ni en `panel-web/`, porque cualquiera
  podría leerla desde el APK o desde el navegador. El repo es público.

### Definición de «terminado»

Una tarea está terminada cuando cumple estas condiciones:

1. El código está en `main` mediante un PR revisado.
2. Tiene pruebas unitarias de su lógica, y pasan.
3. El CI está en verde (build, tests y lint).
4. Se probó en un dispositivo o emulador con API 26 o superior.
5. El issue está cerrado y enlazado al PR.
