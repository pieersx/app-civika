// Fechas del panel. Todo se calcula y se muestra en America/Lima
// (docs/arquitectura.md §11): un reporte de las 9 p. m. es de ese día, no del siguiente en UTC.

export const ZONA_HORARIA = 'America/Lima'
const LOCALE = 'es-PE'
const MS_POR_DIA = 86_400_000

const formatoDia = new Intl.DateTimeFormat('en-CA', {
  timeZone: ZONA_HORARIA,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const formatoFecha = new Intl.DateTimeFormat(LOCALE, {
  timeZone: ZONA_HORARIA,
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const formatoHora = new Intl.DateTimeFormat(LOCALE, {
  timeZone: ZONA_HORARIA,
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const formatoDiaUtc = new Intl.DateTimeFormat(LOCALE, { timeZone: 'UTC', day: 'numeric', month: 'short', year: 'numeric' })
const formatoSemanaUtc = new Intl.DateTimeFormat(LOCALE, { timeZone: 'UTC', day: 'numeric', month: 'short' })

type Instante = string | Date

function aDate(valor: Instante): Date {
  return typeof valor === 'string' ? new Date(valor) : valor
}

/** Día de calendario en Lima, como `AAAA-MM-DD`. */
export function diaLima(valor: Instante): string {
  return formatoDia.format(aDate(valor))
}

/** Días de calendario de Lima entre dos instantes. Nunca negativo. */
export function diasEntre(desde: Instante, hasta: Instante): number {
  const a = Date.parse(`${diaLima(desde)}T00:00:00Z`)
  const b = Date.parse(`${diaLima(hasta)}T00:00:00Z`)
  return Math.max(Math.round((b - a) / MS_POR_DIA), 0)
}

/** «13 nov 2026». */
export function formatearFecha(valor: Instante): string {
  return formatoFecha.format(aDate(valor))
}

/** «13 nov 2026, 21:30». */
export function formatearFechaHora(valor: Instante): string {
  const fecha = aDate(valor)
  return `${formatoFecha.format(fecha)}, ${formatoHora.format(fecha)}`
}

/** Formatea un día `AAAA-MM-DD` (ya expresado en Lima) sin volver a convertirlo. */
export function formatearDia(dia: string): string {
  return formatoDiaUtc.format(new Date(`${dia}T00:00:00Z`))
}

/** Etiqueta corta de una semana a partir de su lunes: «9 nov». */
export function formatearSemana(dia: string): string {
  return formatoSemanaUtc.format(new Date(`${dia}T00:00:00Z`))
}
