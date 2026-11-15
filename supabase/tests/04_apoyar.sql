begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- apoyar_incidente (RF-07) y el tope anti-spam de crear_reporte, que cuenta solo los originales.
insert into auth.users (id, email, raw_user_meta_data)
values
    ('00000000-0000-0000-0000-000000000401', 'autora-apoyo@test.local', '{"nombre":"Autora"}'::jsonb),
    ('00000000-0000-0000-0000-000000000402', 'vecino-apoyo@test.local', '{"nombre":"Vecino"}'::jsonb),
    ('00000000-0000-0000-0000-000000000403', 'vecina-fotos@test.local', '{"nombre":"Vecina con fotos"}'::jsonb),
    ('00000000-0000-0000-0000-000000000404', 'moderacion-apoyo@test.local', '{"nombre":"Moderación"}'::jsonb),
    ('00000000-0000-0000-0000-000000000405', 'entidad-apoyo@test.local', '{"nombre":"Entidad"}'::jsonb),
    ('00000000-0000-0000-0000-000000000406', 'inactivo-apoyo@test.local', '{"nombre":"Inactivo"}'::jsonb);

update public.usuarios set rol = 'moderador' where id = '00000000-0000-0000-0000-000000000404';
update public.usuarios
set rol = 'entidad', entidad_id = (select min(id) from public.entidades)
where id = '00000000-0000-0000-0000-000000000405';
update public.usuarios set activo = false where id = '00000000-0000-0000-0000-000000000406';

insert into public.incidentes (id, codigo, categoria_id, zona_id, geom, status)
select v.id::uuid, v.codigo, c.id, z.id,
       ST_SetSRID(ST_MakePoint(-77.0845, -12.0575), 4326)::geography, v.estado::public.estado
from (values
    ('00000000-0000-0000-0000-0000000004a1', 'T04-001', 'derivado'),
    ('00000000-0000-0000-0000-0000000004a2', 'T04-002', 'resuelto'),
    ('00000000-0000-0000-0000-0000000004a3', 'T04-003', 'rechazado'),
    ('00000000-0000-0000-0000-0000000004a4', 'T04-004', 'en_atencion'),
    ('00000000-0000-0000-0000-0000000004a5', 'T04-005', 'derivado')
) as v(id, codigo, estado),
public.categorias c, public.zonas z
where c.service_code = 'vial' and z.nombre = 'Ciudad Universitaria UNMSM';

insert into public.reportes (usuario_id, cliente_id, incidente_id, tipo, descripcion, geom)
select '00000000-0000-0000-0000-000000000401', gen_random_uuid(), i.id, 'original', 'Bache de prueba en la pista', i.geom
from public.incidentes i
where i.codigo like 'T04-%';

insert into storage.objects (bucket_id, name, owner_id)
values
    ('fotos', '00000000-0000-0000-0000-000000000402/f1.jpg', '00000000-0000-0000-0000-000000000402'),
    ('fotos', '00000000-0000-0000-0000-000000000403/x1.jpg', '00000000-0000-0000-0000-000000000403'),
    ('fotos', '00000000-0000-0000-0000-000000000403/x2.jpg', '00000000-0000-0000-0000-000000000403');

-- Vecino sin fotos.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000402","role":"authenticated"}';

select is(
    (select apoyos from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c1', '00000000-0000-0000-0000-0000000004a1')),
    1,
    'el ciudadano apoya un incidente abierto'
);
select is(
    (select apoyos from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c1', '00000000-0000-0000-0000-0000000004a1')),
    1,
    'apoyar dos veces no duplica ni falla'
);
select lives_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c2', '00000000-0000-0000-0000-0000000004a4', '{}'::text[])$$,
    'una lista de fotos vacía apoya sin error'
);
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c3', '00000000-0000-0000-0000-0000000004a5',
        array['00000000-0000-0000-0000-000000000403/x1.jpg'])$$,
    '42501', null, 'una foto de la carpeta de otra persona falla'
);
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c3', '00000000-0000-0000-0000-0000000004a5',
        array['a', 'b', 'c', 'd'])$$,
    '22023', null, 'más de 3 fotos falla'
);
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c4', '00000000-0000-0000-0000-0000000004a2')$$,
    '22023', null, 'un incidente resuelto no admite apoyos'
);
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c4', '00000000-0000-0000-0000-0000000004a3')$$,
    '22023', null, 'un incidente rechazado no admite apoyos'
);
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004c4', '00000000-0000-0000-0000-0000000004ff')$$,
    '22023', null, 'un incidente inexistente falla'
);
reset role;

