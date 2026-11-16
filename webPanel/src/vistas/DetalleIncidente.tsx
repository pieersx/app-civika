import 'maplibre-gl/dist/maplibre-gl.css'
import { ArrowLeft, Copy, Eye, EyeOff, Flag, ImageOff, MapPin, MessageSquare, Send, ThumbsUp } from 'lucide-react'
import { useMemo, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useAvisos } from '../componentes/avisos'
import { Dialogo, PieDialogo } from '../componentes/Dialogo'
import { EtiquetaEstado, FormaEstado } from '../componentes/Estado'
import { Mapa } from '../componentes/Mapa'
import { PlazoIndicador } from '../componentes/PlazoIndicador'
import {
  AreaTexto,
  Aviso,
  Boton,
  Cargando,
  Dato,
  ErrorVista,
  EstadoVacio,
  Esqueleto,
  Selector,
  Tarjeta,
} from '../componentes/ui'
import {
  useCambiarEstado,
  useComentar,
  useComentarios,
  useEntidades,
  useFotos,
  useHistorial,
  useIncidentes,
  useMarcarDuplicado,
  useOcultarFoto,
} from '../lib/consultas'
import { interpretarError } from '../lib/errores'
import { etiquetaEstado, puedeMarcarDuplicado, transicionesPermitidas, type Transicion } from '../lib/estados'
import { formatearFecha, formatearFechaHora } from '../lib/fechas'
import { calcularPlazo } from '../lib/plazos'
import type { FotoPanel, IncidentePanel, Visibilidad } from '../lib/tipos'
import { useUsuario } from '../sesion/contexto'

export function DetalleIncidente() {
  const { id = '' } = useParams()
  const incidentes = useIncidentes()
  const incidente = incidentes.data?.find((i) => i.id === id)

  const volver = (
    <Link to="/incidentes" className="mb-4 inline-flex min-h-11 items-center gap-1.5 text-sm font-bold text-terracota-texto">
      <ArrowLeft aria-hidden="true" className="size-4" />
      Volver a los incidentes
    </Link>
  )

  if (incidentes.isPending) {
    return (
      <>
        {volver}
        <Cargando texto="Cargando el incidente…">
          <Esqueleto className="h-8 w-2/3" />
          <Esqueleto className="mt-4 h-40 w-full rounded-tarjeta" />
          <Esqueleto className="mt-4 h-64 w-full rounded-tarjeta" />
        </Cargando>
      </>
    )
  }
  if (incidentes.isError) {
    return (
      <>
        {volver}
        <ErrorVista error={incidentes.error} alReintentar={() => void incidentes.refetch()} />
      </>
    )
  }
  if (!incidente) {
    return (
      <>
        {volver}
        <EstadoVacio titulo="No encontramos este incidente">
          Puede que ya no esté en tu lista, por ejemplo porque lo enviaste a revisión. Vuelve a la lista para ver los que tienes.
        </EstadoVacio>
      </>
    )
  }
  return (
    <>
      {volver}
      <Contenido incidente={incidente} />
    </>
  )
}

