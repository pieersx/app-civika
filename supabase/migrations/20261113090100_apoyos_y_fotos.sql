-- Apoyos (RF-07) y lectura de fotos privadas (issue #32, opción B).

-- «Yo también lo vi». Idempotente: apoyar dos veces no falla ni duplica.
-- Con fotos crea además un reporte de tipo `apoyo` en la ubicación del incidente.
create or replace function public.apoyar_incidente(
    p_cliente_id uuid,
    p_incidente_id uuid,
    p_fotos text[] default null
)
returns table (
    incidente_id uuid,
    apoyos integer,
    prioridad integer
)
language plpgsql
security definer
set search_path = ''
as $$
#variable_conflict use_column
declare
    v_uid uuid := auth.uid();
    v_estado public.estado;
    v_geom extensions.geography;
    v_con_fotos boolean := p_fotos is not null and cardinality(p_fotos) > 0;
    v_insertado integer;
    v_reporte_id uuid;
    v_foto text;
begin
    if v_uid is null then
        raise exception 'Se requiere una sesión' using errcode = '42501';
    end if;
    if (select private.rol_actual()) is distinct from 'ciudadano'::public.rol then
        raise exception 'Solo un ciudadano con la cuenta activa puede apoyar' using errcode = '42501';
    end if;
    if p_cliente_id is null or p_incidente_id is null then
        raise exception 'Faltan datos para apoyar' using errcode = '22023';
    end if;

    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(v_uid::text || ':' || p_cliente_id::text, 0)
    );

    select i.status, i.geom into v_estado, v_geom
    from public.incidentes i
    where i.id = p_incidente_id;

    if not found then
        raise exception 'Incidente no encontrado' using errcode = '22023';
    end if;

    -- Reintento (doble toque o cola sin conexión): se devuelve el estado actual.
    if exists (
        select 1 from public.apoyos a
        where a.usuario_id = v_uid and a.incidente_id = p_incidente_id
    ) then
        return query
        select i.id, i.apoyos_count, i.prioridad
        from public.incidentes i
        where i.id = p_incidente_id;
        return;
    end if;

    if v_estado in ('resuelto', 'rechazado') then
        raise exception 'Este incidente ya está cerrado y no admite apoyos' using errcode = '22023';
    end if;
    if exists (
        select 1 from public.reportes r
        where r.incidente_id = p_incidente_id
          and r.usuario_id = v_uid
          and r.tipo = 'original'
    ) then
        raise exception 'No puedes apoyar tu propio reporte' using errcode = '22023';
    end if;

    if v_con_fotos then
        perform private.validar_fotos(p_fotos);
    end if;

    insert into public.apoyos (usuario_id, incidente_id)
    values (v_uid, p_incidente_id)
    on conflict do nothing;
    get diagnostics v_insertado = row_count;

    if v_insertado = 1 and v_con_fotos then
        insert into public.reportes (usuario_id, cliente_id, incidente_id, tipo, geom)
        values (v_uid, p_cliente_id, p_incidente_id, 'apoyo', v_geom)
        returning id into v_reporte_id;

        foreach v_foto in array p_fotos loop
            insert into public.fotos (reporte_id, storage_path)
            values (v_reporte_id, v_foto);
        end loop;
    end if;

    return query
    select i.id, i.apoyos_count, i.prioridad
    from public.incidentes i
    where i.id = p_incidente_id;
end;
$$;

revoke execute on function public.apoyar_incidente(uuid, uuid, text[]) from public, anon;
grant execute on function public.apoyar_incidente(uuid, uuid, text[]) to authenticated;

-- Retirar el apoyo ya es posible con DELETE sobre `apoyos` (política apoyos_borrado_propio).

-- Quién puede leer un objeto del bucket `fotos`:
--   * el autor del reporte, siempre;
--   * cualquiera (también el anónimo), si la foto no está oculta y el incidente no fue rechazado;
--   * moderación con segundo factor, todas (necesita verlas para decidir si las oculta).
-- El cliente pide la URL firmada con su propio JWT; no hace falta `service_role`.
create or replace function private.puede_ver_foto(p_ruta text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.fotos f
        join public.reportes r on r.id = f.reporte_id
        join public.incidentes i on i.id = r.incidente_id
        where f.storage_path = p_ruta
          and (
              r.usuario_id = (select auth.uid())
              or (not f.oculta and i.status <> 'rechazado')
              or ((select private.rol_actual()) = 'moderador' and (select private.aal2()))
          )
    )
$$;

revoke execute on function private.puede_ver_foto(text) from public;
grant execute on function private.puede_ver_foto(text) to anon, authenticated;

create policy fotos_lectura_visible on storage.objects
    as permissive for select to anon, authenticated
    using (bucket_id = 'fotos' and private.puede_ver_foto(name));

-- Rutas de las fotos visibles de un incidente, para el detalle público.
create or replace function public.fotos_incidente(p_incidente_id uuid)
returns table (ruta text)
language sql
stable
security definer
set search_path = ''
as $$
    select f.storage_path
    from public.fotos f
    join public.reportes r on r.id = f.reporte_id
    join public.incidentes i on i.id = r.incidente_id
    where r.incidente_id = p_incidente_id
      and not f.oculta
      and i.status <> 'rechazado'
    order by r.creado_en asc, f.id asc
    limit 12;
$$;

revoke execute on function public.fotos_incidente(uuid) from public, anon;
grant execute on function public.fotos_incidente(uuid) to anon, authenticated;
