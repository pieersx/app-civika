import { Monitor, Moon, Sun } from 'lucide-react'
import { cx } from '../lib/cx'
import { useTema, type Tema } from '../lib/tema'

const OPCIONES: { valor: Tema; etiqueta: string; icono: typeof Sun }[] = [
  { valor: 'sistema', etiqueta: 'Sistema', icono: Monitor },
  { valor: 'claro', etiqueta: 'Claro', icono: Sun },
  { valor: 'oscuro', etiqueta: 'Oscuro', icono: Moon },
]

/** Tres opciones: seguir al sistema, claro u oscuro. La elegida lleva borde y `aria-pressed`. */
export function ConmutadorTema({ className }: { className?: string }) {
  const [tema, cambiar] = useTema()
  return (
    <div role="group" aria-label="Tema" className={cx('flex rounded-campo border border-borde p-0.5', className)}>
      {OPCIONES.map(({ valor, etiqueta, icono: Icono }) => {
        const activo = tema === valor
        return (
          <button
            key={valor}
            type="button"
            aria-pressed={activo}
            aria-label={`Tema: ${etiqueta.toLowerCase()}`}
            title={`Tema: ${etiqueta.toLowerCase()}`}
            onClick={() => cambiar(valor)}
            className={cx(
              'inline-flex min-h-11 flex-1 items-center justify-center gap-1 rounded-[6px] px-1 text-xs font-bold',
              activo ? 'tinte-verde text-verde' : 'text-tinta-suave hover-tinta',
            )}
          >
            <Icono aria-hidden="true" className="size-4" strokeWidth={activo ? 2.5 : 2} />
            <span className="hidden min-[360px]:inline">{etiqueta}</span>
          </button>
        )
      })}
    </div>
  )
}
