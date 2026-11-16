begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- Lectura de fotos, detalle, denuncias, verificación, notificaciones, cuenta y RPC del panel.
insert into public.entidades (id, nombre, tipo)
values (9501, 'Entidad A de prueba 05', 'prueba'), (9502, 'Entidad B de prueba 05', 'prueba');

insert into auth.users (id, email, raw_user_meta_data)
values
    ('00000000-0000-0000-0000-000000000501', 'autora05@test.local', '{"nombre":"Autora","version_politica":"2026-10"}'::jsonb),
    ('00000000-0000-0000-0000-000000000502', 'vecino05a@test.local', '{"nombre":"Vecino A"}'::jsonb),
    ('00000000-0000-0000-0000-000000000503', 'vecino05b@test.local', '{"nombre":"Vecino B"}'::jsonb),
    ('00000000-0000-0000-0000-000000000504', 'vecino05c@test.local', '{"nombre":"Vecino C"}'::jsonb),
    ('00000000-0000-0000-0000-000000000505', 'moderacion05@test.local', '{"nombre":"Rosa"}'::jsonb),
    ('00000000-0000-0000-0000-000000000506', 'entidada05@test.local', '{"nombre":"Luis de A"}'::jsonb),
    ('00000000-0000-0000-0000-000000000507', 'entidadb05@test.local', '{"nombre":"Marta de B"}'::jsonb);

update public.usuarios set rol = 'moderador' where id = '00000000-0000-0000-0000-000000000505';
update public.usuarios set rol = 'entidad', entidad_id = 9501 where id = '00000000-0000-0000-0000-000000000506';
update public.usuarios set rol = 'entidad', entidad_id = 9502 where id = '00000000-0000-0000-0000-000000000507';

select is(
    (select count(*) from public.consentimientos
     where usuario_id = '00000000-0000-0000-0000-000000000501' and version_politica = '2026-10'),
    1::bigint,
    'el registro guarda el consentimiento aceptado en el formulario'
);

insert into public.incidentes (id, codigo, categoria_id, zona_id, entidad_id, geom, status, creado_en)
select v.id::uuid, v.codigo, c.id, z.id, v.entidad,
       ST_SetSRID(ST_MakePoint(-77.08451234, -12.05751234), 4326)::geography, v.estado::public.estado,
       now() - make_interval(days => v.dias)
from (values
    ('00000000-0000-0000-0000-0000000005a1', 'T05-001', 'derivado', 9501, 2),
    ('00000000-0000-0000-0000-0000000005a2', 'T05-002', 'resuelto', 9501, 6),
    ('00000000-0000-0000-0000-0000000005a3', 'T05-003', 'rechazado', 9501, 3),
    ('00000000-0000-0000-0000-0000000005a4', 'T05-004', 'en_atencion', 9502, 1),
    ('00000000-0000-0000-0000-0000000005a5', 'T05-005', 'en_revision', null, 1)
) as v(id, codigo, estado, entidad, dias),
public.categorias c, public.zonas z
where c.service_code = 'vial' and z.nombre = 'Ciudad Universitaria UNMSM';

insert into public.reportes (id, usuario_id, cliente_id, incidente_id, tipo, descripcion, geom)
select ('00000000-0000-0000-0000-0000000005b' || right(i.codigo, 1))::uuid,
       '00000000-0000-0000-0000-000000000501', gen_random_uuid(), i.id, 'original',
       'Descripción del incidente ' || i.codigo, i.geom
from public.incidentes i
where i.codigo like 'T05-%';

-- Un vecino que solo apoyó el futuro duplicado: también debe recibir el aviso de rechazo.
insert into auth.users (id, email, raw_user_meta_data)
values ('00000000-0000-0000-0000-000000000508', 'vecino05d@test.local', '{"nombre":"Vecino D"}'::jsonb);
insert into public.apoyos (usuario_id, incidente_id)
values ('00000000-0000-0000-0000-000000000508', '00000000-0000-0000-0000-0000000005a5');

insert into public.historial_estados (incidente_id, estado_anterior, estado_nuevo, comentario, autor_id)
values
    ('00000000-0000-0000-0000-0000000005a2', 'en_atencion', 'resuelto', null, '00000000-0000-0000-0000-000000000506'),
    ('00000000-0000-0000-0000-0000000005a3', 'en_revision', 'rechazado', 'Foto que no corresponde', '00000000-0000-0000-0000-000000000505');

