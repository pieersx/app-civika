import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { BrowserRouter } from 'react-router'
import { ProveedorAvisos } from './componentes/ProveedorAvisos'
import { configuracion } from './lib/config'
import { interpretarError } from './lib/errores'
import { CambiarContrasena } from './sesion/CambiarContrasena'
import { useSesion } from './sesion/contexto'
import { InicioSesion } from './sesion/InicioSesion'
import { CargandoSesion, ErrorSesion, FaltaConfigurar, SinAcceso } from './sesion/Pantallas'
import { ProveedorSesion } from './sesion/ProveedorSesion'
import { AltaMfa, DesafioMfa } from './sesion/SegundoFactor'

const Panel = lazy(() => import('./Panel'))

const clienteConsultas = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: true,
      // Un error de permiso, validación o sesión no mejora reintentando.
      retry: (intentos, error) => interpretarError(error).tipo === 'red' && intentos < 2,
    },
  },
})

function Puerta() {
  const { estado, recuperacion } = useSesion()
  switch (estado.fase) {
    case 'cargando':
      return <CargandoSesion />
    case 'sin_sesion':
      return <InicioSesion />
    case 'sin_acceso':
      return <SinAcceso motivo={estado.motivo} />
    case 'error':
      return <ErrorSesion error={estado.error} />
    case 'mfa_alta':
      return <AltaMfa usuario={estado.usuario} />
    case 'mfa_desafio':
      return <DesafioMfa usuario={estado.usuario} factorId={estado.factorId} />
    case 'lista':
      if (recuperacion) return <CambiarContrasena />
      return (
        <BrowserRouter>
          <Suspense fallback={<CargandoSesion />}>
            <Panel />
          </Suspense>
        </BrowserRouter>
      )
  }
}

export function App() {
  if (!configuracion.ok) return <FaltaConfigurar configuracion={configuracion} />
  return (
    <QueryClientProvider client={clienteConsultas}>
      <ProveedorAvisos>
        <ProveedorSesion>
          <Puerta />
        </ProveedorSesion>
      </ProveedorAvisos>
    </QueryClientProvider>
  )
}
