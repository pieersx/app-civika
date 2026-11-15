import { describe, expect, it } from 'vitest'
import { leerConfiguracion } from './config'

// JWT de mentira (sin firma válida) solo para comprobar que se lee el campo `role`.
function jwtConRol(role: string): string {
  const b64 = (objeto: object) => btoa(JSON.stringify(objeto)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ role, iss: 'supabase' })}.firma`
}

describe('leerConfiguracion', () => {
  it('acepta URL y clave anon', () => {
    const config = leerConfiguracion({ VITE_SUPABASE_URL: 'http://127.0.0.1:54321/', VITE_SUPABASE_ANON_KEY: jwtConRol('anon') })
    expect(config).toEqual({ ok: true, url: 'http://127.0.0.1:54321', claveAnon: jwtConRol('anon') })
  })

  it('acepta una clave publishable', () => {
    const config = leerConfiguracion({ VITE_SUPABASE_URL: 'https://abc.supabase.co', VITE_SUPABASE_ANON_KEY: 'sb_publishable_abc123' })
    expect(config.ok).toBe(true)
  })

  it('avisa de cada variable que falta', () => {
    const config = leerConfiguracion({})
    expect(config).toEqual({ ok: false, faltan: ['VITE_SUPABASE_URL', 'VITE_SUPABASE_ANON_KEY'], motivo: null })
    expect(leerConfiguracion({ VITE_SUPABASE_URL: ' ', VITE_SUPABASE_ANON_KEY: 'x' })).toMatchObject({
      ok: false,
      faltan: ['VITE_SUPABASE_URL'],
    })
  })

  it('rechaza una URL mal escrita', () => {
    const config = leerConfiguracion({ VITE_SUPABASE_URL: 'abc.supabase.co', VITE_SUPABASE_ANON_KEY: 'x' })
    expect(config).toMatchObject({ ok: false, faltan: [] })
    expect(config.ok === false && config.motivo).toMatch(/VITE_SUPABASE_URL/)
  })

  it('se niega a arrancar con una clave service_role o secreta', () => {
    for (const clave of [jwtConRol('service_role'), 'sb_secret_abc123']) {
      const config = leerConfiguracion({ VITE_SUPABASE_URL: 'https://abc.supabase.co', VITE_SUPABASE_ANON_KEY: clave })
      expect(config.ok).toBe(false)
      expect(config.ok === false && config.motivo).toMatch(/service_role/)
    }
  })
})
