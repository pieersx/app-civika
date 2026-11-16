import { CircleAlert, CircleCheck, Info, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { cx } from '../lib/cx'
import { ContextoAvisos, type TonoAviso } from './avisos'


interface AvisoActivo {
  id: number
  mensaje: string
  tono: TonoAviso
}

const DURACION_MS: Record<TonoAviso, number> = { exito: 5000, info: 6000, error: 9000 }
const ICONO = { exito: CircleCheck, error: CircleAlert, info: Info }
const COLOR_ICONO: Record<TonoAviso, string> = { exito: 'text-resuelto-texto', error: 'text-error', info: 'text-verde' }
const PREFIJO: Record<TonoAviso, string> = { exito: 'Listo', error: 'Error', info: 'Aviso' }

export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<AvisoActivo[]>([])
  const siguienteId = useRef(1)
  const temporizadores = useRef(new Map<number, number>())

  const cerrar = useCallback((id: number) => {
    window.clearTimeout(temporizadores.current.get(id))
    temporizadores.current.delete(id)
    setAvisos((lista) => lista.filter((aviso) => aviso.id !== id))
  }, [])

  const avisar = useCallback(
    (mensaje: string, tono: TonoAviso = 'info') => {
      const id = siguienteId.current++
      // Como mucho 3 a la vez; un mensaje repetido no se apila.
      setAvisos((lista) => [...lista.filter((aviso) => aviso.mensaje !== mensaje).slice(-2), { id, mensaje, tono }])
      temporizadores.current.set(
        id,
        window.setTimeout(() => cerrar(id), DURACION_MS[tono]),
      )
    },
    [cerrar],
  )

  useEffect(() => {
    const pendientes = temporizadores.current
    return () => pendientes.forEach((temporizador) => window.clearTimeout(temporizador))
  }, [])

  const api = useMemo(() => ({ avisar }), [avisar])

  return (
    <ContextoAvisos.Provider value={api}>
      {children}
      <div
        aria-label="Avisos"
        role="region"
        className="pointer-events-none fixed inset-x-0 bottom-0 z-[100] flex flex-col items-center gap-2 p-4 sm:items-end"
      >
        {avisos.map((aviso) => {
          const Icono = ICONO[aviso.tono]
          return (
            <div
              key={aviso.id}
              role={aviso.tono === 'error' ? 'alert' : 'status'}
              className="pointer-events-auto flex w-full max-w-sm animate-entrar items-start gap-2 rounded-tarjeta border border-borde-campo bg-superficie py-2 pl-3 pr-1 shadow-panel"
            >
              <Icono aria-hidden="true" className={cx('mt-2.5 size-5 shrink-0', COLOR_ICONO[aviso.tono])} />
              <p className="min-w-0 flex-1 py-2.5 text-sm text-tinta">
                <span className="sr-only">{PREFIJO[aviso.tono]}: </span>
                {aviso.mensaje}
              </p>
              <button
                type="button"
                aria-label="Cerrar aviso"
                onClick={() => cerrar(aviso.id)}
                className="inline-grid size-11 shrink-0 place-items-center rounded-campo text-tinta-suave hover-tinta"
              >
                <X aria-hidden="true" className="size-4" />
              </button>
            </div>
          )
        })}
      </div>
    </ContextoAvisos.Provider>
  )
}
