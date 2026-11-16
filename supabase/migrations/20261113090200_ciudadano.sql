-- Lo que le faltaba a la app del ciudadano: detalle, «mis reportes», denuncias (RF-16),
-- verificación «¿sigue arreglado?», bandeja de notificaciones (RF-11), tokens FCM,
-- consentimiento al registrarse y eliminación de la cuenta (Ley 29733).

-- ---------------------------------------------------------------------------
-- Detalle público de un incidente. Sin autor ni dirección exacta. Un incidente
-- rechazado solo lo ve quien lo reportó, con el motivo.
-- ---------------------------------------------------------------------------
create or replace function public.detalle_incidente(p_incidente_id uuid)
returns table (
    id uuid,
    codigo text,
    titulo text,
    descripcion text,
    categoria text,
    categoria_id integer,
    estado public.estado,
    prioridad integer,
    apoyos_count integer,
    zona text,
    entidad text,
    lat double precision,
    lon double precision,
    creado_en timestamptz,
    plazo_objetivo_dias integer,
    yo_apoyo boolean,
    es_mio boolean,
    mi_verificacion boolean,
    motivo_rechazo text
)
language sql
stable
security definer
set search_path = ''
as $$
    with yo as (
        select (select auth.uid()) as uid
    ),
    original as (
        select r.usuario_id, r.descripcion
        from public.reportes r
        where r.incidente_id = p_incidente_id and r.tipo = 'original'
        order by r.creado_en asc
        limit 1
    )
    select
        i.id,
        i.codigo,
        coalesce(
            (select pg_catalog.left(pg_catalog.regexp_replace(o.descripcion, '\s+', ' ', 'g'), 80) from original o),
            c.nombre
        ),
        (select o.descripcion from original o),
        c.nombre,
        i.categoria_id,
        i.status,
        i.prioridad,
        i.apoyos_count,
        z.nombre,
        e.nombre,
        pg_catalog.round(extensions.ST_Y(i.geom::extensions.geometry)::numeric, 4)::double precision,
        pg_catalog.round(extensions.ST_X(i.geom::extensions.geometry)::numeric, 4)::double precision,
        i.creado_en,
        c.plazo_objetivo_dias,
        exists (
            select 1 from public.apoyos a, yo
            where a.incidente_id = i.id and a.usuario_id = yo.uid
        ),
        coalesce((select o.usuario_id = yo.uid from original o, yo), false),
        (
            select v.sigue_resuelto from public.verificaciones v, yo
            where v.incidente_id = i.id and v.usuario_id = yo.uid
        ),
        case when i.status = 'rechazado' then (
            select h.comentario
            from public.historial_estados h
            where h.incidente_id = i.id and h.estado_nuevo = 'rechazado'
            order by h.creado_en desc
            limit 1
        ) end
    from public.incidentes i
    join public.categorias c on c.id = i.categoria_id
    join public.zonas z on z.id = i.zona_id
    left join public.entidades e on e.id = i.entidad_id
    where i.id = p_incidente_id
      and (
          i.status <> 'rechazado'
          or coalesce((select o.usuario_id = yo.uid from original o, yo), false)
      );
$$;

revoke execute on function public.detalle_incidente(uuid) from public, anon;
grant execute on function public.detalle_incidente(uuid) to anon, authenticated;

