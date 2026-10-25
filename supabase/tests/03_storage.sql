begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select no_plan();

-- Subida de fotos (docs/arquitectura.md §7.3): el ciudadano solo puede subir al bucket `fotos`, dentro de su
-- propia carpeta `{uid}/`. Un anónimo, una cuenta desactivada o una carpeta ajena no pueden.
insert into auth.users (id, email, raw_user_meta_data)
values
    ('00000000-0000-0000-0000-0000000000b1', 'sube-fotos@test.local', '{"nombre":"Ciudadano que sube fotos"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000b2', 'otra-carpeta@test.local', '{"nombre":"Otro ciudadano"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000b3', 'desactivado@test.local', '{"nombre":"Cuenta desactivada"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000b4', 'moderador-fotos@test.local', '{"nombre":"Moderación"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000b5', 'entidad-fotos@test.local', '{"nombre":"Personal de entidad"}'::jsonb),
    ('00000000-0000-0000-0000-0000000000b6', 'sin-fila@test.local', '{"nombre":"Sin fila en usuarios"}'::jsonb);

update public.usuarios set rol = 'moderador' where id = '00000000-0000-0000-0000-0000000000b4';
update public.usuarios
set rol = 'entidad', entidad_id = (select min(id) from public.entidades)
where id = '00000000-0000-0000-0000-0000000000b5';
delete from public.usuarios where id = '00000000-0000-0000-0000-0000000000b6';

update public.usuarios
set activo = false
where id = '00000000-0000-0000-0000-0000000000b3';

insert into storage.objects (bucket_id, name, owner_id)
values ('fotos', '00000000-0000-0000-0000-0000000000b2/existente.jpg', '00000000-0000-0000-0000-0000000000b2');

-- Ciudadano activo.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b1","role":"authenticated"}';

select lives_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b1/nueva.jpg')$$,
    'el ciudadano sube una foto a su propia carpeta'
);
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b2/intrusa.jpg')$$,
    '42501',
    null,
    'el ciudadano no puede subir a la carpeta de otra persona'
);
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', 'suelta.jpg')$$,
    '42501',
    null,
    'el ciudadano no puede subir fuera de una carpeta con su uid'
);
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('otro-bucket', '00000000-0000-0000-0000-0000000000b1/x.jpg')$$,
    '42501',
    null,
    'la política solo vale para el bucket fotos'
);
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b1/sub/x.jpg')$$,
    '42501',
    null,
    'no se permiten subcarpetas dentro de la carpeta propia'
);
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b1/../00000000-0000-0000-0000-0000000000b2/x.jpg')$$,
    '42501',
    null,
    'no se permite escapar de la carpeta con ..'
);
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', upper('00000000-0000-0000-0000-0000000000b1') || '/mayusculas.jpg')$$,
    '42501',
    null,
    'el uid en mayúsculas no coincide con la carpeta propia'
);
update storage.objects
set name = '00000000-0000-0000-0000-0000000000b1/otra.jpg'
where name = '00000000-0000-0000-0000-0000000000b1/nueva.jpg';
select is(
    (select count(*) from storage.objects where name like '00000000-0000-0000-0000-0000000000b2/%'),
    0::bigint,
    'el ciudadano no ve fotos de otras carpetas (no hay política de lectura)'
);
reset role;

select ok(
    exists (select 1 from storage.objects where name = '00000000-0000-0000-0000-0000000000b1/nueva.jpg')
    and not exists (select 1 from storage.objects where name = '00000000-0000-0000-0000-0000000000b1/otra.jpg'),
    'el ciudadano no puede renombrar su foto (no hay política de update)'
);

-- Cuenta desactivada: no pasa ninguna política (docs/arquitectura.md §7.2, punto 5).
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b3","role":"authenticated"}';
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b3/nueva.jpg')$$,
    '42501',
    null,
    'una cuenta desactivada no puede subir fotos ni a su carpeta'
);
reset role;

-- Otros roles: solo el ciudadano reporta con fotos.
set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b4","role":"authenticated"}';
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b4/nueva.jpg')$$,
    '42501',
    null,
    'la moderación no puede subir fotos'
);
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b5","role":"authenticated"}';
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b5/nueva.jpg')$$,
    '42501',
    null,
    'el personal de entidad no puede subir fotos'
);
reset role;

set local role authenticated;
set local request.jwt.claims = '{"sub":"00000000-0000-0000-0000-0000000000b6","role":"authenticated"}';
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b6/nueva.jpg')$$,
    '42501',
    null,
    'un usuario sin fila en usuarios no puede subir fotos'
);
reset role;

-- Anónimo.
set local role anon;
set local request.jwt.claims = '{"role":"anon"}';
select throws_ok(
    $$insert into storage.objects (bucket_id, name) values ('fotos', '00000000-0000-0000-0000-0000000000b1/anonimo.jpg')$$,
    '42501',
    null,
    'un anónimo no puede subir fotos'
);
reset role;

select * from finish();
rollback;
