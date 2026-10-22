# Registro de riesgos — Cívika

Qué puede salir mal al desarrollar, operar y desplegar Cívika, y qué hacemos para evitarlo. Se revisa al final de
cada sprint: se marca lo mitigado y se añade lo nuevo.

Responsables: **P** Pieers · **E** Emily · **V** Valeria · **K** Keyla · **A** Alex · **Todos**.

## 1. Desarrollo

| Riesgo | Qué pasa | Mitigación | Resp. | Sprint |
|---|---|---|---|---|
| Alcance demasiado grande | 16 RF, 5 personas y 8 semanas; meter extras rompe el plazo | Nada de cobros, SSO, permisos granulares ni panel de admin en el MVP; recorte en el orden del plan | P | Todos |
| Mockup sin aprobar | Se programan pantallas dos veces | Aprobar el mockup antes de pulir pantallas (issue #2) | E | 1 |
| Backend vacío | Las pantallas no tienen contra qué conectarse | Migraciones, RLS, seed y pgTAP primero (issue #1); mientras tanto, repos fake | P | 1 |
| Auth en Wasm y Android | Confirmación de correo, recuperación, sesión y refresco fallan en un solo cliente | Probar login, logout, refresco y recuperación en local y en demo, en ambos clientes | E, A | 2 |
| Correos de Supabase | El correo integrado tiene límites bajos y cae en spam | SMTP propio antes de las pruebas con usuarios (despliegue.md) | P | 3 |
| Notificaciones en Android 13+ | Vienen apagadas por defecto; sin permiso no llega nada | Pedir `POST_NOTIFICATIONS` después del primer reporte; probar token nuevo, logout y token caducado | A | 3 |
| Sin conexión | Reportes duplicados o perdidos | `cliente_id` + `ON CONFLICT` (ADR-6), reintentos de WorkManager, prueba con modo avión | V | 3 |
| Ubicación falsa | Reportes con GPS simulado | Marcar `ubicacion_simulada` y mandar a revisión (arquitectura §6) | V | 3 |
| Galería y permisos | Pedir acceso a todas las fotos asusta y Play lo restringe | Photo Picker, que no necesita permiso | V | 2 |
| Nominatim | Bloquea si se pasa de 1 consulta por segundo o sin User-Agent | Una consulta por reporte confirmado, caché en Room y User-Agent propio (ADR-3) | V | 2 |
| OpenFreeMap sin SLA | El mapa no carga si el servicio cae | Mensaje claro en el mapa y la URL del estilo configurable para cambiar de proveedor (ADR-3) | K | 2 |
| Compose Web en Beta | Fallan entradas de texto, navegación o accesibilidad | Probar en Chrome, Edge y Firefox recientes; simplificar la pantalla si algo falla (ADR-9) | A | 2 |
| Navegadores sin WasmGC | El panel no abre en navegadores antiguos (Safari < 18.2) | Mensaje de «navegador no compatible» en `index.html`; la entidad usa Chrome o Edge | A | 3 |
| Pruebas del panel sobre canvas | Playwright y Lighthouse no ven los elementos | Capturas comparadas y pocos E2E de flujos clave (ADR-15) | A | 2 |
| CI incompleto | Se fusiona algo que rompe la base de datos | Añadir `supabase test db` al CI junto con las migraciones | P | 1 |
| Zona horaria | «Días abierto» e indicadores desfasados en un día | UTC en la base de datos y cálculos en `America/Lima` (arquitectura §11) | P | 2 |
| APK viejo frente al esquema nuevo | Un celular con la versión anterior deja de funcionar | RPC versionadas (`_v1`, `_v2`) (arquitectura §11) | P | 3 |
| Dependencias desactualizadas | Ejemplos de APIs viejas y conflictos de versiones | context7 antes de cada dependencia y Dependabot semanal | Todos | Todos |
| Curva de aprendizaje de KMP | Retrasos al empezar | El esqueleto ya funciona con ejemplos; cada uno parte de una pantalla existente | P | 1 |
| ML Kit acierta poco con baches | La sugerencia confunde más de lo que ayuda | Medir el acierto y apagarla si es menor al 60 % (ADR-13) | V | 4 |

## 2. Permisos y reglas de negocio

| Riesgo | Qué pasa | Mitigación | Resp. | Sprint |
|---|---|---|---|---|
| Fuga entre entidades | La entidad A ve o cambia incidentes de B | RLS por `entidad_id` y pgTAP con entidades A y B (arquitectura §7.3) | P | 2 |
| Vista que salta el RLS | Una vista expone datos que la tabla protege | Lectura pública por RPC con columnas seguras; vistas internas con `security_invoker = true` | P | 1 |
| RPC `security definer` insegura | Escalada de privilegios | `search_path = ''`, `revoke execute` y validar rol y entidad dentro de la función | P | 1 |
| Rol desde el token | Un usuario se pone «moderador» en `user_metadata` | El rol sale siempre de `usuarios` con `rol_actual()` | P | 1 |
| Autoasignarse un rol | Alguien edita su fila y se vuelve moderador | Trigger que bloquea `rol`, `entidad_id` y `activo` salvo para `service_role` y `postgres` | P | 1 |
| Borrar una cuenta borra sus reportes | Desaparecen incidentes e indicadores al eliminar una cuenta | `usuarios` queda como lápida anónima, sin clave foránea en cascada hacia `auth.users` (arquitectura §7.2) | P | 3 |
| Sin auditoría | No se sabe quién rechazó, ocultó o cambió un rol | Tabla `auditoria` solo de inserción (arquitectura §6) | P | 2 |
| Cuentas de entidad mal creadas | Personal con la entidad equivocada | Runbook de alta en despliegue.md y revisión de `auditoria` | P | 3 |
| Cuenta de moderador robada | Alguien oculta o rechaza a su antojo | MFA obligatorio para moderadores y entidad (arquitectura §7.2) | P | 3 |
| Moderación arbitraria | Se rechazan reportes sin criterio | Política de moderación escrita: motivos, plazo de respuesta y aviso al autor | E | 3 |
| Comentarios ambiguos | La entidad escribe algo interno que ve el ciudadano | Tabla `comentarios` con visibilidad `publico` o `interno` | P, A | 2 |
| Dos cambios de estado a la vez | El segundo pisa al primero | `version` y concurrencia optimista en `cambiar_estado` | P | 2 |
| Reportes falsos o masivos | Spam y pérdida de confianza | Límite de 10 por hora, captcha, correo verificado y denuncias | P | 3 |

## 3. Despliegue y operación

| Riesgo | Qué pasa | Mitigación | Resp. | Sprint |
|---|---|---|---|---|
| Supabase se pausa | La demo no carga tras 7 días sin uso | Keep-alive semanal y restaurar desde el dashboard antes de presentar (ADR-14) | P | 1 |
| Límites del plan gratis | A los 500 MB de base de datos queda en solo lectura; 10 GB de egress al mes | Fotos de 2 MB como máximo, monitorear el uso y pasar a Pro si el piloto crece | P | 4 |
| Sin respaldos en el plan gratis | Se pierde la base de datos | `supabase db dump` semanal guardado fuera del proyecto (despliegue.md) | P | 2 |
| Login falla en el panel publicado | Las URLs de Vercel no están permitidas en Auth | Registrar las URLs de producción y de vista previa en Supabase Auth (ADR-16) | A | 3 |
| APK sin firmar | No se puede instalar como actualización | Keystore guardado como secreto y `versionCode` creciente (despliegue.md) | P | 3 |
| `google-services.json` en el CI | El build falla en GitHub o el archivo se filtra | Archivo local ignorado y secreto en el CI (despliegue.md) | A | 3 |
| Secretos filtrados en el repo público | Alguien usa la `service_role` o la cuenta de Firebase | `.gitignore`, secret scanning de GitHub, rotación inmediata si se filtra | Todos | Todos |
| Sin visibilidad de errores | No se sabe por qué falla en la demo | Logs de Supabase y Edge Functions; mensajes de error claros en la app | P | 3 |
| La demo falla en vivo | Sin internet, sin push o sin batería | Ensayo general, datos seed, video y capturas de respaldo (despliegue.md) | Todos | 4 |

## 4. Legal y privacidad (Ley 29733)

| Riesgo | Qué pasa | Mitigación | Resp. | Sprint |
|---|---|---|---|---|
| Consentimiento no registrado | No se puede demostrar que el usuario aceptó | Tabla `consentimientos` con la versión de la política | E, P | 2 |
| Rostros, placas o menores en fotos | Se exponen datos personales de terceros | Aviso al tomar la foto, moderación y ocultamiento; las fotos con menores se borran del bucket | V, P | 3 |
| Ubicación exacta revela un domicilio | Se puede saber dónde vive quien reportó | Coordenadas redondeadas en lo público (arquitectura §11) | P | 2 |
| Derecho de cancelación | El ciudadano pide borrar sus datos | `eliminar_mi_cuenta()` con anonimización (arquitectura §7.2) | P | 3 |
| Retención indefinida | Se guardan fotos y datos sin límite | Política de retención: fotos de incidentes resueltos se archivan o borran tras 12 meses | P | 4 |
| Banco de datos personales | Un piloto real requiere inscribirlo ante la Autoridad Nacional de Protección de Datos Personales (ANPD) | En el curso, solo se menciona en el informe; en un piloto real, lo gestiona la entidad responsable | P | — |
| Política de privacidad ausente | La app pide consentimiento sin documento que lo respalde | Página de privacidad enlazada desde la bienvenida | E | 3 |
