// Lee la configuración pública del panel. Si falta algo, la app muestra la
// pantalla «Falta configurar» en vez de romperse.

export type Configuracion =
  | { ok: true; url: string; claveAnon: string }
  | { ok: false; faltan: string[]; motivo: string | null }

type Entorno = { VITE_SUPABASE_URL?: string; VITE_SUPABASE_ANON_KEY?: string }

/** `true` si la clave es de servidor: nunca puede llegar al navegador. */
function esClaveDeServidor(clave: string): boolean {
  if (clave.startsWith('sb_secret_')) return true
  const cuerpo = clave.split('.')[1]
  if (!cuerpo) return false
  try {
    const json = atob(cuerpo.replace(/-/g, '+').replace(/_/g, '/'))
    return (JSON.parse(json) as { role?: unknown }).role === 'service_role'
  } catch {
    return false
  }
}

export function leerConfiguracion(entorno: Entorno): Configuracion {
  const url = entorno.VITE_SUPABASE_URL?.trim() ?? ''
  const claveAnon = entorno.VITE_SUPABASE_ANON_KEY?.trim() ?? ''

  const faltan: string[] = []
  if (!url) faltan.push('VITE_SUPABASE_URL')
  if (!claveAnon) faltan.push('VITE_SUPABASE_ANON_KEY')
  if (faltan.length > 0) return { ok: false, faltan, motivo: null }

  if (!/^https?:\/\/[^\s/]+/.test(url) || !URL.canParse(url)) {
    return { ok: false, faltan: [], motivo: 'VITE_SUPABASE_URL no es una URL válida. Debe empezar por https://.' }
  }
  if (esClaveDeServidor(claveAnon)) {
    return {
      ok: false,
      faltan: [],
      motivo:
        'VITE_SUPABASE_ANON_KEY contiene una clave service_role o secreta. Esa clave nunca va en el panel: cámbiala por la clave anon y rótala en Supabase, porque ya quedó expuesta.',
    }
  }
  return { ok: true, url: url.replace(/\/+$/, ''), claveAnon }
}

export const configuracion: Configuracion = leerConfiguracion(import.meta.env)
