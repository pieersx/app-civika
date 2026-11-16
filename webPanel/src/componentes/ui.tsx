// Piezas básicas del panel. Todas respetan los tokens de diseño: área de toque
// de 44 px o más, radios de 8 (campos) y 12 (tarjetas) y foco visible.

import { CircleAlert, Inbox, LoaderCircle, RefreshCw, type LucideIcon } from 'lucide-react'
import { useId, type ButtonHTMLAttributes, type ComponentProps, type ReactNode } from 'react'
import { cx } from '../lib/cx'
import { interpretarError } from '../lib/errores'

type Variante = 'principal' | 'secundario' | 'texto' | 'peligro'

const VARIANTES: Record<Variante, string> = {
  principal: 'bg-terracota text-sobre-terracota hover:brightness-95 active:brightness-90',
  secundario: 'border border-borde-campo bg-superficie text-tinta hover-tinta',
  texto: 'text-terracota-texto hover-tinta',
  peligro: 'bg-error text-sobre-error hover:brightness-95 active:brightness-90',
}

interface BotonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante
  icono?: LucideIcon
  cargando?: boolean
  /** 56 px de alto: la acción principal de un formulario. */
  grande?: boolean
  ancho?: boolean
}

export function Boton({
  variante = 'secundario',
  icono: Icono,
  cargando = false,
  grande = false,
  ancho = false,
  className,
  children,
  disabled,
  type = 'button',
  ...resto
}: BotonProps) {
  return (
    <button
      type={type}
      disabled={disabled || cargando}
      aria-busy={cargando || undefined}
      className={cx(
        'inline-flex items-center justify-center gap-2 rounded-campo px-4 text-sm font-bold transition-[filter,background-color]',
        'disabled:opacity-55',
        grande ? 'min-h-14 text-base' : 'min-h-11',
        ancho && 'w-full',
        VARIANTES[variante],
        className,
      )}
      {...resto}
    >
      {cargando ? (
        <LoaderCircle aria-hidden="true" className="size-5 shrink-0 animate-spin" />
      ) : (
        Icono && <Icono aria-hidden="true" className="size-5 shrink-0" />
      )}
      {children}
    </button>
  )
}

interface BotonIconoProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icono: LucideIcon
  /** Nombre accesible: el botón no tiene texto visible. */
  etiqueta: string
}

export function BotonIcono({ icono: Icono, etiqueta, className, type = 'button', ...resto }: BotonIconoProps) {
  return (
    <button
      type={type}
      aria-label={etiqueta}
      title={etiqueta}
      className={cx(
        'inline-grid size-11 shrink-0 place-items-center rounded-campo text-tinta-suave hover-tinta hover:text-tinta',
        className,
      )}
      {...resto}
    >
      <Icono aria-hidden="true" className="size-5" />
    </button>
  )
}

const CLASE_CAMPO =
  'w-full rounded-campo border border-borde-campo bg-superficie px-3 text-base text-tinta placeholder:text-tinta-suave ' +
  'aria-invalid:border-error aria-invalid:border-2 disabled:opacity-60'

interface EnvolturaProps {
  etiqueta: string
  ayuda?: ReactNode
  error?: string | null
  /** Oculta la etiqueta a la vista, pero la mantiene para lectores de pantalla. */
  etiquetaOculta?: boolean
  className?: string
}

function Envoltura({
  id,
  etiqueta,
  ayuda,
  error,
  etiquetaOculta,
  className,
  children,
}: EnvolturaProps & { id: string; children: ReactNode }) {
  return (
    <div className={cx('flex flex-col gap-1', className)}>
      <label htmlFor={id} className={cx('text-sm font-bold text-tinta', etiquetaOculta && 'sr-only')}>
        {etiqueta}
      </label>
      {children}
      {ayuda && !error && (
        <p id={`${id}-ayuda`} className="text-sm text-tinta-suave">
          {ayuda}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="flex items-start gap-1 text-sm font-bold text-error">
          <CircleAlert aria-hidden="true" className="mt-0.5 size-4 shrink-0" />
          {error}
        </p>
      )}
    </div>
  )
}

function describir(id: string, ayuda: ReactNode, error?: string | null): string | undefined {
  if (error) return `${id}-error`
  return ayuda ? `${id}-ayuda` : undefined
}

type CampoProps = EnvolturaProps & Omit<ComponentProps<'input'>, 'className'>

export function Campo({ etiqueta, ayuda, error, etiquetaOculta, className, ...resto }: CampoProps) {
  const id = useId()
  return (
    <Envoltura id={id} etiqueta={etiqueta} ayuda={ayuda} error={error} etiquetaOculta={etiquetaOculta} className={className}>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describir(id, ayuda, error)}
        className={cx(CLASE_CAMPO, 'min-h-12')}
        {...resto}
      />
    </Envoltura>
  )
}

type AreaProps = EnvolturaProps & Omit<ComponentProps<'textarea'>, 'className'>

