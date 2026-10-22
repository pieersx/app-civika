-- Endurece la privacidad de fotos e historial.
-- Va en una migración nueva: las migraciones anteriores ya pueden estar
-- aplicadas en otros entornos y `supabase db push` no las vuelve a ejecutar.

create or replace function private.validar_fotos(p_fotos text[])
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_foto text;
    v_uid text;
begin
    if p_fotos is null or cardinality(p_fotos) = 0 then
        raise exception 'El reporte necesita al menos una foto' using errcode = '22023';
    end if;
    if cardinality(p_fotos) > 3 then
        raise exception 'Máximo 3 fotos por reporte' using errcode = '22023';
    end if;
    if cardinality(p_fotos) <> (select count(distinct x) from unnest(p_fotos) x) then
        raise exception 'No se puede repetir la misma foto' using errcode = '22023';
    end if;

    v_uid := auth.uid()::text;
    foreach v_foto in array p_fotos loop
        if v_uid is null or v_foto not like v_uid || '/%' then
            raise exception 'Cada foto debe estar en la carpeta del usuario' using errcode = '42501';
        end if;
        if not exists (
            select 1
            from storage.objects o
            where o.bucket_id = 'fotos'
              and o.name = v_foto
              and o.archived_at is null
              and not o.is_delete_marker
        ) then
            raise exception 'La foto % no existe en Storage', v_foto using errcode = '22023';
        end if;
        -- El path empieza por el uid, pero además el objeto debe pertenecer al
        -- usuario autenticado. Storage escribe owner_id; owner se conserva para
        -- filas antiguas.
        if not exists (
            select 1
            from storage.objects o
            where o.bucket_id = 'fotos'
              and o.name = v_foto
              and coalesce(o.owner_id, o.owner::text) = auth.uid()::text
              and o.archived_at is null
              and not o.is_delete_marker
        ) then
            raise exception 'La foto % no pertenece al usuario', v_foto using errcode = '42501';
        end if;
    end loop;
end;
$$;

revoke execute on function private.validar_fotos(text[]) from public;

-- `authenticated` es un solo rol. Se revoca el privilegio de tabla y se vuelve
-- a conceder solo lo necesario: el path de Storage contiene el uid del autor y
-- el evento inicial del historial también.
revoke select on public.fotos from authenticated;
grant select (id, reporte_id, oculta) on public.fotos to authenticated;

revoke select on public.historial_estados from authenticated;
grant select (id, incidente_id, estado_anterior, estado_nuevo, comentario, creado_en)
    on public.historial_estados to authenticated;
