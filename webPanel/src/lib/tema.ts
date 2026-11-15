// Tema claro u oscuro. Por defecto sigue al sistema (`prefers-color-scheme`);
// el conmutador guarda la elección en este navegador.

import { useCallback, useSyncExternalStore } from 'react'

export type Tema = 'sistema' | 'claro' | 'oscuro'

const CLAVE = 'civika-panel-tema'
const oyentes = new Set<() => void>()

function leer(): Tema {
  try {
    const guardado = localStorage.getItem(CLAVE)
    return guardado === 'claro' || guardado === 'oscuro' ? guardado : 'sistema'
  } catch {
    return 'sistema'
  }
}

/** Aplica el tema al documento. Se llama antes del primer render para que no parpadee. */
export function aplicarTema(tema: Tema = leer()): void {
  const raiz = document.documentElement
  if (tema === 'sistema') raiz.removeAttribute('data-theme')
  else raiz.setAttribute('data-theme', tema === 'oscuro' ? 'dark' : 'light')
}

function suscribir(oyente: () => void): () => void {
  oyentes.add(oyente)
  window.addEventListener('storage', oyente)
  return () => {
    oyentes.delete(oyente)
    window.removeEventListener('storage', oyente)
  }
}

export function useTema(): [Tema, (tema: Tema) => void] {
  const tema = useSyncExternalStore(suscribir, leer, () => 'sistema' as const)
  const cambiar = useCallback((nuevo: Tema) => {
    try {
      if (nuevo === 'sistema') localStorage.removeItem(CLAVE)
      else localStorage.setItem(CLAVE, nuevo)
    } catch {
      // Sin almacenamiento (modo privado): el tema vale solo para esta visita.
    }
    aplicarTema(nuevo)
    oyentes.forEach((oyente) => oyente())
  }, [])
  return [tema, cambiar]
}

/** `true` si ahora mismo se ve el tema oscuro (por elección o por el sistema). */
export function useEsOscuro(): boolean {
  const [tema] = useTema()
  const delSistema = useSyncExternalStore(
    (oyente) => {
      const consulta = window.matchMedia('(prefers-color-scheme: dark)')
      consulta.addEventListener('change', oyente)
      return () => consulta.removeEventListener('change', oyente)
    },
    () => window.matchMedia('(prefers-color-scheme: dark)').matches,
    () => false,
  )
  return tema === 'oscuro' || (tema === 'sistema' && delSistema)
}
