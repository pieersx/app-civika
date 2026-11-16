import 'maplibre-gl/dist/maplibre-gl.css'
import { MapPin } from 'lucide-react'
import { useMemo } from 'react'
import { Link, useNavigate } from 'react-router'
import { FormaEstado } from '../componentes/Estado'
import { Mapa, type PuntoMapa } from '../componentes/Mapa'
import { Cargando, ErrorVista, EstadoVacio, Esqueleto } from '../componentes/ui'
import { useIncidentes } from '../lib/consultas'
import { etiquetaEstado } from '../lib/estados'
import type { Estado } from '../lib/tipos'
import { Encabezado } from './Encabezado'

const LEYENDA: Estado[] = ['derivado', 'en_atencion', 'resuelto', 'rechazado']

export function MapaGeneral() {
  const incidentes = useIncidentes()
  const navegar = useNavigate()

  const puntos = useMemo<PuntoMapa[]>(
    () =>
      (incidentes.data ?? []).map((i) => ({
        id: i.id,
        lat: i.lat,
        lon: i.lon,
        estado: i.estado,
        etiqueta: `${i.codigo}, ${i.titulo}`,
      })),
    [incidentes.data],
  )

  return (
    <>
      <Encabezado titulo="Mapa" descripcion="Cada marcador es un incidente. Elige uno para abrir su detalle." />
      {incidentes.isPending ? (
        <Cargando texto="Cargando el mapa…">
          <Esqueleto className="h-[60dvh] w-full rounded-tarjeta" />
        </Cargando>
      ) : incidentes.isError ? (
        <ErrorVista error={incidentes.error} alReintentar={() => void incidentes.refetch()} />
      ) : puntos.length === 0 ? (
        <EstadoVacio icono={MapPin} titulo="Aún no hay incidentes para ubicar">
          Cuando haya incidentes, aparecerán aquí con la forma de su estado.
        </EstadoVacio>
      ) : (
        <>
          <Mapa
            puntos={puntos}
            etiqueta="Mapa de incidentes"
            alElegir={(id) => void navegar(`/incidentes/${id}`)}
            zoom={15}
            className="h-[calc(100dvh-20rem)] min-h-80 w-full overflow-hidden rounded-tarjeta border border-borde md:h-[calc(100dvh-14rem)]"
          />
          <ul aria-label="Leyenda" className="mt-3 flex flex-wrap gap-x-5 gap-y-1">
            {LEYENDA.map((estado) => (
              <li key={estado} className="inline-flex min-h-8 items-center gap-1.5 text-sm text-tinta">
                <FormaEstado estado={estado} tamano={18} />
                {estado === 'derivado' ? 'Abierto (reportado, en revisión o derivado)' : etiquetaEstado(estado)}
              </li>
            ))}
          </ul>
          <p className="mt-2 text-sm text-tinta-suave">
            ¿Prefieres una lista?{' '}
            <Link to="/incidentes" className="font-bold text-terracota-texto underline underline-offset-2">
              Ver los incidentes
            </Link>
            .
          </p>
        </>
      )}
    </>
  )
}
