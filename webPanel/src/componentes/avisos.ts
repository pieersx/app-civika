import { createContext, useContext } from 'react'

export type TonoAviso = 'exito' | 'error' | 'info'

export interface AvisosApi {
  avisar: (mensaje: string, tono?: TonoAviso) => void
}

export const ContextoAvisos = createContext<AvisosApi | null>(null)

/** Avisos flotantes (toasts). */
export function useAvisos(): AvisosApi {
  const contexto = useContext(ContextoAvisos)
  if (!contexto) throw new Error('useAvisos se usa dentro de <ProveedorAvisos>')
  return contexto
}
