# Despliegue y operación — Cívika

Guía para dejar Cívika funcionando en **producción** (o en el entorno demo que se presenta) y los procedimientos
manuales (*runbooks*) del día a día. Marca cada casilla cuando esté hecha. El entorno **local** se describe en el
[README](../README.md).

> Ningún valor secreto se escribe en este archivo ni en el repo. Aquí solo va **qué** hay que generar y **dónde** se
> guarda cada cosa.

## 0. Todas las variables, de un vistazo

Hay cuatro lugares donde se configuran valores. Cada uno tiene su plantilla en el repo; copia la plantilla, rellena y
no subas la copia (todas están en `.gitignore`).

| Dónde | Plantilla | Variable | De dónde sale | ¿Secreta? |
|---|---|---|---|---|
| App Android (local) | `local.properties.example` → `local.properties` | `supabase.url` | Supabase → Project Settings → API → *Project URL* | No |
| | | `supabase.anonKey` | Supabase → Project Settings → API keys → *anon / publishable* | No (es pública por diseño) |
| App Android (firma) | `androidApp/keystore.properties.example` → `androidApp/keystore.properties` | ruta, contraseñas y alias del keystore | Las generas tú (§4) | **Sí** |
| App Android (push) | — | `androidApp/google-services.json` | Firebase → Configuración del proyecto → tu app Android | No se sube |
| Panel web (local) | `webPanel/.env.example` → `webPanel/.env.local` | `VITE_SUPABASE_URL` | La misma *Project URL* | No |
| | | `VITE_SUPABASE_ANON_KEY` | La misma clave *anon / publishable* | No |
| Panel web (Vercel) | Project → Settings → Environment Variables | `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` | Igual que arriba, en *Production* y *Preview* | No |
| Edge Function `notificar` | `supabase/functions/.env.example` → `supabase/functions/.env` | `WEBHOOK_SECRET` | Lo generas tú: `openssl rand -hex 32` | **Sí** |
| | | `FCM_SERVICE_ACCOUNT` | Firebase → Cuentas de servicio → *Generar nueva clave privada* (JSON en una línea) | **Sí** |
| Base de datos (Vault) | SQL Editor (§1) | `notificar_url` | `https://<ref>.supabase.co/functions/v1/notificar` | No |
| | | `notificar_secret` | El mismo valor que `WEBHOOK_SECRET` | **Sí** |
| GitHub Actions (secretos del repo) | Settings → Secrets and variables → Actions | `SUPABASE_URL`, `SUPABASE_ANON_KEY` | Para compilar el APK de release | No, pero van como secreto |
| | | `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | El keystore del §4 | **Sí** |
| | | `GOOGLE_SERVICES_JSON_BASE64` | `google-services.json` en base64 (opcional) | No se sube |
| | | `SUPABASE_DEMO_URL`, `SUPABASE_DEMO_ANON_KEY` | Para la Action de keep-alive (ADR-14) | No |

La clave **`service_role`** (o *secret key*) no aparece en ninguna de estas listas a propósito: Supabase la inyecta
sola en las Edge Functions y **nunca** se copia a la app, al panel, a Vercel ni a GitHub.

Convertir un archivo a base64 para un secreto de GitHub:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("civika-release.jks")) | Set-Clipboard
```

## 1. Supabase

- [ ] Crear el proyecto en la región más cercana (São Paulo) y guardar la contraseña de la base de datos en un gestor
  de contraseñas, no en el chat del grupo.
- [ ] Aplicar el esquema: `supabase login`, `supabase link --project-ref <ref>` y `supabase db push`. Después, cargar
  el seed (categorías, entidades, zonas y reglas de ruteo) pegando `supabase/seed.sql` en el SQL Editor.
  **No** cargues `supabase/demo.sql` en producción: son incidentes de ejemplo.
- [ ] Comprobar en *Database → Extensions* que están activas `postgis`, `pg_cron` y `pg_net`. Las migraciones las
  activan; si alguna no estaba disponible, la migración deja un aviso y sigue: sin `pg_cron` la prioridad no se
  recalcula cada día, y sin `pg_net` no hay push.
- [ ] **Storage:** bucket `fotos` **privado**, con `file_size_limit` de 2 MB y `allowed_mime_types` `image/jpeg` e
  `image/webp`. En la nube el bucket no lo crea `config.toml`: créalo en *Storage → New bucket* con esos límites.
- [ ] **Auth → URL Configuration:** *Site URL* con la URL de producción del panel; en *Redirect URLs*, la de
  producción y el patrón de vistas previas de Vercel (`https://*-<tu-equipo>.vercel.app/**`). **No** se añade
  `localhost`: para desarrollar se usa Supabase local.
- [ ] **Auth → Providers → Email:** confirmación de correo activada.
- [ ] **Auth → Attack Protection:** captcha (Turnstile o hCaptcha) en el registro. Ojo: al activarlo, la app y el
  panel tienen que enviar el token del captcha; hasta integrarlo, déjalo desactivado y apóyate en los límites de Auth.
- [ ] **Auth → Sign In / Providers → Email:** longitud mínima de contraseña en **8** (como exige la app; `config.toml`
  solo vale en local, en la nube se cambia aquí).
- [ ] **Auth → MFA:** TOTP activado. Sin él, moderación y entidad no pueden entrar al panel.
- [ ] **SMTP propio** (un proveedor con plan gratuito): el correo integrado de Supabase tiene límites muy bajos y
  suele caer en spam.
- [ ] **Edge Function `notificar`:** rellenar `supabase/functions/.env` y ejecutar
  `supabase secrets set --env-file supabase/functions/.env` y `supabase functions deploy notificar`.
