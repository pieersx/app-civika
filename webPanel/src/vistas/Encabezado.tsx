import type { ReactNode } from 'react'

export function Encabezado({ titulo, descripcion, acciones }: { titulo: string; descripcion?: ReactNode; acciones?: ReactNode }) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
      <div className="min-w-0">
        <h1 className="font-titulo text-encabezado font-semibold text-tinta md:text-pagina">{titulo}</h1>
        {descripcion && <p className="mt-1 max-w-[70ch] text-sm text-tinta-suave">{descripcion}</p>}
      </div>
      {acciones}
    </div>
  )
}
