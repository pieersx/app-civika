begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- ---------------------------------------------------------------------------
-- Datos de prueba: cuatro cuentas, cuatro fotos y cuatro incidentes.
-- El trigger de auth.users crea las filas de public.usuarios.
-- ---------------------------------------------------------------------------
insert into auth.users (id, email, raw_user_meta_data)
values
    ('00000000-0000-0000-0000-000000000001', 'ciudadano@test.local', '{"nombre":"Ciudadano de prueba"}'::jsonb),
    ('00000000-0000-0000-0000-000000000002', 'entidad-a@test.local', '{"nombre":"Personal UNMSM"}'::jsonb),
    ('00000000-0000-0000-0000-000000000003', 'entidad-b@test.local', '{"nombre":"Personal municipal"}'::jsonb),
    ('00000000-0000-0000-0000-000000000004', 'moderador@test.local', '{"nombre":"Moderación"}'::jsonb);

update public.usuarios
set rol = 'entidad',
    entidad_id = (select id from public.entidades where nombre = 'Universidad Nacional Mayor de San Marcos')
where id = '00000000-0000-0000-0000-000000000002';

update public.usuarios
set rol = 'entidad',
    entidad_id = (select id from public.entidades where nombre = 'Municipalidad del distrito piloto')
where id = '00000000-0000-0000-0000-000000000003';

update public.usuarios
set rol = 'moderador'
where id = '00000000-0000-0000-0000-000000000004';

