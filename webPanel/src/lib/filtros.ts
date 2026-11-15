// Filtros, búsqueda y orden de la lista de incidentes. Lógica pura: la lista
// completa (hasta 500 filas) ya está en memoria y se filtra sin ir al servidor.

import { ESTADOS } from './estados'
import { calcularPlazo } from './plazos'
import type { Estado, IncidentePanel } from './tipos'

export interface Filtros {
  estado: Estado | null
  categoriaId: number | null
  texto: string
  soloVencidos: boolean
}

export const FILTROS_VACIOS: Filtros = { estado: null, categoriaId: null, texto: '', soloVencidos: false }

export type Orden = 'prioridad' | 'recientes' | 'antiguos' | 'apoyos'

export const ORDENES: { valor: Orden; etiqueta: string }[] = [
  { valor: 'prioridad', etiqueta: 'Prioridad' },
  { valor: 'recientes', etiqueta: 'Más recientes' },
  { valor: 'antiguos', etiqueta: 'Más antiguos' },
  { valor: 'apoyos', etiqueta: 'Más apoyos' },
]

/** Minúsculas y sin tildes, para que «german» encuentre «Germán». */
export function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim()
}

export function hayFiltrosActivos(filtros: Filtros): boolean {
  return (
    filtros.estado !== null || filtros.categoriaId !== null || filtros.soloVencidos || filtros.texto.trim() !== ''
  )
}

export function filtrarIncidentes(incidentes: IncidentePanel[], filtros: Filtros, ahora: Date = new Date()): IncidentePanel[] {
  const palabras = normalizar(filtros.texto).split(/\s+/).filter(Boolean)

  return incidentes.filter((incidente) => {
    if (filtros.estado !== null && incidente.estado !== filtros.estado) return false
    if (filtros.categoriaId !== null && incidente.categoria_id !== filtros.categoriaId) return false
    if (filtros.soloVencidos && !calcularPlazo(incidente, ahora).vencido) return false
    if (palabras.length > 0) {
      const dondeBuscar = normalizar(`${incidente.codigo} ${incidente.titulo} ${incidente.direccion ?? ''}`)
      if (!palabras.every((palabra) => dondeBuscar.includes(palabra))) return false
    }
    return true
  })
}

export function ordenarIncidentes(incidentes: IncidentePanel[], orden: Orden): IncidentePanel[] {
  const porFecha = (a: IncidentePanel, b: IncidentePanel) => Date.parse(b.creado_en) - Date.parse(a.creado_en)
  const copia = [...incidentes]
  switch (orden) {
    case 'prioridad':
      return copia.sort((a, b) => b.prioridad - a.prioridad || porFecha(a, b))
    case 'recientes':
      return copia.sort(porFecha)
    case 'antiguos':
      return copia.sort((a, b) => porFecha(b, a))
    case 'apoyos':
      return copia.sort((a, b) => b.apoyos_count - a.apoyos_count || b.prioridad - a.prioridad)
  }
}

export type ConteoPorEstado = Record<Estado | 'todos', number>

export function contarPorEstado(incidentes: IncidentePanel[]): ConteoPorEstado {
  const conteo = { todos: incidentes.length } as ConteoPorEstado
  for (const estado of ESTADOS) conteo[estado] = 0
  for (const incidente of incidentes) conteo[incidente.estado] += 1
  return conteo
}