select is(
    (select apoyos_count from public.incidentes where codigo = 'T04-001'), 1,
    'el trigger mantiene apoyos_count'
);
select is(
    (select prioridad from public.incidentes where codigo = 'T04-001'),
    (select private.calcular_prioridad(i.categoria_id, 1, i.creado_en) from public.incidentes i where i.codigo = 'T04-001'),
    'la prioridad se recalcula con el apoyo'
);
select is(
    (select count(*) from public.reportes r
     where r.usuario_id = '00000000-0000-0000-0000-000000000402' and r.tipo = 'apoyo'),
    0::bigint,
    'apoyar sin fotos no crea ningún reporte'
);

-- Vecina con fotos.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000403","role":"authenticated"}';
select is(
    (select apoyos from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004d1', '00000000-0000-0000-0000-0000000004a1',
        array['00000000-0000-0000-0000-000000000403/x1.jpg'])),
    2,
    'apoyar con fotos suma el apoyo'
);
select lives_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004d1', '00000000-0000-0000-0000-0000000004a1',
        array['00000000-0000-0000-0000-000000000403/x1.jpg'])$$,
    'repetir el mismo cliente_id con fotos no falla'
);
select lives_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004d2', '00000000-0000-0000-0000-0000000004a1',
        array['00000000-0000-0000-0000-000000000403/x2.jpg'])$$,
    'repetir con otro cliente_id y fotos nuevas no falla'
);
reset role;

select is(
    (select count(*) from public.reportes r
     where r.usuario_id = '00000000-0000-0000-0000-000000000403' and r.tipo = 'apoyo'),
    1::bigint,
    'con fotos se crea un solo reporte de tipo apoyo, también tras los reintentos'
);
select is(
    (select count(*) from public.fotos f
     join public.reportes r on r.id = f.reporte_id
     where r.usuario_id = '00000000-0000-0000-0000-000000000403'),
    1::bigint,
    'y una sola foto'
);

-- La autora no apoya su propio reporte.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000401","role":"authenticated"}';
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004e1', '00000000-0000-0000-0000-0000000004a1')$$,
    '22023', null, 'el autor no puede apoyar su propio incidente'
);
reset role;

-- Otros roles y cuentas.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000404","role":"authenticated","aal":"aal2"}';
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004e2', '00000000-0000-0000-0000-0000000004a1')$$,
    '42501', null, 'la moderación no apoya'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000405","role":"authenticated","aal":"aal2"}';
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004e3', '00000000-0000-0000-0000-0000000004a1')$$,
    '42501', null, 'el personal de entidad no apoya'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000406","role":"authenticated"}';
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004e4', '00000000-0000-0000-0000-0000000004a1')$$,
    '42501', null, 'una cuenta desactivada no apoya'
);
reset role;

set local role anon;
set local request.jwt.claims = '{"role":"anon"}';
select throws_ok(
    $$select * from public.apoyar_incidente(
        '00000000-0000-0000-0000-0000000004e5', '00000000-0000-0000-0000-0000000004a1')$$,
    '42501', null, 'un anónimo no puede ejecutarla'
);
reset role;

-- Tope anti-spam: 10 reportes de tipo apoyo no bloquean; 10 originales sí.
insert into public.reportes (usuario_id, cliente_id, incidente_id, tipo, geom)
select '00000000-0000-0000-0000-000000000402', gen_random_uuid(), i.id, 'apoyo', i.geom
from public.incidentes i, generate_series(1, 10)
where i.codigo = 'T04-005';

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000402","role":"authenticated"}';
select lives_ok(
    $$select * from public.crear_reporte(
        '00000000-0000-0000-0000-0000000004f1', -12.0580, -77.0850,
        (select id from public.categorias where service_code = 'residuos'),
        'Basura acumulada junto al paradero', array['00000000-0000-0000-0000-000000000402/f1.jpg'])$$,
    'diez reportes de tipo apoyo no bloquean a crear_reporte'
);
reset role;

insert into public.reportes (usuario_id, cliente_id, incidente_id, tipo, descripcion, geom)
select '00000000-0000-0000-0000-000000000402', gen_random_uuid(), i.id, 'original', 'Relleno para el tope', i.geom
from public.incidentes i, generate_series(1, 9)
where i.codigo = 'T04-005';

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000402","role":"authenticated"}';
select throws_ok(
    $$select * from public.crear_reporte(
        '00000000-0000-0000-0000-0000000004f2', -12.0580, -77.0850,
        (select id from public.categorias where service_code = 'residuos'),
        'Otro reporte que ya supera el tope', array['00000000-0000-0000-0000-000000000402/f1.jpg'])$$,
    '22023', null, 'diez originales en una hora sí bloquean'
);
reset role;

select * from finish();
rollback;
