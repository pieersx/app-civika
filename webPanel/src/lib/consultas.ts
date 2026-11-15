// Consultas y mutaciones con TanStack Query. Las claves se agrupan aquí para
// invalidar siempre lo mismo después de cada cambio.

import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import * as api from './api'
import { interpretarError } from './errores'

export const claves = {
  incidentes: ['incidentes'] as const,
  historial: (id: string) => ['incidente', id, 'historial'] as const,
  comentarios: (id: string) => ['incidente', id, 'comentarios'] as const,
  fotos: (id: string) => ['incidente', id, 'fotos'] as const,
  denuncias: ['denuncias'] as const,
  indicadores: (dias: number) => ['indicadores', dias] as const,
  entidades: ['entidades'] as const,
  categorias: ['categorias'] as const,
}

const CATALOGO = { staleTime: 60 * 60_000 }

export function useIncidentes() {
  return useQuery({ queryKey: claves.incidentes, queryFn: api.listarIncidentes, refetchInterval: 2 * 60_000 })
}

export function useHistorial(id: string) {
  return useQuery({ queryKey: claves.historial(id), queryFn: () => api.leerHistorial(id) })
}

export function useComentarios(id: string) {
  return useQuery({ queryKey: claves.comentarios(id), queryFn: () => api.leerComentarios(id) })
}

export function useFotos(id: string) {
  // Las URL firmadas duran 10 minutos: se renuevan antes de que caduquen.
  return useQuery({
    queryKey: claves.fotos(id),
    queryFn: () => api.leerFotos(id),
    staleTime: 8 * 60_000,
    refetchInterval: 8 * 60_000,
  })
}

export function useDenuncias(habilitada = true) {
  return useQuery({ queryKey: claves.denuncias, queryFn: () => api.listarDenuncias(true), enabled: habilitada })
}

export function useIndicadores(dias: number) {
  return useQuery({ queryKey: claves.indicadores(dias), queryFn: () => api.leerIndicadores(dias) })
}

export function useEntidades(habilitada = true) {
  return useQuery({ queryKey: claves.entidades, queryFn: api.listarEntidades, enabled: habilitada, ...CATALOGO })
}

export function useCategorias() {
  return useQuery({ queryKey: claves.categorias, queryFn: api.listarCategorias, ...CATALOGO })
}

/** Tras cualquier cambio sobre un incidente se refresca todo lo que depende de él. */
function refrescarIncidente(cliente: QueryClient, id: string) {
  return Promise.all([
    cliente.invalidateQueries({ queryKey: claves.incidentes }),
    cliente.invalidateQueries({ queryKey: ['incidente', id] }),
    cliente.invalidateQueries({ queryKey: claves.denuncias }),
    cliente.invalidateQueries({ queryKey: ['indicadores'] }),
  ])
}

export function useCambiarEstado() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: api.cambiarEstado,
    onSuccess: (_datos, cambio) => refrescarIncidente(cliente, cambio.incidenteId),
    // Conflicto de versión o incidente que ya no está: se recarga para mostrar lo vigente.
    onError: (error, cambio) => {
      const { tipo } = interpretarError(error)
      if (tipo === 'conflicto' || tipo === 'no_encontrado' || tipo === 'permiso') {
        void refrescarIncidente(cliente, cambio.incidenteId)
      }
    },
  })
}

export function useMarcarDuplicado() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (datos: { incidenteId: string; originalId: string; versionEsperada: number }) =>
      api.marcarDuplicado(datos.incidenteId, datos.originalId, datos.versionEsperada),
    onSuccess: (_datos, variables) => refrescarIncidente(cliente, variables.incidenteId),
    onError: (error, variables) => {
      const { tipo } = interpretarError(error)
      if (tipo === 'conflicto' || tipo === 'no_encontrado') void refrescarIncidente(cliente, variables.incidenteId)
    },
  })
}

export function useComentar(incidenteId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (datos: { texto: string; visibilidad: 'publico' | 'interno' }) =>
      api.comentarIncidente(incidenteId, datos.texto, datos.visibilidad),
    onSuccess: () => cliente.invalidateQueries({ queryKey: claves.comentarios(incidenteId) }),
  })
}

export function useOcultarFoto(incidenteId: string) {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (datos: { fotoId: string; oculta: boolean }) => api.ocultarFoto(datos.fotoId, datos.oculta),
    onSuccess: () => cliente.invalidateQueries({ queryKey: claves.fotos(incidenteId) }),
  })
}

export function useResolverDenuncia() {
  const cliente = useQueryClient()
  return useMutation({
    mutationFn: (datos: { denunciaId: string; resolucion: string }) =>
      api.resolverDenuncia(datos.denunciaId, datos.resolucion),
    onSuccess: () =>
      Promise.all([
        cliente.invalidateQueries({ queryKey: claves.denuncias }),
        cliente.invalidateQueries({ queryKey: claves.incidentes }),
      ]),
  })
}
