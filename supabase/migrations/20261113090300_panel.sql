-- RPC del panel web (RF-13, RF-14, RF-16). Todas exigen cuenta activa de
-- entidad o de moderación y una sesión con segundo factor (aal2): una función
-- `security definer` no pasa por el RLS, así que la comprobación va dentro.
-- El personal de entidad solo ve y toca lo de su entidad.

create or replace function private.exigir_panel(p_solo_moderador boolean default false)
returns public.usuarios
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_usuario public.usuarios%rowtype;
begin
    select * into v_usuario
    from public.usuarios u
    where u.id = auth.uid() and u.activo;

    if not found or v_usuario.rol not in ('entidad', 'moderador') then
        raise exception 'El panel es solo para personal de entidad y moderación' using errcode = '42501';
    end if;
    if p_solo_moderador and v_usuario.rol <> 'moderador' then
        raise exception 'Esta acción es solo de moderación' using errcode = '42501';
    end if;
    if not private.aal2() then
        raise exception 'Se requiere el segundo factor (MFA)' using errcode = '42501';
    end if;
    return v_usuario;
end;
$$;

revoke execute on function private.exigir_panel(boolean) from public;

-- ---------------------------------------------------------------------------
-- Lista de trabajo del panel, con la ubicación exacta y los contadores.
-- ---------------------------------------------------------------------------
create or replace function public.listar_incidentes_panel(
    p_estado public.estado default null,
    p_categoria_id integer default null
)
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
    reportes_count bigint,
    denuncias_pendientes bigint,
    zona text,
    entidad_id integer,
    entidad text,
    direccion text,
    lat double precision,
    lon double precision,
    version integer,
    creado_en timestamptz,
    cerrado_en timestamptz,
    plazo_objetivo_dias integer,
    ubicacion_simulada boolean,
    duplicado_de uuid
)
language plpgsql
stable
security definer
set search_path = ''
as $$
#variable_conflict use_column
declare
    v_usuario public.usuarios := private.exigir_panel();
begin
    return query
    select
        i.id,
        i.codigo,
        coalesce(pg_catalog.left(pg_catalog.regexp_replace(o.descripcion, '\s+', ' ', 'g'), 80), c.nombre),
        o.descripcion,
        c.nombre,
        i.categoria_id,
        i.status,
        i.prioridad,
        i.apoyos_count,
        (select count(*) from public.reportes r where r.incidente_id = i.id),
        (
            select count(*)
            from public.denuncias d
            join public.reportes r on r.id = d.reporte_id
            where r.incidente_id = i.id and d.resolucion is null
        ),
        z.nombre,
        i.entidad_id,
        e.nombre,
        i.direccion,
        extensions.ST_Y(i.geom::extensions.geometry),
        extensions.ST_X(i.geom::extensions.geometry),
        i.version,
        i.creado_en,
        case when i.status in ('resuelto', 'rechazado') then (
            select pg_catalog.max(h.creado_en)
            from public.historial_estados h
            where h.incidente_id = i.id and h.estado_nuevo = i.status
        ) end,
        c.plazo_objetivo_dias,
        coalesce(o.ubicacion_simulada, false),
        i.duplicado_de
    from public.incidentes i
    join public.categorias c on c.id = i.categoria_id
    join public.zonas z on z.id = i.zona_id
    left join public.entidades e on e.id = i.entidad_id
    left join lateral (
        select r.descripcion, r.ubicacion_simulada
        from public.reportes r
        where r.incidente_id = i.id and r.tipo = 'original'
        order by r.creado_en asc
        limit 1
    ) o on true
    where (v_usuario.rol = 'moderador' or i.entidad_id = v_usuario.entidad_id)
      and (p_estado is null or i.status = p_estado)
      and (p_categoria_id is null or i.categoria_id = p_categoria_id)
    order by i.prioridad desc, i.creado_en desc
    limit 500;
end;
$$;

revoke execute on function public.listar_incidentes_panel(public.estado, integer) from public, anon;
grant execute on function public.listar_incidentes_panel(public.estado, integer) to authenticated;

-- Un incidente es visible en el panel si es de la entidad del usuario o si es moderación.
create or replace function private.exigir_incidente_panel(p_incidente_id uuid, p_solo_moderador boolean default false)
returns public.usuarios
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_usuario public.usuarios := private.exigir_panel(p_solo_moderador);
begin
    if not exists (
        select 1 from public.incidentes i
        where i.id = p_incidente_id
          and (v_usuario.rol = 'moderador' or i.entidad_id = v_usuario.entidad_id)
    ) then
        raise exception 'Incidente no encontrado' using errcode = 'P0002';
    end if;
    return v_usuario;
