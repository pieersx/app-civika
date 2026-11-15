import type { ReactNode } from 'react'
import { ConmutadorTema } from '../componentes/ConmutadorTema'
import { Marca } from '../componentes/Marca'

/** Marco de las pantallas previas al panel: inicio de sesión, segundo factor y avisos. */
export function MarcoAcceso({ titulo, descripcion, children }: { titulo: string; descripcion?: ReactNode; children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col items-center bg-arena px-4 py-8 sm:justify-center">
      <main className="w-full max-w-md">
        <Marca className="mb-6" />
        <div className="rounded-tarjeta border border-borde bg-superficie p-5 sm:p-8">
          <h1 className="font-titulo text-encabezado font-semibold text-tinta">{titulo}</h1>
          {descripcion && <p className="mt-2 text-base text-tinta-suave">{descripcion}</p>}
          <div className="mt-6">{children}</div>
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
          <p className="max-w-[32ch] text-sm text-tinta-suave">Panel para entidades y moderación. No es un canal de emergencias.</p>
          <ConmutadorTema className="w-full sm:w-auto" />
        </div>
      </main>
    </div>
  )
}
