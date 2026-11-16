// Pantallas de un solo mensaje: falta configurar, sin acceso, error y carga.

import { LogIn, LogOut, RefreshCw } from 'lucide-react'
import { Aviso, Boton, Cargando, Esqueleto } from '../componentes/ui'
import type { Configuracion } from '../lib/config'
import { mensajeDeError } from '../lib/errores'
import { useSesion, type MotivoSinAcceso } from './contexto'
import { MarcoAcceso } from './MarcoAcceso'

export function FaltaConfigurar({ configuracion }: { configuracion: Extract<Configuracion, { ok: false }> }) {
  return (
    <MarcoAcceso titulo="Falta configurar" descripcion="El panel todavía no sabe a qué proyecto de Supabase conectarse.">
      <div className="flex flex-col gap-4 text-base text-tinta">
        {configuracion.motivo && <Aviso tono="error">{configuracion.motivo}</Aviso>}
        {configuracion.faltan.length > 0 && (
          <div>
            <p>Faltan estas variables de entorno:</p>
            <ul className="mt-2 flex flex-col gap-1">
              {configuracion.faltan.map((variable) => (
                <li key={variable}>
                  <code className="rounded-campo border border-borde bg-arena px-2 py-1 font-mono text-sm">{variable}</code>
                </li>
              ))}
            </ul>
          </div>
        )}
        <p className="text-sm text-tinta-suave">
          En tu computadora: copia <code className="font-mono">.env.example</code> como <code className="font-mono">.env.local</code>,
          complétalo y reinicia <code className="font-mono">pnpm dev</code>. En Vercel: añádelas en Settings → Environment Variables
          y vuelve a desplegar. Usa solo la clave anon, nunca la service_role.
        </p>
      </div>
    </MarcoAcceso>
  )
}

const MOTIVOS: Record<MotivoSinAcceso, string> = {
  ciudadano: 'El panel es solo para el personal de las entidades y el equipo de moderación. Para reportar un problema, usa la app de Cívika en tu celular.',
  inactiva: 'Tu cuenta está desactivada. Si crees que es un error, escribe a quien administra Cívika en tu entidad.',
  sin_perfil: 'No encontramos el perfil de esta cuenta. Escribe a quien administra Cívika para que lo revise.',
}

export function SinAcceso({ motivo }: { motivo: MotivoSinAcceso }) {
  const { volverAlInicio } = useSesion()
  return (
    <MarcoAcceso titulo="Esta cuenta no tiene acceso al panel">
      <div className="flex flex-col gap-4">
        <p className="text-base text-tinta">{MOTIVOS[motivo]}</p>
        <p className="text-sm text-tinta-suave">Cerramos la sesión de esta cuenta en este navegador.</p>
        <Boton variante="principal" grande ancho icono={LogIn} onClick={volverAlInicio}>
          Entrar con otra cuenta
        </Boton>
      </div>
    </MarcoAcceso>
  )
}

export function ErrorSesion({ error }: { error: unknown }) {
  const { reintentar, cerrarSesion } = useSesion()
  return (
    <MarcoAcceso titulo="No pudimos comprobar tu cuenta">
      <div className="flex flex-col gap-4">
        <Aviso tono="error">{mensajeDeError(error)}</Aviso>
        <Boton variante="principal" grande ancho icono={RefreshCw} onClick={reintentar}>
          Reintentar
        </Boton>
        <Boton variante="texto" icono={LogOut} onClick={() => void cerrarSesion()}>
          Cerrar sesión
        </Boton>
      </div>
    </MarcoAcceso>
  )
}

export function CargandoSesion() {
  return (
    <div className="grid min-h-dvh place-items-center bg-arena px-4">
      <Cargando texto="Comprobando tu sesión…">
        <div className="w-[min(24rem,calc(100vw-2rem))]">
          <Esqueleto className="h-9 w-32" />
          <Esqueleto className="mt-6 h-56 w-full rounded-tarjeta" />
        </div>
      </Cargando>
    </div>
  )
}