end;
$$;

revoke execute on function private.exigir_incidente_panel(uuid, boolean) from public;

-- Historial con comentario y autor. Del ciudadano nunca se muestra el nombre.
create or replace function public.historial_panel(p_incidente_id uuid)
returns table (
    id bigint,
    estado_anterior public.estado,
    estado_nuevo public.estado,
    comentario text,
    autor text,
    creado_en timestamptz
)
language plpgsql
stable
security definer
set search_path = ''
as $$
#variable_conflict use_column
begin
    perform private.exigir_incidente_panel(p_incidente_id);

    return query
    select
        h.id,
        h.estado_anterior,
        h.estado_nuevo,
        h.comentario,
        case
            when h.autor_id is null then 'Sistema'
            when u.rol = 'ciudadano' then 'Ciudadano'
            when u.rol = 'moderador' then 'Moderación'
            else u.nombre
        end,
        h.creado_en
    from public.historial_estados h
    left join public.usuarios u on u.id = h.autor_id
    where h.incidente_id = p_incidente_id
    order by h.creado_en asc, h.id asc;
end;
$$;

revoke execute on function public.historial_panel(uuid) from public, anon;
grant execute on function public.historial_panel(uuid) to authenticated;

create or replace function public.comentarios_panel(p_incidente_id uuid)
returns table (
    id uuid,
    texto text,
    visibilidad public.visibilidad,
    autor text,
    creado_en timestamptz
)
language plpgsql
stable
security definer
set search_path = ''
as $$
#variable_conflict use_column
begin
    perform private.exigir_incidente_panel(p_incidente_id);

    return query
    select c.id, c.texto, c.visibilidad, u.nombre, c.creado_en
    from public.comentarios c
    join public.usuarios u on u.id = c.autor_id
    where c.incidente_id = p_incidente_id
    order by c.creado_en asc;
end;
$$;

revoke execute on function public.comentarios_panel(uuid) from public, anon;
grant execute on function public.comentarios_panel(uuid) to authenticated;

