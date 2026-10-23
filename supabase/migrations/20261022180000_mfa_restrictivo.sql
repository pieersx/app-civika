-- MFA obligatorio para moderación y entidad (docs/arquitectura.md §7.2).
-- Una política restrictiva se suma (AND) a las permisivas: el ciudadano pasa sin
-- segundo factor; moderación y entidad solo con una sesión aal2.
-- Va en una migración nueva: las anteriores ya pueden estar aplicadas en otros entornos.

do $$
declare
    v_tabla text;
begin
    foreach v_tabla in array array[
        'incidentes', 'reportes', 'fotos', 'historial_estados', 'denuncias',
        'comentarios', 'auditoria', 'verificaciones', 'apoyos'
    ] loop
        execute format(
            'create policy mfa_requerido on public.%I as restrictive for all to authenticated '
            'using ((select private.rol_actual()) = ''ciudadano'' or (select private.aal2())) '
            'with check ((select private.rol_actual()) = ''ciudadano'' or (select private.aal2()))',
            v_tabla
        );
    end loop;
end;
$$;

-- usuarios solo se restringe al leer: moderación sin aal2 conserva su propia fila,
-- para que el cliente pueda saber su rol y pedir el segundo factor.
create policy mfa_requerido_lectura on public.usuarios
    as restrictive for select to authenticated
    using (
        id = (select auth.uid())
        or (select private.rol_actual()) = 'ciudadano'
        or (select private.aal2())
    );
