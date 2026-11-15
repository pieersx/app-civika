// Estados del incidente y transiciones por rol (docs/arquitectura.md §5).
// La regla real vive en la base (`private.transicion_permitida`); aquí se replica
// solo para no ofrecer en pantalla acciones que el servidor va a rechazar.

import type { Estado, RolPanel } from './tipos'

export const ESTADOS: readonly Estado[] = ['reportado', 'en_revision', 'derivado', 'en_atencion', 'resuelto', 'rechazado']

const ETIQUETAS: Record<Estado, string> = {
  reportado: 'Reportado',
  en_revision: 'En revisión',
  derivado: 'Derivado',
  en_atencion: 'En atención',
  resuelto: 'Resuelto',
  rechazado: 'Rechazado',
}

export function etiquetaEstado(estado: Estado): string {
  return ETIQUETAS[estado]
}

/** Familia visual (docs/diseno/direccion-visual.md): define forma y color. */
export type Familia = 'abierto' | 'en_atencion' | 'resuelto' | 'rechazado'

export function familiaDeEstado(estado: Estado): Familia {
  switch (estado) {
    case 'en_atencion':
      return 'en_atencion'
    case 'resuelto':
      return 'resuelto'
    case 'rechazado':
      return 'rechazado'
    default:
      return 'abierto'
  }
}

export function estaCerrado(estado: Estado): boolean {
  return estado === 'resuelto' || estado === 'rechazado'
}

export interface Transicion {
  hacia: Estado
  /** Texto del botón: la acción mantiene el mismo nombre en todo el flujo. */
  etiqueta: string
  /** Qué pasa al confirmar, en una frase. */
  ayuda: string
  /** Mensaje del aviso tras el cambio. */
  hecho: string
  comentario: 'obligatorio' | 'opcional'
  etiquetaComentario: string
  requiereEntidad: boolean
  /** `principal` es la acción esperada; `secundaria`, la salida alternativa; `peligro`, irreversible. */
  tono: 'principal' | 'secundaria' | 'peligro'
}

const TRANSICIONES: Record<RolPanel, Partial<Record<Estado, Transicion[]>>> = {
  entidad: {
    derivado: [
      {
        hacia: 'en_atencion',
        etiqueta: 'Empezar atención',
        ayuda: 'El incidente pasa a «En atención» y se avisa a quienes lo siguen.',
        hecho: 'Atención empezada',
        comentario: 'opcional',
        etiquetaComentario: 'Nota para el historial (opcional)',
        requiereEntidad: false,
        tono: 'principal',
      },
      {
        hacia: 'en_revision',
        etiqueta: 'No me corresponde',
        ayuda: 'El incidente vuelve a moderación para que lo asigne a otra entidad. Dejará de aparecer en tu lista.',
        hecho: 'Incidente enviado a moderación',
        comentario: 'obligatorio',
        etiquetaComentario: 'Por qué no corresponde a tu entidad',
        requiereEntidad: false,
        tono: 'secundaria',
      },
    ],
    en_atencion: [
      {
        hacia: 'resuelto',
        etiqueta: 'Marcar como resuelto',
        ayuda: 'Se preguntará a quienes siguen el incidente si de verdad quedó arreglado.',
        hecho: 'Incidente marcado como resuelto',
        comentario: 'opcional',
        etiquetaComentario: 'Qué se hizo (opcional)',
        requiereEntidad: false,
        tono: 'principal',
      },
      {
        hacia: 'en_revision',
        etiqueta: 'Enviar a revisión',
        ayuda: 'El incidente vuelve a moderación. Dejará de aparecer en tu lista.',
        hecho: 'Incidente enviado a moderación',
        comentario: 'obligatorio',
        etiquetaComentario: 'Por qué necesita revisión',
        requiereEntidad: false,
        tono: 'secundaria',
      },
    ],
  },
  moderador: {
    en_revision: [
      {
        hacia: 'derivado',
        etiqueta: 'Derivar a una entidad',
        ayuda: 'El incidente pasa a «Derivado» y aparece en la lista de la entidad que elijas.',
        hecho: 'Incidente derivado',
        comentario: 'opcional',
        etiquetaComentario: 'Nota para el historial (opcional)',
        requiereEntidad: true,
        tono: 'principal',
      },
      {
        hacia: 'rechazado',
        etiqueta: 'Rechazar',
        ayuda: 'El incidente deja de verse en el mapa público y se avisa al autor con el motivo. No se puede deshacer.',
        hecho: 'Incidente rechazado',
        comentario: 'obligatorio',
        etiquetaComentario: 'Motivo del rechazo',
        requiereEntidad: false,
        tono: 'peligro',
      },
    ],
  },
}

/** Transiciones que el rol puede pedir desde un estado. Vacío si no hay ninguna. */
export function transicionesPermitidas(rol: RolPanel, estado: Estado): Transicion[] {
  return TRANSICIONES[rol][estado] ?? []
}

/** Marcar como duplicado usa otra RPC (`marcar_duplicado`): solo moderación y solo en revisión. */
export function puedeMarcarDuplicado(rol: RolPanel, estado: Estado): boolean {
  return rol === 'moderador' && estado === 'en_revision'
}
