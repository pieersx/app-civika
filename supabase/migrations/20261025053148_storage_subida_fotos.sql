-- Subida de fotos al bucket `fotos` (docs/arquitectura.md §7.3, issue #29).
-- storage.objects tiene RLS activado y no tenía ninguna política: ningún usuario autenticado podía subir.
-- Esta política permite solo INSERT: el ciudadano activo sube dentro de su propia carpeta `{uid}/`, a un solo nivel (sin subcarpetas ni `..`).
-- La lectura de fotos se resuelve aparte, con la RPC urls_fotos (Sprint 2); aquí no se concede.

create policy fotos_subida_propia on storage.objects
    as permissive for insert to authenticated
    with check (
        bucket_id = 'fotos'
        and (select private.rol_actual()) = 'ciudadano'
        and (storage.foldername(name))[1] = (select auth.uid())::text
        and array_length(storage.foldername(name), 1) = 1
    );
