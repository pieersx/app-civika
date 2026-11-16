import { ChartColumn, LogOut, Map as IconoMapa, ShieldAlert, TriangleAlert, type LucideIcon } from 'lucide-react'
import { useEffect } from 'react'
import { Navigate, NavLink, Route, Routes, useLocation } from 'react-router'
import { useAvisos } from './componentes/avisos'
import { ConmutadorTema } from './componentes/ConmutadorTema'
import { Marca } from './componentes/Marca'
import { BotonIcono, EstadoVacio } from './componentes/ui'
import { cx } from './lib/cx'
import { mensajeDeError } from './lib/errores'
import { useSesion, useUsuario } from './sesion/contexto'
import { DetalleIncidente } from './vistas/DetalleIncidente'
import { Indicadores } from './vistas/Indicadores'
import { ListaIncidentes } from './vistas/ListaIncidentes'
import { MapaGeneral } from './vistas/MapaGeneral'
import { Moderacion } from './vistas/Moderacion'

interface Seccion {
  ruta: string
  etiqueta: string
  icono: LucideIcon
  soloModeracion?: boolean
}

const SECCIONES: Seccion[] = [
  { ruta: '/incidentes', etiqueta: 'Incidentes', icono: TriangleAlert },
  { ruta: '/mapa', etiqueta: 'Mapa', icono: IconoMapa },
  { ruta: '/moderacion', etiqueta: 'Moderación', icono: ShieldAlert, soloModeracion: true },
  { ruta: '/indicadores', etiqueta: 'Indicadores', icono: ChartColumn },
]

function claseEnlace(activo: boolean, movil: boolean): string {
  return cx(
    'flex items-center font-bold',
    movil ? 'min-h-14 flex-1 flex-col justify-center gap-0.5 text-xs' : 'min-h-11 gap-3 rounded-campo px-3 text-sm',
    activo ? 'tinte-verde text-verde' : 'text-tinta-suave hover-tinta hover:text-tinta',
  )
}

function Navegacion({ secciones, movil }: { secciones: Seccion[]; movil: boolean }) {
  return (
    <nav aria-label={movil ? 'Secciones (móvil)' : 'Secciones'} className={movil ? 'flex' : 'flex flex-col gap-1'}>
      {secciones.map(({ ruta, etiqueta, icono: Icono }) => (
        <NavLink key={ruta} to={ruta} className={({ isActive }) => claseEnlace(isActive, movil)}>
          <Icono aria-hidden="true" className="size-5 shrink-0" />
          {etiqueta}
        </NavLink>
      ))}
    </nav>
  )
}

export default function Panel() {
  const usuario = useUsuario()
  const { cerrarSesion } = useSesion()
  const { avisar } = useAvisos()
  const { pathname } = useLocation()
  const secciones = SECCIONES.filter((s) => !s.soloModeracion || usuario.rol === 'moderador')

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  async function salir() {
    try {
      await cerrarSesion()
    } catch (error) {
      avisar(mensajeDeError(error), 'error')
    }
  }

  const rolTexto = usuario.rol === 'moderador' ? 'Moderación' : 'Personal de entidad'

  return (
    <div className="min-h-dvh md:grid md:grid-cols-[16rem_1fr]">
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:fixed focus:left-2 focus:top-2 focus:z-[200] focus:rounded-campo focus:bg-superficie focus:px-4 focus:py-3 focus:font-bold focus:text-tinta"
      >
        Saltar al contenido
      </a>

      <aside className="hidden border-r border-borde bg-superficie md:sticky md:top-0 md:flex md:h-dvh md:flex-col md:gap-6 md:p-4">
        <Marca />
        <Navegacion secciones={secciones} movil={false} />
        <div className="mt-auto flex flex-col gap-3">
          <ConmutadorTema />
          <div className="rounded-campo border border-borde p-3">
            <p className="truncate text-sm font-bold text-tinta">{usuario.nombre}</p>
            <p className="truncate text-sm text-tinta-suave">{rolTexto}</p>
            <p className="truncate text-xs text-tinta-suave">{usuario.correo}</p>
          </div>
          <button
            type="button"
            onClick={() => void salir()}
            className="flex min-h-11 items-center gap-3 rounded-campo px-3 text-sm font-bold text-tinta-suave hover-tinta hover:text-tinta"
          >
            <LogOut aria-hidden="true" className="size-5" />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-col pb-14 md:pb-0">
        <header className="sticky top-0 z-30 flex items-center justify-between gap-2 border-b border-borde bg-superficie py-1 pl-4 pr-1 md:hidden">
          <Marca compacta />
          <div className="flex items-center">
            <span className="mr-1 max-w-[9rem] truncate text-sm text-tinta-suave">{usuario.nombre}</span>
            <BotonIcono icono={LogOut} etiqueta="Cerrar sesión" onClick={() => void salir()} />
          </div>
        </header>

        <main id="contenido" tabIndex={-1} className="min-w-0 flex-1 px-4 py-5 outline-none md:px-8 md:py-8">
          <Routes>
            <Route path="/" element={<Navigate to="/incidentes" replace />} />
            <Route path="/incidentes" element={<ListaIncidentes />} />
            <Route path="/incidentes/:id" element={<DetalleIncidente />} />
            <Route path="/mapa" element={<MapaGeneral />} />
            <Route path="/moderacion" element={usuario.rol === 'moderador' ? <Moderacion /> : <Navigate to="/incidentes" replace />} />
            <Route path="/indicadores" element={<Indicadores />} />
            <Route path="/restablecer" element={<Navigate to="/incidentes" replace />} />
            <Route path="*" element={<EstadoVacio titulo="Esta página no existe">Usa el menú para volver a una sección del panel.</EstadoVacio>} />
          </Routes>
        </main>

        <div className="fixed inset-x-0 bottom-0 z-30 border-t border-borde bg-superficie md:hidden">
          <Navegacion secciones={secciones} movil />
        </div>
      </div>
    </div>
  )
}
