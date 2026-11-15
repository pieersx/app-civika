import { createContext, useContext } from 'react'
import type { RolPanel, Usuario } from '../lib/tipos'

export type UsuarioPanel = Usuario & { rol: RolPanel; correo: string }

export type MotivoSinAcceso = 'ciudadano' | 'inactiva' | 'sin_perfil'

/**
 * Fases de la sesión. El panel solo se muestra en `lista`: cuenta activa de
 * entidad o moderación y sesión con segundo factor (aal2), que es lo que exigen
 * todas las RPC del panel.
 */
export type EstadoSesion =
  | { fase: 'cargando' }
  | { fase: 'sin_sesion' }
  | { fase: 'sin_acceso'; motivo: MotivoSinAcceso }
  | { fase: 'error'; error: unknown }
  | { fase: 'mfa_alta'; usuario: UsuarioPanel }
  | { fase: 'mfa_desafio'; usuario: UsuarioPanel; factorId: string }
  | { fase: 'lista'; usuario: UsuarioPanel }

export interface SesionApi {
  estado: EstadoSesion
  cerrarSesion: () => Promise<void>
  /** Vuelve a comprobar el perfil y el nivel de la sesión (tras un error de red). */
  reintentar: () => void
  /** Sale de la pantalla «sin acceso» y vuelve al inicio de sesión. */
  volverAlInicio: () => void
  /** `true` si la persona llegó con el enlace de «Olvidé mi contraseña» y falta que elija una nueva. */
  recuperacion: boolean
  terminarRecuperacion: () => void
}

export const ContextoSesion = createContext<SesionApi | null>(null)

export function useSesion(): SesionApi {
  const contexto = useContext(ContextoSesion)
  if (!contexto) throw new Error('useSesion se usa dentro de <ProveedorSesion>')
  return contexto
}

/** Usuario del panel ya verificado. Solo dentro de las vistas protegidas. */
export function useUsuario(): UsuarioPanel {
  const { estado } = useSesion()
  if (estado.fase !== 'lista') throw new Error('useUsuario se usa con la sesión lista')
  return estado.usuario
}
