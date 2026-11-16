import { readFileSync } from 'node:fs'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

interface CabeceraVercel {
  key: string
  value: string
}

/**
 * `vite preview` sirve las mismas cabeceras de seguridad que Vercel (vercel.json),
 * para probar la CSP antes de desplegar. Si la URL de Supabase no es *.supabase.co
 * (entorno local), su origen se añade a `connect-src` e `img-src`.
 */
function cabecerasDePrueba(urlSupabase: string | undefined): Record<string, string> {
  const vercel = JSON.parse(readFileSync(new URL('./vercel.json', import.meta.url), 'utf8')) as {
    headers: { source: string; headers: CabeceraVercel[] }[]
  }
  const cabeceras: Record<string, string> = {}
  for (const { key, value } of vercel.headers[0]?.headers ?? []) cabeceras[key] = value

  const csp = cabeceras['Content-Security-Policy']
  if (csp && urlSupabase && !/\.supabase\.co$/.test(new URL(urlSupabase).hostname)) {
    const origen = new URL(urlSupabase).origin
    const ws = origen.replace(/^http/, 'ws')
    cabeceras['Content-Security-Policy'] = csp
      .replace('connect-src ', `connect-src ${origen} ${ws} `)
      .replace('img-src ', `img-src ${origen} `)
  }
  return cabeceras
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_')
  return {
    plugins: [react(), tailwindcss()],
    server: { port: 5173, strictPort: true },
    preview: { port: 4173, strictPort: true, headers: cabecerasDePrueba(env.VITE_SUPABASE_URL) },
    // MapLibre (~1 MB) va en su propio fragmento y solo se descarga al abrir un mapa.
    build: { sourcemap: false, target: 'es2022', chunkSizeWarningLimit: 1200 },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/pruebas/preparar.ts'],
      css: false,
    },
  }
})
