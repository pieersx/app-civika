import { Flag, Search, SearchX, ThumbsUp, TriangleAlert } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { EtiquetaEstado } from '../componentes/Estado'
import { PlazoIndicador } from '../componentes/PlazoIndicador'
import { Boton, Campo, Cargando, Dato, ErrorVista, EstadoVacio, Esqueleto, Selector } from '../componentes/ui'
import { useCategorias, useIncidentes } from '../lib/consultas'
import { cx } from '../lib/cx'
import { ESTADOS, etiquetaEstado } from '../lib/estados'
import {
  contarPorEstado,
  filtrarIncidentes,
  hayFiltrosActivos,
  ordenarIncidentes,
  ORDENES,
  type Filtros,
  type Orden,
} from '../lib/filtros'
import { calcularPlazo } from '../lib/plazos'
import type { Estado, IncidentePanel } from '../lib/tipos'
import { useUsuario } from '../sesion/contexto'
import { Encabezado } from './Encabezado'

function leerFiltros(params: URLSearchParams): { filtros: Filtros; orden: Orden } {
  const estado = params.get('estado')
  const categoria = Number(params.get('categoria'))
  const orden = params.get('orden')
  return {
    filtros: {
      estado: ESTADOS.includes(estado as Estado) ? (estado as Estado) : null,
      categoriaId: Number.isInteger(categoria) && categoria > 0 ? categoria : null,
      texto: params.get('q') ?? '',
      soloVencidos: params.get('vencidos') === '1',
    },
    orden: ORDENES.some((o) => o.valor === orden) ? (orden as Orden) : 'prioridad',
  }
}

const COLUMNAS = 'md:grid md:grid-cols-[9rem_minmax(0,1fr)_11rem_8rem] md:items-center md:gap-4'

function Fila({ incidente, ahora }: { incidente: IncidentePanel; ahora: Date }) {
  const plazo = calcularPlazo(incidente, ahora)
  return (
    <li>
      <Link
        to={`/incidentes/${incidente.id}`}
        className={cx('flex flex-col gap-1.5 border-l-4 px-4 py-3 hover-tinta', COLUMNAS, plazo.vencido ? 'border-l-error' : 'border-l-transparent')}
      >
        <EtiquetaEstado estado={incidente.estado} />
        <span className="min-w-0">
          <span className="block line-clamp-2 text-base font-bold text-tinta md:truncate">{incidente.titulo}</span>
          <span className="block line-clamp-2 text-sm text-tinta-suave md:truncate">
            {incidente.codigo} · {incidente.categoria} · {incidente.direccion ?? incidente.zona}
          </span>
        </span>
        <PlazoIndicador plazo={plazo} />
        <span className="flex flex-wrap items-center gap-x-3 gap-y-1 md:justify-end">
          <Dato icono={ThumbsUp} titulo="Apoyos">
            <span className="sr-only">Apoyos: </span>
            {incidente.apoyos_count}
          </Dato>
          {incidente.denuncias_pendientes > 0 && (
            <span className="inline-flex items-center gap-1 text-sm font-bold text-error" title="Denuncias pendientes">
              <Flag aria-hidden="true" className="size-4" />
              <span className="sr-only">Denuncias pendientes: </span>
              {incidente.denuncias_pendientes}
            </span>
          )}
        </span>
      </Link>
    </li>
  )
}

