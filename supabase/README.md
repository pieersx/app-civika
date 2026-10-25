# supabase/

Backend de Cívika sobre Supabase: PostgreSQL + PostGIS, Auth, Storage y Edge Functions.

- `migrations/`: esquema SQL versionado (extensiones, tablas, triggers, RLS, vistas y RPC base).
- `seed.sql`: categorías, entidades, zonas del piloto (Ciudad Universitaria de la UNMSM y su entorno) y reglas de ruteo.
- `tests/`: pruebas pgTAP de esquema, seed, RLS y RPC. Se ejecutan con `supabase test db`.
- `functions/`: edge functions (por ejemplo, envío de notificaciones FCM). **Todavía no existe**: se crea en el Sprint 3.

## Entorno local

Requiere Docker y la [Supabase CLI](https://supabase.com/docs/guides/cli).

```bash
supabase start          # levanta Postgres, Auth, Storage, Studio y aplica migraciones + seed
supabase db reset       # vuelve a aplicar migraciones y seed desde cero
supabase test db        # ejecuta supabase/tests/*.sql con pgTAP
supabase stop           # detiene los contenedores
```

La API local queda en `http://127.0.0.1:54321` y Studio en `http://127.0.0.1:54323`. Las claves locales salen de
`supabase status`; no se copian al repo.

El bucket privado `fotos` se declara en `config.toml` y se crea al levantar el entorno. Los paths deben empezar por
el uid del usuario (`fotos/{uid}/...`) y `crear_reporte` valida que el objeto exista y que su `owner` sea el usuario
autenticado. La API no expone `storage_path` a `authenticated`; la descarga de fotos se hará con la RPC `urls_fotos`
(Sprint 2).

## Migraciones

Las migraciones son la única fuente del esquema. Para cambiar el modelo:

```bash
supabase migration new nombre_del_cambio
# editar el archivo generado en supabase/migrations/
supabase db reset
supabase test db
```

No se edita el esquema desde Studio ni se suben cambios manuales a producción. El orden actual es:

1. `esquema_base`: enums, tablas, índices, triggers, prioridad y funciones internas.
2. `rls_y_permisos`: RLS, `GRANT` explícitos, helpers de visibilidad y vista `reportes_entidad`.
3. `rpc_base`: `buscar_duplicados`, `crear_reporte`, `cambiar_estado` y lecturas públicas.
4. `privacidad_fotos_historial`: oculta `storage_path` y `autor_id` de `authenticated` y valida el dueño de cada foto.
5. `mfa_restrictivo`: política restrictiva que exige `aal2` (segundo factor) a moderación y entidad.

**Estado de las RPC (funciones que llama la app):**

| Ya existen | Pendientes (están en `docs/arquitectura.md`) |
|---|---|
| `buscar_duplicados`, `crear_reporte`, `cambiar_estado`, `listar_incidentes_publicos`, `historial_publico`, `comentarios_publicos` | `apoyar_incidente`, `eliminar_mi_cuenta`, `eliminar_cuenta`, `urls_fotos`, recálculo diario de prioridad con `pg_cron` y políticas de subida en `storage.objects` |

Las tablas nuevas **no** se exponen automáticamente a la Data API. Cada tabla necesita `GRANT` explícito; las
escrituras con reglas de negocio pasan por RPC `security definer`, no por `UPDATE` directo.

## Pruebas

`supabase test db` levanta las pruebas de `supabase/tests/` contra Postgres local. Cubren:

- seed y bucket de Storage;
- `crear_reporte` (ruteo, idempotencia, ubicación simulada, fuera de zona y fotos);
- `buscar_duplicados` (metros, radio y categoría);
- `cambiar_estado` (rol, entidad, versión, transiciones, MFA y auditoría);
- RLS de entidades A/B y lectura pública sin datos personales;
- privacidad de fotos e historial: columnas ocultas, dueño del objeto y versiones archivadas.

La CI ejecuta el mismo comando en el job `Base de datos (pgTAP)`.

## Entorno demo

El proyecto demo de Supabase se mantiene activo con `.github/workflows/keep-alive.yml` (ADR-14). El workflow hace una
consulta semanal de solo lectura a `categorias` con la clave `anon`. Configura estos secretos en GitHub:

- `SUPABASE_DEMO_URL`: URL del proyecto demo.
- `SUPABASE_DEMO_ANON_KEY`: clave anon del proyecto demo.

Las claves de servicio, la contraseña de la base y los secretos de Auth **no** van en el repo ni en el chat del grupo.
La puesta en marcha del proyecto real está en [docs/despliegue.md](../docs/despliegue.md).
