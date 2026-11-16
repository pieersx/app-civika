// Indicador de plazo: días abiertos frente al plazo objetivo de la categoría.
// Mismo criterio que `indicadores_panel`: vencido si días abierto > plazo, en días
// de calendario de America/Lima.

import { estaCerrado } from './estados'
import { diasEntre } from './fechas'
import type { IncidentePanel } from './tipos'

export type SituacionPlazo =
  | 'en_plazo'
  | 'por_vencer'
  | 'vencido'
  | 'cerrado_en_plazo'
  | 'cerrado_fuera_de_plazo'
  | 'sin_plazo'

export interface Plazo {
  situacion: SituacionPlazo
  /** Días abierto (o días hasta el cierre). */
  dias: number
  objetivo: number
  /** Días que faltan para vencer; 0 si ya venció o está cerrado. */
  restantes: number
  /** Días por encima del plazo. */
  exceso: number
  /** Solo los incidentes abiertos pueden estar vencidos. */
  vencido: boolean
  texto: string
}

type DatosDePlazo = Pick<IncidentePanel, 'estado' | 'creado_en' | 'cerrado_en' | 'plazo_objetivo_dias'>

function dias(n: number): string {
  return n === 1 ? '1 día' : `${n} días`
}

export function calcularPlazo(incidente: DatosDePlazo, ahora: Date = new Date()): Plazo {
  const objetivo = incidente.plazo_objetivo_dias
  const base = { objetivo, restantes: 0, exceso: 0, vencido: false }

  if (incidente.estado === 'rechazado') {
    return { ...base, situacion: 'sin_plazo', dias: 0, texto: 'Sin plazo' }
  }

  if (estaCerrado(incidente.estado)) {
    const transcurridos = diasEntre(incidente.creado_en, incidente.cerrado_en ?? ahora)
    const enPlazo = transcurridos <= objetivo
    const cuanto = transcurridos === 0 ? 'Resuelto el mismo día' : `Resuelto en ${dias(transcurridos)}`
    return {
      ...base,
      situacion: enPlazo ? 'cerrado_en_plazo' : 'cerrado_fuera_de_plazo',
      dias: transcurridos,
      exceso: Math.max(transcurridos - objetivo, 0),
      texto: enPlazo ? cuanto : `${cuanto}, fuera del plazo`,
    }
  }

  const abiertos = diasEntre(incidente.creado_en, ahora)
  if (abiertos > objetivo) {
    const exceso = abiertos - objetivo
    return { ...base, situacion: 'vencido', dias: abiertos, exceso, vencido: true, texto: `Vencido hace ${dias(exceso)}` }
  }

  const restantes = objetivo - abiertos
  if (restantes <= 1) {
    return {
      ...base,
      situacion: 'por_vencer',
      dias: abiertos,
      restantes,
      texto: restantes === 0 ? 'Vence hoy' : 'Vence mañana',
    }
  }
  return { ...base, situacion: 'en_plazo', dias: abiertos, restantes, texto: `${abiertos} de ${dias(objetivo)}` }
}
