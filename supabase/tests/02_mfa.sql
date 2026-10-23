begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- MFA (docs/arquitectura.md §7.2): moderación y entidad solo ven datos internos con aal2.
-- El ciudadano no necesita segundo factor.
insert into auth.users (id, email, raw_user_meta_data)
values
    ('00000000-0000-0000-0000-0000000000a1', 'ciudadano-mfa@test.local', '{"nombre":"Ciudadano MFA"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000a2', 'entidad-mfa@test.local', '{"nombre":"Entidad MFA"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000a3', 'moderador-mfa@test.local', '{"nombre":"Moderación MFA"}'::jsonb);

update public.usuarios
set rol = 'entidad',
    entidad_id = (select id from public.entidades where nombre = 'Universidad Nacional Mayor de San Marcos')
where id = '00000000-0000-0000-0000-0000000000a2';

update public.usuarios
set rol = 'moderador'
where id = '00000000-0000-0000-0000-0000000000a3';

insert into public.incidentes (id, codigo, categoria_id, zona_id, entidad_id, geom)
values (
    '00000000-0000-0000-0000-0000000000b1',
    'MFA-0001',
    (select id from public.categorias order by id limit 1),
    (select id from public.zonas order by id limit 1),
    (select id from public.entidades where nombre = 'Universidad Nacional Mayor de San Marcos'),
    extensions.ST_SetSRID(extensions.ST_MakePoint(-77.085, -12.057), 4326)::extensions.geography
);

insert into public.reportes (id, usuario_id, cliente_id, incidente_id, geom)
values (
    '00000000-0000-0000-0000-0000000000c1',
    '00000000-0000-0000-0000-0000000000a1',
    '00000000-0000-0000-0000-0000000000d1',
    '00000000-0000-0000-0000-0000000000b1',
    extensions.ST_SetSRID(extensions.ST_MakePoint(-77.085, -12.057), 4326)::extensions.geography
);

insert into public.denuncias (id, reporte_id, usuario_id, motivo)
values (
    '00000000-0000-0000-0000-0000000000e1',
    '00000000-0000-0000-0000-0000000000c1',
    '00000000-0000-0000-0000-0000000000a1',
    'Prueba de MFA'
);

insert into public.historial_estados (incidente_id, estado_nuevo, comentario)
values ('00000000-0000-0000-0000-0000000000b1', 'en_revision', 'Prueba de MFA');

insert into public.comentarios (incidente_id, autor_id, texto)
values ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-0000000000a2', 'Comentario de prueba');

insert into public.fotos (reporte_id, storage_path)
values ('00000000-0000-0000-0000-0000000000c1', '00000000-0000-0000-0000-0000000000a1/mfa.jpg');

insert into public.apoyos (usuario_id, incidente_id)
values ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000b1');

insert into public.verificaciones (usuario_id, incidente_id, sigue_resuelto)
values ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-0000000000b1', true);

-- Moderación desactivada: con aal2 tampoco ve nada (rol_actual() es nulo).
insert into auth.users (id, email, raw_user_meta_data)
values ('00000000-0000-0000-0000-0000000000a4', 'inactivo-mfa@test.local', '{"nombre":"Moderación inactiva"}'::jsonb);
update public.usuarios set rol = 'moderador', activo = false
where id = '00000000-0000-0000-0000-0000000000a4';

-- Moderación sin segundo factor (aal1): no ve nada interno.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated","aal":"aal1"}';
select is((select count(*) from public.incidentes), 0::bigint, 'moderación aal1 no ve incidentes');
select is((select count(*) from public.reportes), 0::bigint, 'moderación aal1 no ve reportes');
select is((select count(*) from public.denuncias), 0::bigint, 'moderación aal1 no ve denuncias');
select is((select count(*) from public.auditoria), 0::bigint, 'moderación aal1 no ve la auditoría');
select is(
    (select count(*) from public.historial_estados),
    0::bigint,
    'moderación aal1 no ve el historial'
);
select is(
    (select count(*) from public.comentarios),
    0::bigint,
    'moderación aal1 no ve comentarios'
);
select is(
    (select count(*) from public.fotos),
    0::bigint,
    'moderación aal1 no ve fotos'
);
select is(
    (select count(*) from public.verificaciones),
    0::bigint,
    'moderación aal1 no ve verificaciones'
);
select is(
    (select count(*) from public.apoyos),
    0::bigint,
    'moderación aal1 no ve apoyos'
);
reset role;

