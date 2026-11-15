-- Push (RF-11, ADR-7). Cada fila nueva de `notificaciones` llama a la Edge Function
-- `notificar`, que envía el aviso por FCM. La URL y el secreto compartido son de cada
-- entorno y viven en Vault (no en el repo):
--
--   select vault.create_secret('https://<ref>.supabase.co/functions/v1/notificar', 'notificar_url');
--   select vault.create_secret('<el mismo valor que WEBHOOK_SECRET de la función>', 'notificar_secret');
--
-- Sin esos dos secretos no se envía nada y la bandeja de la app sigue funcionando.
-- Un fallo aquí nunca deshace el cambio de estado: la notificación no bloquea la transacción.

do $$
begin
    create extension if not exists pg_net with schema extensions;
exception when others then
    raise notice 'pg_net no está disponible (%): no habrá push, solo la bandeja.', sqlerrm;
end;
$$;

create or replace function private.enviar_push()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_url text;
    v_secreto text;
begin
    begin
        select s.decrypted_secret into v_url
        from vault.decrypted_secrets s
        where s.name = 'notificar_url';

        select s.decrypted_secret into v_secreto
        from vault.decrypted_secrets s
        where s.name = 'notificar_secret';

        if v_url is not null and v_secreto is not null then
            perform net.http_post(
                url := v_url,
                body := pg_catalog.jsonb_build_object(
                    'type', 'INSERT',
                    'table', 'notificaciones',
                    'record', pg_catalog.to_jsonb(new)
                ),
                headers := pg_catalog.jsonb_build_object(
                    'Content-Type', 'application/json',
                    'x-webhook-secret', v_secreto
                ),
                timeout_milliseconds := 5000
            );
        end if;
    exception when others then
        raise warning 'No se pudo encolar el push de la notificación %: %', new.id, sqlerrm;
    end;
    return new;
end;
$$;

revoke execute on function private.enviar_push() from public;

create trigger notificaciones_push
after insert on public.notificaciones
for each row execute function private.enviar_push();
