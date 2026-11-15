import { describe, expect, it } from 'vitest'
import { diaLima, diasEntre, formatearDia, formatearFecha, formatearFechaHora, formatearSemana } from './fechas'

describe('diaLima', () => {
  it('usa el día de calendario de America/Lima, no el de UTC', () => {
    // 02:30 UTC del 14 son las 21:30 del 13 en Lima (UTC-5, sin horario de verano).
    expect(diaLima('2026-11-14T02:30:00Z')).toBe('2026-11-13')
    expect(diaLima('2026-11-14T05:00:00Z')).toBe('2026-11-14')
  })

  it('acepta objetos Date', () => {
    expect(diaLima(new Date('2026-01-01T04:59:59Z'))).toBe('2025-12-31')
  })
})

describe('diasEntre', () => {
  it('cuenta días de calendario en Lima', () => {
    // Reportado a las 21:00 de Lima del día 10 y consultado a las 08:00 del 11: 1 día.
    expect(diasEntre('2026-11-11T02:00:00Z', '2026-11-11T13:00:00Z')).toBe(1)
  })

  it('devuelve 0 dentro del mismo día de Lima aunque cambie el día en UTC', () => {
    expect(diasEntre('2026-11-10T23:00:00Z', '2026-11-11T03:00:00Z')).toBe(0)
  })

  it('nunca es negativo', () => {
    expect(diasEntre('2026-11-12T12:00:00Z', '2026-11-10T12:00:00Z')).toBe(0)
  })

  it('cruza meses y años', () => {
    expect(diasEntre('2025-12-30T15:00:00Z', '2026-01-02T15:00:00Z')).toBe(3)
  })
})

describe('formato', () => {
  it('muestra la fecha en Lima', () => {
    expect(formatearFecha('2026-11-14T02:30:00Z')).toMatch(/^13 nov\.? 2026$/)
  })

  it('muestra fecha y hora de 24 horas en Lima', () => {
    expect(formatearFechaHora('2026-11-14T02:30:00Z')).toMatch(/^13 nov\.? 2026, 21:30$/)
  })

  it('formatea un día AAAA-MM-DD sin desplazarlo', () => {
    expect(formatearDia('2026-11-09')).toMatch(/^9 nov\.? 2026$/)
    expect(formatearSemana('2026-11-09')).toMatch(/^9 nov\.?$/)
  })
})