function Contenido({ incidente }: { incidente: IncidentePanel }) {
  const usuario = useUsuario()
  const plazo = calcularPlazo(incidente)
  const acciones = transicionesPermitidas(usuario.rol, incidente.estado)
  const [transicion, setTransicion] = useState<Transicion | null>(null)
  const [duplicado, setDuplicado] = useState(false)
  const puedeDuplicado = puedeMarcarDuplicado(usuario.rol, incidente.estado)
  const puntos = useMemo(
    () => [{ id: incidente.id, lat: incidente.lat, lon: incidente.lon, estado: incidente.estado, etiqueta: incidente.codigo }],
    [incidente.id, incidente.lat, incidente.lon, incidente.estado, incidente.codigo],
  )

  return (
    <article aria-labelledby="titulo-incidente">
      <header className="mb-5">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
          <EtiquetaEstado estado={incidente.estado} />
          <span className="text-sm text-tinta-suave">{incidente.codigo}</span>
          <PlazoIndicador plazo={plazo} />
        </div>
        <h1 id="titulo-incidente" className="mt-2 font-titulo text-encabezado font-semibold text-tinta md:text-pagina">
          {incidente.titulo}
        </h1>
        <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1">
          <Dato icono={ThumbsUp} titulo="Apoyos">
            {incidente.apoyos_count} {incidente.apoyos_count === 1 ? 'apoyo' : 'apoyos'}
          </Dato>
          <Dato icono={Copy} titulo="Reportes">
            {incidente.reportes_count} {incidente.reportes_count === 1 ? 'reporte' : 'reportes'}
          </Dato>
          {incidente.denuncias_pendientes > 0 && (
            <span className="inline-flex items-center gap-1 text-sm font-bold text-error">
              <Flag aria-hidden="true" className="size-4" />
              {incidente.denuncias_pendientes} {incidente.denuncias_pendientes === 1 ? 'denuncia pendiente' : 'denuncias pendientes'}
            </span>
          )}
        </div>
      </header>

      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,24rem)] xl:grid-cols-[minmax(0,1fr)_minmax(0,28rem)]">
        <div className="flex min-w-0 flex-col gap-5">
          {(acciones.length > 0 || puedeDuplicado) && (
            <Tarjeta className="p-4" aria-labelledby="acciones">
              <h2 id="acciones" className="font-titulo text-titular font-semibold">
                Qué quieres hacer
              </h2>
              <div className="mt-3 flex flex-col gap-2 sm:flex-row sm:flex-wrap">
                {acciones.map((accion) => (
                  <Boton
                    key={accion.hacia}
                    variante={accion.tono === 'principal' ? 'principal' : accion.tono === 'peligro' ? 'peligro' : 'secundario'}
                    onClick={() => setTransicion(accion)}
                  >
                    {accion.etiqueta}
                  </Boton>
                ))}
                {puedeDuplicado && (
                  <Boton icono={Copy} onClick={() => setDuplicado(true)}>
                    Marcar como duplicado
                  </Boton>
                )}
              </div>
            </Tarjeta>
          )}
          {acciones.length === 0 && !puedeDuplicado && (
            <Aviso>
              {usuario.rol === 'entidad'
                ? incidente.estado === 'resuelto'
                  ? 'Este incidente ya está resuelto.'
                  : 'En este estado no tienes acciones pendientes.'
                : 'Moderación solo actúa sobre los incidentes en revisión.'}
            </Aviso>
          )}

          <Tarjeta className="p-4" aria-labelledby="descripcion">
            <h2 id="descripcion" className="font-titulo text-titular font-semibold">
              Descripción
            </h2>
            <p className="mt-2 whitespace-pre-wrap break-words text-base text-tinta">
              {incidente.descripcion?.trim() || 'El reporte no incluye descripción.'}
            </p>
            <dl className="mt-4 grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
              <Dato2 etiqueta="Categoría" valor={incidente.categoria} />
              <Dato2 etiqueta="Zona" valor={incidente.zona} />
              <Dato2 etiqueta="Entidad" valor={incidente.entidad ?? 'Sin asignar'} />
              <Dato2 etiqueta="Reportado" valor={formatearFechaHora(incidente.creado_en)} />
              {incidente.cerrado_en && <Dato2 etiqueta="Cerrado" valor={formatearFechaHora(incidente.cerrado_en)} />}
              <Dato2 etiqueta="Plazo objetivo" valor={`${incidente.plazo_objetivo_dias} días`} />
              <Dato2 etiqueta="Prioridad" valor={String(incidente.prioridad)} />
              {incidente.duplicado_de && <Dato2 etiqueta="Estado de duplicado" valor="Duplicado de otro incidente" />}
            </dl>
          </Tarjeta>

          <Fotos incidente={incidente} />
          <Comentarios incidente={incidente} />
        </div>

        <div className="flex min-w-0 flex-col gap-5">
          <Tarjeta className="overflow-hidden" aria-labelledby="ubicacion">
            <h2 id="ubicacion" className="px-4 pt-4 font-titulo text-titular font-semibold">
              Ubicación
            </h2>
            <p className="flex items-start gap-1.5 px-4 pb-3 pt-1 text-sm text-tinta-suave">
              <MapPin aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
              <span>
                {incidente.direccion ?? 'Sin dirección'} · {incidente.lat.toFixed(5)}, {incidente.lon.toFixed(5)}
              </span>
            </p>
            {incidente.ubicacion_simulada && (
              <Aviso tono="atencion" className="mx-4 mb-3">
                La persona que reportó usó una ubicación simulada: confirma el lugar antes de actuar.
              </Aviso>
            )}
            <Mapa puntos={puntos} etiqueta={`Ubicación de ${incidente.codigo}`} zoom={17} tamanoMarcador={34} className="h-56 w-full border-t border-borde" />
          </Tarjeta>
          <Historial incidenteId={incidente.id} />
        </div>
      </div>

      {transicion && <DialogoTransicion incidente={incidente} transicion={transicion} alCerrar={() => setTransicion(null)} />}
      {duplicado && <DialogoDuplicado incidente={incidente} alCerrar={() => setDuplicado(false)} />}
    </article>
  )
}

