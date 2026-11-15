begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- La escritura directa en `verificaciones` está cerrada: solo vale la RPC verificar_resolucion.
insert into auth.users (id, email, raw_user_meta_data)
values
    ('00000000-0000-0000-0000-000000000601', 'autora06@test.local', '{"nombre":"Autora"}'::jsonb),
    ('00000000-0000-0000-0000-000000000602', 'ajeno06@test.local', '{"nombre":"Sin relación"}'::jsonb);

insert into public.incidentes (id, codigo, categoria_id, zona_id, geom, status)
select '00000000-0000-0000-0000-0000000006a1', 'T06-001', c.id, z.id,
       ST_SetSRID(ST_MakePoint(-77.0845, -12.0575), 4326)::geography, 'resuelto'
from public.categorias c, public.zonas z
where c.service_code = 'vial' and z.nombre = 'Ciudad Universitaria UNMSM';

insert into public.reportes (usuario_id, cliente_id, incidente_id, tipo, descripcion, geom)
select '00000000-0000-0000-0000-000000000601', gen_random_uuid(), i.id, 'original', 'Bache de prueba en la pista', i.geom
from public.incidentes i where i.codigo = 'T06-001';

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-000000000602","role":"authenticated"}';

select throws_ok(
    $$insert into public.verificaciones (usuario_id, incidente_id, sigue_resuelto)
      values ('00000000-0000-0000-0000-000000000602', '00000000-0000-0000-0000-0000000006a1', false)$$,
    '42501', null, 'un ciudadano no escribe directo en verificaciones'
);
select throws_ok(
    $$update public.verificaciones set sigue_resuelto = false$$,
    '42501', null, 'tampoco actualiza directo'
);
select throws_ok(
    $$select public.verificar_resolucion('00000000-0000-0000-0000-0000000006a1', false)$$,
    '42501', null, 'y la RPC rechaza a quien no reportó ni apoyó el incidente'
);
reset role;

select is(
    (select status from public.incidentes where codigo = 'T06-001'), 'resuelto'::public.estado,
    'el incidente resuelto sigue resuelto'
);

select * from finish();
rollback;
