import { ChartColumn } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { Cargando, ErrorVista, EstadoVacio, Esqueleto, Tarjeta } from '../componentes/ui'
import { useIndicadores } from '../lib/consultas'
import { cx } from '../lib/cx'
import { formatearDia, formatearSemana } from '../lib/fechas'
import type { Indicadores as DatosIndicadores, PuntoSerie } from '../lib/tipos'
import { Encabezado } from './Encabezado'

const PERIODOS = [30, 90, 180] as const

// Los números de las RPC (count, round) llegan como número en json; se normalizan por si llegan como texto.
const num = (valor: unknown): number => Number(valor ?? 0)

function dias(valor: number | null): string {
  if (valor === null) return '–'
  return `${valor.toLocaleString('es-PE', { maximumFractionDigits: 1 })} ${valor === 1 ? 'día' : 'días'}`
}

function porcentaje(valor: number | null): string {
  return valor === null ? '–' : `${valor.toLocaleString('es-PE', { maximumFractionDigits: 1 })} %`
}

function Kpi({ etiqueta, valor, nota }: { etiqueta: string; valor: string; nota?: string }) {
  return (
    <div className="rounded-tarjeta border border-borde bg-superficie p-4">
      <dt className="text-sm text-tinta-suave">{etiqueta}</dt>
      <dd className="mt-1 font-titulo text-cifra font-bold text-tinta">{valor}</dd>
      {nota && <p className="text-xs text-tinta-suave">{nota}</p>}
    </div>
  )
}

interface FilaBarra {
  clave: string | number
  etiqueta: string
  valor: number
  detalle: string
}

/** Barras horizontales de una sola serie. El valor va escrito, así que no depende del color. */
function Barras({ filas, descripcion }: { filas: FilaBarra[]; descripcion: string }) {
  const maximo = Math.max(...filas.map((f) => f.valor), 1)
  return (
    <ul aria-label={descripcion} className="flex flex-col gap-3">
      {filas.map((fila) => (
        <li key={fila.clave}>
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="min-w-0 truncate font-bold text-tinta">{fila.etiqueta}</span>
            <span className="shrink-0 font-bold text-tinta">{fila.valor}</span>
          </div>
          <div className="mt-1 h-3 rounded-[4px] bg-borde" aria-hidden="true">
            <div className="h-full rounded-[4px] bg-datos" style={{ width: `${(fila.valor / maximo) * 100}%`, minWidth: fila.valor > 0 ? 4 : 0 }} />
          </div>
          <p className="mt-0.5 text-xs text-tinta-suave">{fila.detalle}</p>
        </li>
      ))}
    </ul>
  )
}