insert into storage.objects (bucket_id, name, owner, owner_id, archived_at)
values
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-1.jpg', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-2.jpg', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-3.jpg', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-4.jpg', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-5.jpg', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-ajena.jpg', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-legacy.jpg', '00000000-0000-0000-0000-000000000001', null, null),
    ('fotos', '00000000-0000-0000-0000-000000000001/foto-archivada.jpg', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', now());

insert into storage.objects (bucket_id, name, owner, owner_id, is_delete_marker)
values (
    'fotos',
    '00000000-0000-0000-0000-000000000001/foto-delete-marker.jpg',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    true
);

-- La función privada se prueba como postgres; auth.uid() sale de los claims.
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000001","role":"authenticated"}';
select lives_ok(
    $$select private.validar_fotos(array['00000000-0000-0000-0000-000000000001/foto-legacy.jpg'])$$,
    'validar_fotos acepta un objeto antiguo con owner y owner_id nulo'
);
select throws_ok(
    $$select private.validar_fotos(array['00000000-0000-0000-0000-000000000001/foto-delete-marker.jpg'])$$,
    '22023',
    'La foto 00000000-0000-0000-0000-000000000001/foto-delete-marker.jpg no existe en Storage',
    'validar_fotos rechaza delete markers'
);

create temp table creados (
    nombre text primary key,
    incidente_id uuid not null,
    codigo text not null,
    estado public.estado not null,
    entidad_id integer
);
grant select, insert on creados to authenticated, anon;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000001","role":"authenticated"}';

insert into creados
select 'campus', * from public.crear_reporte(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    -12.0565,
    -77.0850,
    (select id from public.categorias where service_code = 'vial'),
    'Bache profundo en la pista principal',
    array['00000000-0000-0000-0000-000000000001/foto-1.jpg'],
    false,
    'Av. Universitaria 123'
);

insert into creados
select 'campus_retry', * from public.crear_reporte(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    -12.0565,
    -77.0850,
    (select id from public.categorias where service_code = 'vial'),
    'Bache profundo en la pista principal',
    array['00000000-0000-0000-0000-000000000001/foto-1.jpg'],
    false,
    'Av. Universitaria 123'
);

insert into creados
select 'distrito', * from public.crear_reporte(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    -12.0500,
    -77.0750,
    (select id from public.categorias where service_code = 'vial'),
    'Pista hundida frente al mercado',
    array['00000000-0000-0000-0000-000000000001/foto-2.jpg'],
    false,
    'Av. Venezuela 456'
);

insert into creados
select 'simulado', * from public.crear_reporte(
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    -12.0566,
    -77.0851,
    (select id from public.categorias where service_code = 'residuos'),
    'Basura acumulada en la esquina',
    array['00000000-0000-0000-0000-000000000001/foto-3.jpg'],
    true,
    'Av. Universitaria 789'
);

insert into creados
select 'simulado2', * from public.crear_reporte(
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    -12.0567,
    -77.0852,
    (select id from public.categorias where service_code = 'accesibilidad'),
    'Vereda sin rampa en el cruce',
    array['00000000-0000-0000-0000-000000000001/foto-4.jpg'],
    true,
    'Av. Universitaria 101'
);

reset role;

-- ---------------------------------------------------------------------------
-- Esquema, seed y bucket.
-- ---------------------------------------------------------------------------
select is((select count(*) from public.categorias), 3::bigint, 'seed: 3 categorías');
select is((select count(*) from public.zonas), 2::bigint, 'seed: 2 zonas del piloto');
select is((select count(*) from public.entidades), 3::bigint, 'seed: 3 entidades simuladas');
select is((select count(*) from public.reglas_ruteo), 6::bigint, 'seed: 6 reglas de ruteo');
select is((select count(*) from storage.buckets where id = 'fotos'), 1::bigint, 'bucket privado fotos');
select is((select count(*) from public.usuarios where id in (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000003',
    '00000000-0000-0000-0000-000000000004'
)), 4::bigint, 'el trigger de auth.users crea los perfiles');

-- ---------------------------------------------------------------------------
-- crear_reporte: ruteo, idempotencia, estado simulado y código.
-- ---------------------------------------------------------------------------
select is((select count(*) from creados), 5::bigint, 'se ejecutaron las cinco llamadas de creación');
select is((select count(distinct incidente_id) from creados), 4::bigint, 'el reintento no crea otro incidente');
select is((select count(*) from public.incidentes), 4::bigint, 'cuatro incidentes persistidos');
select is((select count(*) from public.reportes), 4::bigint, 'cuatro reportes persistidos');
select is(
    (select count(*) from public.reportes where cliente_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    1::bigint,
    'idempotencia por (usuario_id, cliente_id)'
);
select is(
    (select incidente_id from creados where nombre = 'campus'),
    (select incidente_id from creados where nombre = 'campus_retry'),
    'el reintento devuelve el mismo incidente'
);
select is((select estado from creados where nombre = 'campus'), 'derivado'::public.estado, 'campus vial se deriva');
select is(
    (select entidad_id from creados where nombre = 'campus'),
    (select id from public.entidades where nombre = 'Universidad Nacional Mayor de San Marcos'),
    'campus vial se rutea a la UNMSM'
);
select is((select estado from creados where nombre = 'distrito'), 'derivado'::public.estado, 'distrito vial se deriva');
select is(
    (select entidad_id from creados where nombre = 'distrito'),
    (select id from public.entidades where nombre = 'Municipalidad del distrito piloto'),
    'distrito vial se rutea a la municipalidad'
);
select is((select estado from creados where nombre = 'simulado'), 'en_revision'::public.estado, 'ubicación simulada va a revisión');
select is((select entidad_id from creados where nombre = 'simulado'), null::integer, 'ubicación simulada no se asigna a entidad');
select is(
    (select bool_and(codigo ~ '^CVK-[0-9]{6}$') from creados),
    true,
    'todos los códigos tienen el formato CVK-000000'
);
select is(
    (select prioridad from public.incidentes where id = (select incidente_id from creados where nombre = 'campus')),
    30,
    'prioridad inicial: peso 3 × 10, sin apoyos ni días'
);

-- ---------------------------------------------------------------------------
-- buscar_duplicados: metros, ventana y categoría.
-- ---------------------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000001","role":"authenticated"}';

select is(
    (select count(*) from public.buscar_duplicados(
        -12.0568,
        -77.0850,
        (select id from public.categorias where service_code = 'vial')
    )),
    1::bigint,
    'buscar_duplicados encuentra el bache dentro de 50 m'
);
select is(
    (select distancia_m between 20 and 50 from public.buscar_duplicados(
        -12.0568,
        -77.0850,
        (select id from public.categorias where service_code = 'vial')
    )),
    true,
    'buscar_duplicados devuelve la distancia en metros'
);
select is(
    (select count(*) from public.buscar_duplicados(
        -12.0572,
        -77.0850,
        (select id from public.categorias where service_code = 'vial')
    )),
    0::bigint,
    'buscar_duplicados respeta el radio de 50 m'
);
select is(
    (select count(*) from public.buscar_duplicados(
        -12.0500,
        -77.0750,
        (select id from public.categorias where service_code = 'vial')
    )),
    1::bigint,
    'buscar_duplicados filtra por categoría'
);

-- ---------------------------------------------------------------------------
-- Crear fuera de zona y sin fotos.
-- ---------------------------------------------------------------------------
select throws_ok(
    $$select * from public.crear_reporte(
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        0,
        0,
        (select id from public.categorias where service_code = 'vial'),
        'Reporte fuera de la zona piloto',
        array['00000000-0000-0000-0000-000000000001/foto-5.jpg'],
        false,
        null
    )$$,
    'P0001',
    'Fuera de la zona piloto',
    'crear_reporte rechaza coordenadas fuera de las zonas'
);

select throws_ok(
    $$select * from public.crear_reporte(
        'ffffffff-ffff-ffff-ffff-ffffffffffff',
        -12.0565,
        -77.0850,
        (select id from public.categorias where service_code = 'vial'),
        'Reporte sin foto',
        array[]::text[],
        false,
        null
    )$$,
    '22023',
    'El reporte necesita al menos una foto',
    'crear_reporte exige al menos una foto'
);

select throws_ok(
    $$select * from public.crear_reporte(
        'abababab-abab-abab-abab-abababababab',
        -12.0565,
        -77.0850,
        (select id from public.categorias where service_code = 'vial'),
        'Reporte con una foto de otra cuenta',
        array['00000000-0000-0000-0000-000000000001/foto-ajena.jpg'],
        false,
        null
    )$$,
    '42501',
    'La foto 00000000-0000-0000-0000-000000000001/foto-ajena.jpg no pertenece al usuario',
    'crear_reporte rechaza fotos cuyo owner es otro usuario'
);

select throws_ok(
    $$select * from public.crear_reporte(
        'acacacac-acac-acac-acac-acacacacacac',
        -12.0565,
        -77.0850,
        (select id from public.categorias where service_code = 'vial'),
        'Reporte con una foto archivada',
        array['00000000-0000-0000-0000-000000000001/foto-archivada.jpg'],
        false,
        null
    )$$,
    '22023',
    'La foto 00000000-0000-0000-0000-000000000001/foto-archivada.jpg no existe en Storage',
    'crear_reporte ignora objetos archivados'
);

-- ---------------------------------------------------------------------------
-- Lectura pública: sin autor, sin dirección, coordenadas redondeadas.
-- ---------------------------------------------------------------------------
reset role;
set local role anon;
select is(
    (select count(*) from public.listar_incidentes_publicos()),
    4::bigint,
    'listar_incidentes_publicos devuelve los incidentes abiertos'
);
select is(
    (select count(*) from public.listar_incidentes_publicos() where estado = 'rechazado'),
    0::bigint,
    'listar_incidentes_publicos no muestra rechazados'
);
select is(
    (select lat from public.listar_incidentes_publicos() where id = (select incidente_id from creados where nombre = 'campus')),
    -12.0565::double precision,
    'la latitud pública está redondeada a 4 decimales'
);
select is(
    (select count(*) from public.historial_publico((select incidente_id from creados where nombre = 'campus'))),
    1::bigint,
    'historial_publico muestra el estado inicial'
);
select is(
    (select count(*) from public.comentarios_publicos((select incidente_id from creados where nombre = 'campus'))),
    0::bigint,
    'comentarios_publicos empieza vacío'
);
reset role;

-- ---------------------------------------------------------------------------
-- RLS: entidad A ve lo suyo; entidad B no; moderación ve todo.
-- ---------------------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000002","role":"authenticated"}';

select is((select count(*) from public.incidentes), 1::bigint, 'entidad A solo ve su incidente');
select is((select count(*) from public.reportes), 1::bigint, 'entidad A solo ve sus reportes');
select is((select count(*) from public.reportes_entidad), 1::bigint, 'la vista reportes_entidad aplica el RLS');
select is(
    (select count(*) from public.historial_estados where incidente_id = (select incidente_id from creados where nombre = 'campus')),
    1::bigint,
    'entidad A ve el historial de su incidente'
);
select is(
    (
        select count(*)
        from (
            select id, reporte_id, oculta
            from public.fotos
            where reporte_id in (select id from public.reportes)
        ) f
    ),
    1::bigint,
    'entidad A ve solo las fotos de sus reportes y sus columnas permitidas'
);
select throws_ok(
    'select storage_path from public.fotos',
    '42501',
    null,
    'entidad A no puede leer storage_path de fotos'
);
select throws_ok(
    'select autor_id from public.historial_estados',
    '42501',
    null,
    'entidad A no puede leer autor_id del historial'
);
select throws_ok(
    'select usuario_id from public.reportes',
    '42501',
    null,
    'entidad A no puede leer usuario_id de reportes'
);
select throws_ok(
    format(
        'update public.usuarios set rol = %L where id = %L',
        'moderador',
        '00000000-0000-0000-0000-000000000002'
    ),
    '42501',
    null,
    'authenticated no puede cambiar su propio rol'
);

insert into public.comentarios (incidente_id, autor_id, visibilidad, texto)
values (
    (select incidente_id from creados where nombre = 'campus'),
    '00000000-0000-0000-0000-000000000002',
    'publico',
    'Cuadrilla asignada para mañana'
);
select is(
    (select count(*) from public.comentarios where incidente_id = (select incidente_id from creados where nombre = 'campus')),
    1::bigint,
    'entidad A puede comentar su incidente'
);

reset role;
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000003","role":"authenticated"}';

select is((select count(*) from public.incidentes), 1::bigint, 'entidad B solo ve su incidente');
select throws_ok(
    format(
        'insert into public.comentarios (incidente_id, autor_id, visibilidad, texto) values (%L, %L, %L, %L)',
        (select incidente_id from creados where nombre = 'campus'),
        '00000000-0000-0000-0000-000000000003',
        'publico',
        'Comentario de otra entidad'
    ),
    '42501',
    null,
    'entidad B no puede comentar un incidente ajeno'
);

reset role;
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000004","role":"authenticated"}';
select is((select count(*) from public.incidentes), 4::bigint, 'moderación ve todos los incidentes');
select is((select count(*) from public.reportes), 4::bigint, 'moderación ve todos los reportes');
select is(
    (select count(*) from public.auditoria where accion = 'usuario_actualizado'),
    3::bigint,
    'los cambios de rol quedaron auditados'
);
reset role;

-- ---------------------------------------------------------------------------
-- cambiar_estado: entidad, versión, transición, MFA y moderación.
-- ---------------------------------------------------------------------------
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000002","role":"authenticated","aal":"aal2"}';

select is(
    (select count(*) from public.cambiar_estado(
        (select incidente_id from creados where nombre = 'campus'),
        'en_atencion',
        0,
        'Cuadrilla en camino',
        null
    )),
    1::bigint,
    'entidad A cambia su incidente a En atención'
);
reset role;
select is(
    (select status from public.incidentes where id = (select incidente_id from creados where nombre = 'campus')),
    'en_atencion'::public.estado,
    'el estado quedó persistido'
);
select is(
    (select version from public.incidentes where id = (select incidente_id from creados where nombre = 'campus')),
    1,
    'el trigger incrementó la versión'
);

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000003","role":"authenticated","aal":"aal2"}';
select throws_ok(
    format(
        'select * from public.cambiar_estado(%L, %L, %s, %L, null)',
        (select incidente_id from creados where nombre = 'campus'),
        'en_revision',
        1,
        'Otra entidad intenta moverlo'
    ),
    '42501',
    'El incidente no pertenece a tu entidad',
    'entidad B no puede cambiar un incidente de A'
);

reset role;
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000002","role":"authenticated","aal":"aal2"}';
select throws_ok(
    format(
        'select * from public.cambiar_estado(%L, %L, %s, %L, null)',
        (select incidente_id from creados where nombre = 'campus'),
        'derivado',
        1,
        'Transición inválida'
    ),
    '42501',
    'Transición no permitida para este rol',
    'entidad no puede derivar'
);
select throws_ok(
    format(
        'select * from public.cambiar_estado(%L, %L, %s, %L, null)',
        (select incidente_id from creados where nombre = 'campus'),
        'en_revision',
        0,
        'Versión vieja'
    ),
    '40001',
    'La versión del incidente cambió; recarga antes de intentar de nuevo',
    'cambiar_estado exige la versión actual'
);
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000002","role":"authenticated"}';
select throws_ok(
    format(
        'select * from public.cambiar_estado(%L, %L, %s, %L, null)',
        (select incidente_id from creados where nombre = 'campus'),
        'en_revision',
        1,
        'Sin MFA'
    ),
    '42501',
    'Se requiere MFA para cambiar estados',
    'cambiar_estado exige AAL2'
);
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000001","role":"authenticated","aal":"aal2"}';
select throws_ok(
    format(
        'select * from public.cambiar_estado(%L, %L, %s, %L, null)',
        (select incidente_id from creados where nombre = 'campus'),
        'en_revision',
        1,
        'Ciudadano intenta cambiar'
    ),
    '42501',
    'Este rol no puede cambiar estados',
    'el ciudadano no puede cambiar estados'
);
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000004","role":"authenticated","aal":"aal2"}';
select throws_ok(
    format(
        'select * from public.cambiar_estado(%L, %L, %s, %L, null)',
        (select incidente_id from creados where nombre = 'simulado'),
        'rechazado',
        0,
        null
    ),
    '22023',
    'El rechazo necesita un motivo',
    'el rechazo exige motivo'
);
select is(
    (select count(*) from public.cambiar_estado(
        (select incidente_id from creados where nombre = 'simulado'),
        'rechazado',
        0,
        'No se pudo verificar la ubicación',
        null
    )),
    1::bigint,
    'moderación rechaza con motivo'
);
reset role;
select is(
    (select status from public.incidentes where id = (select incidente_id from creados where nombre = 'simulado')),
    'rechazado'::public.estado,
    'el rechazo quedó persistido'
);

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000004","role":"authenticated","aal":"aal2"}';
select is(
    (select count(*) from public.cambiar_estado(
        (select incidente_id from creados where nombre = 'simulado2'),
        'derivado',
        0,
        'Se reasigna a la municipalidad',
        (select id from public.entidades where nombre = 'Municipalidad del distrito piloto')
    )),
    1::bigint,
    'moderación deriva con entidad explícita'
);
reset role;
select is(
    (select entidad_id from public.incidentes where id = (select incidente_id from creados where nombre = 'simulado2')),
    (select id from public.entidades where nombre = 'Municipalidad del distrito piloto'),
    'la entidad asignada al derivar quedó persistida'
);
select is(
    (select count(*) from public.auditoria where accion = 'reporte_rechazado'),
    1::bigint,
    'el rechazo dejó auditoría'
);

-- ---------------------------------------------------------------------------
-- Un rechazado ya no aparece en la lectura pública.
-- ---------------------------------------------------------------------------
reset role;
set local role anon;
select is(
    (select count(*) from public.listar_incidentes_publicos()),
    3::bigint,
    'el incidente rechazado sale de la lectura pública'
);
select is(
    (select count(*) from public.historial_publico((select incidente_id from creados where nombre = 'simulado'))),
    0::bigint,
    'un rechazado no expone historial público'
);
reset role;

select * from finish();
rollback;