function Dato2({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div>
      <dt className="text-tinta-suave">{etiqueta}</dt>
      <dd className="font-bold text-tinta">{valor}</dd>
    </div>
  )
}

// ---------------------------------------------------------------------------
// Cambio de estado
// ---------------------------------------------------------------------------

function DialogoTransicion({ incidente, transicion, alCerrar }: { incidente: IncidentePanel; transicion: Transicion; alCerrar: () => void }) {
  const { avisar } = useAvisos()
  const cambiar = useCambiarEstado()
  const entidades = useEntidades(transicion.requiereEntidad)
  const [comentario, setComentario] = useState('')
  const [entidadId, setEntidadId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const obligatorio = transicion.comentario === 'obligatorio'
  const faltaComentario = obligatorio && comentario.trim() === ''
  const faltaEntidad = transicion.requiereEntidad && entidadId === ''

  function confirmar(evento: FormEvent) {
    evento.preventDefault()
    if (faltaComentario || faltaEntidad) return
    setError(null)
    cambiar.mutate(
      {
        incidenteId: incidente.id,
        nuevoEstado: transicion.hacia,
        versionEsperada: incidente.version,
        comentario: comentario.trim() || null,
        entidadId: transicion.requiereEntidad ? Number(entidadId) : null,
      },
      {
        onSuccess: () => {
          avisar(transicion.hecho, 'exito')
          alCerrar()
        },
        onError: (fallo) => {
          const { tipo, mensaje } = interpretarError(fallo)
          if (tipo === 'conflicto' || tipo === 'no_encontrado') {
            avisar(mensaje, 'error')
            alCerrar()
          } else {
            setError(mensaje)
          }
        },
      },
    )
  }

  return (
    <Dialogo titulo={transicion.etiqueta} descripcion={transicion.ayuda} alCerrar={alCerrar} bloqueado={cambiar.isPending}>
      <form onSubmit={confirmar} className="flex flex-col gap-4 p-4 sm:p-5" noValidate>
        <p className="text-sm text-tinta-suave">
          {incidente.codigo} · de <strong className="text-tinta">{etiquetaEstado(incidente.estado)}</strong> a{' '}
          <strong className="text-tinta">{etiquetaEstado(transicion.hacia)}</strong>
        </p>
        {error && <Aviso tono="error">{error}</Aviso>}
        {transicion.requiereEntidad && (
          <Selector
            etiqueta="Entidad que atenderá el incidente"
            required
            value={entidadId}
            onChange={(e) => setEntidadId(e.target.value)}
            error={entidades.isError ? interpretarError(entidades.error).mensaje : null}
          >
            <option value="">{entidades.isPending ? 'Cargando entidades…' : 'Elige una entidad'}</option>
            {entidades.data?.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nombre}
              </option>
            ))}
          </Selector>
        )}
        <AreaTexto
          etiqueta={transicion.etiquetaComentario}
          required={obligatorio}
          maxLength={2000}
          autoFocus={!transicion.requiereEntidad}
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
          ayuda={obligatorio ? 'Obligatorio. Queda en el historial del incidente.' : 'Queda en el historial del incidente.'}
        />
        <PieDialogo>
          <Boton onClick={alCerrar} disabled={cambiar.isPending}>
            Cancelar
          </Boton>
          <Boton
            type="submit"
            variante={transicion.tono === 'peligro' ? 'peligro' : 'principal'}
            cargando={cambiar.isPending}
            disabled={faltaComentario || faltaEntidad}
          >
            {transicion.etiqueta}
          </Boton>
        </PieDialogo>
      </form>
    </Dialogo>
  )
}

