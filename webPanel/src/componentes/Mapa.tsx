// Mapa con MapLibre GL JS y teselas de OpenFreeMap. Los marcadores usan la forma
// de cada estado (triángulo, rombo, círculo, cuadrado) y son botones con nombre.

import { MapPinOff } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { familiaDeEstado, etiquetaEstado, type Familia } from '../lib/estados'
import type { Estado } from '../lib/tipos'
import { EstadoVacio } from './ui'

export const ESTILO_MAPA = 'https://tiles.openfreemap.org/styles/liberty'

export interface PuntoMapa {
  id: string
  lat: number
  lon: number
  estado: Estado
  etiqueta: string
}

// Valores fijos de direccion-visual.md; el marcador vive fuera de React y no lee variables CSS.
const RELLENO: Record<Familia, string> = { abierto: '#D64545', en_atencion: '#E0A100', resuelto: '#2E8B57', rechazado: '#6B6560' }
const ICONO: Record<Familia, string> = { abierto: '#FFFFFF', en_atencion: '#1F1B16', resuelto: '#FFFFFF', rechazado: '#FFFFFF' }
const FORMA: Record<Familia, string> = {
  abierto: '<path d="M12 2.5 22.5 21h-21z"/>',
  en_atencion: '<path d="M12 1.5 22.5 12 12 22.5 1.5 12z"/>',
  resuelto: '<circle cx="12" cy="12" r="10"/>',
  rechazado: '<rect x="2.5" y="2.5" width="19" height="19" rx="2.5"/>',
}
const GLIFO: Record<Familia, string> = {
  abierto: '<path d="M12 9.5v5M12 17.6v.1"/>',
  en_atencion: '<path d="M7.6 12h.1M12 12h.1M16.4 12h.1"/>',
  resuelto: '<path d="m7.5 12.3 3 3 6-6.3"/>',
  rechazado: '<path d="m8.2 8.2 7.6 7.6m0-7.6-7.6 7.6"/>',
}

function crearMarcador(punto: PuntoMapa, tamano: number, alElegir?: (id: string) => void): HTMLElement {
  const familia = familiaDeEstado(punto.estado)
  const boton = document.createElement('button')
  boton.type = 'button'
  boton.className = 'marcador-incidente'
  boton.setAttribute('aria-label', `${punto.etiqueta}. ${etiquetaEstado(punto.estado)}`)
  boton.title = `${punto.etiqueta} · ${etiquetaEstado(punto.estado)}`
  // Solo constantes del código: ningún dato del servidor entra en el SVG.
  boton.innerHTML =
    `<svg width="${tamano}" height="${tamano}" viewBox="0 0 24 24" aria-hidden="true">` +
    `<g fill="${RELLENO[familia]}" stroke="#1F1B16" stroke-width="1.5" stroke-linejoin="round">${FORMA[familia]}</g>` +
    `<g fill="none" stroke="${ICONO[familia]}" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">${GLIFO[familia]}</g></svg>`
  if (alElegir) boton.addEventListener('click', () => alElegir(punto.id))
  return boton
}

interface MapaProps {
  puntos: PuntoMapa[]
  /** Nombre del mapa para lectores de pantalla. */
  etiqueta: string
  alElegir?: (id: string) => void
  zoom?: number
  tamanoMarcador?: number
  className?: string
}

const CENTRO_PILOTO: [number, number] = [-77.0845, -12.0575] // Ciudad Universitaria, UNMSM

export function Mapa({ puntos, etiqueta, alElegir, zoom = 16, tamanoMarcador = 30, className }: MapaProps) {
  const contenedor = useRef<HTMLDivElement>(null)
  const alElegirRef = useRef(alElegir)
  const [fallo, setFallo] = useState(false)

  useEffect(() => {
    alElegirRef.current = alElegir
  }, [alElegir])

  useEffect(() => {
    const elemento = contenedor.current
    if (!elemento) return
    let cancelado = false
    let limpiar = () => {}

    void Promise.all([import('maplibre-gl'), import('maplibre-gl/dist/maplibre-gl-worker.mjs?url')])
      .then(([{ Map, Marker, NavigationControl, LngLatBounds, setWorkerUrl }, { default: urlDelWorker }]) => {
        if (cancelado) return
        // Con Vite 8 el worker de MapLibre 6 se declara como asset; sin esto no se copia a dist/.
        setWorkerUrl(urlDelWorker)
        const primero = puntos[0]
        const mapa = new Map({
          container: elemento,
          style: ESTILO_MAPA,
          center: primero ? [primero.lon, primero.lat] : CENTRO_PILOTO,
          zoom,
          attributionControl: { compact: true },
          cooperativeGestures: true,
        })
        mapa.addControl(new NavigationControl({ showCompass: false }), 'top-right')
        mapa.on('error', (evento) => {
          // Un error de teselas suelto no tumba el mapa; solo si no hay estilo es un fallo real.
          if (!mapa.isStyleLoaded() && /style|fetch|Failed/i.test(String(evento.error?.message))) setFallo(true)
        })

        const marcadores = puntos.map((punto) =>
          new Marker({ element: crearMarcador(punto, tamanoMarcador, (id) => alElegirRef.current?.(id)), anchor: 'center' })
            .setLngLat([punto.lon, punto.lat])
            .addTo(mapa),
        )

        if (puntos.length > 1) {
          const limites = new LngLatBounds()
          for (const p of puntos) limites.extend([p.lon, p.lat])
          mapa.fitBounds(limites, { padding: 64, maxZoom: 17, duration: 0 })
        }
        limpiar = () => {
          marcadores.forEach((m) => m.remove())
          mapa.remove()
        }
      })
      .catch(() => {
        if (!cancelado) setFallo(true)
      })

    return () => {
      cancelado = true
      limpiar()
    }
  }, [puntos, zoom, tamanoMarcador])

  if (fallo) {
    return (
      <div className={className}>
        <EstadoVacio icono={MapPinOff} titulo="No pudimos cargar el mapa" compacto>
          Revisa tu conexión o que tu navegador permita WebGL. La ubicación también aparece como texto.
        </EstadoVacio>
      </div>
    )
  }
  return <div ref={contenedor} role="region" aria-label={etiqueta} className={className} />
}
