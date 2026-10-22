-- RPC base del MVP. Todas son security definer porque escriben con reglas de
-- negocio y porque el RLS no debe depender de que el cliente mande datos válidos.
-- Se revoca EXECUTE de public y se concede solo a los roles que corresponden.

-- Devuelve incidentes abiertos dentro del radio y la ventana de la categoría.
create or replace function public.buscar_duplicados(
    p_lat double precision,
    p_lon double precision,
    p_categoria_id integer
)
returns table (
    incidente_id uuid,
    codigo text,
    distancia_m double precision,
    prioridad integer,
    apoyos_count integer,
    creado_en timestamptz
)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    v_categoria public.categorias%rowtype;
begin
    if auth.uid() is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;
    if p_lat is null or p_lon is null or p_lat < -90 or p_lat > 90 or p_lon < -180 or p_lon > 180 then
        raise exception 'Coordenadas inválidas' using errcode = '22023';
    end if;

    select * into v_categoria
    from public.categorias c
    where c.id = p_categoria_id and c.activa;

    if not found then
        raise exception 'Categoría inexistente o inactiva' using errcode = '22023';
    end if;

    return query
    select
        i.id,
        i.codigo,
        extensions.ST_Distance(
            i.geom,
            extensions.ST_SetSRID(extensions.ST_MakePoint(p_lon, p_lat), 4326)::extensions.geography
        ) as distancia_m,
        i.prioridad,
        i.apoyos_count,
        i.creado_en
    from public.incidentes i
    where i.categoria_id = p_categoria_id
      and i.status not in ('resuelto', 'rechazado')
      and i.creado_en >= pg_catalog.now() - pg_catalog.make_interval(days => v_categoria.ventana_dias)
      and extensions.ST_DWithin(
          i.geom,
          extensions.ST_SetSRID(extensions.ST_MakePoint(p_lon, p_lat), 4326)::extensions.geography,
          v_categoria.radio_duplicado_m
      )
    order by distancia_m asc
    limit 10;
end;
$$;

revoke execute on function public.buscar_duplicados(double precision, double precision, integer) from public;
grant execute on function public.buscar_duplicados(double precision, double precision, integer) to authenticated;

-- Crea un incidente y su primer reporte. La idempotencia se resuelve con
-- (usuario_id, cliente_id); el advisory lock evita dos inserciones simultáneas.
create or replace function public.crear_reporte(
    p_cliente_id uuid,
    p_lat double precision,
    p_lon double precision,
    p_categoria_id integer,
    p_descripcion text,
    p_fotos text[],
    p_ubicacion_simulada boolean default false,
    p_direccion text default null
)
returns table (
    incidente_id uuid,
    codigo text,
    estado public.estado,
    entidad_id integer
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
    v_usuario_id uuid;
    v_zona_id integer;
    v_entidad_id integer;
    v_estado public.estado;
    v_incidente_id uuid;
    v_reporte_id uuid;
    v_codigo text;
    v_foto text;
    v_geog extensions.geography;
    v_reportes_recientes integer;
begin
    if v_uid is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;
    if p_cliente_id is null then
        raise exception 'cliente_id es obligatorio' using errcode = '22023';
    end if;
    if p_lat is null or p_lon is null or p_lat < -90 or p_lat > 90 or p_lon < -180 or p_lon > 180 then
        raise exception 'Coordenadas inválidas' using errcode = '22023';
    end if;

    select u.id into v_usuario_id
    from public.usuarios u
    where u.id = v_uid and u.activo;

    if v_usuario_id is null then
        raise exception 'La cuenta no existe o está desactivada' using errcode = '42501';
    end if;

    -- Si el cliente reintenta el mismo envío, devolvemos el incidente original.
    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(v_uid::text || ':' || p_cliente_id::text, 0)
    );

    select r.incidente_id into v_incidente_id
    from public.reportes r
    where r.usuario_id = v_uid and r.cliente_id = p_cliente_id;

    if found then
        select i.codigo, i.status, i.entidad_id
        into v_codigo, v_estado, v_entidad_id
        from public.incidentes i
        where i.id = v_incidente_id;

        return query select v_incidente_id, v_codigo, v_estado, v_entidad_id;
        return;
    end if;

    select count(*) into v_reportes_recientes
    from public.reportes r
    where r.usuario_id = v_uid
      and r.creado_en >= pg_catalog.now() - interval '1 hour';

    if v_reportes_recientes >= 10 then
        raise exception 'Demasiados reportes en la última hora' using errcode = '22023';
    end if;

    if not exists (
        select 1 from public.categorias c
        where c.id = p_categoria_id and c.activa
    ) then
        raise exception 'Categoría inexistente o inactiva' using errcode = '22023';
    end if;

    if p_descripcion is null or length(pg_catalog.btrim(p_descripcion)) < 10 or length(p_descripcion) > 2000 then
        raise exception 'La descripción debe tener entre 10 y 2000 caracteres' using errcode = '22023';
    end if;

    perform private.validar_fotos(p_fotos);

    v_geog := extensions.ST_SetSRID(
        extensions.ST_MakePoint(p_lon, p_lat),
        4326
    )::extensions.geography;

    select z.id into v_zona_id
    from public.zonas z
    where extensions.ST_Covers(z.geom, v_geog::extensions.geometry)
    limit 1;

    if v_zona_id is null then
        raise exception 'Fuera de la zona piloto' using errcode = 'P0001';
    end if;

    select rr.entidad_id into v_entidad_id
    from public.reglas_ruteo rr
    where rr.categoria_id = p_categoria_id and rr.zona_id = v_zona_id;

    if v_entidad_id is null or p_ubicacion_simulada then
        v_estado := 'en_revision';
        v_entidad_id := null;
    else
        v_estado := 'derivado';
    end if;

    v_codigo := 'CVK-' || pg_catalog.lpad(
        pg_catalog.nextval('public.incidentes_codigo_seq')::text,
        6,
        '0'
    );

    insert into public.incidentes (
        codigo, categoria_id, zona_id, entidad_id, geom, direccion, status
    )
    values (
        v_codigo,
        p_categoria_id,
        v_zona_id,
        v_entidad_id,
        v_geog,
        nullif(pg_catalog.btrim(p_direccion), ''),
        v_estado
    )
    returning id into v_incidente_id;

    insert into public.reportes (
        usuario_id, cliente_id, incidente_id, tipo, descripcion, geom,
        categoria_sugerida, ubicacion_simulada
    )
    values (
        v_uid,
        p_cliente_id,
        v_incidente_id,
        'original',
        pg_catalog.btrim(p_descripcion),
        v_geog,
        p_categoria_id,
        coalesce(p_ubicacion_simulada, false)
    )
    returning id into v_reporte_id;

    foreach v_foto in array p_fotos loop
        insert into public.fotos (reporte_id, storage_path)
        values (v_reporte_id, v_foto);
    end loop;

    insert into public.historial_estados (
        incidente_id, estado_anterior, estado_nuevo, comentario, autor_id
    )
    values (
        v_incidente_id, null, v_estado, 'Reporte creado', v_uid
    );

    return query select v_incidente_id, v_codigo, v_estado, v_entidad_id;