function DialogoDuplicado({ incidente, alCerrar }: { incidente: IncidentePanel; alCerrar: () => void }) {
  const { avisar } = useAvisos()
  const incidentes = useIncidentes()
  const marcar = useMarcarDuplicado()
  const [originalId, setOriginalId] = useState('')
  const [error, setError] = useState<string | null>(null)

  // El original no puede ser él mismo ni uno rechazado; los de su categoría salen primero.
  const candidatos = useMemo(
    () =>
      (incidentes.data ?? [])
        .filter((i) => i.id !== incidente.id && i.estado !== 'rechazado')
        .sort((a, b) => Number(b.categoria_id === incidente.categoria_id) - Number(a.categoria_id === incidente.categoria_id)),
    [incidentes.data, incidente.id, incidente.categoria_id],
  )

  function confirmar(evento: FormEvent) {
    evento.preventDefault()
    if (!originalId) return
    setError(null)
    marcar.mutate(
      { incidenteId: incidente.id, originalId, versionEsperada: incidente.version },
      {
        onSuccess: () => {
          avisar('Incidente marcado como duplicado', 'exito')
          alCerrar()
        },
        onError: (fallo) => {
          const { tipo, mensaje } = interpretarError(fallo)
          if (tipo === 'conflicto' || tipo === 'no_encontrado') {
            avisar(mensaje, 'error')
            alCerrar()
          } else {
            setError(mensaje)
          }
        },
      },
    )
  }

  return (
    <Dialogo
      titulo="Marcar como duplicado"
      descripcion="Los reportes y apoyos de este incidente pasan al original, y este queda rechazado. No se puede deshacer."
      alCerrar={alCerrar}
      bloqueado={marcar.isPending}
    >
      <form onSubmit={confirmar} className="flex flex-col gap-4 p-4 sm:p-5" noValidate>
        {error && <Aviso tono="error">{error}</Aviso>}
        <Selector etiqueta="Incidente original" required value={originalId} onChange={(e) => setOriginalId(e.target.value)}>
          <option value="">{incidentes.isPending ? 'Cargando…' : 'Elige el incidente original'}</option>
          {candidatos.map((c) => (
            <option key={c.id} value={c.id}>
              {c.codigo} · {c.titulo.slice(0, 50)} ({etiquetaEstado(c.estado)})
            </option>
          ))}
        </Selector>
        <PieDialogo>
          <Boton onClick={alCerrar} disabled={marcar.isPending}>
            Cancelar
          </Boton>
          <Boton type="submit" variante="peligro" cargando={marcar.isPending} disabled={!originalId}>
            Marcar como duplicado
          </Boton>
        </PieDialogo>
      </form>
    </Dialogo>
  )
}

// ---------------------------------------------------------------------------
// Fotos
// ---------------------------------------------------------------------------

