// Tipos del contrato de datos del panel. Reflejan las columnas que devuelven las
// RPC de `supabase/migrations/20261113090300_panel.sql`.

export type Estado = 'reportado' | 'en_revision' | 'derivado' | 'en_atencion' | 'resuelto' | 'rechazado'
export type Rol = 'ciudadano' | 'entidad' | 'moderador'
export type RolPanel = Exclude<Rol, 'ciudadano'>
export type Visibilidad = 'publico' | 'interno'

export interface Usuario {
  id: string
  nombre: string
  rol: Rol
  entidad_id: number | null
  activo: boolean
}

export interface IncidentePanel {
  id: string
  codigo: string
  titulo: string
  descripcion: string | null
  categoria: string
  categoria_id: number
  estado: Estado
  prioridad: number
  apoyos_count: number
  reportes_count: number
  denuncias_pendientes: number
  zona: string
  entidad_id: number | null
  entidad: string | null
  direccion: string | null
  lat: number
  lon: number
  version: number
  creado_en: string
  cerrado_en: string | null
  plazo_objetivo_dias: number
  ubicacion_simulada: boolean
  duplicado_de: string | null
}

export interface EntradaHistorial {
  id: number
  estado_anterior: Estado | null
  estado_nuevo: Estado
  comentario: string | null
  autor: string
  creado_en: string
}

export interface Comentario {
  id: string
  texto: string
  visibilidad: Visibilidad
  autor: string
  creado_en: string
}

export interface FotoPanel {
  id: string
  ruta: string
  oculta: boolean
  tipo: 'original' | 'apoyo'
  creado_en: string
  /** URL firmada (10 minutos). `null` si Storage no pudo firmarla. */
  url: string | null
}

export interface Denuncia {
  id: string
  motivo: string
  resolucion: string | null
  creado_en: string
  incidente_id: string
  codigo: string
  titulo: string
  categoria: string
  estado: Estado
  version: number
  denuncias_incidente: number
}

export interface Entidad {
  id: number
  nombre: string
  tipo: string
}

export interface Categoria {
  id: number
  nombre: string
  service_code: string
  plazo_objetivo_dias: number
}

export interface IndicadorCategoria {
  categoria_id: number
  categoria: string
  plazo_objetivo_dias: number
  total: number
  resueltos: number
  vencidos: number
  tiempo_promedio_dias: number | null
  pct_dentro_plazo: number | null
}

export interface IndicadorZona {
  zona_id: number
  zona: string
  total: number
  abiertos: number
  resueltos: number
  pct_dentro_plazo: number | null
}

export interface PuntoSerie {
  /** Lunes de la semana, `AAAA-MM-DD` en America/Lima. */
  semana: string
  creados: number
  resueltos: number
}

export interface Indicadores {
  desde: string
  hasta: string
  total: number
  abiertos: number
  en_atencion: number
  resueltos: number
  rechazados: number
  vencidos: number
  tiempo_promedio_dias: number | null
  pct_dentro_plazo: number | null
  por_categoria: IndicadorCategoria[]
  por_zona: IndicadorZona[]
  serie: PuntoSerie[] | null
}