/** Serie semanal: creados (relleno sólido) frente a resueltos (rayado). Distintos por textura, no solo por color. */
function SerieSemanal({ serie }: { serie: PuntoSerie[] }) {
  const alto = 180
  const ancho = 640
  const margenIzq = 28
  const margenInf = 28
  const maximo = Math.max(...serie.flatMap((p) => [num(p.creados), num(p.resueltos)]), 1)
  const tope = Math.max(Math.ceil(maximo / 2) * 2, 2)
  const paso = (ancho - margenIzq) / serie.length
  const barra = Math.min(paso / 2 - 4, 22)
  const y = (v: number) => alto - margenInf - (v / tope) * (alto - margenInf - 8)

  return (
    <div>
      <ul className="mb-2 flex flex-wrap gap-x-5 gap-y-1 text-sm text-tinta" aria-label="Leyenda">
        <li className="inline-flex items-center gap-2">
          <span aria-hidden="true" className="size-3.5 rounded-[3px] bg-datos" />
          Creados
        </li>
        <li className="inline-flex items-center gap-2">
          <svg aria-hidden="true" width="14" height="14">
            <rect x="1" y="1" width="12" height="12" rx="3" fill="url(#rayas)" stroke="var(--datos)" strokeWidth="2" />
          </svg>
          Resueltos
        </li>
      </ul>
      <svg viewBox={`0 0 ${ancho} ${alto}`} role="img" aria-label="Incidentes creados y resueltos por semana. Los datos están en la tabla de abajo." className="h-auto w-full">
        <defs>
          <pattern id="rayas" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
            <rect width="6" height="6" fill="var(--superficie)" />
            <rect width="3" height="6" fill="var(--datos)" />
          </pattern>
        </defs>
        {[0, tope / 2, tope].map((marca) => (
          <g key={marca}>
            <line x1={margenIzq} x2={ancho} y1={y(marca)} y2={y(marca)} stroke="var(--borde)" strokeWidth="1" />
            <text x={margenIzq - 6} y={y(marca) + 4} textAnchor="end" fontSize="11" fill="var(--tinta-suave)">
              {marca}
            </text>
          </g>
        ))}
        {serie.map((punto, i) => {
          const x0 = margenIzq + i * paso + paso / 2
          const creados = num(punto.creados)
          const resueltos = num(punto.resueltos)
          return (
            <g key={punto.semana}>
              <rect x={x0 - barra - 1} y={y(creados)} width={barra} height={alto - margenInf - y(creados)} rx="3" fill="var(--datos)" />
              <rect x={x0 + 1} y={y(resueltos)} width={barra} height={alto - margenInf - y(resueltos)} rx="3" fill="url(#rayas)" stroke="var(--datos)" strokeWidth="1.5" />
              <text x={x0} y={alto - 10} textAnchor="middle" fontSize="11" fill="var(--tinta-suave)">
                {formatearSemana(punto.semana)}
              </text>
            </g>
          )
        })}
      </svg>
      <details className="mt-2">
        <summary className="inline-flex min-h-11 items-center text-sm font-bold text-terracota-texto">Ver los datos en una tabla</summary>
        <div className="overflow-x-auto">
          <table className="mt-1 w-full text-left text-sm">
            <caption className="sr-only">Incidentes creados y resueltos por semana</caption>
            <thead>
              <tr className="border-b border-borde text-tinta-suave">
                <th scope="col" className="py-2 pr-4 font-bold">Semana del</th>
                <th scope="col" className="py-2 pr-4 text-right font-bold">Creados</th>
                <th scope="col" className="py-2 text-right font-bold">Resueltos</th>
              </tr>
            </thead>
            <tbody>
              {serie.map((p) => (
                <tr key={p.semana} className="border-b border-borde last:border-0">
                  <th scope="row" className="py-2 pr-4 font-normal">{formatearDia(p.semana)}</th>
                  <td className="py-2 pr-4 text-right">{num(p.creados)}</td>
                  <td className="py-2 text-right">{num(p.resueltos)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </div>
  )
}

function Seccion({ titulo, id, children }: { titulo: string; id: string; children: ReactNode }) {
  return (
    <Tarjeta className="p-4" aria-labelledby={id}>
      <h2 id={id} className="mb-4 font-titulo text-titular font-semibold">
        {titulo}
      </h2>
      {children}
    </Tarjeta>
  )
}

function Contenido({ datos }: { datos: DatosIndicadores }) {
  const total = num(datos.total)
  if (total === 0) {
    return (
      <Tarjeta>
        <EstadoVacio icono={ChartColumn} titulo="Todavía no hay datos en este periodo">
          Prueba con un periodo más largo. Los indicadores se llenan a medida que llegan incidentes.
        </EstadoVacio>
      </Tarjeta>
    )
  }
  const serie = datos.serie ?? []
  return (
    <div className="flex flex-col gap-5">
      <dl className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Kpi etiqueta="Incidentes" valor={String(total)} nota="creados en el periodo" />
        <Kpi etiqueta="Abiertos" valor={String(num(datos.abiertos))} nota="reportados, en revisión o derivados" />
        <Kpi etiqueta="En atención" valor={String(num(datos.en_atencion))} />
        <Kpi etiqueta="Resueltos" valor={String(num(datos.resueltos))} />
        <Kpi etiqueta="Vencidos" valor={String(num(datos.vencidos))} nota="abiertos fuera del plazo" />
        <Kpi etiqueta="Tiempo promedio" valor={dias(datos.tiempo_promedio_dias)} nota="hasta resolver" />
        <Kpi etiqueta="Dentro del plazo" valor={porcentaje(datos.pct_dentro_plazo)} nota="de los resueltos" />
        <Kpi etiqueta="Rechazados" valor={String(num(datos.rechazados))} />
      </dl>

      <div className="grid gap-5 lg:grid-cols-2">
        <Seccion titulo="Por categoría" id="por-categoria">
          <Barras
            descripcion="Incidentes por categoría"
            filas={datos.por_categoria.map((c) => ({
              clave: c.categoria_id,
              etiqueta: c.categoria,
              valor: num(c.total),
              detalle: `${num(c.resueltos)} resueltos · ${num(c.vencidos)} vencidos · plazo de ${c.plazo_objetivo_dias} días · dentro del plazo: ${porcentaje(c.pct_dentro_plazo)}`,
            }))}
          />
        </Seccion>
        <Seccion titulo="Por zona" id="por-zona">
          <Barras
            descripcion="Incidentes por zona"
            filas={datos.por_zona.map((z) => ({
              clave: z.zona_id,
              etiqueta: z.zona,
              valor: num(z.total),
              detalle: `${num(z.abiertos)} abiertos · ${num(z.resueltos)} resueltos · dentro del plazo: ${porcentaje(z.pct_dentro_plazo)}`,
            }))}
          />
        </Seccion>
      </div>

      <Seccion titulo="Creados y resueltos por semana" id="serie">
        {serie.length === 0 ? <EstadoVacio titulo="Sin semanas para mostrar" compacto /> : <SerieSemanal serie={serie} />}
      </Seccion>
    </div>
  )
}

export function Indicadores() {
  const [periodo, setPeriodo] = useState<number>(90)
  const indicadores = useIndicadores(periodo)

  return (
    <>
      <Encabezado
        titulo="Indicadores"
        descripcion={
          indicadores.data
            ? `Del ${formatearDia(indicadores.data.desde)} al ${formatearDia(indicadores.data.hasta)}, en hora de Lima.`
            : 'Cómo va la atención en el periodo que elijas.'
        }
        acciones={
          <div role="group" aria-label="Periodo" className="flex rounded-campo border border-borde-campo bg-superficie p-0.5">
            {PERIODOS.map((p) => (
              <button
                key={p}
                type="button"
                aria-pressed={periodo === p}
                onClick={() => setPeriodo(p)}
                className={cx(
                  'min-h-11 min-w-20 rounded-[6px] px-3 text-sm font-bold',
                  periodo === p ? 'tinte-verde text-verde' : 'text-tinta hover-tinta',
                )}
              >
                {p} días
              </button>
            ))}
          </div>
        }
      />
      {indicadores.isPending ? (
        <Cargando texto="Cargando indicadores…">
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            {Array.from({ length: 8 }, (_, i) => (
              <Esqueleto key={i} className="h-24 rounded-tarjeta" />
            ))}
          </div>
          <Esqueleto className="mt-5 h-64 w-full rounded-tarjeta" />
        </Cargando>
      ) : indicadores.isError ? (
        <ErrorVista error={indicadores.error} alReintentar={() => void indicadores.refetch()} />
      ) : (
        <Contenido datos={indicadores.data} />
      )}
    </>
  )
}
