import { describe, expect, it } from 'vitest'
import { calcularPlazo } from './plazos'

const AHORA = new Date('2026-11-13T17:00:00Z') // 12:00 en Lima

function incidente(parcial: Partial<Parameters<typeof calcularPlazo>[0]>) {
  return {
    estado: 'derivado' as const,
    creado_en: '2026-11-10T17:00:00Z',
    cerrado_en: null,
    plazo_objetivo_dias: 7,
    ...parcial,
  }
}

describe('calcularPlazo', () => {
  it('abierto y dentro del plazo', () => {
    const plazo = calcularPlazo(incidente({}), AHORA)
    expect(plazo).toMatchObject({ situacion: 'en_plazo', dias: 3, restantes: 4, vencido: false })
    expect(plazo.texto).toBe('3 de 7 días')
  })

  it('el día exacto del plazo todavía no está vencido (igual que indicadores_panel)', () => {
    const plazo = calcularPlazo(incidente({ creado_en: '2026-11-06T17:00:00Z' }), AHORA)
    expect(plazo).toMatchObject({ situacion: 'por_vencer', dias: 7, restantes: 0, vencido: false })
    expect(plazo.texto).toBe('Vence hoy')
  })

  it('avisa cuando queda un día', () => {
    const plazo = calcularPlazo(incidente({ creado_en: '2026-11-07T17:00:00Z' }), AHORA)
    expect(plazo.situacion).toBe('por_vencer')
    expect(plazo.texto).toBe('Vence mañana')
  })

  it('vencido cuando supera el plazo', () => {
    const plazo = calcularPlazo(incidente({ creado_en: '2026-11-03T17:00:00Z' }), AHORA)
    expect(plazo).toMatchObject({ situacion: 'vencido', dias: 10, vencido: true, exceso: 3 })
    expect(plazo.texto).toBe('Vencido hace 3 días')
  })

  it('usa el singular', () => {
    expect(calcularPlazo(incidente({ creado_en: '2026-11-05T17:00:00Z' }), AHORA).texto).toBe('Vencido hace 1 día')
    expect(calcularPlazo(incidente({ creado_en: '2026-11-12T17:00:00Z', plazo_objetivo_dias: 5 }), AHORA).texto).toBe(
      '1 de 5 días',
    )
  })

  it('cuenta los días en Lima: un reporte de las 21:00 es de ese día', () => {
    // 02:00 UTC del 13 = 21:00 del 12 en Lima. A las 12:00 del 13 lleva 1 día.
    expect(calcularPlazo(incidente({ creado_en: '2026-11-13T02:00:00Z' }), AHORA).dias).toBe(1)
  })

  it('resuelto dentro del plazo', () => {
    const plazo = calcularPlazo(incidente({ estado: 'resuelto', cerrado_en: '2026-11-12T17:00:00Z' }), AHORA)
    expect(plazo).toMatchObject({ situacion: 'cerrado_en_plazo', dias: 2, vencido: false })
    expect(plazo.texto).toBe('Resuelto en 2 días')
  })

  it('resuelto el mismo día', () => {
    const plazo = calcularPlazo(incidente({ estado: 'resuelto', cerrado_en: '2026-11-10T20:00:00Z' }), AHORA)
    expect(plazo.texto).toBe('Resuelto el mismo día')
  })

  it('resuelto fuera del plazo', () => {
    const plazo = calcularPlazo(
      incidente({ estado: 'resuelto', creado_en: '2026-11-01T17:00:00Z', cerrado_en: '2026-11-12T17:00:00Z' }),
      AHORA,
    )
    expect(plazo).toMatchObject({ situacion: 'cerrado_fuera_de_plazo', dias: 11, vencido: false })
    expect(plazo.texto).toBe('Resuelto en 11 días, fuera del plazo')
  })

  it('un rechazado no tiene plazo', () => {
    const plazo = calcularPlazo(incidente({ estado: 'rechazado', cerrado_en: '2026-11-11T17:00:00Z' }), AHORA)
    expect(plazo).toMatchObject({ situacion: 'sin_plazo', vencido: false })
    expect(plazo.texto).toBe('Sin plazo')
  })
})
