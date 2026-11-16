-- Hallazgo de /code-review: marcar_duplicado movía los reportes y los apoyos del duplicado al
-- original antes de registrar el rechazo, así que el trigger de avisos no encontraba a nadie.
-- Ahora avisa primero, y el detalle del duplicado rechazado sigue visible para quien lo recibió.

create or replace function public.marcar_duplicado(
    p_incidente_id uuid,
    p_original_id uuid,
    p_version_esperada integer
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_usuario public.usuarios := private.exigir_panel(true);
    v_estado public.estado;
    v_version integer;
    v_codigo_original text;
    v_estado_original public.estado;
begin
    if p_incidente_id is null or p_original_id is null or p_incidente_id = p_original_id then
        raise exception 'Elige un incidente original distinto' using errcode = '22023';
    end if;

    select i.status, i.version into v_estado, v_version
    from public.incidentes i
    where i.id = p_incidente_id
    for update;

    if not found then
        raise exception 'Incidente no encontrado' using errcode = 'P0002';
    end if;
    if v_version is distinct from p_version_esperada then
        raise exception 'La versión del incidente cambió; recarga antes de intentar de nuevo'
            using errcode = '40001';
    end if;
    if v_estado <> 'en_revision' then
        raise exception 'Solo se marca como duplicado un incidente en revisión' using errcode = '22023';
    end if;

    select i.codigo, i.status into v_codigo_original, v_estado_original
    from public.incidentes i
    where i.id = p_original_id;

    if not found or v_estado_original = 'rechazado' then
        raise exception 'El incidente original no existe o fue rechazado' using errcode = '22023';
    end if;

    -- Primero se rechaza y se registra el historial: el trigger avisa a quienes reportaron o
    -- apoyaron el duplicado, y para encontrarlos sus reportes y apoyos aún deben estar aquí.
    update public.incidentes i
    set status = 'rechazado',
        duplicado_de = p_original_id
    where i.id = p_incidente_id;

    insert into public.historial_estados (incidente_id, estado_anterior, estado_nuevo, comentario, autor_id)
    values (p_incidente_id, v_estado, 'rechazado', 'Duplicado de ' || v_codigo_original, v_usuario.id);

    -- Quienes reportaron o apoyaron el duplicado pasan a apoyar el original,
    -- salvo que ya sean sus autores.
    insert into public.apoyos (usuario_id, incidente_id)
    select s.usuario_id, p_original_id
    from (
        select r.usuario_id from public.reportes r where r.incidente_id = p_incidente_id
        union
        select a.usuario_id from public.apoyos a where a.incidente_id = p_incidente_id
    ) s
    where not exists (
        select 1 from public.reportes ro
        where ro.incidente_id = p_original_id
          and ro.usuario_id = s.usuario_id
          and ro.tipo = 'original'
    )
    on conflict do nothing;

    delete from public.apoyos a where a.incidente_id = p_incidente_id;

    update public.reportes r
    set incidente_id = p_original_id,
        tipo = 'apoyo'
    where r.incidente_id = p_incidente_id;
end;
$$;

revoke execute on function public.marcar_duplicado(uuid, uuid, integer) from public, anon;
grant execute on function public.marcar_duplicado(uuid, uuid, integer) to authenticated;

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
          -- Un duplicado rechazado ya no conserva sus reportes: lo ve quien recibió el aviso de rechazo.
          or exists (
              select 1 from public.notificaciones nt, yo
              where nt.incidente_id = i.id and nt.usuario_id = yo.uid and nt.estado = 'rechazado'
          )
      );
$$;

revoke execute on function public.detalle_incidente(uuid) from public, anon;
grant execute on function public.detalle_incidente(uuid) to anon, authenticated;