export function AreaTexto({ etiqueta, ayuda, error, etiquetaOculta, className, ...resto }: AreaProps) {
  const id = useId()
  return (
    <Envoltura id={id} etiqueta={etiqueta} ayuda={ayuda} error={error} etiquetaOculta={etiquetaOculta} className={className}>
      <textarea
        id={id}
        rows={3}
        aria-invalid={error ? true : undefined}
        aria-describedby={describir(id, ayuda, error)}
        className={cx(CLASE_CAMPO, 'py-2.5')}
        {...resto}
      />
    </Envoltura>
  )
}

type SelectorProps = EnvolturaProps & Omit<ComponentProps<'select'>, 'className'>

export function Selector({ etiqueta, ayuda, error, etiquetaOculta, className, children, ...resto }: SelectorProps) {
  const id = useId()
  return (
    <Envoltura id={id} etiqueta={etiqueta} ayuda={ayuda} error={error} etiquetaOculta={etiquetaOculta} className={className}>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describir(id, ayuda, error)}
        className={cx(CLASE_CAMPO, 'min-h-11 pr-8 text-sm')}
        {...resto}
      >
        {children}
      </select>
    </Envoltura>
  )
}

type TonoAviso = 'error' | 'info' | 'atencion'

/** Mensaje dentro de la página (no flotante). El ícono acompaña siempre al color. */
export function Aviso({ tono = 'info', children, className }: { tono?: TonoAviso; children: ReactNode; className?: string }) {
  const estilos: Record<TonoAviso, string> = {
    error: 'tinte-error border-error',
    info: 'tinte-verde border-verde',
    atencion: 'tinte-atencion border-borde-campo',
  }
  return (
    <div
      role={tono === 'error' ? 'alert' : 'status'}
      className={cx('flex items-start gap-2 rounded-campo border px-3 py-2.5 text-sm text-tinta', estilos[tono], className)}
    >
      <CircleAlert aria-hidden="true" className={cx('mt-0.5 size-4 shrink-0', tono === 'error' ? 'text-error' : 'text-tinta')} />
      <div className="min-w-0">{children}</div>
    </div>
  )
}

export function Esqueleto({ className }: { className?: string }) {
  return <div aria-hidden="true" className={cx('esqueleto', className)} />
}

/** Anuncia la carga a lectores de pantalla mientras se ven los esqueletos. */
export function Cargando({ texto = 'Cargando…', children }: { texto?: string; children: ReactNode }) {
  return (
    <div role="status" aria-live="polite">
      <span className="sr-only">{texto}</span>
      {children}
    </div>
  )
}

interface VacioProps {
  icono?: LucideIcon
  titulo: string
  children?: ReactNode
  accion?: ReactNode
  compacto?: boolean
}

export function EstadoVacio({ icono: Icono = Inbox, titulo, children, accion, compacto = false }: VacioProps) {
  return (
    <div className={cx('flex flex-col items-center gap-2 px-4 text-center', compacto ? 'py-6' : 'py-14')}>
      <Icono aria-hidden="true" className={cx('text-tinta-suave', compacto ? 'size-6' : 'size-9')} strokeWidth={1.5} />
      <p className="text-base font-bold text-tinta">{titulo}</p>
      {children && <p className="max-w-[46ch] text-sm text-tinta-suave">{children}</p>}
      {accion && <div className="mt-2">{accion}</div>}
    </div>
  )
}

interface ErrorVistaProps {
  error: unknown
  alReintentar?: () => void
  compacto?: boolean
}

export function ErrorVista({ error, alReintentar, compacto = false }: ErrorVistaProps) {
  const { mensaje } = interpretarError(error)
  return (
    <div role="alert" className={cx('flex flex-col items-center gap-2 px-4 text-center', compacto ? 'py-6' : 'py-14')}>
      <CircleAlert aria-hidden="true" className={cx('text-error', compacto ? 'size-6' : 'size-9')} strokeWidth={1.5} />
      <p className="text-base font-bold text-tinta">No pudimos cargar esto</p>
      <p className="max-w-[46ch] text-sm text-tinta-suave">{mensaje}</p>
      {alReintentar && (
        <Boton className="mt-2" icono={RefreshCw} onClick={alReintentar}>
          Reintentar
        </Boton>
      )}
    </div>
  )
}

export function Tarjeta({ className, children, ...resto }: ComponentProps<'section'>) {
  return (
    <section className={cx('rounded-tarjeta border border-borde bg-superficie', className)} {...resto}>
      {children}
    </section>
  )
}

/** Dato pequeño con ícono y texto: nunca solo el ícono ni solo el color. */
export function Dato({ icono: Icono, children, titulo }: { icono: LucideIcon; children: ReactNode; titulo?: string }) {
  return (
    <span className="inline-flex items-center gap-1 text-sm text-tinta-suave" title={titulo}>
      <Icono aria-hidden="true" className="size-4 shrink-0" />
      {children}
    </span>
  )
}