insert into storage.objects (bucket_id, name, owner_id)
values
    ('fotos', '00000000-0000-0000-0000-000000000501/visible.jpg', '00000000-0000-0000-0000-000000000501'),
    ('fotos', '00000000-0000-0000-0000-000000000501/oculta.jpg', '00000000-0000-0000-0000-000000000501'),
    ('fotos', '00000000-0000-0000-0000-000000000501/rechazada.jpg', '00000000-0000-0000-0000-000000000501'),
    ('fotos', '00000000-0000-0000-0000-000000000501/suelta.jpg', '00000000-0000-0000-0000-000000000501');

insert into public.fotos (id, reporte_id, storage_path, oculta)
values
    ('00000000-0000-0000-0000-0000000005f1', '00000000-0000-0000-0000-0000000005b1', '00000000-0000-0000-0000-000000000501/visible.jpg', false),
    ('00000000-0000-0000-0000-0000000005f2', '00000000-0000-0000-0000-0000000005b1', '00000000-0000-0000-0000-000000000501/oculta.jpg', true),
    ('00000000-0000-0000-0000-0000000005f3', '00000000-0000-0000-0000-0000000005b3', '00000000-0000-0000-0000-000000000501/rechazada.jpg', false);

insert into public.apoyos (usuario_id, incidente_id)
values
    ('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-0000000005a1'),
    ('00000000-0000-0000-0000-000000000502', '00000000-0000-0000-0000-0000000005a2'),
    ('00000000-0000-0000-0000-000000000503', '00000000-0000-0000-0000-0000000005a2'),
    ('00000000-0000-0000-0000-000000000504', '00000000-0000-0000-0000-0000000005a2');

-- ---------------------------------------------------------------- Fotos
set local role anon;
set local request.jwt.claims = '{"role":"anon"}';
select is(
    (select count(*) from storage.objects where name like '00000000-0000-0000-0000-000000000501/%'),
    1::bigint,
    'el anónimo solo ve la foto visible de un incidente no rechazado'
);
select results_eq(
    $$select ruta from public.fotos_incidente('00000000-0000-0000-0000-0000000005a1')$$,
    $$values ('00000000-0000-0000-0000-000000000501/visible.jpg')$$,
    'fotos_incidente omite las fotos ocultas'
);
select is_empty(
    $$select ruta from public.fotos_incidente('00000000-0000-0000-0000-0000000005a3')$$,
    'fotos_incidente no devuelve nada de un incidente rechazado'
);
select is(
    (select estado from public.detalle_incidente('00000000-0000-0000-0000-0000000005a1')),
    'derivado'::public.estado,
    'el anónimo lee el detalle público'
);
select is(
    (select lat from public.detalle_incidente('00000000-0000-0000-0000-0000000005a1')),
    -12.0575::double precision,
    'el detalle público redondea la ubicación'
);
select is_empty(
    $$select * from public.detalle_incidente('00000000-0000-0000-0000-0000000005a3')$$,
    'el anónimo no ve un incidente rechazado'
);
select throws_ok(
    $$select * from public.mis_incidentes()$$, '42501', null, 'mis_incidentes no es para anónimos'
);
select lives_ok($$select * from public.resumen_publico()$$, 'el resumen público es legible sin sesión');
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000501","role":"authenticated"}';
select is(
    (select count(*) from storage.objects where name like '00000000-0000-0000-0000-000000000501/%'),
    3::bigint,
    'la autora ve todas sus fotos registradas (también la oculta y la del rechazado), no el objeto suelto'
);
select is(
    (select motivo_rechazo from public.detalle_incidente('00000000-0000-0000-0000-0000000005a3')),
    'Foto que no corresponde',
    'la autora ve su incidente rechazado con el motivo'
);
select ok(
    (select es_mio and not yo_apoyo from public.detalle_incidente('00000000-0000-0000-0000-0000000005a1')),
    'el detalle marca el incidente como propio'
);
select is(
    (select count(*) from public.mis_incidentes() where relacion = 'autor'),
    5::bigint,
    'mis_incidentes lista los 5 reportados, incluido el rechazado'
);
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000502","role":"authenticated"}';
select is(
    (select count(*) from storage.objects where name like '00000000-0000-0000-0000-000000000501/%'),
    1::bigint,
    'otro ciudadano solo ve la foto visible'
);
select ok(
    (select yo_apoyo and not es_mio from public.detalle_incidente('00000000-0000-0000-0000-0000000005a1')),
    'el detalle marca que ya apoyo el incidente'
);
select is(
    (select count(*) from public.mis_incidentes() where relacion = 'apoyo'),
    2::bigint,
    'mis_incidentes incluye los que apoyo'
);

