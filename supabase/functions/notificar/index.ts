// Edge Function `notificar` (RF-11, ADR-7).
//
// La llama el trigger `notificaciones_push` cada vez que se inserta una fila en
// `public.notificaciones`. Busca los tokens FCM del destinatario y envía el aviso
// con la API HTTP v1 de Firebase Cloud Messaging.
//
// Secretos (supabase secrets set …; ver supabase/functions/.env.example):
//   WEBHOOK_SECRET        secreto compartido con el trigger (Vault: notificar_secret)
//   FCM_SERVICE_ACCOUNT   JSON completo de la cuenta de servicio de Firebase
// SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY los inyecta Supabase; la clave
// service_role solo existe aquí, nunca en la app ni en el panel.

interface Notificacion {
  id: string;
  usuario_id: string;
  incidente_id: string | null;
  tipo: string;
  estado: string | null;
}

interface CuentaServicio {
  project_id: string;
  client_email: string;
  private_key: string;
}

const ETIQUETA_ESTADO: Record<string, string> = {
  reportado: "Reportado",
  en_revision: "En revisión",
  derivado: "Derivado",
  en_atencion: "En atención",
  resuelto: "Resuelto",
  rechazado: "Rechazado",
};

/** Texto del aviso. Se habla de «tú» y sin datos personales (docs/diseno/direccion-visual.md). */
export function redactar(n: Notificacion, codigo: string | null): { titulo: string; cuerpo: string } {
  const ref = codigo ? `El reporte ${codigo}` : "Tu reporte";
  if (n.tipo === "comentario") {
    return { titulo: "La entidad respondió", cuerpo: `${ref} tiene un comentario nuevo.` };
  }
  if (n.tipo === "reabierto") {
    return {
      titulo: "Se reabrió un reporte",
      cuerpo: `${ref} volvió a «En atención»: varios vecinos indican que sigue igual.`,
    };
  }
  if (n.estado === "resuelto") {
    return { titulo: "¿Sigue arreglado?", cuerpo: `${ref} se marcó como resuelto. Cuéntanos si es cierto.` };
  }
  const etiqueta = n.estado ? ETIQUETA_ESTADO[n.estado] ?? n.estado : "actualizado";
  return { titulo: "Tu reporte avanzó", cuerpo: `${ref} ahora está «${etiqueta}».` };
}

function iguales(a: string, b: string): boolean {
  const x = new TextEncoder().encode(a);
  const y = new TextEncoder().encode(b);
  if (x.length !== y.length) return false;
  let diff = 0;
  for (let i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
  return diff === 0;
}

function base64url(datos: ArrayBuffer | string): string {
  const bytes = typeof datos === "string" ? new TextEncoder().encode(datos) : new Uint8Array(datos);
  let binario = "";
  for (const b of bytes) binario += String.fromCharCode(b);
  return btoa(binario).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

let tokenGoogle: { valor: string; vence: number } | null = null;

/** Token OAuth de Google para FCM, firmado con la cuenta de servicio y guardado hasta que venza. */
async function obtenerTokenGoogle(cuenta: CuentaServicio): Promise<string> {
  const ahora = Math.floor(Date.now() / 1000);
  if (tokenGoogle && tokenGoogle.vence - 60 > ahora) return tokenGoogle.valor;

  const cabecera = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const cuerpo = base64url(JSON.stringify({
    iss: cuenta.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: ahora,
    exp: ahora + 3600,
  }));
  const pem = cuenta.private_key.replace(/-----[A-Z ]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const clave = await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const firma = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", clave, new TextEncoder().encode(`${cabecera}.${cuerpo}`));

  const respuesta = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${cabecera}.${cuerpo}.${base64url(firma)}`,
    }),
  });
  if (!respuesta.ok) throw new Error(`Google OAuth respondió ${respuesta.status}`);
  const json = await respuesta.json();
  tokenGoogle = { valor: json.access_token, vence: ahora + (json.expires_in ?? 3600) };
  return tokenGoogle.valor;
}

function responder(estado: number, cuerpo: unknown): Response {
  return new Response(JSON.stringify(cuerpo), { status: estado, headers: { "Content-Type": "application/json" } });
}

async function manejar(req: Request): Promise<Response> {
  if (req.method !== "POST") return responder(405, { error: "Método no permitido" });

  const secreto = Deno.env.get("WEBHOOK_SECRET");
  if (!secreto || !iguales(req.headers.get("x-webhook-secret") ?? "", secreto)) {
    return responder(401, { error: "No autorizado" });
  }

  let n: Notificacion;
  try {
    const carga = await req.json();
    n = carga?.record;
    if (carga?.table !== "notificaciones" || !n?.id || !n?.usuario_id) throw new Error("carga inesperada");
  } catch {
    return responder(400, { error: "Carga inválida" });
  }

  const cuentaJson = Deno.env.get("FCM_SERVICE_ACCOUNT");
  if (!cuentaJson) return responder(200, { enviados: 0, motivo: "FCM sin configurar" });
  const cuenta: CuentaServicio = JSON.parse(cuentaJson);

  const url = Deno.env.get("SUPABASE_URL")!;
  const clave = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const cabeceras = { apikey: clave, Authorization: `Bearer ${clave}` };
  const rest = `${url}/rest/v1`;

  const dispositivos: { fcm_token: string }[] = await fetch(
    `${rest}/dispositivos?select=fcm_token&usuario_id=eq.${encodeURIComponent(n.usuario_id)}`,
    { headers: cabeceras },
  ).then((r) => (r.ok ? r.json() : []));
  if (dispositivos.length === 0) return responder(200, { enviados: 0 });

  let codigo: string | null = null;
  if (n.incidente_id) {
    const filas: { codigo: string }[] = await fetch(
      `${rest}/incidentes?select=codigo&id=eq.${encodeURIComponent(n.incidente_id)}`,
      { headers: cabeceras },
    ).then((r) => (r.ok ? r.json() : []));
    codigo = filas[0]?.codigo ?? null;
  }

  const { titulo, cuerpo } = redactar(n, codigo);
  const token = await obtenerTokenGoogle(cuenta);
  let enviados = 0;

  await Promise.all(dispositivos.map(async ({ fcm_token }) => {
    const r = await fetch(`https://fcm.googleapis.com/v1/projects/${cuenta.project_id}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({
        message: {
          token: fcm_token,
          notification: { title: titulo, body: cuerpo },
          data: {
            notificacion_id: n.id,
            incidente_id: n.incidente_id ?? "",
            tipo: n.tipo,
            estado: n.estado ?? "",
          },
          android: { priority: "HIGH", notification: { channel_id: "estado_reportes" } },
        },
      }),
    });
    if (r.ok) {
      enviados++;
    } else if (r.status === 404 || r.status === 400) {
      // Token caducado o desinstalado: se borra para no reintentar en cada aviso.
      const detalle = await r.text();
      if (r.status === 404 || detalle.includes("UNREGISTERED") || detalle.includes("INVALID_ARGUMENT")) {
        await fetch(`${rest}/dispositivos?fcm_token=eq.${encodeURIComponent(fcm_token)}`, {
          method: "DELETE",
          headers: cabeceras,
        });
      }
    } else {
      console.error(`FCM respondió ${r.status} para la notificación ${n.id}`);
    }
  }));

  return responder(200, { enviados });
}

if (import.meta.main) {
  Deno.serve(async (req) => {
    try {
      return await manejar(req);
    } catch (e) {
      console.error("notificar falló:", e instanceof Error ? e.message : e);
      return responder(500, { error: "Error interno" });
    }
  });
}
