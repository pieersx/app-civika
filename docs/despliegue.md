# Despliegue y operación — Cívika

Checklist para dejar funcionando el entorno **demo** (el que se presenta) y los procedimientos manuales (*runbooks*)
del día a día. Marca cada casilla cuando esté hecha. El entorno **local** se describe en el [README](../README.md).

> Ningún valor secreto se escribe en este archivo ni en el repo. Aquí solo va **dónde** se guarda cada cosa.

## 1. Supabase (proyecto demo)

- [ ] Crear el proyecto en la región más cercana (São Paulo) y guardar la contraseña de la base de datos en un gestor
  de contraseñas, no en el chat del grupo.
- [ ] Aplicar las migraciones con `supabase link` y `supabase db push`, y después cargar el seed.
- [ ] **Auth → URL Configuration:** *Site URL* con la URL de producción del panel; en *Redirect URLs*, la de
  producción y el patrón de vistas previas de Vercel. **No** se añade `localhost` al proyecto demo: para desarrollar
  se usa Supabase local (`supabase start`), que ya acepta `localhost`.
- [ ] **Auth → Providers → Email:** confirmación de correo activada.
- [ ] **Auth → Attack Protection:** captcha (Turnstile o hCaptcha) en el registro.
- [ ] **Auth → MFA:** TOTP activado (lo exigen las políticas de moderador y entidad).
- [ ] **SMTP propio** (por ejemplo, un proveedor con plan gratuito): el correo integrado de Supabase tiene límites
  bajos y suele caer en spam.
- [ ] Storage: bucket `fotos` privado, con `file_size_limit` de 2 MB y `allowed_mime_types` `image/jpeg` e `image/webp`.
- [ ] Secretos de las Edge Functions (`supabase secrets set`): credenciales de FCM. La `service_role` nunca sale de aquí.
- [ ] Secreto del repo `SUPABASE_DEMO_URL` + `SUPABASE_DEMO_ANON_KEY` para la Action de keep-alive (ADR-14).

## 2. Firebase (notificaciones)

- [ ] Crear el proyecto de Firebase y registrar la app Android con el paquete `pe.civika.app`.
- [ ] Descargar `google-services.json` en `androidApp/` (está en `.gitignore`).
- [ ] En el CI, guardarlo como secreto (`GOOGLE_SERVICES_JSON`, en base64) y escribirlo en el job antes de compilar.
- [ ] Crear la cuenta de servicio para FCM HTTP v1 y guardar su JSON **solo** como secreto de la Edge Function `notificar`.

## 3. Panel web en Vercel (ADR-16)

- [ ] Crear el proyecto en Vercel **sin** conectar el build automático (Vercel no puede compilar Kotlin).
- [ ] Guardar en GitHub los secretos `VERCEL_TOKEN`, `VERCEL_ORG_ID` y `VERCEL_PROJECT_ID`.
- [ ] Workflow: compilar `:webPanel:wasmJsBrowserDistribution`, preparar `.vercel/output` y ejecutar
  `vercel deploy --prebuilt` (`--prod` en `main`; vista previa en los PR).
- [ ] Comprobar que los `.wasm` se sirven con `Content-Type: application/wasm` (pestaña Network de Chrome DevTools).
- [ ] Añadir la URL final a *Redirect URLs* de Supabase (sección 1).

## 4. APK de release

- [ ] Generar el keystore **una sola vez** y guardarlo en un lugar seguro: si se pierde, no se puede actualizar la app
  instalada.
- [ ] Secretos del repo: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` y `ANDROID_KEY_PASSWORD`.
- [ ] `versionCode` sube en cada entrega; `versionName` sigue `0.MINOR.PATCH`.
- [ ] Publicar el APK firmado como GitHub Release para que los compañeros y los usuarios de prueba lo instalen.

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
3. La persona inicia sesión de nuevo, activa el MFA (TOTP) y comprueba que solo ve lo de su entidad.

### Desactivar una cuenta (alguien sale de la entidad)
1. En el SQL Editor: `activo = false` para ese usuario. **No se borra** la fila.
2. Comprobar en `auditoria` que quedó registrado y que el usuario ya no puede entrar al panel.

### El ciudadano pide eliminar su cuenta
1. Lo hace él mismo desde *Perfil → Eliminar mi cuenta* (`eliminar_mi_cuenta()`).
2. Si escribe por otro canal, se verifica su identidad por el correo registrado y, en el SQL Editor, se ejecuta
   `eliminar_cuenta('<id del usuario>')`. Es la variante administrativa: `eliminar_mi_cuenta()` no sirve aquí porque
   depende de `auth.uid()`, que fuera de una sesión es nulo.

### Una foto expone a un menor o un dato sensible
1. Moderación oculta la foto desde el panel (queda en `auditoria`).
2. Además se **borra el archivo** del bucket, porque una URL firmada ya entregada sigue funcionando unos minutos.

## 7. Plan de demo

- [ ] Restaurar el proyecto de Supabase el día anterior si estuviera pausado.
- [ ] Cargar los datos de demostración (unos 30 incidentes del campus con fotos propias).
- [ ] Ensayo completo en el celular real y en el panel publicado, con la red de la universidad.
- [ ] Notificación push probada en el mismo celular de la demo.
- [ ] Respaldo: video del flujo completo y capturas, por si falla la conexión.
