begin;
create extension if not exists pgtap with schema extensions;
set search_path = public, extensions;
select plan(1);
select ok(true, 'pgTAP responde');
select * from finish();
rollback;
