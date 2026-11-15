// Diálogo modal sobre <dialog> nativo: el navegador atrapa el foco,
// cierra con Escape y devuelve el foco al elemento que lo abrió.

import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'
import { BotonIcono } from './ui'

interface DialogoProps {
  titulo: string
  /** Texto bajo el título, enlazado con aria-describedby. */
  descripcion?: ReactNode
  alCerrar: () => void
  children: ReactNode
  /** Impide cerrar (Escape, clic fuera) mientras se guarda. */
  bloqueado?: boolean
}

export function Dialogo({ titulo, descripcion, alCerrar, children, bloqueado = false }: DialogoProps) {
  const ref = useRef<HTMLDialogElement>(null)
  const idTitulo = useId()
  const idDescripcion = useId()

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (!dialogo.open) dialogo.showModal()
    // showModal enfoca el primer control (el botón de cerrar): mejor llevar el foco al primer campo.
    dialogo.querySelector<HTMLElement>('textarea, input, select')?.focus()
    return () => {
      if (dialogo.open) dialogo.close()
    }
  }, [])

  return (
    <dialog
      ref={ref}
      aria-labelledby={idTitulo}
      aria-describedby={descripcion ? idDescripcion : undefined}
      onCancel={(evento) => {
        // Escape: se cierra desde React, para que el estado mande.
        evento.preventDefault()
        if (!bloqueado) alCerrar()
      }}
      onClick={(evento) => {
        // Clic en el velo (fuera del contenido).
        if (evento.target === ref.current && !bloqueado) alCerrar()
      }}
      className="fixed inset-x-0 bottom-0 top-auto m-0 max-h-[92dvh] w-full max-w-none animate-entrar overflow-hidden rounded-t-hoja bg-superficie p-0 text-tinta shadow-panel open:flex open:flex-col sm:inset-0 sm:m-auto sm:h-fit sm:max-h-[88dvh] sm:w-[min(32rem,calc(100vw-2rem))] sm:rounded-tarjeta"
    >
      <header className="flex shrink-0 items-start gap-2 border-b border-borde bg-superficie py-2 pl-4 pr-2 sm:pl-5">
        <div className="min-w-0 flex-1 py-2">
          <h2 id={idTitulo} className="font-titulo text-titular font-semibold text-tinta">
            {titulo}
          </h2>
          {descripcion && (
            <p id={idDescripcion} className="mt-1 text-sm text-tinta-suave">
              {descripcion}
            </p>
          )}
        </div>
        <BotonIcono icono={X} etiqueta="Cerrar" onClick={alCerrar} disabled={bloqueado} />
      </header>
      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">{children}</div>
    </dialog>
  )
}

/** Pie con las acciones del diálogo: la principal a la derecha. */
export function PieDialogo({ children }: { children: ReactNode }) {
  return <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">{children}</div>
}
