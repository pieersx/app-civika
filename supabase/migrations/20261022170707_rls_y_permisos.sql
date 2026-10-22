-- RLS, privilegios y vistas internas (docs/arquitectura.md §7).
-- Regla: el RLS decide filas; los GRANT deciden si la tabla/columna es accesible.
-- Las escrituras con reglas de negocio pasan por RPC, no por UPDATE directo.

alter table public.usuarios enable row level security;
alter table public.zonas enable row level security;
alter table public.categorias enable row level security;
alter table public.entidades enable row level security;
alter table public.reglas_ruteo enable row level security;
alter table public.incidentes enable row level security;
alter table public.reportes enable row level security;
alter table public.fotos enable row level security;
alter table public.apoyos enable row level security;
alter table public.historial_estados enable row level security;
alter table public.denuncias enable row level security;
alter table public.verificaciones enable row level security;
alter table public.notificaciones enable row level security;
alter table public.dispositivos enable row level security;
alter table public.auditoria enable row level security;
alter table public.comentarios enable row level security;
alter table public.consentimientos enable row level security;

-- Supabase concede privilegios amplios por defecto a las tablas nuevas del
-- esquema public. Se revocan y se vuelven a conceder solo los necesarios.
revoke all on all tables in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;
revoke all on all functions in schema public from anon, authenticated;

