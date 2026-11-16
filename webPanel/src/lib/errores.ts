// Traduce los errores de Supabase (PostgREST, Auth y red) a mensajes para personas.
// Los errores dicen qué pasó y qué hacer, sin disculpas (docs/diseno/direccion-visual.md).
//
// Códigos de las RPC del panel:
//   42501 permiso · 22023 validación · 40001 la versión cambió · P0002 no encontrado.

export type TipoError =
  | 'permiso'
  | 'mfa'
  | 'validacion'
  | 'conflicto'
  | 'no_encontrado'
  | 'sesion'
  | 'limite'
  | 'red'
  | 'desconocido'

export interface ErrorPanel {
  tipo: TipoError
  mensaje: string
}

interface ErrorCrudo {
  code?: unknown
  message?: unknown
  name?: unknown
  status?: unknown
}

const GENERICO = 'Algo falló al hablar con el servidor. Inténtalo de nuevo en un momento.'
const SIN_PERMISO = 'No tienes permiso para hacer esto.'

const AUTH: Record<string, ErrorPanel> = {
  invalid_credentials: { tipo: 'validacion', mensaje: 'El correo o la contraseña no coinciden.' },
  email_not_confirmed: {
    tipo: 'validacion',
    mensaje: 'Todavía no confirmas tu correo. Abre el enlace que te enviamos y vuelve a entrar.',
  },
  user_banned: { tipo: 'permiso', mensaje: 'Esta cuenta está suspendida. Escribe a quien administra Cívika.' },
  mfa_verification_failed: {
    tipo: 'validacion',
    mensaje: 'El código no es correcto o ya caducó. Escribe el que muestra ahora tu app.',
  },
  mfa_challenge_expired: { tipo: 'validacion', mensaje: 'El intento caducó. Escribe el código de nuevo.' },
  insufficient_aal: {
    tipo: 'mfa',
    mensaje: 'Esta acción necesita el segundo factor. Cierra sesión y vuelve a entrar con tu código.',
  },
  weak_password: {
    tipo: 'validacion',
    mensaje: 'Elige una contraseña más larga y difícil de adivinar.',
  },
  same_password: { tipo: 'validacion', mensaje: 'La contraseña nueva tiene que ser distinta de la anterior.' },
  over_request_rate_limit: { tipo: 'limite', mensaje: 'Demasiados intentos seguidos. Espera un minuto y vuelve a probar.' },
  over_email_send_rate_limit: {
    tipo: 'limite',
    mensaje: 'Ya enviamos varios correos seguidos. Espera unos minutos antes de pedir otro.',
  },
  session_not_found: { tipo: 'sesion', mensaje: 'Tu sesión caducó. Vuelve a iniciar sesión.' },
  session_expired: { tipo: 'sesion', mensaje: 'Tu sesión caducó. Vuelve a iniciar sesión.' },
  refresh_token_not_found: { tipo: 'sesion', mensaje: 'Tu sesión caducó. Vuelve a iniciar sesión.' },
  otp_expired: { tipo: 'validacion', mensaje: 'El enlace caducó o ya se usó. Pide uno nuevo.' },
}

function texto(valor: unknown): string {
  return typeof valor === 'string' ? valor : ''
}

/** Los mensajes de nuestras RPC están en español; los de Postgres o PostgREST, en inglés. */
function esMensajeDelPanel(mensaje: string): boolean {
  return /[áéíóúñ¿¡]/i.test(mensaje) || /^(El|La|Los|Las|Solo|Se|Este|Esta|No|Elige|Faltan|Incidente|Denuncia|Foto|Entidad|Derivado|Transición)\b/.test(mensaje)
}

function conPunto(mensaje: string): string {
  const limpio = mensaje.trim()
  return /[.!?»]$/.test(limpio) ? limpio : `${limpio}.`
}

export function interpretarError(error: unknown): ErrorPanel {
  if (error === null || typeof error !== 'object') return { tipo: 'desconocido', mensaje: GENERICO }

  const { code, message, name, status } = error as ErrorCrudo
  const codigo = texto(code)
  const mensaje = texto(message)
  const nombre = texto(name)

  if (/failed to fetch|networkerror|load failed|network request failed/i.test(mensaje) || nombre === 'AuthRetryableFetchError') {
    return { tipo: 'red', mensaje: 'No hay conexión con el servidor. Revisa tu internet y vuelve a intentarlo.' }
  }

  switch (codigo) {
    case '40001':
      return {
        tipo: 'conflicto',
        mensaje: 'El incidente cambió mientras lo veías. Ya cargamos la versión nueva: revísala y vuelve a intentarlo.',
      }
    case '42501':
      if (/MFA|segundo factor/i.test(mensaje)) {
        return {
          tipo: 'mfa',
          mensaje: 'Tu sesión ya no tiene el segundo factor. Cierra sesión y vuelve a entrar con tu código.',
        }
      }
      return {
        tipo: 'permiso',
        mensaje: esMensajeDelPanel(mensaje) ? `${SIN_PERMISO} ${conPunto(mensaje)}` : SIN_PERMISO,
      }
    case '22023':
      return { tipo: 'validacion', mensaje: esMensajeDelPanel(mensaje) ? conPunto(mensaje) : 'Revisa los datos e inténtalo de nuevo.' }
    case 'P0002':
      return {
        tipo: 'no_encontrado',
        mensaje: `${esMensajeDelPanel(mensaje) ? conPunto(mensaje) : 'No lo encontramos.'} Puede que ya no esté en tu lista: recarga para ver los datos al día.`,
      }
    case 'PGRST301':
    case 'PGRST302':
    case 'PGRST303':
      return { tipo: 'sesion', mensaje: 'Tu sesión caducó. Vuelve a iniciar sesión.' }
  }

  const deAuth = AUTH[codigo]
  if (deAuth) return deAuth
  if (nombre === 'AuthSessionMissingError') return { tipo: 'sesion', mensaje: 'Tu sesión caducó. Vuelve a iniciar sesión.' }
  if (status === 429) return { tipo: 'limite', mensaje: 'Demasiados intentos seguidos. Espera un minuto y vuelve a probar.' }

  return { tipo: 'desconocido', mensaje: GENERICO }
}

/** Atajo para mostrar el mensaje en un aviso. */
export function mensajeDeError(error: unknown): string {
  return interpretarError(error).mensaje
}
