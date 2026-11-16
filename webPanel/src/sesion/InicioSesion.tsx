import { ArrowLeft, Eye, EyeOff, LogIn, Send } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Aviso, Boton, BotonIcono, Campo } from '../componentes/ui'
import { mensajeDeError } from '../lib/errores'
import { supabase } from '../lib/supabase'
import { MarcoAcceso } from './MarcoAcceso'

/** `true` si la persona llegó con un enlace de recuperación que ya no sirve. */
function enlaceCaducado(): boolean {
  return window.location.pathname === '/restablecer' || /error_code=otp_expired|error=access_denied/.test(window.location.hash)
}

export function InicioSesion() {
  const [modo, setModo] = useState<'entrar' | 'recuperar'>('entrar')
  const [correo, setCorreo] = useState('')
  return modo === 'entrar' ? (
    <Entrar correo={correo} setCorreo={setCorreo} alRecuperar={() => setModo('recuperar')} />
  ) : (
    <Recuperar correo={correo} setCorreo={setCorreo} alVolver={() => setModo('entrar')} />
  )
}

interface PropsCorreo {
  correo: string
  setCorreo: (correo: string) => void
}

function Entrar({ correo, setCorreo, alRecuperar }: PropsCorreo & { alRecuperar: () => void }) {
  const [contrasena, setContrasena] = useState('')
  const [visible, setVisible] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [caducado] = useState(enlaceCaducado)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    const { error: fallo } = await supabase().auth.signInWithPassword({ email: correo.trim(), password: contrasena })
    // Si sale bien, el proveedor de sesión cambia de pantalla; aquí solo se atiende el fallo.
    if (fallo) {
      setError(mensajeDeError(fallo))
      setEnviando(false)
    }
  }

  return (
    <MarcoAcceso titulo="Inicia sesión" descripcion="Entra con la cuenta que te dio tu entidad o el equipo de moderación.">
      <form onSubmit={enviar} className="flex flex-col gap-4" noValidate>
        {caducado && !error && <Aviso tono="atencion">El enlace para cambiar la contraseña caducó o ya se usó. Pide uno nuevo.</Aviso>}
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo
          etiqueta="Correo"
          type="email"
          name="email"
          autoComplete="username"
          inputMode="email"
          autoCapitalize="none"
          spellCheck={false}
          required
          value={correo}
          onChange={(e) => setCorreo(e.target.value)}
        />
        <div className="relative">
          <Campo
            etiqueta="Contraseña"
            type={visible ? 'text' : 'password'}
            name="password"
            autoComplete="current-password"
            required
            value={contrasena}
            onChange={(e) => setContrasena(e.target.value)}
          />
          <BotonIcono
            icono={visible ? EyeOff : Eye}
            etiqueta={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={visible}
            onClick={() => setVisible((v) => !v)}
            className="absolute bottom-0.5 right-0.5"
          />
        </div>
        <Boton type="submit" variante="principal" grande ancho icono={LogIn} cargando={enviando} disabled={!correo.trim() || !contrasena}>
          Iniciar sesión
        </Boton>
        <Boton variante="texto" onClick={alRecuperar}>
          Olvidé mi contraseña
        </Boton>
      </form>
    </MarcoAcceso>
  )
}

function Recuperar({ correo, setCorreo, alVolver }: PropsCorreo & { alVolver: () => void }) {
  const [enviando, setEnviando] = useState(false)
  const [enviado, setEnviado] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    setEnviando(true)
    const { error: fallo } = await supabase().auth.resetPasswordForEmail(correo.trim(), {
      redirectTo: `${window.location.origin}/restablecer`,
    })
    setEnviando(false)
    if (fallo) setError(mensajeDeError(fallo))
    else setEnviado(true)
  }

  return (
    <MarcoAcceso titulo="Recupera tu contraseña" descripcion="Te enviamos un enlace para elegir una contraseña nueva.">
      {enviado ? (
        <div className="flex flex-col gap-4">
          {/* No se confirma si el correo existe: así nadie averigua quién tiene cuenta. */}
          <Aviso>
            Si <strong>{correo.trim()}</strong> tiene una cuenta, en unos minutos llegará un correo con el enlace. Revisa también la
            carpeta de correo no deseado.
          </Aviso>
          <Boton icono={ArrowLeft} onClick={alVolver}>
            Volver a iniciar sesión
          </Boton>
        </div>
      ) : (
        <form onSubmit={enviar} className="flex flex-col gap-4" noValidate>
          {error && <Aviso tono="error">{error}</Aviso>}
          <Campo
            etiqueta="Correo"
            type="email"
            name="email"
            autoComplete="username"
            inputMode="email"
            autoCapitalize="none"
            spellCheck={false}
            required
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
          />
          <Boton type="submit" variante="principal" grande ancho icono={Send} cargando={enviando} disabled={!correo.trim()}>
            Enviar enlace
          </Boton>
          <Boton variante="texto" icono={ArrowLeft} onClick={alVolver}>
            Volver a iniciar sesión
          </Boton>
        </form>
      )}
    </MarcoAcceso>
  )
}