-- Helpers de visibilidad. Están en private para que PostgREST no los exponga;
-- evitan que las políticas tengan que conceder columnas sensibles como usuario_id.
create or replace function private.puede_ver_incidente(p_incidente_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select
        (select private.rol_actual()) = 'moderador'
        or (
            (select private.rol_actual()) = 'entidad'
            and exists (
                select 1
                from public.incidentes i
                where i.id = p_incidente_id
                  and i.entidad_id = (select private.entidad_actual())
            )
        )
$$;

create or replace function private.puede_ver_reporte(p_reporte_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select
        exists (
            select 1
            from public.reportes r
            where r.id = p_reporte_id
              and r.usuario_id = auth.uid()
        )
        or (select private.rol_actual()) = 'moderador'
        or (
            (select private.rol_actual()) = 'entidad'
            and exists (
                select 1
                from public.reportes r
                join public.incidentes i on i.id = r.incidente_id
                where r.id = p_reporte_id
                  and i.entidad_id = (select private.entidad_actual())
            )
        )
$$;

create or replace function private.es_entidad_del_incidente(p_incidente_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select
        (select private.rol_actual()) = 'entidad'
        and exists (
            select 1
            from public.incidentes i
            where i.id = p_incidente_id
              and i.entidad_id = (select private.entidad_actual())
        )
$$;

revoke execute on function private.puede_ver_incidente(uuid) from public;
revoke execute on function private.puede_ver_reporte(uuid) from public;
revoke execute on function private.es_entidad_del_incidente(uuid) from public;
grant execute on function private.puede_ver_incidente(uuid) to authenticated;
grant execute on function private.puede_ver_reporte(uuid) to authenticated;
grant execute on function private.es_entidad_del_incidente(uuid) to authenticated;

-- Catálogos públicos.
grant select on public.zonas, public.categorias, public.entidades to anon, authenticated;

create policy zonas_lectura_publica on public.zonas
for select to anon, authenticated using (true);

create policy categorias_lectura_publica on public.categorias
for select to anon, authenticated using (true);

create policy entidades_lectura_publica on public.entidades
for select to anon, authenticated using (true);

-- Usuarios: cada uno ve su fila; el moderador puede leer todas.
grant select on public.usuarios to authenticated;
grant update (nombre, zona_id) on public.usuarios to authenticated;

create policy usuarios_lectura_propia on public.usuarios
for select to authenticated
using ((select auth.uid()) = id or (select private.rol_actual()) = 'moderador');

create policy usuarios_actualizacion_propia on public.usuarios
for update to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

-- Incidentes: la entidad solo ve los de su entidad; moderación ve todos.
-- El ciudadano y el anónimo leen por las RPC públicas.
grant select on public.incidentes to authenticated;

create policy incidentes_lectura_entidad_o_moderador on public.incidentes
for select to authenticated
using (private.puede_ver_incidente(id));

-- Reportes: el ciudadano ve los propios; la entidad, los de su entidad;
-- moderación, todos. No se concede usuario_id a la entidad.
grant select (id, incidente_id, tipo, descripcion, geom, categoria_sugerida, confianza, ubicacion_simulada, creado_en)
    on public.reportes to authenticated;

create policy reportes_lectura on public.reportes
for select to authenticated
using (private.puede_ver_reporte(id));

-- La vista interna no expone usuario_id. security_invoker hace que se aplique
-- el RLS de reportes e incidentes con el rol de quien consulta.
create view public.reportes_entidad
with (security_invoker = true)
as
select
    r.id,
    r.incidente_id,
    i.codigo,
    r.tipo,
    r.descripcion,
    r.geom,
    r.categoria_sugerida,
    r.confianza,
    r.ubicacion_simulada,
    r.creado_en
from public.reportes r
join public.incidentes i on i.id = r.incidente_id;

revoke all on public.reportes_entidad from anon, authenticated;
grant select on public.reportes_entidad to authenticated;

-- Fotos: solo el dueño del reporte, su entidad o moderación.
grant select (id, reporte_id, storage_path, oculta) on public.fotos to authenticated;

create policy fotos_lectura on public.fotos
for select to authenticated
using (private.puede_ver_reporte(reporte_id));

-- Apoyos: el ciudadano ve los suyos y puede retirarlos; moderación lee.
grant select (usuario_id, incidente_id, creado_en) on public.apoyos to authenticated;
grant delete on public.apoyos to authenticated;

create policy apoyos_lectura on public.apoyos
for select to authenticated
using (usuario_id = (select auth.uid()) or (select private.rol_actual()) = 'moderador');

create policy apoyos_borrado_propio on public.apoyos
for delete to authenticated
using (usuario_id = (select auth.uid()));

-- Historial: la entidad ve el de sus incidentes; moderación ve todo.
grant select (id, incidente_id, estado_anterior, estado_nuevo, comentario, autor_id, creado_en)
    on public.historial_estados to authenticated;

create policy historial_lectura on public.historial_estados
for select to authenticated
using (private.puede_ver_incidente(incidente_id));

-- Denuncias: el ciudadano crea y ve las suyas; moderación las gestiona.
grant select (id, reporte_id, usuario_id, motivo, resolucion, creado_en) on public.denuncias to authenticated;
grant insert (reporte_id, usuario_id, motivo) on public.denuncias to authenticated;
grant update (resolucion) on public.denuncias to authenticated;

create policy denuncias_lectura on public.denuncias
for select to authenticated
using (usuario_id = (select auth.uid()) or (select private.rol_actual()) = 'moderador');

create policy denuncias_insercion_propia on public.denuncias
for insert to authenticated
with check (usuario_id = (select auth.uid()));

create policy denuncias_actualizacion_moderador on public.denuncias
for update to authenticated
using ((select private.rol_actual()) = 'moderador')
with check ((select private.rol_actual()) = 'moderador');

-- Verificaciones: solo el ciudadano dueño; moderación lee.
grant select (usuario_id, incidente_id, sigue_resuelto, creado_en) on public.verificaciones to authenticated;
grant insert (usuario_id, incidente_id, sigue_resuelto) on public.verificaciones to authenticated;
grant update (sigue_resuelto) on public.verificaciones to authenticated;

create policy verificaciones_lectura on public.verificaciones
for select to authenticated
using (usuario_id = (select auth.uid()) or (select private.rol_actual()) = 'moderador');

create policy verificaciones_insercion_propia on public.verificaciones
for insert to authenticated
with check (usuario_id = (select auth.uid()));

create policy verificaciones_actualizacion_propia on public.verificaciones
for update to authenticated
using (usuario_id = (select auth.uid()))
with check (usuario_id = (select auth.uid()));

-- Bandeja de notificaciones y tokens FCM.
grant select (id, usuario_id, incidente_id, tipo, leida, creado_en) on public.notificaciones to authenticated;
grant update (leida) on public.notificaciones to authenticated;

create policy notificaciones_lectura_propia on public.notificaciones
for select to authenticated
using (usuario_id = (select auth.uid()));

create policy notificaciones_actualizacion_propia on public.notificaciones
for update to authenticated
using (usuario_id = (select auth.uid()))
with check (usuario_id = (select auth.uid()));

grant select, insert, update, delete on public.dispositivos to authenticated;

create policy dispositivos_propios on public.dispositivos
for all to authenticated
using (usuario_id = (select auth.uid()))
with check (usuario_id = (select auth.uid()));

-- Comentarios: la entidad lee y escribe los de sus incidentes; moderación lee.
-- El ciudadano y el anónimo leen los públicos por la RPC comentarios_publicos.
grant select (id, incidente_id, autor_id, visibilidad, texto, creado_en) on public.comentarios to authenticated;
grant insert (incidente_id, autor_id, visibilidad, texto) on public.comentarios to authenticated;

create policy comentarios_lectura on public.comentarios
for select to authenticated
using (private.puede_ver_incidente(incidente_id));

create policy comentarios_insercion_entidad on public.comentarios
for insert to authenticated
with check (
    autor_id = (select auth.uid())
    and private.es_entidad_del_incidente(incidente_id)
);

-- Consentimientos: solo los propios.
grant select, insert, update on public.consentimientos to authenticated;

create policy consentimientos_propios on public.consentimientos
for all to authenticated
using (usuario_id = (select auth.uid()))
with check (usuario_id = (select auth.uid()));

-- Auditoría: solo el moderador. Los triggers la escriben como postgres.
grant select on public.auditoria to authenticated;

create policy auditoria_lectura_moderador on public.auditoria
for select to authenticated
using ((select private.rol_actual()) = 'moderador');

-- Reglas de ruteo: sin acceso desde la app; solo administración con service_role.
revoke all on public.reglas_ruteo from anon, authenticated;
