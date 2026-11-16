-- Seed del piloto (issues #1 y #3). Idempotente: se puede volver a ejecutar
-- con `supabase db reset` sin duplicar filas.
--
-- El polígono del campus es el contorno de OpenStreetMap de la «Ciudad Universitaria de la Universidad Nacional Mayor
-- de San Marcos» (relación R6252388, © OpenStreetMap contributors, ODbL 1.0). No sustituye al polígono oficial de la
-- universidad; el administrador puede reemplazarlo desde el dashboard sin tocar el esquema. «Alrededores» es el
-- rectángulo de la zona piloto menos el campus.

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

-- Renombra las entidades de la primera versión del seed (sin esto, el insert de abajo crearía filas nuevas y las
-- reglas de ruteo seguirían apuntando a las viejas). Los nombres llevan «(simulada)»: ninguna entidad real recibe estos
-- reportes (ADR-12). Solo renombra si el nombre nuevo no existe todavía; si alguien ya creó una entidad con ese nombre
-- a mano, hay que fusionarlas a mano.
update public.entidades set nombre = 'Municipalidad de Lima (simulada)'
where nombre = 'Municipalidad del distrito piloto'
  and not exists (select 1 from public.entidades where nombre = 'Municipalidad de Lima (simulada)');
update public.entidades set nombre = 'Servicio de limpieza pública (simulado)'
where nombre = 'Concesionaria de residuos del distrito piloto'
  and not exists (select 1 from public.entidades where nombre = 'Servicio de limpieza pública (simulado)');

insert into public.entidades (nombre, tipo)
values
    ('Municipalidad de Lima (simulada)', 'municipalidad'),
    ('Universidad Nacional Mayor de San Marcos', 'universidad'),
    ('Servicio de limpieza pública (simulado)', 'operador')
on conflict (nombre) do update set tipo = excluded.tipo;

with campus as (
    select extensions.ST_SetSRID(
        extensions.ST_GeomFromText(
            'POLYGON((
                -77.088412 -12.058867,
                -77.088066 -12.059292,
                -77.087961 -12.059211,
                -77.087800 -12.059414,
                -77.087914 -12.059508,
                -77.088145 -12.059849,
                -77.088251 -12.060166,
                -77.088241 -12.060434,
                -77.088212 -12.060624,
                -77.088159 -12.060827,
                -77.088111 -12.060894,
                -77.087670 -12.061186,
                -77.087453 -12.061329,
                -77.087430 -12.061336,
                -77.087375 -12.061332,
                -77.087286 -12.061331,
                -77.087179 -12.061342,
                -77.087115 -12.061362,
                -77.087079 -12.061396,
                -77.087071 -12.061418,
                -77.086946 -12.061415,
                -77.086802 -12.061414,
                -77.086659 -12.061409,
                -77.086502 -12.061388,
                -77.086460 -12.061378,
                -77.086421 -12.061365,
                -77.086375 -12.061348,
                -77.086331 -12.061331,
                -77.086302 -12.061317,
                -77.086221 -12.061315,
                -77.086153 -12.061180,
                -77.085984 -12.060875,
                -77.085943 -12.060898,
                -77.085886 -12.060928,
                -77.085844 -12.060950,
                -77.085812 -12.060964,
                -77.085794 -12.060974,
                -77.085754 -12.060995,
                -77.085685 -12.061030,
                -77.085584 -12.061083,
                -77.085518 -12.061116,
                -77.085505 -12.061199,
                -77.085173 -12.061152,
                -77.084667 -12.061082,
                -77.083812 -12.060978,
                -77.082100 -12.060748,
                -77.082060 -12.060738,
                -77.082008 -12.060723,
                -77.081947 -12.060707,
                -77.081341 -12.060577,
                -77.080692 -12.060410,
                -77.080635 -12.060398,
                -77.080588 -12.060388,
                -77.080545 -12.060378,
                -77.080536 -12.060377,
                -77.080498 -12.060367,
                -77.080354 -12.060333,
                -77.079968 -12.060245,
                -77.079385 -12.060086,
                -77.079340 -12.060071,
                -77.079312 -12.060055,
                -77.079296 -12.060042,
                -77.079279 -12.060024,
                -77.079266 -12.060001,
                -77.079259 -12.059974,
                -77.079259 -12.059946,
                -77.079263 -12.059900,
                -77.079273 -12.059854,
                -77.079321 -12.059666,
                -77.079343 -12.059584,
                -77.079350 -12.059556,
                -77.079366 -12.059496,
                -77.079389 -12.059417,
                -77.079616 -12.058506,
                -77.079770 -12.057865,
                -77.079865 -12.057509,
                -77.079926 -12.057300,
                -77.079953 -12.057215,
                -77.079993 -12.057197,
                -77.080070 -12.057224,
                -77.080082 -12.057194,
                -77.080088 -12.057178,
                -77.080093 -12.057163,
                -77.080096 -12.057156,
                -77.080108 -12.057121,
                -77.080112 -12.057113,
                -77.080046 -12.057089,
                -77.080038 -12.057079,
                -77.080030 -12.057062,
                -77.080028 -12.057043,
                -77.080031 -12.057025,
                -77.080164 -12.056833,
                -77.080445 -12.056494,
                -77.080597 -12.056352,
                -77.080751 -12.056224,
                -77.080912 -12.056106,
                -77.081056 -12.056014,
                -77.081087 -12.055998,
                -77.081107 -12.055995,
                -77.081126 -12.055999,
                -77.081141 -12.056007,
                -77.081158 -12.056022,
                -77.081623 -12.055681,
                -77.081839 -12.055528,
                -77.082136 -12.055350,
                -77.082368 -12.055218,
                -77.082403 -12.055137,
                -77.083594 -12.054409,
                -77.083652 -12.054490,
                -77.083673 -12.054495,
                -77.083762 -12.054447,
                -77.083831 -12.054413,
                -77.083855 -12.054397,
                -77.083844 -12.054370,
                -77.083846 -12.054329,
                -77.084055 -12.054206,
                -77.084185 -12.054120,
                -77.084315 -12.054025,
                -77.084427 -12.053930,
                -77.084552 -12.053815,
                -77.084678 -12.053693,
                -77.084791 -12.053560,
                -77.084883 -12.053433,
                -77.084964 -12.053306,
                -77.085053 -12.053133,
                -77.085121 -12.052979,
                -77.085177 -12.052836,
                -77.085212 -12.052688,
                -77.085232 -12.052577,
                -77.085236 -12.052547,
                -77.085313 -12.052124,
                -77.085431 -12.051478,
                -77.085537 -12.051494,
                -77.085570 -12.051500,
                -77.085776 -12.051621,
                -77.086253 -12.051706,
                -77.087542 -12.051931,
                -77.087569 -12.051966,
                -77.087616 -12.051964,
                -77.087638 -12.051964,
                -77.087660 -12.051986,
                -77.087680 -12.052133,
                -77.087735 -12.052607,
                -77.087788 -12.053065,
                -77.087818 -12.053342,
                -77.087841 -12.053578,
                -77.087884 -12.053989,
                -77.087952 -12.054631,
                -77.088054 -12.055592,
                -77.088289 -12.057742,
                -77.088412 -12.058867
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
    -- Se lee el campus ya insertado: una sola copia del polígono, así las dos zonas nunca se desfasan.
    select z.geom::extensions.geometry as geom
    from public.zonas z
    where z.nombre = 'Ciudad Universitaria UNMSM'
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
-- municipalidad y el servicio de limpieza pública (los dos simulados).
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
join public.entidades e on e.nombre = 'Municipalidad de Lima (simulada)'
where z.nombre = 'Alrededores de la Ciudad Universitaria'
  and c.service_code in ('vial', 'accesibilidad')
on conflict (categoria_id, zona_id) do update set entidad_id = excluded.entidad_id;

insert into public.reglas_ruteo (categoria_id, zona_id, entidad_id)
select c.id, z.id, e.id
from public.categorias c
cross join public.zonas z
join public.entidades e on e.nombre = 'Servicio de limpieza pública (simulado)'
where z.nombre = 'Alrededores de la Ciudad Universitaria'
  and c.service_code = 'residuos'
on conflict (categoria_id, zona_id) do update set entidad_id = excluded.entidad_id;
