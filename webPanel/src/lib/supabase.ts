import { createClient, type SupabaseClient } from '@supabase/supabase-js'
import { configuracion } from './config'

// Solo la clave anon: los permisos reales los deciden el RLS y las RPC (aal2 incluido).
let cliente: SupabaseClient | null = null

export function supabase(): SupabaseClient {
  if (!configuracion.ok) throw new Error('Falta configurar Supabase')
  cliente ??= createClient(configuracion.url, configuracion.claveAnon, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
      storageKey: 'civika-panel-sesion',
    },
  })
  return cliente
}