-- ---------------------------------------------------------------------------
-- «Mis reportes» (RF-12): incidentes que el usuario reportó o apoyó.
-- ---------------------------------------------------------------------------
create or replace function public.mis_incidentes()
returns table (
    id uuid,
    codigo text,
    titulo text,
    categoria text,
    categoria_id integer,
    estado public.estado,
    apoyos_count integer,
    zona text,
    lat double precision,
    lon double precision,
    creado_en timestamptz,
    relacion text,
    desde timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    with mios as (
        select r.incidente_id,
               pg_catalog.bool_or(r.tipo = 'original') as autor,
               pg_catalog.min(r.creado_en) as desde
        from public.reportes r
        where r.usuario_id = (select auth.uid())
        group by r.incidente_id
        union all
        select a.incidente_id, false, a.creado_en
        from public.apoyos a
        where a.usuario_id = (select auth.uid())
    ),
    seguidos as (
        select m.incidente_id,
               pg_catalog.bool_or(m.autor) as autor,
               pg_catalog.min(m.desde) as desde
        from mios m
        group by m.incidente_id
    )
    select
        i.id,
        i.codigo,
        coalesce(
            (
                select pg_catalog.left(pg_catalog.regexp_replace(r.descripcion, '\s+', ' ', 'g'), 80)
                from public.reportes r
                where r.incidente_id = i.id and r.tipo = 'original'
                order by r.creado_en asc
                limit 1
            ),
            c.nombre
        ),
        c.nombre,
        i.categoria_id,
        i.status,
        i.apoyos_count,
        z.nombre,
        pg_catalog.round(extensions.ST_Y(i.geom::extensions.geometry)::numeric, 4)::double precision,
        pg_catalog.round(extensions.ST_X(i.geom::extensions.geometry)::numeric, 4)::double precision,
        i.creado_en,
        case when s.autor then 'autor' else 'apoyo' end,
        s.desde
    from seguidos s
    join public.incidentes i on i.id = s.incidente_id
    join public.categorias c on c.id = i.categoria_id
    join public.zonas z on z.id = i.zona_id
    where s.autor or i.status <> 'rechazado'
    order by s.desde desc
    limit 200;
$$;

revoke execute on function public.mis_incidentes() from public, anon;
grant execute on function public.mis_incidentes() to authenticated;

-- ---------------------------------------------------------------------------
-- Denuncias (RF-16). Una por persona y reporte. Con 3 denuncias pendientes de
-- vecinos distintos, el Sistema envía el incidente a En revisión (docs/arquitectura.md §5).
-- El umbral evita que una sola persona saque un incidente del flujo de la entidad.
-- ---------------------------------------------------------------------------
create unique index if not exists denuncias_una_por_usuario_idx
    on public.denuncias (reporte_id, usuario_id);

create or replace function public.denunciar_incidente(p_incidente_id uuid, p_motivo text)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
    v_estado public.estado;
    v_reporte_id uuid;
    v_autor uuid;
    v_pendientes integer;
begin
    if v_uid is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;
    if (select private.rol_actual()) is distinct from 'ciudadano'::public.rol then
        raise exception 'Solo un ciudadano con la cuenta activa puede denunciar' using errcode = '42501';
    end if;
    if p_motivo is null or length(pg_catalog.btrim(p_motivo)) < 3 or length(p_motivo) > 500 then
        raise exception 'El motivo debe tener entre 3 y 500 caracteres' using errcode = '22023';
    end if;

    select i.status into v_estado
    from public.incidentes i
    where i.id = p_incidente_id
    for update;

    if not found or v_estado = 'rechazado' then
        raise exception 'Incidente no encontrado' using errcode = '22023';
    end if;

    select r.id, r.usuario_id into v_reporte_id, v_autor
    from public.reportes r
    where r.incidente_id = p_incidente_id and r.tipo = 'original'
    order by r.creado_en asc
    limit 1;

    if v_reporte_id is null then
        raise exception 'Incidente no encontrado' using errcode = '22023';
    end if;
    if v_autor = v_uid then
        raise exception 'No puedes denunciar tu propio reporte' using errcode = '22023';
    end if;

    insert into public.denuncias (reporte_id, usuario_id, motivo)
    values (v_reporte_id, v_uid, pg_catalog.btrim(p_motivo))
    on conflict do nothing;

    select count(*) into v_pendientes
    from public.denuncias d
    join public.reportes r on r.id = d.reporte_id
    where r.incidente_id = p_incidente_id and d.resolucion is null;

    if v_pendientes >= 3 and v_estado in ('derivado', 'en_atencion') then
        update public.incidentes i
        set status = 'en_revision'
        where i.id = p_incidente_id;

        insert into public.historial_estados (incidente_id, estado_anterior, estado_nuevo, comentario, autor_id)
        values (p_incidente_id, v_estado, 'en_revision', 'Enviado a revisión por denuncias de vecinos', null);
        return true;
    end if;
    return false;
end;
$$;

revoke execute on function public.denunciar_incidente(uuid, text) from public, anon;
grant execute on function public.denunciar_incidente(uuid, text) to authenticated;

-- ---------------------------------------------------------------------------
-- Verificación «¿sigue arreglado?». Con 3 «sigue igual» el incidente se reabre.
-- El trigger cubre también la escritura directa que ya permitía el RLS.
-- ---------------------------------------------------------------------------
create or replace function private.reabrir_si_sigue_igual()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_negativas integer;
begin
    if new.sigue_resuelto then
        return new;
    end if;

    select count(*) into v_negativas
    from public.verificaciones v
    where v.incidente_id = new.incidente_id and not v.sigue_resuelto;

    if v_negativas >= 3 then
        update public.incidentes i
        set status = 'en_atencion'
        where i.id = new.incidente_id and i.status = 'resuelto';

        if found then
            insert into public.historial_estados (incidente_id, estado_anterior, estado_nuevo, comentario, autor_id)
            values (
                new.incidente_id, 'resuelto', 'en_atencion',
                'Reabierto: 3 vecinos indican que el problema sigue igual', null
            );
            delete from public.verificaciones v where v.incidente_id = new.incidente_id;
        end if;
    end if;
    return new;
end;
$$;

revoke execute on function private.reabrir_si_sigue_igual() from public;

create trigger verificaciones_reabrir
after insert or update of sigue_resuelto on public.verificaciones
for each row execute function private.reabrir_si_sigue_igual();

create or replace function public.verificar_resolucion(p_incidente_id uuid, p_sigue_resuelto boolean)
returns public.estado
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
    v_estado public.estado;
begin
    if v_uid is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;
    if (select private.rol_actual()) is distinct from 'ciudadano'::public.rol then
        raise exception 'Solo un ciudadano con la cuenta activa puede verificar' using errcode = '42501';
    end if;
    if p_sigue_resuelto is null then
        raise exception 'Falta la respuesta' using errcode = '22023';
    end if;

    select i.status into v_estado
    from public.incidentes i
    where i.id = p_incidente_id
    for update;

    if not found then
        raise exception 'Incidente no encontrado' using errcode = '22023';
    end if;
    if v_estado <> 'resuelto' then
        raise exception 'Solo se verifica un incidente resuelto' using errcode = '22023';
    end if;
    if not exists (
        select 1 from public.reportes r where r.incidente_id = p_incidente_id and r.usuario_id = v_uid
        union all
        select 1 from public.apoyos a where a.incidente_id = p_incidente_id and a.usuario_id = v_uid
    ) then
        raise exception 'Solo quienes reportaron o apoyaron pueden verificar' using errcode = '42501';
    end if;

    insert into public.verificaciones as v (usuario_id, incidente_id, sigue_resuelto)
    values (v_uid, p_incidente_id, p_sigue_resuelto)
    on conflict on constraint verificaciones_pkey
    do update set sigue_resuelto = excluded.sigue_resuelto, creado_en = pg_catalog.now();

    select i.status into v_estado from public.incidentes i where i.id = p_incidente_id;
    return v_estado;
end;
$$;

revoke execute on function public.verificar_resolucion(uuid, boolean) from public, anon;
grant execute on function public.verificar_resolucion(uuid, boolean) to authenticated;

-- ---------------------------------------------------------------------------
-- Bandeja de notificaciones (RF-11). La escribe la base de datos, no la Edge
-- Function: así la bandeja funciona aunque FCM falle o no esté configurado.
-- Un Database Webhook sobre el INSERT llama a la Edge Function `notificar`.
-- ---------------------------------------------------------------------------
alter table public.notificaciones add column if not exists estado public.estado;
grant select (estado) on public.notificaciones to authenticated;

create or replace function private.notificar_cambio_estado()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    -- La fila de creación (sin estado anterior) no avisa: el autor ya ve la confirmación.
    if new.estado_anterior is null then
        return new;
    end if;

    insert into public.notificaciones (usuario_id, incidente_id, tipo, estado)
    select s.usuario_id,
           new.incidente_id,
           case
               when new.estado_anterior = 'resuelto' and new.estado_nuevo = 'en_atencion' then 'reabierto'
               else 'estado'
           end,
           new.estado_nuevo
    from (
        select r.usuario_id from public.reportes r where r.incidente_id = new.incidente_id
        union
        select a.usuario_id from public.apoyos a where a.incidente_id = new.incidente_id
    ) s
    join public.usuarios u on u.id = s.usuario_id and u.activo and u.rol = 'ciudadano'
    where s.usuario_id is distinct from new.autor_id;
    return new;
end;
$$;

create or replace function private.notificar_comentario()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.visibilidad <> 'publico' then
        return new;
    end if;

    insert into public.notificaciones (usuario_id, incidente_id, tipo)
    select s.usuario_id, new.incidente_id, 'comentario'
    from (
        select r.usuario_id from public.reportes r where r.incidente_id = new.incidente_id
        union
        select a.usuario_id from public.apoyos a where a.incidente_id = new.incidente_id
    ) s
    join public.usuarios u on u.id = s.usuario_id and u.activo and u.rol = 'ciudadano'
    where s.usuario_id is distinct from new.autor_id;
    return new;
end;
$$;

revoke execute on function private.notificar_cambio_estado() from public;
revoke execute on function private.notificar_comentario() from public;

create trigger historial_notificar
after insert on public.historial_estados
for each row execute function private.notificar_cambio_estado();

create trigger comentarios_notificar
after insert on public.comentarios
for each row execute function private.notificar_comentario();

create or replace function public.mis_notificaciones()
returns table (
    id uuid,
    tipo text,
    estado public.estado,
    leida boolean,
    creado_en timestamptz,
    incidente_id uuid,
    codigo text,
    titulo text
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        n.id,
        n.tipo,
        n.estado,
        n.leida,
        n.creado_en,
        n.incidente_id,
        i.codigo,
        coalesce(
            (
                select pg_catalog.left(pg_catalog.regexp_replace(r.descripcion, '\s+', ' ', 'g'), 80)
                from public.reportes r
                where r.incidente_id = i.id and r.tipo = 'original'
                order by r.creado_en asc
                limit 1
            ),
            c.nombre
        )
    from public.notificaciones n
    left join public.incidentes i on i.id = n.incidente_id
    left join public.categorias c on c.id = i.categoria_id
    where n.usuario_id = (select auth.uid())
    order by n.creado_en desc
    limit 100;
$$;

revoke execute on function public.mis_notificaciones() from public, anon;
grant execute on function public.mis_notificaciones() to authenticated;

-- Token de FCM del dispositivo. Un token pertenece a un solo usuario: si otra
-- persona inicia sesión en el mismo celular, el token pasa a ella.
create or replace function public.registrar_dispositivo(p_token text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null or (select private.rol_actual()) is null then
        raise exception 'Se requiere una sesión activa' using errcode = '42501';
    end if;
    if p_token is null or length(p_token) < 20 or length(p_token) > 4096 then
        raise exception 'Token de dispositivo inválido' using errcode = '22023';
    end if;

    insert into public.dispositivos as d (fcm_token, usuario_id)
    values (p_token, v_uid)
    on conflict on constraint dispositivos_pkey
    do update set usuario_id = excluded.usuario_id;
end;
$$;

revoke execute on function public.registrar_dispositivo(text) from public, anon;
grant execute on function public.registrar_dispositivo(text) to authenticated;

-- ---------------------------------------------------------------------------
-- Registro: el nombre se recorta y el consentimiento aceptado en el formulario
-- se guarda en el mismo momento (con confirmación de correo aún no hay sesión
-- para insertarlo desde la app).
-- ---------------------------------------------------------------------------
create or replace function private.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_politica text := nullif(pg_catalog.btrim(new.raw_user_meta_data ->> 'version_politica'), '');
begin
    insert into public.usuarios (id, nombre)
    values (
        new.id,
        coalesce(nullif(pg_catalog.left(pg_catalog.btrim(new.raw_user_meta_data ->> 'nombre'), 80), ''), 'Ciudadano')
    )
    on conflict (id) do nothing;

    if v_politica is not null then
        insert into public.consentimientos (usuario_id, version_politica)
        values (new.id, pg_catalog.left(v_politica, 20))
        on conflict do nothing;
    end if;
    return new;
end;
$$;

revoke execute on function private.handle_new_user() from public;

-- ---------------------------------------------------------------------------
-- Eliminación de la cuenta (docs/arquitectura.md §7.2, punto 6). La fila de
-- `usuarios` queda como lápida anónima; reportes y apoyos se conservan.
-- ---------------------------------------------------------------------------
create or replace function private.anonimizar_usuario(p_usuario_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    delete from public.dispositivos d where d.usuario_id = p_usuario_id;
    delete from public.notificaciones n where n.usuario_id = p_usuario_id;
    delete from public.consentimientos c where c.usuario_id = p_usuario_id;

    update public.usuarios u
    set nombre = 'Cuenta eliminada',
        zona_id = null,
        activo = false
    where u.id = p_usuario_id;

    delete from auth.users au where au.id = p_usuario_id;
end;
$$;

revoke execute on function private.anonimizar_usuario(uuid) from public;

create or replace function public.eliminar_mi_cuenta()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;
    -- Las cuentas de entidad y de moderación se desactivan por el runbook, no se borran.
    if (select private.rol_actual()) is distinct from 'ciudadano'::public.rol then
        raise exception 'Esta cuenta no se elimina desde la app' using errcode = '42501';
    end if;
    perform private.anonimizar_usuario(v_uid);
end;
$$;

revoke execute on function public.eliminar_mi_cuenta() from public, anon;
grant execute on function public.eliminar_mi_cuenta() to authenticated;

-- Variante administrativa: solo desde el SQL Editor o con `service_role`.
create or replace function public.eliminar_cuenta(p_usuario_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    if not exists (select 1 from public.usuarios u where u.id = p_usuario_id) then
        raise exception 'Usuario no encontrado' using errcode = 'P0002';
    end if;
    perform private.anonimizar_usuario(p_usuario_id);
end;
$$;

revoke execute on function public.eliminar_cuenta(uuid) from public, anon, authenticated;
grant execute on function public.eliminar_cuenta(uuid) to service_role;

-- ---------------------------------------------------------------------------
-- Resumen público para la pantalla de inicio. Solo totales globales, sin
-- desglose por zona, para que no se pueda inferir quién reportó.
-- ---------------------------------------------------------------------------
create or replace function public.resumen_publico()
returns table (
    abiertos bigint,
    en_atencion bigint,
    resueltos_30d bigint
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        count(*) filter (where i.status in ('reportado', 'en_revision', 'derivado')),
        count(*) filter (where i.status = 'en_atencion'),
        count(*) filter (
            where i.status = 'resuelto'
              and exists (
                  select 1 from public.historial_estados h
                  where h.incidente_id = i.id
                    and h.estado_nuevo = 'resuelto'
                    and h.creado_en >= pg_catalog.now() - interval '30 days'
              )
        )
    from public.incidentes i;
$$;

revoke execute on function public.resumen_publico() from public, anon;
grant execute on function public.resumen_publico() to anon, authenticated;

-- ---------------------------------------------------------------------------
-- Recálculo diario de la prioridad (los días abiertos cambian sin que nadie
-- toque la fila). 05:10 UTC = 00:10 en Lima. El trigger incidentes_prioridad
-- hace la cuenta; aquí solo se le da la ocasión.
-- ---------------------------------------------------------------------------
do $$
begin
    create extension if not exists pg_cron with schema pg_catalog;
    perform cron.schedule(
        'civika-recalcular-prioridad',
        '10 5 * * *',
        $cron$update public.incidentes set prioridad = prioridad where status not in ('resuelto', 'rechazado')$cron$
    );
exception when others then
    raise notice 'pg_cron no está disponible (%): la prioridad se recalcula solo con cada apoyo.', sqlerrm;
end;
$$;
