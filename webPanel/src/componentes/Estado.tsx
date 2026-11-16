// El estado nunca se comunica solo con color: forma + ícono + palabra.
// Triángulo = abierto, rombo = en atención, círculo = resuelto, cuadrado = rechazado.

import { cx } from '../lib/cx'
import { etiquetaEstado, familiaDeEstado, type Familia } from '../lib/estados'
import type { Estado } from '../lib/tipos'


const RELLENO: Record<Familia, string> = {
  abierto: 'var(--abierto)',
  en_atencion: 'var(--atencion)',
  resuelto: 'var(--resuelto)',
  rechazado: 'var(--rechazado)',
}

const ICONO: Record<Familia, string> = {
  abierto: 'var(--sobre-abierto)',
  en_atencion: 'var(--sobre-atencion)',
  resuelto: 'var(--sobre-resuelto)',
  rechazado: 'var(--sobre-rechazado)',
}

const TEXTO: Record<Familia, string> = {
  abierto: 'text-abierto-texto',
  en_atencion: 'text-atencion-texto',
  resuelto: 'text-resuelto-texto',
  rechazado: 'text-rechazado-texto',
}

function Silueta({ familia }: { familia: Familia }) {
  const comun = { fill: RELLENO[familia], stroke: 'var(--contorno-marcador)', strokeWidth: 1.5, strokeLinejoin: 'round' as const }
  switch (familia) {
    case 'abierto':
      return <path d="M12 2.5 22.5 21h-21z" {...comun} />
    case 'en_atencion':
      return <path d="M12 1.5 22.5 12 12 22.5 1.5 12z" {...comun} />
    case 'resuelto':
      return <circle cx="12" cy="12" r="10" {...comun} />
    case 'rechazado':
      return <rect x="2.5" y="2.5" width="19" height="19" rx="2.5" {...comun} />
  }
}

function Glifo({ familia }: { familia: Familia }) {
  const trazo = { stroke: ICONO[familia], strokeWidth: 2.2, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, fill: 'none' }
  switch (familia) {
    case 'abierto': // signo de exclamación
      return (
        <>
          <path d="M12 9.5v5" {...trazo} />
          <circle cx="12" cy="17.6" r="1.2" fill={ICONO[familia]} />
        </>
      )
    case 'en_atencion': // obra en curso: tres puntos
      return (
        <>
          <circle cx="7.6" cy="12" r="1.4" fill={ICONO[familia]} />
          <circle cx="12" cy="12" r="1.4" fill={ICONO[familia]} />
          <circle cx="16.4" cy="12" r="1.4" fill={ICONO[familia]} />
        </>
      )
    case 'resuelto': // visto bueno
      return <path d="m7.5 12.3 3 3 6-6.3" {...trazo} />
    case 'rechazado': // aspa
      return <path d="m8.2 8.2 7.6 7.6m0-7.6-7.6 7.6" {...trazo} />
  }
}

/** Solo la forma. Úsala junto a la palabra del estado, o con `titulo` si va sola (mapa). */
export function FormaEstado({ estado, tamano = 20, titulo }: { estado: Estado; tamano?: number; titulo?: string }) {
  const familia = familiaDeEstado(estado)
  return (
    <svg
      width={tamano}
      height={tamano}
      viewBox="0 0 24 24"
      className="shrink-0"
      role={titulo ? 'img' : undefined}
      aria-label={titulo}
      aria-hidden={titulo ? undefined : true}
    >
      <Silueta familia={familia} />
      <Glifo familia={familia} />
    </svg>
  )
}

/** Forma + palabra: la manera normal de mostrar un estado. */
export function EtiquetaEstado({ estado, className }: { estado: Estado; className?: string }) {
  return (
    <span className={cx('inline-flex items-center gap-1.5 whitespace-nowrap text-sm font-bold', TEXTO[familiaDeEstado(estado)], className)}>
      <FormaEstado estado={estado} tamano={18} />
      {etiquetaEstado(estado)}
    </span>
  )
}