- [ ] **Vault** (SQL Editor), para que la base de datos sepa a dónde llamar:

  ```sql
  select vault.create_secret('https://<ref>.supabase.co/functions/v1/notificar', 'notificar_url');
  select vault.create_secret('<el mismo valor que WEBHOOK_SECRET>', 'notificar_secret');
  ```

- [ ] Probar el push de punta a punta: cambiar un estado desde el panel y ver la respuesta en
  *Edge Functions → notificar → Logs* (`{"enviados": 1}`).

## 2. Firebase (notificaciones)

- [ ] Crear el proyecto de Firebase y registrar la app Android con el paquete `pe.civika.app`.
- [ ] Descargar `google-services.json` en `androidApp/` (está en `.gitignore`). Con ese archivo presente, el build
  activa Firebase Messaging; sin él, la app compila igual y solo usa la bandeja interna.
- [ ] Crear la cuenta de servicio para FCM HTTP v1 y guardar su JSON **solo** como el secreto `FCM_SERVICE_ACCOUNT`
  de la Edge Function.
- [ ] Para el APK del CI: guardar `google-services.json` en base64 como `GOOGLE_SERVICES_JSON_BASE64`.

## 3. Panel web en Vercel (ADR-18)

- [ ] *Add New → Project*, importar el repo y poner **Root Directory** `webPanel`. Vercel detecta Vite
  (`npm run build`, salida `dist`).
- [ ] En *Settings → Environment Variables*, definir `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY` para
  *Production* y *Preview*. Un cambio de variables necesita un *Redeploy*.
- [ ] Añadir la URL final a *Site URL* y *Redirect URLs* de Supabase (§1).
- [ ] Abrir el panel publicado, iniciar sesión con una cuenta de entidad y completar el alta del segundo factor.

## 4. APK de release

- [ ] Generar el keystore **una sola vez** y guardarlo en un lugar seguro (si se pierde, no se puede actualizar la
  app instalada):

  ```powershell
  keytool -genkeypair -v -keystore civika-release.jks -alias civika -keyalg RSA -keysize 4096 -validity 10000
  ```

- [ ] Para compilar en tu máquina: copiar `androidApp/keystore.properties.example` a `androidApp/keystore.properties`
  y rellenarlo. Luego `./gradlew :androidApp:assembleRelease` (falla con un mensaje claro si faltan
  `supabase.url` o `supabase.anonKey`).
- [ ] Para compilar en GitHub: cargar los secretos de la tabla del §0 y subir una etiqueta
  (`git tag v0.2.0 && git push origin v0.2.0`). El workflow `release.yml` publica el APK firmado como GitHub Release.
- [ ] `versionCode` sube en cada entrega; `versionName` sigue `0.MINOR.PATCH`.

## 5. Secretos y respaldos

- [ ] Activar *Secret scanning* y *Push protection* en GitHub (Settings → Code security).
- [ ] Si una clave se filtra: rotarla en el servicio, actualizar el secreto y avisar en el grupo.
- [ ] Respaldo semanal: `supabase db dump --data-only` y guardar el archivo **cifrado y fuera del repo** (por ejemplo,
  un `.7z` con contraseña en una carpeta privada). Contiene datos personales, y el plan gratis no tiene respaldos
  descargables.

## 6. Runbooks

### Crear una cuenta de entidad o de moderador
1. La persona se registra en la app como ciudadano y confirma su correo.
2. Pieers, en el SQL Editor del dashboard (que se ejecuta como el rol `postgres`, al que el trigger sí deja cambiar
   roles), actualiza `usuarios` con su `rol` y, si es personal de entidad, su `entidad_id`. El trigger deja un
   registro en `auditoria`.

   ```sql
   update public.usuarios set rol = 'entidad', entidad_id = <id de la entidad>
   where id = (select id from auth.users where email = '<correo>');
   -- o bien: set rol = 'moderador'
   ```

3. La persona entra al panel, activa el MFA (TOTP) cuando el panel se lo pide y comprueba que solo ve lo de su entidad.

### Desactivar una cuenta (alguien sale de la entidad)
1. En el SQL Editor: `activo = false` para ese usuario. **No se borra** la fila.
2. Comprobar en `auditoria` que quedó registrado y que el usuario ya no puede entrar al panel.

### El ciudadano pide eliminar su cuenta
1. Lo hace él mismo desde *Perfil → Eliminar mi cuenta* (`eliminar_mi_cuenta()`).
2. Si escribe por otro canal, se verifica su identidad por el correo registrado y, en el SQL Editor, se ejecuta
   `select public.eliminar_cuenta('<id del usuario>')`. Es la variante administrativa: `eliminar_mi_cuenta()` no sirve
   aquí porque depende de `auth.uid()`, que fuera de una sesión es nulo.

### Una foto expone a un menor o un dato sensible
1. Moderación oculta la foto desde el panel (queda en `auditoria`).
2. Además se **borra el archivo** del bucket, porque una URL firmada ya entregada sigue funcionando unos minutos.

### Alguien perdió su segundo factor
1. Verificar su identidad por un canal distinto del correo.
2. En *Authentication → Users*, abrir el usuario y eliminar su factor MFA. Al volver a entrar, el panel le pedirá
   dar de alta uno nuevo.

## 7. Plan de demo

- [ ] Restaurar el proyecto de Supabase el día anterior si estuviera pausado.
- [ ] Cargar los datos de demostración (unos 30 incidentes del campus con fotos propias).
- [ ] Ensayo completo en el celular real y en el panel publicado, con la red de la universidad.
- [ ] Notificación push probada en el mismo celular de la demo.
- [ ] Respaldo: video del flujo completo y capturas, por si falla la conexión.