function Fotos({ incidente }: { incidente: IncidentePanel }) {
  const usuario = useUsuario()
  const fotos = useFotos(incidente.id)
  const ocultar = useOcultarFoto(incidente.id)
  const { avisar } = useAvisos()
  const [ampliada, setAmpliada] = useState<FotoPanel | null>(null)
  const esModerador = usuario.rol === 'moderador'

  function alternar(foto: FotoPanel) {
    ocultar.mutate(
      { fotoId: foto.id, oculta: !foto.oculta },
      {
        onSuccess: () => avisar(foto.oculta ? 'La foto vuelve a mostrarse' : 'Foto oculta', 'exito'),
        onError: (fallo) => avisar(interpretarError(fallo).mensaje, 'error'),
      },
    )
  }

  return (
    <Tarjeta className="p-4" aria-labelledby="fotos">
      <h2 id="fotos" className="font-titulo text-titular font-semibold">
        Fotos
      </h2>
      {fotos.isPending ? (
        <Cargando texto="Cargando fotos…">
          <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
            <Esqueleto className="aspect-[4/3]" />
            <Esqueleto className="aspect-[4/3]" />
          </div>
        </Cargando>
      ) : fotos.isError ? (
        <ErrorVista error={fotos.error} alReintentar={() => void fotos.refetch()} compacto />
      ) : fotos.data.length === 0 ? (
        <EstadoVacio icono={ImageOff} titulo="Sin fotos" compacto>
          {esModerador ? 'Este incidente no tiene fotos.' : 'Este incidente no tiene fotos visibles.'}
        </EstadoVacio>
      ) : (
        <ul className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {fotos.data.map((foto, i) => (
            <li key={foto.id} className="flex flex-col gap-1.5">
              <button
                type="button"
                disabled={!foto.url}
                onClick={() => setAmpliada(foto)}
                aria-label={`Ampliar la foto ${i + 1}${foto.oculta ? ' (oculta al público)' : ''}`}
                className="relative aspect-[4/3] overflow-hidden rounded-campo border border-borde bg-arena"
              >
                {foto.url ? (
                  <img
                    src={foto.url}
                    alt={`Foto ${i + 1} del incidente`}
                    loading="lazy"
                    className={foto.oculta ? 'size-full object-cover opacity-40' : 'size-full object-cover'}
                  />
                ) : (
                  <span className="grid size-full place-items-center p-2 text-center text-sm text-tinta-suave">No se pudo cargar</span>
                )}
                {foto.oculta && (
                  <span className="absolute left-1.5 top-1.5 inline-flex items-center gap-1 rounded-campo bg-superficie px-2 py-1 text-xs font-bold text-tinta">
                    <EyeOff aria-hidden="true" className="size-3.5" />
                    Oculta
                  </span>
                )}
              </button>
              <p className="text-xs text-tinta-suave">
                {foto.tipo === 'original' ? 'Reporte original' : 'Apoyo'} · {formatearFecha(foto.creado_en)}
              </p>
              {esModerador && (
                <Boton
                  icono={foto.oculta ? Eye : EyeOff}
                  onClick={() => alternar(foto)}
                  disabled={ocultar.isPending}
                  className="text-xs"
                  aria-label={`${foto.oculta ? 'Mostrar' : 'Ocultar'} la foto ${i + 1}`}
                >
                  {foto.oculta ? 'Mostrar' : 'Ocultar'}
                </Boton>
              )}
            </li>
          ))}
        </ul>
      )}
      {ampliada?.url && (
        <Dialogo titulo="Foto del incidente" alCerrar={() => setAmpliada(null)}>
          <img src={ampliada.url} alt="Foto del incidente ampliada" className="max-h-[70dvh] w-full object-contain" />
        </Dialogo>
      )}
    </Tarjeta>
  )
}

// ---------------------------------------------------------------------------
// Historial y comentarios
// ---------------------------------------------------------------------------

function Historial({ incidenteId }: { incidenteId: string }) {
  const historial = useHistorial(incidenteId)
  return (
    <Tarjeta className="p-4" aria-labelledby="historial">
      <h2 id="historial" className="font-titulo text-titular font-semibold">
        Historial
      </h2>
      {historial.isPending ? (
        <Cargando texto="Cargando historial…">
          <Esqueleto className="mt-3 h-12 w-full" />
          <Esqueleto className="mt-2 h-12 w-full" />
        </Cargando>
      ) : historial.isError ? (
        <ErrorVista error={historial.error} alReintentar={() => void historial.refetch()} compacto />
      ) : historial.data.length === 0 ? (
        <EstadoVacio titulo="Sin movimientos" compacto />
      ) : (
        <ol className="mt-3 flex flex-col">
          {[...historial.data].reverse().map((entrada, i, lista) => (
            <li key={entrada.id} className="flex gap-3">
              <div className="flex flex-col items-center">
                <span className="mt-0.5 shrink-0">
                  <FormaEstado estado={entrada.estado_nuevo} tamano={20} />
                </span>
                {i < lista.length - 1 && <span aria-hidden="true" className="my-1 w-px flex-1 bg-borde-campo" />}
              </div>
              <div className="min-w-0 pb-4">
                <p className="text-sm font-bold text-tinta">
                  {etiquetaEstado(entrada.estado_nuevo)}
                  {entrada.estado_anterior && (
                    <span className="font-normal text-tinta-suave"> · desde {etiquetaEstado(entrada.estado_anterior).toLowerCase()}</span>
                  )}
                </p>
                {entrada.comentario && <p className="mt-0.5 whitespace-pre-wrap break-words text-sm text-tinta">{entrada.comentario}</p>}
                <p className="mt-0.5 text-xs text-tinta-suave">
                  {entrada.autor} · {formatearFechaHora(entrada.creado_en)}
                </p>
              </div>
            </li>
          ))}
        </ol>
      )}
    </Tarjeta>
  )
}

