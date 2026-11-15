# webPanel/

Panel web de Cívika para el **personal de las entidades** y la **moderación** (RF-13, RF-14 y RF-16). Es una aplicación
estática en **React 19 + Vite + TypeScript**, con Tailwind CSS v4, TanStack Query, React Router y `@supabase/supabase-js`.
No es Kotlin ni forma parte del build de Gradle.

No tiene backend propio: habla directamente con Supabase (Auth, PostgREST y Storage). Los permisos reales los deciden
el RLS y las RPC (`supabase/migrations/20261113090300_panel.sql`); el panel solo esconde lo que el servidor rechazaría.

## Qué hace

- **Sesión:** correo y contraseña, **segundo factor TOTP obligatorio** (alta con QR y desafío), «Olvidé mi contraseña» y
  cierre de sesión. Una cuenta de ciudadano o inactiva ve «Esta cuenta no tiene acceso al panel» y se cierra su sesión.
- **Incidentes:** lista por prioridad con filtros (estado, categoría, vencidos), búsqueda, plazo en días de Lima,
  apoyos y denuncias. Detalle con fotos (URL firmadas de 10 minutos), mapa, historial y comentarios.
- **Cambio de estado:** solo las transiciones válidas para el rol, siempre con `p_version_esperada`. Si otra persona
  cambió el incidente, el panel avisa («El incidente cambió mientras lo veías») y recarga.
- **Moderación** (solo moderadores): cola de incidentes en revisión, denuncias pendientes, ocultar o mostrar fotos,
  derivar a una entidad, rechazar con motivo y marcar duplicados.
- **Indicadores:** KPIs, barras por categoría y zona y serie semanal (30, 90 o 180 días).
- Tema claro y oscuro (sigue al sistema o se elige con el conmutador). Tokens de `docs/diseno/direccion-visual.md`.

## Cómo ejecutarlo

Requiere Node 22.12 o superior y un Supabase en marcha (`supabase start` en la raíz del repo).

```bash
cd webPanel
npm install
cp .env.example .env.local     # y completa los dos valores (ver abajo)
npm run dev                    # http://localhost:5173
```

| Script | Para qué |
|---|---|
| `npm run dev` | Servidor de desarrollo |
| `npm run build` | Comprueba tipos y genera `dist/` |
| `npm run preview` | Sirve `dist/` con las mismas cabeceras de seguridad que Vercel (`vercel.json`) |
| `npm test` | Pruebas de la lógica pura con Vitest |
| `npm run lint` | ESLint |
| `npm run typecheck` | `tsc` sin emitir |

Antes de un PR: `npm run typecheck && npm run lint && npm test && npm run build`.

## Variables de entorno

| Variable | Valor |
|---|---|
| `VITE_SUPABASE_URL` | URL del proyecto. En local, `http://127.0.0.1:54321` |
| `VITE_SUPABASE_ANON_KEY` | Clave **anon / publishable**. En local sale de `supabase status -o env` |

**Jamás la `service_role` ni una clave `sb_secret_…`.** Todo lo que empieza por `VITE_` termina en el JavaScript que
descarga el navegador. Si falta una variable, o si la clave es de servidor, la app muestra «Falta configurar» en lugar
de arrancar. `.env.local` está ignorado por git.

## Datos de prueba en local

1. Crea las cuentas con el signup de Auth (el trigger las crea como ciudadanas) y promuévelas con SQL:

   ```bash
   docker exec -i supabase_db_app-civika psql -U postgres -c \
     "update public.usuarios set rol='moderador' where id=(select id from auth.users where email='mod@civika.test')"
   docker exec -i supabase_db_app-civika psql -U postgres -c \
     "update public.usuarios set rol='entidad', entidad_id=1 where id=(select id from auth.users where email='ent@civika.test')"
   ```

2. Carga los incidentes de ejemplo con `supabase/demo.sql` (ver `supabase/README.md`).
3. En el primer inicio de sesión el panel pide activar el segundo factor. Si no tienes una app de autenticación a mano,
   `node scripts/totp.mjs <CLAVE>` calcula el código a partir de la clave que aparece en «No puedo escanear el código».
   Es solo para pruebas locales.

## Despliegue en Vercel

Es una SPA estática; `vercel.json` ya trae el rewrite a `index.html`, las cabeceras de seguridad (CSP, `nosniff`,
`Referrer-Policy`, `frame-ancestors 'none'`, HSTS) y la caché de `assets/`.

1. Importa el repositorio en Vercel y fija **Root Directory** = `webPanel`.
2. **Build Command** = `npm run build` y **Output Directory** = `dist` (Vercel los detecta con `vercel.json`).
3. En *Settings → Environment Variables* define `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY` (clave anon del proyecto
   real) y vuelve a desplegar: las variables `VITE_` se incrustan al compilar.
4. En Supabase, *Authentication → URL Configuration*: pon la URL del panel en **Site URL** y añade a **Redirect URLs**
   cada URL desde la que se pueda entrar: la de producción, las de previsualización de Vercel (por ejemplo
   `https://*-tu-equipo.vercel.app/**`) y `http://localhost:5173/**` para desarrollo. El enlace de «Olvidé mi contraseña»
   apunta a `<origen del panel>/restablecer`; si esa URL no está en la lista, Supabase redirige a la Site URL y la
   recuperación falla.
5. En el proyecto real, activa el MFA TOTP (*Authentication → Multi-Factor*) y traduce al español las plantillas de
   correo (*Authentication → Email Templates*): las de fábrica salen en inglés.

La CSP permite `https://*.supabase.co` (API, Storage y Realtime) y `https://tiles.openfreemap.org` (estilo, teselas,
sprites y tipografías del mapa). Si usas un dominio propio para Supabase, añádelo a `connect-src` e `img-src`.
`npm run preview` añade automáticamente el origen de Supabase local a la CSP cuando no es `*.supabase.co`.

## Estructura

```
src/
  lib/         lógica pura con pruebas (fechas en Lima, plazos, filtros, transiciones, errores, configuración)
               y acceso a datos (api.ts, consultas.ts)
  sesion/      proveedor de sesión, inicio de sesión, segundo factor y pantallas sin acceso
  componentes/ piezas de interfaz (botones, campos, diálogo, estado con forma, mapa, avisos)
  vistas/      incidentes, detalle, mapa, moderación e indicadores
  styles/      tokens de diseño y Tailwind
docs/capturas/ capturas de la verificación en navegador
```

## Límites conocidos

- La lista usa `listar_incidentes_panel`, que devuelve como máximo 500 filas. El detalle sale de esa misma lista (no hay
  una RPC de detalle por id), así que un incidente fuera de esas 500 no se puede abrir por enlace.
- El mapa usa el estilo claro de OpenFreeMap también en el tema oscuro.