export function ListaIncidentes() {
  const usuario = useUsuario()
  const incidentes = useIncidentes()
  const categorias = useCategorias()
  const [params, setParams] = useSearchParams()
  const consulta = params.toString()
  const { filtros, orden } = useMemo(() => leerFiltros(new URLSearchParams(consulta)), [consulta])
  const [ahora] = useState(() => new Date())

  function cambiar(parcial: Partial<Filtros> & { orden?: Orden }) {
    const { orden: nuevoOrden = orden, ...resto } = parcial
    const siguiente = { ...filtros, ...resto }
    const nuevo = new URLSearchParams()
    if (siguiente.estado) nuevo.set('estado', siguiente.estado)
    if (siguiente.categoriaId) nuevo.set('categoria', String(siguiente.categoriaId))
    if (siguiente.texto) nuevo.set('q', siguiente.texto)
    if (siguiente.soloVencidos) nuevo.set('vencidos', '1')
    if (nuevoOrden !== 'prioridad') nuevo.set('orden', nuevoOrden)
    setParams(nuevo, { replace: true })
  }

  const datos = incidentes.data
  const visibles = useMemo(
    () => (datos ? ordenarIncidentes(filtrarIncidentes(datos, filtros, ahora), orden) : []),
    [datos, filtros, orden, ahora],
  )
  const conteo = useMemo(() => contarPorEstado(datos ?? []), [datos])
  const activos = hayFiltrosActivos(filtros)

  return (
    <>
      <Encabezado
        titulo="Incidentes"
        descripcion={
          usuario.rol === 'moderador'
            ? 'Todos los incidentes del piloto, ordenados por prioridad.'
            : 'Los incidentes asignados a tu entidad, ordenados por prioridad.'
        }
      />

      <div className="mb-4 flex flex-col gap-3">
        <div role="group" aria-label="Filtrar por estado" className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 md:mx-0 md:flex-wrap md:px-0">
          {[null, ...ESTADOS].map((estado) => {
            const activo = filtros.estado === estado
            const total = conteo[estado ?? 'todos']
            return (
              <button
                key={estado ?? 'todos'}
                type="button"
                aria-pressed={activo}
                onClick={() => cambiar({ estado })}
                className={cx(
                  'inline-flex min-h-11 shrink-0 items-center gap-1.5 rounded-campo border px-3 text-sm font-bold',
                  activo ? 'tinte-verde border-verde text-verde' : 'border-borde-campo bg-superficie text-tinta hover-tinta',
                )}
              >
                {estado ? etiquetaEstado(estado) : 'Todos'}
                <span className={activo ? 'text-verde' : 'text-tinta-suave'}>{datos ? total : '–'}</span>
              </button>
            )
          })}
        </div>

        <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_14rem_12rem]">
          <div className="relative">
            <Campo
              etiqueta="Buscar por código, título o dirección"
              etiquetaOculta
              type="search"
              placeholder="Buscar por código, título o dirección"
              autoComplete="off"
              value={filtros.texto}
              onChange={(e) => cambiar({ texto: e.target.value })}
              className="[&_input]:pl-10"
            />
            <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-3.5 size-5 text-tinta-suave" />
          </div>
          <Selector
            etiqueta="Categoría"
            etiquetaOculta
            value={filtros.categoriaId ?? ''}
            onChange={(e) => cambiar({ categoriaId: e.target.value ? Number(e.target.value) : null })}
          >
            <option value="">Todas las categorías</option>
            {categorias.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </Selector>
          <Selector etiqueta="Ordenar por" etiquetaOculta value={orden} onChange={(e) => cambiar({ orden: e.target.value as Orden })}>
            {ORDENES.map((o) => (
              <option key={o.valor} value={o.valor}>
                Orden: {o.etiqueta.toLowerCase()}
              </option>
            ))}
          </Selector>
        </div>

        <label className="inline-flex min-h-11 w-fit cursor-pointer items-center gap-2 text-sm font-bold text-tinta">
          <input
            type="checkbox"
            checked={filtros.soloVencidos}
            onChange={(e) => cambiar({ soloVencidos: e.target.checked })}
            className="size-5 accent-[var(--verde)]"
          />
          <TriangleAlert aria-hidden="true" className="size-4 text-error" />
          Solo los vencidos
        </label>
      </div>

      <div className="overflow-hidden rounded-tarjeta border border-borde bg-superficie">
        {incidentes.isPending ? (
          <Cargando texto="Cargando incidentes…">
            {Array.from({ length: 6 }, (_, i) => (
              <div key={i} className="border-b border-borde px-4 py-3 last:border-b-0">
                <Esqueleto className="h-5 w-24" />
                <Esqueleto className="mt-2 h-5 w-3/4" />
                <Esqueleto className="mt-2 h-4 w-1/2" />
              </div>
            ))}
          </Cargando>
        ) : incidentes.isError ? (
          <ErrorVista error={incidentes.error} alReintentar={() => void incidentes.refetch()} />
        ) : visibles.length === 0 ? (
          activos ? (
            <EstadoVacio
              icono={SearchX}
              titulo="Ningún incidente coincide"
              accion={<Boton onClick={() => setParams({}, { replace: true })}>Quitar los filtros</Boton>}
            >
              Prueba con otra palabra o quita algún filtro.
            </EstadoVacio>
          ) : (
            <EstadoVacio titulo="Aún no hay incidentes">
              {usuario.rol === 'moderador'
                ? 'Cuando alguien reporte un problema en el piloto, aparecerá aquí.'
                : 'Cuando se derive un incidente a tu entidad, aparecerá aquí.'}
            </EstadoVacio>
          )
        ) : (
          <>
            <div
              aria-hidden="true"
              className={cx('hidden border-b border-borde bg-arena px-4 py-2 pl-5 text-sm font-bold text-tinta-suave', COLUMNAS)}
            >
              <span>Estado</span>
              <span>Incidente</span>
              <span>Plazo</span>
              <span className="text-right">Apoyos</span>
            </div>
            <ul className="divide-y divide-borde">
              {visibles.map((incidente) => (
                <Fila key={incidente.id} incidente={incidente} ahora={ahora} />
              ))}
            </ul>
            <p className="border-t border-borde px-4 py-2 text-sm text-tinta-suave" aria-live="polite">
              {visibles.length === 1 ? '1 incidente' : `${visibles.length} incidentes`}
              {activos && datos ? ` de ${datos.length}` : ''}
              {datos && datos.length >= 500 ? '. El panel muestra como máximo 500: usa los filtros para acotar.' : ''}
            </p>
          </>
        )}
      </div>
    </>
  )
}
