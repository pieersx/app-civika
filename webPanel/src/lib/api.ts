// Acceso a datos del panel. Cada función envuelve una RPC o una lectura de
// catálogo y lanza el error de Supabase tal cual: `interpretarError` lo traduce.
// Contrato: supabase/migrations/20261022170708_rpc_base.sql y 20261113090300_panel.sql.

import { supabase } from './supabase'
import type {
  Categoria,
  Comentario,
  Denuncia,
  Entidad,
  EntradaHistorial,
  Estado,
  FotoPanel,
  IncidentePanel,
  Indicadores,
  Usuario,
  Visibilidad,
} from './tipos'

const SEGUNDOS_URL_FIRMADA = 600

async function rpc<T>(nombre: string, parametros?: Record<string, unknown>): Promise<T> {
  const { data, error } = await supabase().rpc(nombre, parametros)
  if (error) throw error
  return data as T
}

export async function leerUsuario(id: string): Promise<Usuario | null> {
  const { data, error } = await supabase()
    .from('usuarios')
    .select('id, nombre, rol, entidad_id, activo')
    .eq('id', id)
    .maybeSingle()
  if (error) throw error
  return data as Usuario | null
}

export async function listarEntidades(): Promise<Entidad[]> {
  const { data, error } = await supabase().from('entidades').select('id, nombre, tipo').order('nombre')
  if (error) throw error
  return data as Entidad[]
}

export async function listarCategorias(): Promise<Categoria[]> {
  const { data, error } = await supabase()
    .from('categorias')
    .select('id, nombre, service_code, plazo_objetivo_dias')
    .order('nombre')
  if (error) throw error
  return data as Categoria[]
}

export function listarIncidentes(): Promise<IncidentePanel[]> {
  return rpc<IncidentePanel[]>('listar_incidentes_panel')
}

export function leerHistorial(incidenteId: string): Promise<EntradaHistorial[]> {
  return rpc<EntradaHistorial[]>('historial_panel', { p_incidente_id: incidenteId })
}

export function leerComentarios(incidenteId: string): Promise<Comentario[]> {
  return rpc<Comentario[]>('comentarios_panel', { p_incidente_id: incidenteId })
}

/** Fotos del incidente con su URL firmada de 10 minutos (el bucket `fotos` es privado). */
export async function leerFotos(incidenteId: string): Promise<FotoPanel[]> {
  const fotos = await rpc<Omit<FotoPanel, 'url'>[]>('fotos_incidente_panel', { p_incidente_id: incidenteId })
  if (fotos.length === 0) return []

  const { data, error } = await supabase()
    .storage.from('fotos')
    .createSignedUrls(
      fotos.map((foto) => foto.ruta),
      SEGUNDOS_URL_FIRMADA,
    )
  if (error) throw error

  const urlPorRuta = new Map<string, string>()
  for (const firmada of data) {
    if (firmada.path && firmada.signedUrl && !firmada.error) urlPorRuta.set(firmada.path, firmada.signedUrl)
  }
  return fotos.map((foto) => ({ ...foto, url: urlPorRuta.get(foto.ruta) ?? null }))
}

export interface CambioDeEstado {
  incidenteId: string
  nuevoEstado: Estado
  versionEsperada: number
  comentario: string | null
  entidadId: number | null
}

export async function cambiarEstado(cambio: CambioDeEstado): Promise<{ estado: Estado; version: number }> {
  const filas = await rpc<{ estado: Estado; version: number }[]>('cambiar_estado', {
    p_incidente_id: cambio.incidenteId,
    p_nuevo_estado: cambio.nuevoEstado,
    p_version_esperada: cambio.versionEsperada,
    p_comentario: cambio.comentario,
    p_entidad_id: cambio.entidadId,
  })
  const fila = filas[0]
  if (!fila) throw new Error('cambiar_estado no devolvió el estado nuevo')
  return fila
}

export function comentarIncidente(incidenteId: string, texto: string, visibilidad: Visibilidad): Promise<string> {
  return rpc<string>('comentar_incidente', {
    p_incidente_id: incidenteId,
    p_texto: texto,
    p_visibilidad: visibilidad,
  })
}

export function listarDenuncias(soloPendientes: boolean): Promise<Denuncia[]> {
  return rpc<Denuncia[]>('listar_denuncias', { p_solo_pendientes: soloPendientes })
}

export function resolverDenuncia(denunciaId: string, resolucion: string): Promise<void> {
  return rpc<void>('resolver_denuncia', { p_denuncia_id: denunciaId, p_resolucion: resolucion })
}

export function ocultarFoto(fotoId: string, oculta: boolean): Promise<void> {
  return rpc<void>('ocultar_foto', { p_foto_id: fotoId, p_oculta: oculta })
}

export function marcarDuplicado(incidenteId: string, originalId: string, versionEsperada: number): Promise<void> {
  return rpc<void>('marcar_duplicado', {
    p_incidente_id: incidenteId,
    p_original_id: originalId,
    p_version_esperada: versionEsperada,
  })
}

export function leerIndicadores(dias: number): Promise<Indicadores> {
  return rpc<Indicadores>('indicadores_panel', { p_dias: dias })
}
