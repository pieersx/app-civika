import { KeyRound } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useAvisos } from '../componentes/avisos'
import { Aviso, Boton, Campo } from '../componentes/ui'
import { mensajeDeError } from '../lib/errores'
import { supabase } from '../lib/supabase'
import { useSesion } from './contexto'
import { MarcoAcceso } from './MarcoAcceso'

/**
 * Último paso de «Olvidé mi contraseña». Llega después del segundo factor: Supabase
 * exige una sesión aal2 para cambiar la contraseña de una cuenta con MFA.
 */
export function CambiarContrasena() {
  const { terminarRecuperacion } = useSesion()
  const { avisar } = useAvisos()
  const [nueva, setNueva] = useState('')
  const [repetida, setRepetida] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const noCoinciden = repetida !== '' && repetida !== nueva

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (nueva.length < 8 || nueva !== repetida) return
    setError(null)
    setEnviando(true)
    const { error: fallo } = await supabase().auth.updateUser({ password: nueva })
    setEnviando(false)
    if (fallo) {
      setError(mensajeDeError(fallo))
      return
    }
    avisar('Contraseña cambiada', 'exito')
    window.history.replaceState(null, '', '/')
    terminarRecuperacion()
  }

  return (
    <MarcoAcceso titulo="Elige una contraseña nueva" descripcion="Escríbela dos veces. Usa al menos 8 caracteres.">
      <form onSubmit={enviar} className="flex flex-col gap-4" noValidate>
        {error && <Aviso tono="error">{error}</Aviso>}
        <Campo etiqueta="Contraseña nueva" type="password" autoComplete="new-password" minLength={8} required value={nueva} onChange={(e) => setNueva(e.target.value)} />
        <Campo
          etiqueta="Repite la contraseña"
          type="password"
          autoComplete="new-password"
          required
          error={noCoinciden ? 'Las contraseñas no coinciden.' : null}
          value={repetida}
          onChange={(e) => setRepetida(e.target.value)}
        />
        <Boton type="submit" variante="principal" grande ancho icono={KeyRound} cargando={enviando} disabled={nueva.length < 8 || nueva !== repetida}>
          Cambiar contraseña
        </Boton>
      </form>
    </MarcoAcceso>
  )
}
