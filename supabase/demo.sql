-- Datos de demostración del piloto (Sprint 2, tarea 2 del plan).
--
-- Se carga a mano, solo en Supabase local y en el proyecto demo; nunca en producción. No es parte de
-- `seed.sql`: `supabase db reset` no lo ejecuta, así que hay que volver a cargarlo después de cada reset.
-- Idempotente: los ids son fijos y cada insert usa `on conflict do nothing`.
--
-- Las pruebas pgTAP (`supabase test db`) asumen una base SIN incidentes: con el demo cargado fallan. Antes de
-- ejecutarlas, haz `supabase db reset`.
--
-- Requiere el seed (categorías, zonas y reglas de ruteo). `apoyos_count` y `prioridad` NO se escriben a mano:
-- los calculan los triggers a partir de las filas de `public.apoyos`. Por eso hay 5 usuarios que apoyan; un sexto
-- usuario es la autora de los 3 reportes (el autor de un incidente no lo apoya).
--
-- Los 6 usuarios demo se insertan directo en `public.usuarios`, sin fila en `auth.users`: no son cuentas de acceso.
--
-- Para borrar el demo (en este orden):
--   delete from public.incidentes where codigo like 'DEMO-%';
--   delete from public.usuarios where nombre like 'Ciudadano demo%';

begin;

-- Guarda: este archivo no se carga sobre una base que ya tiene incidentes reales.
do $$
begin
    if exists (select 1 from public.incidentes where codigo not like 'DEMO-%') then
        raise exception 'Hay incidentes que no son de demostración: el demo no se carga aquí. En local, haz primero supabase db reset.';
    end if;
end
$$;

-- 6 ciudadanos demo (rol ciudadano y cuenta activa por defecto).
insert into public.usuarios (id, nombre)
select
    ('00000000-0000-0000-0000-0000000d' || lpad(n::text, 4, '0'))::uuid,
    'Ciudadano demo ' || n
from generate_series(1, 6) as n
on conflict do nothing;

-- Un incidente por fila. `dias` = antigüedad; `apoyos` = cuántos usuarios demo (del 1 al 5) lo apoyan.
-- Prioridad esperada = peso × 10 + apoyos + días × 2, más 20 si pasa del plazo (15 / 5 / 10 días).
create temporary table demo_incidentes on commit drop as
select *
from (
    values
        (1, 'DEMO-001', 'vial',          'Alrededores de la Ciudad Universitaria', 'derivado'::public.estado,    12, -77.0950, -12.0500, 5),
        (2, 'DEMO-002', 'accesibilidad', 'Ciudad Universitaria UNMSM',             'en_revision',                 2, -77.0830, -12.0590, 3),
        (3, 'DEMO-003', 'residuos',      'Alrededores de la Ciudad Universitaria', 'en_atencion',                 8, -77.0750, -12.0560, 4),
        (4, 'DEMO-004', 'vial',          'Ciudad Universitaria UNMSM',             'reportado',                   1, -77.0850, -12.0560, 0),
        (5, 'DEMO-005', 'residuos',      'Ciudad Universitaria UNMSM',             'derivado',                    0, -77.0870, -12.0610, 1),
        (6, 'DEMO-006', 'accesibilidad', 'Alrededores de la Ciudad Universitaria', 'en_atencion',                20, -77.0830, -12.0650, 2),
        (7, 'DEMO-007', 'vial',          'Ciudad Universitaria UNMSM',             'resuelto',                   30, -77.0800, -12.0540, 5),
        (8, 'DEMO-008', 'residuos',      'Ciudad Universitaria UNMSM',             'rechazado',                   3, -77.0860, -12.0580, 0)
) as t (n, codigo, categoria, zona, estado, dias, lon, lat, apoyos);

-- Con entidad solo si el incidente ya fue derivado (igual que crear_reporte con una regla de ruteo).
insert into public.incidentes (id, codigo, categoria_id, zona_id, entidad_id, geom, status, creado_en)
select
    ('00000000-0000-0000-0000-0000000e' || lpad(d.n::text, 4, '0'))::uuid,
    d.codigo,
    c.id,
    z.id,
    case when d.estado in ('derivado', 'en_atencion', 'resuelto') then rr.entidad_id end,
    extensions.ST_SetSRID(extensions.ST_MakePoint(d.lon, d.lat), 4326)::extensions.geography,
    d.estado,
    now() - make_interval(days => d.dias)
from demo_incidentes d
join public.categorias c on c.service_code = d.categoria
join public.zonas z on z.nombre = d.zona
left join public.reglas_ruteo rr on rr.categoria_id = c.id and rr.zona_id = z.id
on conflict do nothing;

-- Apoyos reales: el trigger apoyos_recalcular actualiza apoyos_count y prioridad.
insert into public.apoyos (usuario_id, incidente_id)
select
    ('00000000-0000-0000-0000-0000000d' || lpad(u::text, 4, '0'))::uuid,
    ('00000000-0000-0000-0000-0000000e' || lpad(d.n::text, 4, '0'))::uuid
from demo_incidentes d
cross join lateral generate_series(1, d.apoyos) as u
on conflict do nothing;

-- Tres reportes con descripción, de la usuaria 6 (que no apoya): sin ellos, el título que devuelve la RPC es el
-- nombre de la categoría. No hay fotos ni historial: el detalle del incidente es del Sprint 3.
insert into public.reportes (usuario_id, cliente_id, incidente_id, tipo, descripcion, geom, categoria_sugerida, creado_en)
select
    '00000000-0000-0000-0000-0000000d0006'::uuid,
    ('00000000-0000-0000-0000-0000000c' || lpad(r.n::text, 4, '0'))::uuid,
    i.id,
    'original',
    r.descripcion,
    i.geom,
    i.categoria_id,
    i.creado_en
from (
    values
        (1, 'Bache profundo en la pista frente a la puerta 3'),
        (3, 'Basura acumulada en la esquina del mercado'),
        (6, 'Rampa bloqueada por un poste en la vereda')
) as r (n, descripcion)
join public.incidentes i on i.id = ('00000000-0000-0000-0000-0000000e' || lpad(r.n::text, 4, '0'))::uuid
on conflict do nothing;

commit;
