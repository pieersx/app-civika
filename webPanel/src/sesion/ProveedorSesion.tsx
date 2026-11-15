import type { Session } from '@supabase/supabase-js'
import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { leerUsuario } from '../lib/api'
import { supabase } from '../lib/supabase'
import { ContextoSesion, type EstadoSesion, type MotivoSinAcceso, type SesionApi } from './contexto'

/** Nivel de autenticación (aal1 o aal2) que declara el token de acceso. */
function nivelDelToken(token: string): string {
  try {
    const cuerpo = token.split('.')[1] ?? ''
    const json = atob(cuerpo.replace(/-/g, '+').replace(/_/g, '/'))
    return String((JSON.parse(json) as { aal?: unknown }).aal ?? 'aal1')
  } catch {
    return 'aal1'
  }
}

type Resuelto = Exclude<EstadoSesion, { fase: 'cargando' | 'sin_sesion' | 'sin_acceso' }>

/**
 * Decide qué puede ver la sesión actual:
 *  1. lee la fila propia de `usuarios` (rol, entidad, activo);
 *  2. un ciudadano o una cuenta inactiva no entra;
 *  3. sin factor TOTP verificado → alta; con factor y aal1 → desafío; aal2 → panel.
 * El rol sale siempre de la tabla, nunca del token.
 */
async function resolver(sesion: Session): Promise<Resuelto | { fase: 'sin_acceso'; motivo: MotivoSinAcceso }> {
  const usuario = await leerUsuario(sesion.user.id)
  if (!usuario) return { fase: 'sin_acceso', motivo: 'sin_perfil' }
  if (!usuario.activo) return { fase: 'sin_acceso', motivo: 'inactiva' }
  if (usuario.rol !== 'entidad' && usuario.rol !== 'moderador') return { fase: 'sin_acceso', motivo: 'ciudadano' }

  const usuarioPanel = { ...usuario, rol: usuario.rol, correo: sesion.user.email ?? '' }
  if (nivelDelToken(sesion.access_token) === 'aal2') return { fase: 'lista', usuario: usuarioPanel }

  const { data, error } = await supabase().auth.mfa.listFactors()
  if (error) throw error
  const factor = data.totp[0]
  return factor
    ? { fase: 'mfa_desafio', usuario: usuarioPanel, factorId: factor.id }
    : { fase: 'mfa_alta', usuario: usuarioPanel }
}

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const clienteConsultas = useQueryClient()
  // `undefined`: todavía no sabemos si hay sesión guardada.
  const [sesion, setSesion] = useState<Session | null | undefined>(undefined)
  const [resuelto, setResuelto] = useState<{ clave: string; estado: Resuelto } | null>(null)
  const [sinAcceso, setSinAcceso] = useState<MotivoSinAcceso | null>(null)
  const [intento, setIntento] = useState(0)
  const [recuperacion, setRecuperacion] = useState(false)

  useEffect(() => {
    const { data } = supabase().auth.onAuthStateChange((evento, nueva) => {
      setSesion(nueva)
      if (evento === 'PASSWORD_RECOVERY') setRecuperacion(true)
      if (evento === 'SIGNED_OUT') {
        clienteConsultas.clear()
        setRecuperacion(false)
      }
    })
    return () => data.subscription.unsubscribe()
  }, [clienteConsultas])

  // La comprobación solo se repite si cambia la persona o el nivel (no en cada renovación del token).
  const usuarioId = sesion?.user.id ?? null
  const nivel = sesion ? nivelDelToken(sesion.access_token) : null
  const clave = usuarioId ? `${usuarioId}:${nivel}:${intento}` : null

  useEffect(() => {
    if (!clave) return
    let vigente = true
    void (async () => {
      const { data } = await supabase().auth.getSession()
      if (!vigente || !data.session) return
      try {
        const estado = await resolver(data.session)
        if (!vigente) return
        if (estado.fase === 'sin_acceso') {
          setSinAcceso(estado.motivo)
          await supabase().auth.signOut({ scope: 'local' })
        } else {
          setResuelto({ clave, estado })
        }
      } catch (error) {
        if (vigente) setResuelto({ clave, estado: { fase: 'error', error } })
      }
    })()
    return () => {
      vigente = false
    }
  }, [clave])

  const cerrarSesion = useCallback(async () => {
    const { error } = await supabase().auth.signOut({ scope: 'local' })
    if (error) throw error
  }, [])

  const reintentar = useCallback(() => setIntento((n) => n + 1), [])
  const volverAlInicio = useCallback(() => setSinAcceso(null), [])

  const estado: EstadoSesion = useMemo(() => {
    if (sesion === undefined) return { fase: 'cargando' }
    if (sinAcceso) return { fase: 'sin_acceso', motivo: sinAcceso }
    if (sesion === null) return { fase: 'sin_sesion' }
    if (resuelto && resuelto.clave === clave) return resuelto.estado
    return { fase: 'cargando' }
  }, [sesion, sinAcceso, resuelto, clave])

  const terminarRecuperacion = useCallback(() => setRecuperacion(false), [])

  const api: SesionApi = useMemo(
    () => ({ estado, cerrarSesion, reintentar, volverAlInicio, recuperacion, terminarRecuperacion }),
    [estado, cerrarSesion, reintentar, volverAlInicio, recuperacion, terminarRecuperacion],
  )

  return <ContextoSesion.Provider value={api}>{children}</ContextoSesion.Provider>
}
