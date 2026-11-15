import { CircleCheck, Flag, ShieldCheck } from 'lucide-react'
import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useAvisos } from '../componentes/avisos'
import { Dialogo, PieDialogo } from '../componentes/Dialogo'
import { EtiquetaEstado } from '../componentes/Estado'
import { PlazoIndicador } from '../componentes/PlazoIndicador'
import { AreaTexto, Aviso, Boton, Cargando, Dato, ErrorVista, EstadoVacio, Esqueleto, Tarjeta } from '../componentes/ui'
import { useDenuncias, useIncidentes, useResolverDenuncia } from '../lib/consultas'
import { cx } from '../lib/cx'
import { interpretarError } from '../lib/errores'
import { formatearFechaHora } from '../lib/fechas'
import { calcularPlazo } from '../lib/plazos'
import type { Denuncia } from '../lib/tipos'
import { Encabezado } from './Encabezado'

type Pestana = 'revision' | 'denuncias'

export function Moderacion() {
  const [pestana, setPestana] = useState<Pestana>('revision')
  const incidentes = useIncidentes()
  const denuncias = useDenuncias()

  const enRevision = useMemo(
    () => (incidentes.data ?? []).filter((i) => i.estado === 'en_revision').sort((a, b) => b.prioridad - a.prioridad),
    [incidentes.data],
  )

  const pestanas: { valor: Pestana; etiqueta: string; total: number | null }[] = [
    { valor: 'revision', etiqueta: 'En revisión', total: incidentes.data ? enRevision.length : null },
    { valor: 'denuncias', etiqueta: 'Denuncias pendientes', total: denuncias.data ? denuncias.data.length : null },
  ]

  return (
    <>
      <Encabezado
        titulo="Moderación"
        descripcion="Decide qué hacer con los incidentes sin entidad o enviados a revisión, y atiende las denuncias de los vecinos."
      />

      <div role="tablist" aria-label="Cola de moderación" className="mb-4 flex gap-2">
        {pestanas.map(({ valor, etiqueta, total }) => {
          const activa = pestana === valor
          return (
            <button
              key={valor}
              id={`pestana-${valor}`}
              type="button"
              role="tab"
              aria-selected={activa}
              aria-controls={`panel-${valor}`}
              onClick={() => setPestana(valor)}
              className={cx(
                'inline-flex min-h-11 items-center gap-2 rounded-campo border px-4 text-sm font-bold',
                activa ? 'tinte-verde border-verde text-verde' : 'border-borde-campo bg-superficie text-tinta hover-tinta',
              )}
            >
              {etiqueta}
              <span className={activa ? 'text-verde' : 'text-tinta-suave'}>{total ?? '–'}</span>
            </button>
          )
        })}
      </div>

      {pestana === 'revision' ? (
        <div role="tabpanel" id="panel-revision" aria-labelledby="pestana-revision">
          {incidentes.isPending ? (
            <Cargando texto="Cargando la cola…">
              <Esqueleto className="h-20 w-full" />
              <Esqueleto className="mt-3 h-20 w-full" />
            </Cargando>
          ) : incidentes.isError ? (
            <ErrorVista error={incidentes.error} alReintentar={() => void incidentes.refetch()} />
          ) : enRevision.length === 0 ? (
            <Tarjeta>
              <EstadoVacio icono={ShieldCheck} titulo="No hay nada en revisión">
                Los incidentes sin regla de ruteo, denunciados o devueltos por una entidad aparecerán aquí.
              </EstadoVacio>
            </Tarjeta>
          ) : (
            <ul className="flex flex-col gap-3">
              {enRevision.map((incidente) => (
                <li key={incidente.id}>
                  <Link
                    to={`/incidentes/${incidente.id}`}
                    className="flex flex-col gap-1.5 rounded-tarjeta border border-borde bg-superficie p-4 hover-tinta"
                  >
                    <span className="flex flex-wrap items-center gap-x-4 gap-y-1">
                      <EtiquetaEstado estado={incidente.estado} />
                      <PlazoIndicador plazo={calcularPlazo(incidente)} />
                      {incidente.denuncias_pendientes > 0 && (
                        <span className="inline-flex items-center gap-1 text-sm font-bold text-error">
                          <Flag aria-hidden="true" className="size-4" />
                          {incidente.denuncias_pendientes} {incidente.denuncias_pendientes === 1 ? 'denuncia' : 'denuncias'}
                        </span>
                      )}
                    </span>
                    <span className="text-base font-bold text-tinta">{incidente.titulo}</span>
                    <span className="text-sm text-tinta-suave">
                      {incidente.codigo} · {incidente.categoria} · {incidente.entidad ?? 'Sin entidad'}
                      {incidente.ubicacion_simulada ? ' · Ubicación simulada' : ''}
                    </span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : (
        <div role="tabpanel" id="panel-denuncias" aria-labelledby="pestana-denuncias">
          <ListaDenuncias />
        </div>
      )}
    </>
  )
}

function ListaDenuncias() {
  const denuncias = useDenuncias()
  const [resolviendo, setResolviendo] = useState<Denuncia | null>(null)

  if (denuncias.isPending) {
    return (
      <Cargando texto="Cargando denuncias…">
        <Esqueleto className="h-24 w-full" />
        <Esqueleto className="mt-3 h-24 w-full" />
      </Cargando>
    )
  }
  if (denuncias.isError) return <ErrorVista error={denuncias.error} alReintentar={() => void denuncias.refetch()} />
  if (denuncias.data.length === 0) {
    return (
      <Tarjeta>
        <EstadoVacio icono={CircleCheck} titulo="No hay denuncias pendientes">
          Cuando un vecino denuncie un reporte, lo verás aquí para resolverlo.
        </EstadoVacio>
      </Tarjeta>
    )
  }
  return (
    <>
      <ul className="flex flex-col gap-3">
        {denuncias.data.map((denuncia) => (
          <li key={denuncia.id}>
            <Tarjeta className="flex flex-col gap-2 p-4">
              <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
                <EtiquetaEstado estado={denuncia.estado} />
                <Dato icono={Flag}>
                  {denuncia.denuncias_incidente} {denuncia.denuncias_incidente === 1 ? 'denuncia' : 'denuncias'} en este incidente
                </Dato>
              </div>
              <p className="text-base font-bold text-tinta">
                <Link to={`/incidentes/${denuncia.incidente_id}`} className="text-terracota-texto underline underline-offset-2">
                  {denuncia.codigo}
                </Link>{' '}
                · {denuncia.titulo}
              </p>
              <p className="whitespace-pre-wrap break-words text-sm text-tinta">
                <span className="font-bold">Motivo: </span>
                {denuncia.motivo}
              </p>
              <p className="text-xs text-tinta-suave">
                {denuncia.categoria} · denunciado el {formatearFechaHora(denuncia.creado_en)}
              </p>
              <div className="pt-1">
                <Boton icono={CircleCheck} onClick={() => setResolviendo(denuncia)}>
                  Resolver denuncia
                </Boton>
              </div>
            </Tarjeta>
          </li>
        ))}
      </ul>
      {resolviendo && <DialogoResolver denuncia={resolviendo} alCerrar={() => setResolviendo(null)} />}
    </>
  )
}

function DialogoResolver({ denuncia, alCerrar }: { denuncia: Denuncia; alCerrar: () => void }) {
  const { avisar } = useAvisos()
  const resolver = useResolverDenuncia()
  const [resolucion, setResolucion] = useState('')
  const [error, setError] = useState<string | null>(null)

  function confirmar(evento: FormEvent) {
    evento.preventDefault()
    if (!resolucion.trim()) return
    setError(null)
    resolver.mutate(
      { denunciaId: denuncia.id, resolucion: resolucion.trim() },
      {
        onSuccess: () => {
          avisar('Denuncia resuelta', 'exito')
          alCerrar()
        },
        onError: (fallo) => setError(interpretarError(fallo).mensaje),
      },
    )
  }

  return (
    <Dialogo
      titulo="Resolver denuncia"
      descripcion={`${denuncia.codigo}: «${denuncia.motivo.slice(0, 120)}»`}
      alCerrar={alCerrar}
      bloqueado={resolver.isPending}
    >
      <form onSubmit={confirmar} className="flex flex-col gap-4 p-4 sm:p-5" noValidate>
        {error && <Aviso tono="error">{error}</Aviso>}
        <AreaTexto
          etiqueta="Qué decidiste"
          required
          maxLength={500}
          autoFocus
          value={resolucion}
          onChange={(e) => setResolucion(e.target.value)}
          ayuda={`Obligatorio. ${resolucion.length} de 500 caracteres. Si el reporte no cumple las normas, rechaza el incidente desde su detalle.`}
        />
        <PieDialogo>
          <Boton onClick={alCerrar} disabled={resolver.isPending}>
            Cancelar
          </Boton>
          <Boton type="submit" variante="principal" cargando={resolver.isPending} disabled={!resolucion.trim()}>
            Resolver denuncia
          </Boton>
        </PieDialogo>
      </form>
    </Dialogo>
  )
}
