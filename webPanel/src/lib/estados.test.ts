import { describe, expect, it } from 'vitest'
import { ESTADOS, etiquetaEstado, familiaDeEstado, puedeMarcarDuplicado, transicionesPermitidas } from './estados'
import type { Estado, RolPanel } from './tipos'

const destinos = (rol: RolPanel, estado: Estado) => transicionesPermitidas(rol, estado).map((t) => t.hacia)

describe('transicionesPermitidas', () => {
  it('la entidad empieza la atención o dice que no le corresponde', () => {
    expect(destinos('entidad', 'derivado')).toEqual(['en_atencion', 'en_revision'])
  })

  it('la entidad resuelve o devuelve a revisión lo que está en atención', () => {
    expect(destinos('entidad', 'en_atencion')).toEqual(['resuelto', 'en_revision'])
  })

  it('la entidad no toca los demás estados', () => {
    for (const estado of ['reportado', 'en_revision', 'resuelto', 'rechazado'] as const) {
      expect(destinos('entidad', estado)).toEqual([])
    }
  })

  it('moderación solo actúa sobre lo que está en revisión', () => {
    expect(destinos('moderador', 'en_revision')).toEqual(['derivado', 'rechazado'])
    for (const estado of ['reportado', 'derivado', 'en_atencion', 'resuelto', 'rechazado'] as const) {
      expect(destinos('moderador', estado)).toEqual([])
    }
  })

  it('moderación nunca hace cambios operativos de la entidad', () => {
    for (const estado of ESTADOS) {
      expect(destinos('moderador', estado)).not.toContain('en_atencion')
      expect(destinos('moderador', estado)).not.toContain('resuelto')
    }
  })

  it('coincide con private.transicion_permitida: 6 transiciones en total', () => {
    const total = (['entidad', 'moderador'] as const).flatMap((rol) =>
      ESTADOS.flatMap((estado) => transicionesPermitidas(rol, estado)),
    )
    expect(total).toHaveLength(6)
  })

  it('exige motivo al rechazar y entidad al derivar', () => {
    const [derivar, rechazar] = transicionesPermitidas('moderador', 'en_revision')
    expect(derivar).toMatchObject({ hacia: 'derivado', requiereEntidad: true, comentario: 'opcional' })
    expect(rechazar).toMatchObject({ hacia: 'rechazado', requiereEntidad: false, comentario: 'obligatorio' })
  })

  it('pide el motivo cuando la entidad devuelve el incidente a revisión', () => {
    const noMeCorresponde = transicionesPermitidas('entidad', 'derivado').find((t) => t.hacia === 'en_revision')
    expect(noMeCorresponde?.etiqueta).toBe('No me corresponde')
    expect(noMeCorresponde?.comentario).toBe('obligatorio')
  })
})

describe('puedeMarcarDuplicado', () => {
  it('solo moderación y solo en revisión', () => {
    expect(puedeMarcarDuplicado('moderador', 'en_revision')).toBe(true)
    expect(puedeMarcarDuplicado('moderador', 'derivado')).toBe(false)
    expect(puedeMarcarDuplicado('entidad', 'en_revision')).toBe(false)
  })
})

describe('familias y etiquetas', () => {
  it('agrupa los 6 estados en 4 familias visuales', () => {
    expect(familiaDeEstado('reportado')).toBe('abierto')
    expect(familiaDeEstado('en_revision')).toBe('abierto')
    expect(familiaDeEstado('derivado')).toBe('abierto')
    expect(familiaDeEstado('en_atencion')).toBe('en_atencion')
    expect(familiaDeEstado('resuelto')).toBe('resuelto')
    expect(familiaDeEstado('rechazado')).toBe('rechazado')
  })

  it('escribe los estados con mayúscula inicial', () => {
    expect(etiquetaEstado('en_revision')).toBe('En revisión')
    expect(etiquetaEstado('en_atencion')).toBe('En atención')
  })
})
