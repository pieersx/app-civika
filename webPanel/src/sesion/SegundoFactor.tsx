// Segundo factor (TOTP), obligatorio para entidad y moderación: todas las RPC
// del panel exigen una sesión aal2 (docs/arquitectura.md §7.2).

import { LogOut, ShieldCheck } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Aviso, Boton, Campo, Cargando, Esqueleto } from '../componentes/ui'
import { mensajeDeError } from '../lib/errores'
import { supabase } from '../lib/supabase'
import { useSesion, type UsuarioPanel } from './contexto'
import { MarcoAcceso } from './MarcoAcceso'

function soloDigitos(valor: string): string {
  return valor.replace(/\D/g, '').slice(0, 6)
}

interface FormularioCodigoProps {
  factorId: string
  etiquetaBoton: string
}

/** Pide el código de 6 dígitos y lo verifica. Si es correcto, la sesión pasa a aal2 y el proveedor cambia de pantalla. */
function FormularioCodigo({ factorId, etiquetaBoton }: FormularioCodigoProps) {
  const { cerrarSesion } = useSesion()
  const [codigo, setCodigo] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    const { error: fallo } = await supabase().auth.mfa.challengeAndVerify({ factorId, code: codigo })
    if (fallo) {
      setError(mensajeDeError(fallo))
      setCodigo('')
      setEnviando(false)
    }
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4" noValidate>
      <Campo
        etiqueta="Código de 6 dígitos"
        ayuda="Cambia cada 30 segundos."
        error={error}
        name="codigo"
        inputMode="numeric"
        autoComplete="one-time-code"
        pattern="[0-9]{6}"
        maxLength={6}
        autoFocus
        required
        value={codigo}
        onChange={(e) => setCodigo(soloDigitos(e.target.value))}
      />
      <Boton type="submit" variante="principal" grande ancho icono={ShieldCheck} cargando={enviando} disabled={codigo.length !== 6}>
        {etiquetaBoton}
      </Boton>
      <Boton variante="texto" icono={LogOut} onClick={() => void cerrarSesion()}>
        Cerrar sesión
      </Boton>
    </form>
  )
}

export function DesafioMfa({ usuario, factorId }: { usuario: UsuarioPanel; factorId: string }) {
  return (
    <MarcoAcceso
      titulo="Escribe tu código"
      descripcion={`Hola, ${usuario.nombre}. Abre tu app de autenticación y escribe el código de Cívika.`}
    >
      <FormularioCodigo factorId={factorId} etiquetaBoton="Verificar código" />
    </MarcoAcceso>
  )
}

interface Alta {
  factorId: string
  qr: string
  secreto: string
}

/** Crea un factor TOTP nuevo. Antes borra los intentos sin terminar, que bloquearían el alta. */
async function crearFactor(): Promise<Alta> {
  const auth = supabase().auth
  const { data: factores, error: errorLista } = await auth.mfa.listFactors()
  if (errorLista) throw errorLista
  for (const factor of factores.all) {
    if (factor.factor_type === 'totp' && factor.status === 'unverified') {
      await auth.mfa.unenroll({ factorId: factor.id })
    }
  }

  const { data, error } = await auth.mfa.enroll({
    factorType: 'totp',
    issuer: 'Cívika',
    friendlyName: `Panel ${new Date().toISOString()}`,
  })
  if (error) throw error

  // El servidor devuelve el SVG del QR; se vuelve a codificar para usarlo sin riesgos en <img>.
  const svg = data.totp.qr_code.replace(/^data:image\/svg\+xml;utf-8,/, '')
  return { factorId: data.id, qr: `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`, secreto: data.totp.secret }
}

export function AltaMfa({ usuario }: { usuario: UsuarioPanel }) {
  const { cerrarSesion } = useSesion()
  const [alta, setAlta] = useState<Alta | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [intento, setIntento] = useState(0)
  const iniciado = useRef(-1)

  useEffect(() => {
    // En desarrollo React monta dos veces: el alta solo debe pedirse una.
    if (iniciado.current === intento) return
    iniciado.current = intento
    crearFactor().then(setAlta, (fallo: unknown) => setError(mensajeDeError(fallo)))
  }, [intento])

  return (
    <MarcoAcceso
      titulo="Activa el segundo factor"
      descripcion={`Hola, ${usuario.nombre}. Para proteger los datos de los vecinos, el panel pide un código además de tu contraseña.`}
    >
      {error ? (
        <div className="flex flex-col gap-4">
          <Aviso tono="error">{error}</Aviso>
          <Boton
            onClick={() => {
              setError(null)
              setIntento((n) => n + 1)
            }}
          >
            Reintentar
          </Boton>
          <Boton variante="texto" icono={LogOut} onClick={() => void cerrarSesion()}>
            Cerrar sesión
          </Boton>
        </div>
      ) : !alta ? (
        <Cargando texto="Preparando el código QR…">
          <Esqueleto className="mx-auto size-48" />
          <Esqueleto className="mt-4 h-12 w-full" />
        </Cargando>
      ) : (
        <div className="flex flex-col gap-5">
          <ol className="flex list-decimal flex-col gap-3 pl-5 text-base text-tinta marker:font-bold">
            <li>Instala una app de autenticación en tu celular, como Google Authenticator, Microsoft Authenticator o Aegis.</li>
            <li>
              Escanea este código QR con la app.
              {/* El QR va sobre blanco en los dos temas: así lo lee cualquier cámara. */}
              <img
                src={alta.qr}
                alt="Código QR para añadir Cívika a tu app de autenticación"
                width={192}
                height={192}
                className="mx-auto mt-3 size-48 rounded-campo border border-borde bg-[#fff] p-2"
              />
              <details className="mt-3 text-sm text-tinta-suave">
                <summary className="inline-flex min-h-11 items-center font-bold text-terracota-texto">No puedo escanear el código</summary>
                <p className="mt-1">Escribe esta clave en la app, en «Introducir clave de configuración»:</p>
                <code className="mt-2 block select-all break-all rounded-campo border border-borde bg-arena p-2 font-mono text-sm text-tinta">
                  {alta.secreto}
                </code>
              </details>
            </li>
            <li>Escribe el código de 6 dígitos que muestra la app.</li>
          </ol>
          <FormularioCodigo factorId={alta.factorId} etiquetaBoton="Activar y entrar" />
        </div>
      )}
    </MarcoAcceso>
  )
}
