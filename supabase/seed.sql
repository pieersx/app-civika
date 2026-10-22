-- Seed del piloto (issues #1 y #3). Idempotente: se puede volver a ejecutar
-- con `supabase db reset` sin duplicar filas.
--
-- El polígono es una aproximación operativa de la Ciudad Universitaria de la
-- UNMSM y su entorno inmediato. No sustituye al polígono oficial del distrito;
-- el administrador puede reemplazarlo desde el dashboard sin tocar el esquema.

insert into public.categorias (
    service_code, nombre, peso_prioridad, radio_duplicado_m, ventana_dias, plazo_objetivo_dias
)
values
    ('vial', 'Pistas y veredas', 3, 50, 30, 15),
    ('residuos', 'Residuos', 2, 30, 30, 5),
    ('accesibilidad', 'Accesibilidad', 3, 25, 30, 10)
on conflict (service_code) do update
set nombre = excluded.nombre,
    peso_prioridad = excluded.peso_prioridad,
    radio_duplicado_m = excluded.radio_duplicado_m,
    ventana_dias = excluded.ventana_dias,
    plazo_objetivo_dias = excluded.plazo_objetivo_dias,
    activa = true;

insert into public.entidades (nombre, tipo)
values
    ('Municipalidad del distrito piloto', 'municipalidad'),
    ('Universidad Nacional Mayor de San Marcos', 'universidad'),
    ('Concesionaria de residuos del distrito piloto', 'operador')
on conflict (nombre) do update set tipo = excluded.tipo;

with campus as (
    select extensions.ST_SetSRID(
        extensions.ST_GeomFromText(
            'POLYGON((
                -77.0920 -12.0520,
                -77.0770 -12.0520,
                -77.0770 -12.0630,
                -77.0920 -12.0630,
                -77.0920 -12.0520
            ))'
        ),
        4326
    ) as geom
)
insert into public.zonas (nombre, tipo, ubigeo, geom)
select
    'Ciudad Universitaria UNMSM',
    'campus',
    '150101',
    extensions.ST_Multi(campus.geom)
from campus
on conflict (nombre) do update
set tipo = excluded.tipo,
    ubigeo = excluded.ubigeo,
    geom = excluded.geom;

with
campus as (
    select extensions.ST_SetSRID(
        extensions.ST_GeomFromText(
            'POLYGON((
                -77.0920 -12.0520,
                -77.0770 -12.0520,
                -77.0770 -12.0630,
                -77.0920 -12.0630,
                -77.0920 -12.0520
            ))'
        ),
        4326
    ) as geom
),
entorno as (
    select extensions.ST_SetSRID(
        extensions.ST_MakeEnvelope(-77.0980, -12.0680, -77.0710, -12.0470, 4326),
        4326
    ) as geom
)
insert into public.zonas (nombre, tipo, ubigeo, geom)
select
    'Alrededores de la Ciudad Universitaria',
    'distrito',
    '150101',
    extensions.ST_Multi(extensions.ST_Difference(entorno.geom, campus.geom))
from entorno, campus
on conflict (nombre) do update
set tipo = excluded.tipo,
    ubigeo = excluded.ubigeo,
    geom = excluded.geom;

-- Ruteo: el campus va a la universidad; el resto se reparte entre la
-- municipalidad y la concesionaria de residuos.
insert into public.reglas_ruteo (categoria_id, zona_id, entidad_id)
select c.id, z.id, e.id
from public.categorias c
cross join public.zonas z
join public.entidades e on e.nombre = 'Universidad Nacional Mayor de San Marcos'
where z.nombre = 'Ciudad Universitaria UNMSM'
on conflict (categoria_id, zona_id) do update set entidad_id = excluded.entidad_id;

insert into public.reglas_ruteo (categoria_id, zona_id, entidad_id)
select c.id, z.id, e.id
from public.categorias c
cross join public.zonas z
join public.entidades e on e.nombre = 'Municipalidad del distrito piloto'
where z.nombre = 'Alrededores de la Ciudad Universitaria'
  and c.service_code in ('vial', 'accesibilidad')
on conflict (categoria_id, zona_id) do update set entidad_id = excluded.entidad_id;

insert into public.reglas_ruteo (categoria_id, zona_id, entidad_id)
select c.id, z.id, e.id
from public.categorias c
cross join public.zonas z
join public.entidades e on e.nombre = 'Concesionaria de residuos del distrito piloto'
where z.nombre = 'Alrededores de la Ciudad Universitaria'
  and c.service_code = 'residuos'
on conflict (categoria_id, zona_id) do update set entidad_id = excluded.entidad_id;
