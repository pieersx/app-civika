import { describe, expect, it } from 'vitest'
import { contarPorEstado, filtrarIncidentes, FILTROS_VACIOS, hayFiltrosActivos, normalizar, ordenarIncidentes } from './filtros'
import type { IncidentePanel } from './tipos'

const AHORA = new Date('2026-11-13T17:00:00Z')

function incidente(parcial: Partial<IncidentePanel>): IncidentePanel {
  return {
    id: 'a',
    codigo: 'CVK-000001',
    titulo: 'Bache profundo frente a la biblioteca',
    descripcion: 'Bache profundo frente a la biblioteca central',
    categoria: 'Infraestructura vial',
    categoria_id: 1,
    estado: 'derivado',
    prioridad: 50,
    apoyos_count: 0,
    reportes_count: 1,
    denuncias_pendientes: 0,
    zona: 'Ciudad Universitaria',
    entidad_id: 1,
    entidad: 'Oficina de Servicios Generales',
    direccion: 'Av. Germán Amézaga 375',
    lat: -12.05,
    lon: -77.08,
    version: 1,
    creado_en: '2026-11-12T17:00:00Z',
    cerrado_en: null,
    plazo_objetivo_dias: 7,
    ubicacion_simulada: false,
    duplicado_de: null,
    ...parcial,
  }
}

const lista = [
  incidente({ id: '1', codigo: 'CVK-000001', estado: 'derivado', categoria_id: 1, prioridad: 80, apoyos_count: 4 }),
  incidente({
    id: '2',
    codigo: 'CVK-000002',
    titulo: 'Basura acumulada en la puerta 3',
    descripcion: 'Basura acumulada',
    estado: 'en_atencion',
    categoria_id: 2,
    prioridad: 60,
    direccion: 'Puerta 3, Av. Venezuela',
    creado_en: '2026-11-01T17:00:00Z',
    denuncias_pendientes: 2,
    apoyos_count: 9,
  }),
  incidente({
    id: '3',
    codigo: 'DEMO-003',
    titulo: 'Rampa rota',
    descripcion: null,
    estado: 'resuelto',
    categoria_id: 3,
    prioridad: 20,
    direccion: null,
    creado_en: '2026-10-01T17:00:00Z',
    cerrado_en: '2026-10-20T17:00:00Z',
  }),
]

const ids = (incidentes: IncidentePanel[]) => incidentes.map((i) => i.id)

describe('normalizar', () => {
  it('quita tildes y mayúsculas, pero conserva la ñ como n', () => {
    expect(normalizar('  Germán AMÉZAGA  ')).toBe('german amezaga')
    expect(normalizar('Señal')).toBe('senal')
  })
})

describe('filtrarIncidentes', () => {
  it('sin filtros devuelve todo', () => {
    expect(ids(filtrarIncidentes(lista, FILTROS_VACIOS, AHORA))).toEqual(['1', '2', '3'])
  })

  it('por estado', () => {
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, estado: 'en_atencion' }, AHORA))).toEqual(['2'])
  })

  it('por categoría', () => {
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, categoriaId: 3 }, AHORA))).toEqual(['3'])
  })

  it('busca por código sin importar mayúsculas', () => {
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'cvk-000002' }, AHORA))).toEqual(['2'])
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'demo' }, AHORA))).toEqual(['3'])
  })

  it('busca por título y por dirección sin tildes', () => {
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'BIBLIOTECA' }, AHORA))).toEqual(['1'])
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'german amezaga' }, AHORA))).toEqual(['1'])
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'venezuela' }, AHORA))).toEqual(['2'])
  })

  it('todas las palabras de la búsqueda tienen que aparecer', () => {
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'bache biblioteca' }, AHORA))).toEqual(['1'])
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, texto: 'bache venezuela' }, AHORA))).toEqual([])
  })

  it('solo vencidos: abiertos que superaron el plazo; un resuelto tardío no cuenta', () => {
    expect(ids(filtrarIncidentes(lista, { ...FILTROS_VACIOS, soloVencidos: true }, AHORA))).toEqual(['2'])
  })

  it('combina los filtros', () => {
    const filtros = { ...FILTROS_VACIOS, estado: 'en_atencion' as const, categoriaId: 2, texto: 'puerta' }
    expect(ids(filtrarIncidentes(lista, filtros, AHORA))).toEqual(['2'])
    expect(ids(filtrarIncidentes(lista, { ...filtros, categoriaId: 1 }, AHORA))).toEqual([])
  })
})

describe('hayFiltrosActivos', () => {
  it('distingue la búsqueda en blanco', () => {
    expect(hayFiltrosActivos(FILTROS_VACIOS)).toBe(false)
    expect(hayFiltrosActivos({ ...FILTROS_VACIOS, texto: '   ' })).toBe(false)
    expect(hayFiltrosActivos({ ...FILTROS_VACIOS, texto: 'a' })).toBe(true)
    expect(hayFiltrosActivos({ ...FILTROS_VACIOS, soloVencidos: true })).toBe(true)
  })
})

describe('ordenarIncidentes', () => {
  it('por prioridad descendente y, en empate, el más reciente primero', () => {
    const empate = [
      incidente({ id: 'viejo', prioridad: 50, creado_en: '2026-11-01T00:00:00Z' }),
      incidente({ id: 'alto', prioridad: 90 }),
      incidente({ id: 'nuevo', prioridad: 50, creado_en: '2026-11-10T00:00:00Z' }),
    ]
    expect(ids(ordenarIncidentes(empate, 'prioridad'))).toEqual(['alto', 'nuevo', 'viejo'])
  })

  it('por fecha y por apoyos, sin modificar la lista original', () => {
    expect(ids(ordenarIncidentes(lista, 'recientes'))).toEqual(['1', '2', '3'])
    expect(ids(ordenarIncidentes(lista, 'antiguos'))).toEqual(['3', '2', '1'])
    expect(ids(ordenarIncidentes(lista, 'apoyos'))).toEqual(['2', '1', '3'])
    expect(ids(lista)).toEqual(['1', '2', '3'])
  })
})

describe('contarPorEstado', () => {
  it('cuenta cada estado y el total', () => {
    expect(contarPorEstado(lista)).toEqual({
      todos: 3,
      reportado: 0,
      en_revision: 0,
      derivado: 1,
      en_atencion: 1,
      resuelto: 1,
      rechazado: 0,
    })
  })
})