end;
$$;

revoke execute on function public.crear_reporte(
    uuid, double precision, double precision, integer, text, text[], boolean, text
) from public;
grant execute on function public.crear_reporte(
    uuid, double precision, double precision, integer, text, text[], boolean, text
) to authenticated;

-- Cambia el estado con control de rol, entidad, versión y transición.
-- p_entidad_id solo se usa al devolver a Derivado; si falta y no hay entidad, falla.
create or replace function public.cambiar_estado(
    p_incidente_id uuid,
    p_nuevo_estado public.estado,
    p_version_esperada integer,
    p_comentario text default null,
    p_entidad_id integer default null
)
returns table (
    estado public.estado,
    version integer
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := auth.uid();
    v_usuario public.usuarios%rowtype;
    v_estado_actual public.estado;
    v_version integer;
    v_entidad_incidente integer;
    v_nueva_entidad integer;
    v_nuevo_estado public.estado;
    v_nueva_version integer;
begin
    if v_uid is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;

    select * into v_usuario
    from public.usuarios u
    where u.id = v_uid and u.activo;

    if not found then
        raise exception 'La cuenta no existe o está desactivada' using errcode = '42501';
    end if;
    if v_usuario.rol not in ('entidad', 'moderador') then
        raise exception 'Este rol no puede cambiar estados' using errcode = '42501';
    end if;
    if not private.aal2() then
        raise exception 'Se requiere MFA para cambiar estados' using errcode = '42501';
    end if;
    if p_incidente_id is null or p_nuevo_estado is null or p_version_esperada is null then
        raise exception 'Faltan datos para cambiar el estado' using errcode = '22023';
    end if;
    if p_comentario is not null and length(p_comentario) > 2000 then
        raise exception 'El comentario supera los 2000 caracteres' using errcode = '22023';
    end if;

    if p_nuevo_estado = 'rechazado' and nullif(pg_catalog.btrim(p_comentario), '') is null then
        raise exception 'El rechazo necesita un motivo' using errcode = '22023';
    end if;

    select i.status, i.version, i.entidad_id
    into v_estado_actual, v_version, v_entidad_incidente
    from public.incidentes i
    where i.id = p_incidente_id
    for update;

    if not found then
        raise exception 'Incidente no encontrado' using errcode = 'P0002';
    end if;
    if v_version <> p_version_esperada then
        raise exception 'La versión del incidente cambió; recarga antes de intentar de nuevo'
            using errcode = '40001';
    end if;
    if v_usuario.rol = 'entidad'
       and v_entidad_incidente is distinct from v_usuario.entidad_id then
        raise exception 'El incidente no pertenece a tu entidad' using errcode = '42501';
    end if;
    if not private.transicion_permitida(v_estado_actual, p_nuevo_estado, v_usuario.rol) then
        raise exception 'Transición no permitida para este rol' using errcode = '42501';
    end if;

    if p_nuevo_estado = 'en_revision' then
        v_nueva_entidad := null;
    elsif p_nuevo_estado = 'derivado' then
        if v_usuario.rol = 'entidad' then
            raise exception 'Solo moderación deriva un incidente' using errcode = '42501';
        end if;
        v_nueva_entidad := coalesce(p_entidad_id, v_entidad_incidente);
        if v_nueva_entidad is null then
            raise exception 'Derivado necesita una entidad' using errcode = '22023';
        end if;
        if not exists (select 1 from public.entidades e where e.id = v_nueva_entidad) then
            raise exception 'Entidad inexistente' using errcode = '22023';
        end if;
    else
        v_nueva_entidad := v_entidad_incidente;
    end if;

    if v_usuario.rol = 'entidad' and p_entidad_id is not null and p_entidad_id <> v_usuario.entidad_id then
        raise exception 'No puedes asignar el incidente a otra entidad' using errcode = '42501';
    end if;

    update public.incidentes i
    set status = p_nuevo_estado,
        entidad_id = v_nueva_entidad
    where i.id = p_incidente_id
    returning i.status, i.version into v_nuevo_estado, v_nueva_version;

    insert into public.historial_estados (
        incidente_id, estado_anterior, estado_nuevo, comentario, autor_id
    )
    values (
        p_incidente_id,
        v_estado_actual,
        v_nuevo_estado,
        nullif(pg_catalog.btrim(p_comentario), ''),
        v_uid
    );

    if p_nuevo_estado = 'resuelto' then
        delete from public.verificaciones v where v.incidente_id = p_incidente_id;
    end if;

    return query select v_nuevo_estado, v_nueva_version;
end;
$$;

revoke execute on function public.cambiar_estado(
    uuid, public.estado, integer, text, integer
) from public;
grant execute on function public.cambiar_estado(
    uuid, public.estado, integer, text, integer
) to authenticated;

-- Lectura pública sin datos personales: coordenadas redondeadas, sin autor,
-- sin dirección exacta y sin incidentes rechazados.
create or replace function public.listar_incidentes_publicos(
    p_zona_id integer default null,
    p_categoria_id integer default null,
    p_estado public.estado default null
)
returns table (
    id uuid,
    codigo text,
    titulo text,
    categoria text,
    categoria_id integer,
    estado public.estado,
    prioridad integer,
    apoyos_count integer,
    zona text,
    lat double precision,
    lon double precision,
    creado_en timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        i.id,
        i.codigo,
        coalesce(
            (
                select pg_catalog.left(pg_catalog.regexp_replace(r.descripcion, '\s+', ' ', 'g'), 80)
                from public.reportes r
                where r.incidente_id = i.id
                order by r.creado_en asc
                limit 1
            ),
            c.nombre
        ) as titulo,
        c.nombre as categoria,
        i.categoria_id,
        i.status,
        i.prioridad,
        i.apoyos_count,
        z.nombre as zona,
        pg_catalog.round(extensions.ST_Y(i.geom::extensions.geometry)::numeric, 4)::double precision as lat,
        pg_catalog.round(extensions.ST_X(i.geom::extensions.geometry)::numeric, 4)::double precision as lon,
        i.creado_en
    from public.incidentes i
    join public.categorias c on c.id = i.categoria_id
    join public.zonas z on z.id = i.zona_id
    where i.status <> 'rechazado'
      and (p_zona_id is null or i.zona_id = p_zona_id)
      and (p_categoria_id is null or i.categoria_id = p_categoria_id)
      and (p_estado is null or i.status = p_estado)
    order by i.prioridad desc, i.creado_en desc
    limit 200;
$$;

revoke execute on function public.listar_incidentes_publicos(integer, integer, public.estado) from public;
grant execute on function public.listar_incidentes_publicos(integer, integer, public.estado) to anon, authenticated;

create or replace function public.historial_publico(p_incidente_id uuid)
returns table (
    estado public.estado,
    creado_en timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select h.estado_nuevo, h.creado_en
    from public.historial_estados h
    join public.incidentes i on i.id = h.incidente_id
    where h.incidente_id = p_incidente_id
      and i.status <> 'rechazado'
    order by h.creado_en asc;
$$;

revoke execute on function public.historial_publico(uuid) from public;
grant execute on function public.historial_publico(uuid) to anon, authenticated;

create or replace function public.comentarios_publicos(p_incidente_id uuid)
returns table (
    texto text,
    creado_en timestamptz
)
language sql
stable
security definer
set search_path = ''
as $$
    select c.texto, c.creado_en
    from public.comentarios c
    join public.incidentes i on i.id = c.incidente_id
    where c.incidente_id = p_incidente_id
      and c.visibilidad = 'publico'
      and i.status <> 'rechazado'
    order by c.creado_en asc;
$$;

revoke execute on function public.comentarios_publicos(uuid) from public;
grant execute on function public.comentarios_publicos(uuid) to anon, authenticated;
