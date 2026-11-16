// Pruebas de la Edge Function `notificar`. Ejecutar con: deno test supabase/functions/notificar
import { tokenInvalido } from "./index.ts";

function afirmar(condicion: boolean, mensaje: string) {
  if (!condicion) throw new Error(mensaje);
}

Deno.test("UNREGISTERED borra el token", () => {
  const cuerpo = '{"error":{"code":404,"status":"NOT_FOUND","details":[{"errorCode":"UNREGISTERED"}]}}';
  afirmar(tokenInvalido(404, cuerpo), "un token desinstalado debe borrarse");
});

Deno.test("INVALID_ARGUMENT sobre el token lo borra", () => {
  const cuerpo = '{"error":{"code":400,"status":"INVALID_ARGUMENT","details":[{"fieldViolations":[{"field":"message.token"}]}]}}';
  afirmar(tokenInvalido(400, cuerpo), "un token mal formado debe borrarse");
});

Deno.test("INVALID_ARGUMENT por un mensaje mal armado no borra el token", () => {
  const cuerpo = '{"error":{"code":400,"status":"INVALID_ARGUMENT","details":[{"fieldViolations":[{"field":"message.android.notification.channel_id"}]}]}}';
  afirmar(!tokenInvalido(400, cuerpo), "un error del mensaje no invalida el token");
});

Deno.test("404 por proyecto equivocado no borra tokens", () => {
  afirmar(!tokenInvalido(404, '{"error":{"code":404,"status":"NOT_FOUND","message":"Requested entity was not found."}}'), "no debe borrar");
});

Deno.test("otros errores no borran el token", () => {
  afirmar(!tokenInvalido(500, "error interno"), "500 no invalida el token");
  afirmar(!tokenInvalido(401, "UNAUTHENTICATED"), "401 no invalida el token");
});
