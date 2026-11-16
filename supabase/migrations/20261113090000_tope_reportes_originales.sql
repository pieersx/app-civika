-- El tope anti-spam de crear_reporte (10 por hora) cuenta solo los reportes originales:
-- apoyar con fotos crea reportes de tipo `apoyo` y no debe impedir reportar.
-- Es la misma función de rpc_base con ese único cambio; se conservan firma y permisos.

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
      and r.tipo = 'original'
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
) from public, anon;
grant execute on function public.crear_reporte(
    uuid, double precision, double precision, integer, text, text[], boolean, text
) to authenticated;
