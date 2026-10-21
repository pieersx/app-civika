# supabase/

Aquí irá el backend de Cívika sobre Supabase: PostgreSQL + PostGIS, Auth, Storage y Edge Functions.

- `migrations/`: esquema SQL versionado (tablas, RLS y funciones RPC de duplicados, ruteo y prioridad).
- `seed.sql`: categorías, entidades y polígono del distrito piloto.
- `functions/`: edge functions (por ejemplo, envío de notificaciones FCM).

Se gestiona con la [Supabase CLI](https://supabase.com/docs/guides/cli). Las claves van en `.env` y nunca se suben al repo.