function Comentarios({ incidente }: { incidente: IncidentePanel }) {
  const usuario = useUsuario()
  const comentarios = useComentarios(incidente.id)
  const comentar = useComentar(incidente.id)
  const { avisar } = useAvisos()
  const [texto, setTexto] = useState('')
  const [visibilidad, setVisibilidad] = useState<Visibilidad>('publico')
  const [error, setError] = useState<string | null>(null)
  const puedeComentar = usuario.rol === 'entidad'

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!texto.trim()) return
    setError(null)
    comentar.mutate(
      { texto: texto.trim(), visibilidad },
      {
        onSuccess: () => {
          setTexto('')
          avisar(visibilidad === 'interno' ? 'Comentario interno guardado' : 'Comentario publicado', 'exito')
        },
        onError: (fallo) => setError(interpretarError(fallo).mensaje),
      },
    )
  }

  return (
    <Tarjeta className="p-4" aria-labelledby="comentarios">
      <h2 id="comentarios" className="font-titulo text-titular font-semibold">
        Comentarios
      </h2>
      {comentarios.isPending ? (
        <Cargando texto="Cargando comentarios…">
          <Esqueleto className="mt-3 h-14 w-full" />
        </Cargando>
      ) : comentarios.isError ? (
        <ErrorVista error={comentarios.error} alReintentar={() => void comentarios.refetch()} compacto />
      ) : comentarios.data.length === 0 ? (
        <EstadoVacio icono={MessageSquare} titulo="Aún no hay comentarios" compacto>
          {puedeComentar ? 'Cuéntales a los vecinos cómo avanza la atención.' : undefined}
        </EstadoVacio>
      ) : (
        <ul className="mt-3 flex flex-col gap-3">
          {comentarios.data.map((c) => (
            <li key={c.id} className="rounded-campo border border-borde p-3">
              <p className="whitespace-pre-wrap break-words text-sm text-tinta">{c.texto}</p>
              <p className="mt-1 flex flex-wrap items-center gap-x-2 text-xs text-tinta-suave">
                <span>
                  {c.autor} · {formatearFechaHora(c.creado_en)}
                </span>
                <span className="inline-flex items-center gap-1 font-bold">
                  {c.visibilidad === 'interno' ? <EyeOff aria-hidden="true" className="size-3.5" /> : <Eye aria-hidden="true" className="size-3.5" />}
                  {c.visibilidad === 'interno' ? 'Interno' : 'Público'}
                </span>
              </p>
            </li>
          ))}
        </ul>
      )}

      {puedeComentar ? (
        <form onSubmit={enviar} className="mt-4 flex flex-col gap-3 border-t border-borde pt-4" noValidate>
          {error && <Aviso tono="error">{error}</Aviso>}
          <AreaTexto etiqueta="Escribe un comentario" maxLength={2000} value={texto} onChange={(e) => setTexto(e.target.value)} ayuda={`${texto.length} de 2000 caracteres`} />
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
            <Selector
              etiqueta="Quién lo ve"
              value={visibilidad}
              onChange={(e) => setVisibilidad(e.target.value as Visibilidad)}
              className="sm:w-64"
            >
              <option value="publico">Público: lo ven los vecinos</option>
              <option value="interno">Interno: solo el personal</option>
            </Selector>
            <Boton type="submit" variante="principal" icono={Send} cargando={comentar.isPending} disabled={!texto.trim()}>
              Comentar
            </Boton>
          </div>
        </form>
      ) : (
        <p className="mt-4 border-t border-borde pt-3 text-sm text-tinta-suave">Solo la entidad asignada comenta los incidentes.</p>
      )}
    </Tarjeta>
  )
}