-- Moderación con segundo factor (aal2): ve todo.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated","aal":"aal2"}';
select is((select count(*) from public.incidentes), 1::bigint, 'moderación aal2 ve incidentes');
select is((select count(*) from public.reportes), 1::bigint, 'moderación aal2 ve reportes');
select is((select count(*) from public.denuncias), 1::bigint, 'moderación aal2 ve denuncias');
select ok((select count(*) from public.auditoria) > 0, 'moderación aal2 ve la auditoría');
select ok((select count(*) from public.historial_estados) > 0, 'moderación aal2 ve el historial');
select is((select count(*) from public.comentarios), 1::bigint, 'moderación aal2 ve comentarios');
select is((select count(*) from public.fotos), 1::bigint, 'moderación aal2 ve fotos');
select is((select count(*) from public.apoyos), 1::bigint, 'moderación aal2 ve apoyos');
select is((select count(*) from public.verificaciones), 1::bigint, 'moderación aal2 ve verificaciones');
reset role;

-- Entidad sin segundo factor: no ve incidentes ni reportes de su entidad.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated","aal":"aal1"}';
select is((select count(*) from public.incidentes), 0::bigint, 'entidad aal1 no ve incidentes');
select is((select count(*) from public.reportes), 0::bigint, 'entidad aal1 no ve reportes');
reset role;

-- Entidad con segundo factor: ve lo suyo.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a2","role":"authenticated","aal":"aal2"}';
select is((select count(*) from public.incidentes), 1::bigint, 'entidad aal2 ve sus incidentes');
select is((select count(*) from public.reportes), 1::bigint, 'entidad aal2 ve los reportes de su entidad');
reset role;

-- El ciudadano sigue funcionando sin segundo factor.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a1","role":"authenticated","aal":"aal1"}';
select is((select count(*) from public.reportes), 1::bigint, 'ciudadano aal1 ve su propio reporte');
select is((select count(*) from public.denuncias), 1::bigint, 'ciudadano aal1 ve su propia denuncia');
reset role;

-- Moderación sin segundo factor: solo ve su propia fila de usuarios.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated","aal":"aal1"}';
select is(
    (select count(*) from public.usuarios),
    1::bigint,
    'moderación aal1 solo ve su propia fila de usuarios'
);
-- Sin el claim aal en el JWT se trata como aal1.
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated"}';
select is((select count(*) from public.incidentes), 0::bigint, 'moderación sin claim aal no ve incidentes');
-- Escritura: sin aal2 la moderación no puede resolver denuncias.
update public.denuncias set resolucion = 'sin MFA' where id = '00000000-0000-0000-0000-0000000000e1';
reset role;
select is(
    (select resolucion from public.denuncias where id = '00000000-0000-0000-0000-0000000000e1'),
    null,
    'moderación aal1 no puede actualizar denuncias'
);

-- Moderación con segundo factor ve a todos los usuarios.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a3","role":"authenticated","aal":"aal2"}';
select ok((select count(*) from public.usuarios) >= 3, 'moderación aal2 ve a todos los usuarios');
update public.denuncias set resolucion = 'con MFA' where id = '00000000-0000-0000-0000-0000000000e1';
reset role;
select is(
    (select resolucion from public.denuncias where id = '00000000-0000-0000-0000-0000000000e1'),
    'con MFA',
    'moderación aal2 sí puede actualizar denuncias'
);

-- Moderación desactivada con aal2: no ve nada.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000a4","role":"authenticated","aal":"aal2"}';
select is((select count(*) from public.incidentes), 0::bigint, 'moderación desactivada no ve incidentes ni con aal2');
select is((select count(*) from public.denuncias), 0::bigint, 'moderación desactivada no ve denuncias ni con aal2');
reset role;

select * from finish();
rollback;