-- ---------------------------------------------------------------- Denuncias
select is(
    public.denunciar_incidente('00000000-0000-0000-0000-0000000005a1', 'Parece falso'),
    false,
    'una denuncia sola no saca el incidente del flujo'
);
select lives_ok(
    $$select public.denunciar_incidente('00000000-0000-0000-0000-0000000005a1', 'Parece falso')$$,
    'denunciar dos veces no falla'
);
select throws_ok(
    $$select public.denunciar_incidente('00000000-0000-0000-0000-0000000005a1', 'x')$$,
    '22023', null, 'el motivo necesita al menos 3 caracteres'
);
reset role;

select is(
    (select count(*) from public.denuncias where usuario_id = '00000000-0000-0000-0000-000000000502'),
    1::bigint,
    'solo hay una denuncia por persona y reporte'
);

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000501","role":"authenticated"}';
select throws_ok(
    $$select public.denunciar_incidente('00000000-0000-0000-0000-0000000005a1', 'Es mío')$$,
    '22023', null, 'no se puede denunciar el reporte propio'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000503","role":"authenticated"}';
select is(public.denunciar_incidente('00000000-0000-0000-0000-0000000005a1', 'Es ofensivo'), false, 'segunda denuncia');
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000504","role":"authenticated"}';
select is(
    public.denunciar_incidente('00000000-0000-0000-0000-0000000005a1', 'Está repetido'),
    true,
    'la tercera denuncia envía el incidente a revisión'
);
reset role;

select is(
    (select status from public.incidentes where codigo = 'T05-001'), 'en_revision'::public.estado,
    'el incidente quedó en revisión'
);
select is(
    (select entidad_id from public.incidentes where codigo = 'T05-001'), 9501,
    'y conserva su entidad para poder restaurarlo'
);
select is(
    (select count(*) from public.notificaciones
     where incidente_id = '00000000-0000-0000-0000-0000000005a1' and tipo = 'estado' and estado = 'en_revision'),
    2::bigint,
    'el cambio de estado avisa a la autora y a quien apoyó'
);

-- ---------------------------------------------------------------- Verificación
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000502","role":"authenticated"}';
select throws_ok(
    $$select public.verificar_resolucion('00000000-0000-0000-0000-0000000005a4', false)$$,
    '22023', null, 'solo se verifica un incidente resuelto'
);
select is(
    public.verificar_resolucion('00000000-0000-0000-0000-0000000005a2', false), 'resuelto'::public.estado,
    'primer «sigue igual»'
);
select is(
    public.verificar_resolucion('00000000-0000-0000-0000-0000000005a2', false), 'resuelto'::public.estado,
    'repetir la respuesta no cuenta dos veces'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000503","role":"authenticated"}';
select is(
    public.verificar_resolucion('00000000-0000-0000-0000-0000000005a2', false), 'resuelto'::public.estado,
    'segundo «sigue igual»'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000504","role":"authenticated"}';
select is(
    public.verificar_resolucion('00000000-0000-0000-0000-0000000005a2', false), 'en_atencion'::public.estado,
    'con tres «sigue igual» el incidente se reabre'
);
reset role;

select is(
    (select count(*) from public.verificaciones where incidente_id = '00000000-0000-0000-0000-0000000005a2'),
    0::bigint,
    'al reabrir se borran las verificaciones'
);
select is(
    (select count(*) from public.notificaciones
     where incidente_id = '00000000-0000-0000-0000-0000000005a2' and tipo = 'reabierto'),
    4::bigint,
    'la reapertura avisa a los 4 seguidores'
);

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000507","role":"authenticated","aal":"aal2"}';
select throws_ok(
    $$select public.verificar_resolucion('00000000-0000-0000-0000-0000000005a2', true)$$,
    '42501', null, 'el personal de entidad no verifica'
);
reset role;

-- ---------------------------------------------------------------- Notificaciones y dispositivos
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000502","role":"authenticated"}';
select is(
    (select count(*) from public.mis_notificaciones()), 2::bigint,
    'la bandeja trae las notificaciones propias con el código del incidente'
);
select lives_ok(
    $$select public.registrar_dispositivo('token-de-prueba-0123456789-abcdefghij')$$,
    'registra el token del dispositivo'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000503","role":"authenticated"}';
select lives_ok(
    $$select public.registrar_dispositivo('token-de-prueba-0123456789-abcdefghij')$$,
    'el mismo token pasa a otra persona sin error'
);
select throws_ok(
    $$select public.registrar_dispositivo('corto')$$, '22023', null, 'un token demasiado corto falla'
);
reset role;

select is(
    (select usuario_id from public.dispositivos where fcm_token = 'token-de-prueba-0123456789-abcdefghij'),
    '00000000-0000-0000-0000-000000000503'::uuid,
    'el token pertenece al último que inició sesión'
);

-- ---------------------------------------------------------------- Panel
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000506","role":"authenticated","aal":"aal1"}';
select throws_ok(
    $$select * from public.listar_incidentes_panel()$$, '42501', null, 'sin segundo factor el panel no responde'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000502","role":"authenticated","aal":"aal2"}';
select throws_ok(
    $$select * from public.listar_incidentes_panel()$$, '42501', null, 'un ciudadano no entra al panel'
);
select throws_ok(
    $$select public.indicadores_panel()$$, '42501', null, 'ni a los indicadores'
);

set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000506","role":"authenticated","aal":"aal2"}';
select set_eq(
    $$select codigo from public.listar_incidentes_panel() where codigo like 'T05-%'$$,
    array['T05-001', 'T05-002', 'T05-003'],
    'la entidad A ve solo sus incidentes'
);
select is(
    (select lat from public.listar_incidentes_panel() where codigo = 'T05-001'),
    -12.05751234::double precision,
    'el panel recibe la ubicación exacta'
);
select is(
    (select denuncias_pendientes from public.listar_incidentes_panel() where codigo = 'T05-001'),
    3::bigint,
    'y el número de denuncias pendientes'
);
select throws_ok(
    $$select * from public.historial_panel('00000000-0000-0000-0000-0000000005a4')$$,
    'P0002', null, 'la entidad A no lee el historial de un incidente de B'
);
select throws_ok(
    $$select public.comentar_incidente('00000000-0000-0000-0000-0000000005a4', 'Intruso')$$,
    'P0002', null, 'ni lo comenta'
);
select lives_ok(
    $$select public.comentar_incidente('00000000-0000-0000-0000-0000000005a2', 'Cuadrilla programada para el lunes')$$,
    'la entidad comenta su incidente'
);
select lives_ok(
    $$select public.comentar_incidente('00000000-0000-0000-0000-0000000005a2', 'Nota interna', 'interno')$$,
    'y deja una nota interna'
);
select is(
    (select count(*) from public.comentarios_panel('00000000-0000-0000-0000-0000000005a2')), 2::bigint,
    'el panel lista los dos comentarios'
);
select is(
    (select autor from public.historial_panel('00000000-0000-0000-0000-0000000005a2')
     where estado_nuevo = 'en_atencion' and estado_anterior = 'resuelto'),
    'Sistema',
    'el historial del panel nombra al Sistema en la reapertura'
);
select is(
    (select count(*) from public.fotos_incidente_panel('00000000-0000-0000-0000-0000000005a1')), 1::bigint,
    'la entidad no ve la foto oculta'
);
select throws_ok(
    $$select * from public.listar_denuncias()$$, '42501', null, 'las denuncias son solo de moderación'
);
select throws_ok(
    $$select public.ocultar_foto('00000000-0000-0000-0000-0000000005f1')$$, '42501', null,
    'la entidad no oculta fotos'
);
select is(
    (public.indicadores_panel(90) ->> 'total')::integer, 3,
    'los indicadores de la entidad A cuentan solo sus 3 incidentes'
);
reset role;

select is(
    (select count(*) from public.notificaciones
     where incidente_id = '00000000-0000-0000-0000-0000000005a2' and tipo = 'comentario'),
    4::bigint,
    'solo el comentario público avisa a los seguidores'
);
select is(
    (select count(*) from public.comentarios_publicos('00000000-0000-0000-0000-0000000005a2')), 1::bigint,
    'el ciudadano no ve la nota interna'
);

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000505","role":"authenticated","aal":"aal2"}';
select is(
    (select count(*) from public.listar_incidentes_panel() where codigo like 'T05-%'), 5::bigint,
    'moderación ve todos los incidentes'
);
select is(
    (select count(*) from public.listar_denuncias() where codigo = 'T05-001'), 3::bigint,
    'moderación ve las denuncias pendientes'
);
select is(
    (select count(*) from public.fotos_incidente_panel('00000000-0000-0000-0000-0000000005a1')), 2::bigint,
    'moderación ve también la foto oculta'
);
select is(
    (select count(*) from storage.objects where name like '00000000-0000-0000-0000-000000000501/%'),
    3::bigint,
    'y puede firmar las URL de todas las fotos registradas'
);
select lives_ok(
    $$select public.ocultar_foto('00000000-0000-0000-0000-0000000005f1')$$, 'moderación oculta una foto'
);
select lives_ok(
    $$select public.resolver_denuncia(
        (select id from public.listar_denuncias() where codigo = 'T05-001' limit 1), 'No procede')$$,
    'moderación resuelve una denuncia'
);
select throws_ok(
    $$select public.comentar_incidente('00000000-0000-0000-0000-0000000005a2', 'Moderación opina')$$,
    '42501', null, 'moderación no comenta como entidad'
);
select throws_ok(
    $$select public.marcar_duplicado(
        '00000000-0000-0000-0000-0000000005a5', '00000000-0000-0000-0000-0000000005a4', 99)$$,
    '40001', null, 'marcar duplicado respeta la versión'
);
select lives_ok(
    $$select public.marcar_duplicado(
        '00000000-0000-0000-0000-0000000005a5', '00000000-0000-0000-0000-0000000005a4',
        (select version from public.incidentes where codigo = 'T05-005'))$$,
    'moderación marca un incidente como duplicado'
);
select is(
    (public.indicadores_panel(90) ->> 'total')::integer >= 5, true,
    'los indicadores de moderación cubren todo el piloto'
);
reset role;

select is(
    (select count(*) from public.auditoria where accion = 'foto_oculta' and objeto = 'fotos:00000000-0000-0000-0000-0000000005f1'),
    1::bigint,
    'ocultar la foto queda en auditoría'
);
select ok(
    (select status = 'rechazado' and duplicado_de = '00000000-0000-0000-0000-0000000005a4'
     from public.incidentes where codigo = 'T05-005'),
    'el duplicado queda rechazado y apunta al original'
);
select is(
    (select incidente_id from public.reportes where id = '00000000-0000-0000-0000-0000000005b5'),
    '00000000-0000-0000-0000-0000000005a4'::uuid,
    'el reporte del duplicado pasa al incidente original'
);
select is(
    (select count(*) from public.notificaciones
     where usuario_id = '00000000-0000-0000-0000-000000000501'
       and incidente_id = '00000000-0000-0000-0000-0000000005a5' and estado = 'rechazado'),
    1::bigint,
    'quien reportó el duplicado recibe el aviso de rechazo'
);
select is(
    (select count(*) from public.notificaciones
     where usuario_id = '00000000-0000-0000-0000-000000000508'
       and incidente_id = '00000000-0000-0000-0000-0000000005a5' and estado = 'rechazado'),
    1::bigint,
    'quien solo lo apoyó también lo recibe'
);
select is(
    (select count(*) from public.notificaciones
     where usuario_id = '00000000-0000-0000-0000-000000000505'
       and incidente_id = '00000000-0000-0000-0000-0000000005a5'),
    0::bigint,
    'y quien marcó el duplicado no recibe aviso'
);
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000501","role":"authenticated"}';
select is(
    (select motivo_rechazo from public.detalle_incidente('00000000-0000-0000-0000-0000000005a5')),
    'Duplicado de T05-004',
    'y puede abrir el duplicado rechazado para ver el motivo'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000503","role":"authenticated"}';
select is(
    (select count(*) from public.detalle_incidente('00000000-0000-0000-0000-0000000005a5')), 0::bigint,
    'otra persona no ve el duplicado rechazado'
);
reset role;
reset request.jwt.claims;

-- ---------------------------------------------------------------- Cuenta
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000506","role":"authenticated","aal":"aal2"}';
select throws_ok(
    $$select public.eliminar_mi_cuenta()$$, '42501', null, 'una cuenta de entidad no se elimina desde la app'
);
select throws_ok(
    $$select public.eliminar_cuenta('00000000-0000-0000-0000-000000000502')$$, '42501', null,
    'la variante administrativa no es para usuarios'
);
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000502","role":"authenticated"}';
select lives_ok($$select public.eliminar_mi_cuenta()$$, 'el ciudadano elimina su cuenta');
reset role;

select ok(
    (select nombre = 'Cuenta eliminada' and not activo from public.usuarios
     where id = '00000000-0000-0000-0000-000000000502'),
    'la fila de usuarios queda como lápida anónima'
);
select is(
    (select count(*) from auth.users where id = '00000000-0000-0000-0000-000000000502'), 0::bigint,
    'el usuario desaparece de auth.users'
);
select is(
    (select count(*) from public.apoyos where usuario_id = '00000000-0000-0000-0000-000000000502') > 0, true,
    'sus apoyos se conservan'
);
select is(
    (select count(*) from public.notificaciones where usuario_id = '00000000-0000-0000-0000-000000000502'), 0::bigint,
    'su bandeja se borra'
);

select * from finish();
rollback;