-- Comentar (RF-13): solo el personal de la entidad asignada.
create or replace function public.comentar_incidente(
    p_incidente_id uuid,
    p_texto text,
    p_visibilidad public.visibilidad default 'publico'
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_usuario public.usuarios := private.exigir_incidente_panel(p_incidente_id);
    v_id uuid;
begin
    if v_usuario.rol <> 'entidad' then
        raise exception 'Solo la entidad asignada comenta el incidente' using errcode = '42501';
    end if;
    if p_texto is null or length(pg_catalog.btrim(p_texto)) < 1 or length(p_texto) > 2000 then
        raise exception 'El comentario debe tener entre 1 y 2000 caracteres' using errcode = '22023';
    end if;

    insert into public.comentarios (incidente_id, autor_id, visibilidad, texto)
    values (p_incidente_id, v_usuario.id, coalesce(p_visibilidad, 'publico'), pg_catalog.btrim(p_texto))
    returning id into v_id;
    return v_id;
end;
$$;

revoke execute on function public.comentar_incidente(uuid, text, public.visibilidad) from public, anon;
grant execute on function public.comentar_incidente(uuid, text, public.visibilidad) to authenticated;

-- Fotos del incidente para el panel: la entidad ve las no ocultas; moderación, todas.
create or replace function public.fotos_incidente_panel(p_incidente_id uuid)
returns table (
    id uuid,
    ruta text,
    oculta boolean,
    tipo public.tipo_reporte,
    creado_en timestamptz
)
language plpgsql
stable
security definer
set search_path = ''
as $$
#variable_conflict use_column
declare
    v_usuario public.usuarios := private.exigir_incidente_panel(p_incidente_id);
begin
    return query
    select f.id, f.storage_path, f.oculta, r.tipo, r.creado_en
    from public.fotos f
    join public.reportes r on r.id = f.reporte_id
    join public.incidentes i on i.id = r.incidente_id
    where r.incidente_id = p_incidente_id
      and (v_usuario.rol = 'moderador' or (not f.oculta and i.status <> 'rechazado'))
    order by r.creado_en asc, f.id asc;
end;
$$;

revoke execute on function public.fotos_incidente_panel(uuid) from public, anon;
grant execute on function public.fotos_incidente_panel(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- Moderación (RF-16).
-- ---------------------------------------------------------------------------
create or replace function public.listar_denuncias(p_solo_pendientes boolean default true)
returns table (
    id uuid,
    motivo text,
    resolucion text,
    creado_en timestamptz,
    incidente_id uuid,
    codigo text,
    titulo text,
    categoria text,
    estado public.estado,
    version integer,
    denuncias_incidente bigint
)
language plpgsql
stable
security definer
set search_path = ''
as $$
#variable_conflict use_column
begin
    perform private.exigir_panel(true);

    return query
    select
        d.id,
        d.motivo,
        d.resolucion,
        d.creado_en,
        i.id,
        i.codigo,
        coalesce(pg_catalog.left(pg_catalog.regexp_replace(r.descripcion, '\s+', ' ', 'g'), 80), c.nombre),
        c.nombre,
        i.status,
        i.version,
        (
            select count(*)
            from public.denuncias d2
            join public.reportes r2 on r2.id = d2.reporte_id
            where r2.incidente_id = i.id and d2.resolucion is null
        )
    from public.denuncias d
    join public.reportes r on r.id = d.reporte_id
    join public.incidentes i on i.id = r.incidente_id
    join public.categorias c on c.id = i.categoria_id
    where not coalesce(p_solo_pendientes, true) or d.resolucion is null
    order by d.creado_en desc
    limit 300;
end;
$$;

revoke execute on function public.listar_denuncias(boolean) from public, anon;
grant execute on function public.listar_denuncias(boolean) to authenticated;

create or replace function public.resolver_denuncia(p_denuncia_id uuid, p_resolucion text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform private.exigir_panel(true);

    if p_resolucion is null or length(pg_catalog.btrim(p_resolucion)) < 1 or length(p_resolucion) > 500 then
        raise exception 'La resolución debe tener entre 1 y 500 caracteres' using errcode = '22023';
    end if;

    update public.denuncias d
    set resolucion = pg_catalog.btrim(p_resolucion)
    where d.id = p_denuncia_id;

    if not found then
        raise exception 'Denuncia no encontrada' using errcode = 'P0002';
    end if;
end;
$$;

revoke execute on function public.resolver_denuncia(uuid, text) from public, anon;
grant execute on function public.resolver_denuncia(uuid, text) to authenticated;

-- Ocultar (o volver a mostrar) una foto. El trigger fotos_auditar_ocultamiento deja el registro.
create or replace function public.ocultar_foto(p_foto_id uuid, p_oculta boolean default true)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
    perform private.exigir_panel(true);

    update public.fotos f
    set oculta = coalesce(p_oculta, true)
    where f.id = p_foto_id;

    if not found then
        raise exception 'Foto no encontrada' using errcode = 'P0002';
    end if;
end;
$$;

revoke execute on function public.ocultar_foto(uuid, boolean) from public, anon;
grant execute on function public.ocultar_foto(uuid, boolean) to authenticated;

-- Rechazar por duplicado: llena `duplicado_de` y pasa reportes y apoyos al original (§5).
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

    update public.incidentes i
    set status = 'rechazado',
        duplicado_de = p_original_id
    where i.id = p_incidente_id;

    insert into public.historial_estados (incidente_id, estado_anterior, estado_nuevo, comentario, autor_id)
    values (p_incidente_id, v_estado, 'rechazado', 'Duplicado de ' || v_codigo_original, v_usuario.id);
end;
$$;

revoke execute on function public.marcar_duplicado(uuid, uuid, integer) from public, anon;
grant execute on function public.marcar_duplicado(uuid, uuid, integer) to authenticated;

-- ---------------------------------------------------------------------------
-- Indicadores (RF-14). La entidad ve los suyos; moderación, los de todo el piloto.
-- El tiempo de atención va de la creación a la última entrada «Resuelto», en
-- días de calendario de America/Lima (docs/arquitectura.md §6).
-- ---------------------------------------------------------------------------
create or replace function public.indicadores_panel(p_dias integer default 90)
returns json
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_usuario public.usuarios := private.exigir_panel();
    v_dias integer := least(greatest(coalesce(p_dias, 90), 7), 365);
    v_hoy date := (pg_catalog.timezone('America/Lima', pg_catalog.now()))::date;
    v_desde date := v_hoy - v_dias;
    v_resultado json;
begin
    with base as (
        select
            i.id,
            i.categoria_id,
            c.nombre as categoria,
            c.plazo_objetivo_dias,
            i.zona_id,
            z.nombre as zona,
            i.status,
            (pg_catalog.timezone('America/Lima', i.creado_en))::date as creado,
            (
                select (pg_catalog.timezone('America/Lima', pg_catalog.max(h.creado_en)))::date
                from public.historial_estados h
                where h.incidente_id = i.id and h.estado_nuevo = 'resuelto'
            ) as resuelto
        from public.incidentes i
        join public.categorias c on c.id = i.categoria_id
        join public.zonas z on z.id = i.zona_id
        where (v_usuario.rol = 'moderador' or i.entidad_id = v_usuario.entidad_id)
          and (pg_catalog.timezone('America/Lima', i.creado_en))::date >= v_desde
    ),
    calc as (
        select
            b.*,
            case when b.status = 'resuelto' and b.resuelto is not null
                 then greatest(b.resuelto - b.creado, 0) end as dias_resolucion,
            greatest(v_hoy - b.creado, 0) as dias_abierto
        from base b
    ),
    semanas as (
        select (pg_catalog.date_trunc('week', v_hoy::timestamp)::date - (n * 7)) as semana
        from pg_catalog.generate_series(0, 7) as n
    )
    select pg_catalog.json_build_object(
        'desde', v_desde,
        'hasta', v_hoy,
        'total', (select count(*) from calc),
        'abiertos', (select count(*) from calc k where k.status in ('reportado', 'en_revision', 'derivado')),
        'en_atencion', (select count(*) from calc k where k.status = 'en_atencion'),
        'resueltos', (select count(*) from calc k where k.status = 'resuelto'),
        'rechazados', (select count(*) from calc k where k.status = 'rechazado'),
        'vencidos', (
            select count(*) from calc k
            where k.status not in ('resuelto', 'rechazado') and k.dias_abierto > k.plazo_objetivo_dias
        ),
        'tiempo_promedio_dias', (select pg_catalog.round(pg_catalog.avg(k.dias_resolucion), 1) from calc k),
        'pct_dentro_plazo', (
            select pg_catalog.round(
                100.0 * count(*) filter (where k.dias_resolucion <= k.plazo_objetivo_dias)
                / nullif(count(*), 0), 1
            )
            from calc k
            where k.dias_resolucion is not null
        ),
        'por_categoria', coalesce((
            select pg_catalog.json_agg(t order by t.total desc)
            from (
                select
                    k.categoria_id,
                    k.categoria,
                    pg_catalog.max(k.plazo_objetivo_dias) as plazo_objetivo_dias,
                    count(*) as total,
                    count(*) filter (where k.status = 'resuelto') as resueltos,
                    count(*) filter (
                        where k.status not in ('resuelto', 'rechazado') and k.dias_abierto > k.plazo_objetivo_dias
                    ) as vencidos,
                    pg_catalog.round(pg_catalog.avg(k.dias_resolucion), 1) as tiempo_promedio_dias,
                    pg_catalog.round(
                        100.0 * count(*) filter (where k.dias_resolucion <= k.plazo_objetivo_dias)
                        / nullif(count(*) filter (where k.dias_resolucion is not null), 0), 1
                    ) as pct_dentro_plazo
                from calc k
                group by k.categoria_id, k.categoria
            ) t
        ), '[]'::json),
        'por_zona', coalesce((
            select pg_catalog.json_agg(t order by t.total desc)
            from (
                select
                    k.zona_id,
                    k.zona,
                    count(*) as total,
                    count(*) filter (where k.status not in ('resuelto', 'rechazado')) as abiertos,
                    count(*) filter (where k.status = 'resuelto') as resueltos,
                    pg_catalog.round(
                        100.0 * count(*) filter (where k.dias_resolucion <= k.plazo_objetivo_dias)
                        / nullif(count(*) filter (where k.dias_resolucion is not null), 0), 1
                    ) as pct_dentro_plazo
                from calc k
                group by k.zona_id, k.zona
            ) t
        ), '[]'::json),
        'serie', (
            select pg_catalog.json_agg(t order by t.semana)
            from (
                select
                    s.semana,
                    (select count(*) from calc k where k.creado >= s.semana and k.creado < s.semana + 7) as creados,
                    (
                        select count(*) from calc k
                        where k.status = 'resuelto' and k.resuelto >= s.semana and k.resuelto < s.semana + 7
                    ) as resueltos
                from semanas s
            ) t
        )
    ) into v_resultado;

    return v_resultado;
end;
$$;

revoke execute on function public.indicadores_panel(integer) from public, anon;
grant execute on function public.indicadores_panel(integer) to authenticated;
