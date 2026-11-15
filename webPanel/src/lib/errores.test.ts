import { describe, expect, it } from 'vitest'
import { interpretarError } from './errores'

describe('interpretarError: RPC de Postgres', () => {
  it('40001: el incidente cambió y hay que recargar', () => {
    const error = interpretarError({
      code: '40001',
      message: 'La versión del incidente cambió; recarga antes de intentar de nuevo',
    })
    expect(error.tipo).toBe('conflicto')
    expect(error.mensaje).toBe('El incidente cambió mientras lo veías. Ya cargamos la versión nueva: revísala y vuelve a intentarlo.')
  })

  it('42501: sin permiso', () => {
    const error = interpretarError({ code: '42501', message: 'Transición no permitida para este rol' })
    expect(error.tipo).toBe('permiso')
    expect(error.mensaje).toBe('No tienes permiso para hacer esto. Transición no permitida para este rol.')
  })

  it('42501 por falta de segundo factor pide volver a verificar', () => {
    for (const message of ['Se requiere el segundo factor (MFA)', 'Se requiere MFA para cambiar estados']) {
      const error = interpretarError({ code: '42501', message })
      expect(error.tipo).toBe('mfa')
      expect(error.mensaje).toBe('Tu sesión ya no tiene el segundo factor. Cierra sesión y vuelve a entrar con tu código.')
    }
  })

  it('42501 del propio Postgres (RLS) no muestra el texto técnico en inglés', () => {
    const error = interpretarError({ code: '42501', message: 'permission denied for table incidentes' })
    expect(error.mensaje).toBe('No tienes permiso para hacer esto.')
  })

  it('22023: muestra la validación que escribió el servidor', () => {
    const error = interpretarError({ code: '22023', message: 'El rechazo necesita un motivo' })
    expect(error.tipo).toBe('validacion')
    expect(error.mensaje).toBe('El rechazo necesita un motivo.')
  })

  it('P0002: no encontrado', () => {
    const error = interpretarError({ code: 'P0002', message: 'Incidente no encontrado' })
    expect(error.tipo).toBe('no_encontrado')
    expect(error.mensaje).toBe('Incidente no encontrado. Puede que ya no esté en tu lista: recarga para ver los datos al día.')
  })

  it('PGRST301 / JWT caducado: sesión vencida', () => {
    expect(interpretarError({ code: 'PGRST301', message: 'JWT expired' }).tipo).toBe('sesion')
    expect(interpretarError({ code: 'PGRST303', message: 'JWT expired' }).tipo).toBe('sesion')
  })

  it('un código desconocido da un mensaje genérico sin filtrar detalles técnicos', () => {
    const error = interpretarError({ code: 'XX000', message: 'internal error: relation "x" does not exist' })
    expect(error.tipo).toBe('desconocido')
    expect(error.mensaje).toBe('Algo falló al hablar con el servidor. Inténtalo de nuevo en un momento.')
  })
})

describe('interpretarError: red', () => {
  it('fallo de fetch', () => {
    expect(interpretarError(new TypeError('Failed to fetch')).tipo).toBe('red')
    expect(interpretarError({ message: 'TypeError: Failed to fetch', code: '' }).tipo).toBe('red')
    expect(interpretarError({ name: 'AuthRetryableFetchError', message: 'Failed to fetch', status: 0 }).mensaje).toBe(
      'No hay conexión con el servidor. Revisa tu internet y vuelve a intentarlo.',
    )
  })
})

describe('interpretarError: Auth', () => {
  const auth = (code: string, status = 400) => ({ name: 'AuthApiError', code, status, message: 'x' })

  it('credenciales incorrectas', () => {
    expect(interpretarError(auth('invalid_credentials')).mensaje).toBe('El correo o la contraseña no coinciden.')
  })

  it('código de MFA incorrecto', () => {
    const error = interpretarError(auth('mfa_verification_failed', 422))
    expect(error.tipo).toBe('validacion')
    expect(error.mensaje).toBe('El código no es correcto o ya caducó. Escribe el que muestra ahora tu app.')
  })

  it('límite de intentos', () => {
    expect(interpretarError(auth('over_request_rate_limit', 429)).tipo).toBe('limite')
    expect(interpretarError({ name: 'AuthApiError', status: 429, message: 'x' }).tipo).toBe('limite')
  })

  it('contraseña débil o repetida', () => {
    expect(interpretarError(auth('weak_password', 422)).mensaje).toMatch(/contraseña más larga/)
    expect(interpretarError(auth('same_password', 422)).mensaje).toBe('La contraseña nueva tiene que ser distinta de la anterior.')
  })

  it('sesión caducada', () => {
    expect(interpretarError(auth('session_not_found', 403)).tipo).toBe('sesion')
    expect(interpretarError({ name: 'AuthSessionMissingError', message: 'Auth session missing!', status: 400 }).tipo).toBe('sesion')
  })

  it('falta el segundo factor para una operación de Auth', () => {
    expect(interpretarError(auth('insufficient_aal', 401)).tipo).toBe('mfa')
  })
})

describe('interpretarError: valores raros', () => {
  it('no se rompe con null, cadenas o Error', () => {
    expect(interpretarError(null).tipo).toBe('desconocido')
    expect(interpretarError('boom').tipo).toBe('desconocido')
    expect(interpretarError(new Error('boom')).tipo).toBe('desconocido')
  })
})
