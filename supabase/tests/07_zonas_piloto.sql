begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- Zonas del piloto (seed): el campus sale del contorno de OpenStreetMap y «Alrededores» es el resto del rectángulo.
-- Lee los datos del seed: se ejecuta sobre una base recién reseteada (`supabase db reset`), sin demo.sql.

select is(
    (select count(*) from public.zonas where nombre in ('Ciudad Universitaria UNMSM', 'Alrededores de la Ciudad Universitaria')),
    2::bigint,
    'existen las dos zonas del piloto'
);
select ok(
    (select bool_and(ST_IsValid(geom::geometry)) from public.zonas
     where nombre in ('Ciudad Universitaria UNMSM', 'Alrededores de la Ciudad Universitaria')),
    'las geometrías de las zonas son válidas'
);
select ok(
    (select ST_Area(geom::geography) / 10000 between 65 and 75 from public.zonas where nombre = 'Ciudad Universitaria UNMSM'),
    'el campus mide entre 65 y 75 hectáreas (el contorno real; el rectángulo antiguo medía unas 150)'
);
select is(
    (select round(ST_Area(ST_Intersection(c.geom::geometry, a.geom::geometry)::geography))
     from public.zonas c, public.zonas a
     where c.nombre = 'Ciudad Universitaria UNMSM' and a.nombre = 'Alrededores de la Ciudad Universitaria'),
    0::double precision,
    'las zonas no se superponen'
);

-- Cada punto cae en una sola zona; fuera del rectángulo no hay zona (crear_reporte responde «fuera de la zona piloto»).
select is(
    (select z.nombre from public.zonas z
     where ST_Covers(z.geom, ST_SetSRID(ST_MakePoint(-77.0845, -12.0575), 4326)::geography)),
    'Ciudad Universitaria UNMSM',
    'el centro del campus cae en la zona del campus'
);
select is(
    (select z.nombre from public.zonas z
     where ST_Covers(z.geom, ST_SetSRID(ST_MakePoint(-77.0960, -12.0560), 4326)::geography)),
    'Alrededores de la Ciudad Universitaria',
    'la calle de enfrente cae en los alrededores'
);
select is(
    (select count(*) from public.zonas z
     where ST_Covers(z.geom, ST_SetSRID(ST_MakePoint(-77.0428, -12.0464), 4326)::geography)),
    0::bigint,
    'la Plaza de Armas queda fuera de la zona piloto'
);
-- Sin huecos: campus + alrededores cubren exactamente el rectángulo de la zona piloto (un hueco dejaría a quien
-- reporta junto al borde del campus con «fuera de la zona piloto»).
select ok(
    (select abs(
        ST_Area(ST_Union(c.geom::geometry, a.geom::geometry)::geography)
        - ST_Area(ST_MakeEnvelope(-77.0980, -12.0680, -77.0710, -12.0470, 4326)::geography)
    ) < 1
     from public.zonas c, public.zonas a
     where c.nombre = 'Ciudad Universitaria UNMSM' and a.nombre = 'Alrededores de la Ciudad Universitaria'),
    'las dos zonas juntas cubren todo el rectángulo, sin huecos'
);
-- Un punto 3 m fuera de cada vértice del campus siempre pertenece a una zona (la de los alrededores).
select is(
    (select count(*) from public.zonas c,
        lateral (select (ST_DumpPoints(ST_ExteriorRing(ST_GeometryN(c.geom::geometry, 1)))).geom as v) puntos
     where c.nombre = 'Ciudad Universitaria UNMSM'
       and not exists (
           select 1 from public.zonas z
           where ST_Covers(z.geom, ST_SetSRID(ST_Translate(puntos.v, 0.00003, 0), 4326)::geography)
              or ST_Covers(z.geom, ST_SetSRID(ST_Translate(puntos.v, -0.00003, 0), 4326)::geography)
       )),
    0::bigint,
    'ningún punto pegado al borde del campus queda sin zona'
);

select * from finish();
rollback;
