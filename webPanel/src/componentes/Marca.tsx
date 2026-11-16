import { cx } from '../lib/cx'

/** Logotipo: la señal triangular (lo único memorable del sistema visual) y el nombre. */
export function Marca({ className, compacta = false }: { className?: string; compacta?: boolean }) {
  return (
    <div className={cx('flex items-center gap-2.5', className)}>
      <svg width={compacta ? 28 : 36} height={compacta ? 28 : 36} viewBox="0 0 32 32" aria-hidden="true" className="shrink-0">
        <rect width="32" height="32" rx="8" fill="var(--terracota)" />
        <path d="M16 6.5 26 24H6z" fill="none" stroke="var(--sobre-terracota)" strokeWidth="2.6" strokeLinejoin="round" />
        <circle cx="16" cy="19.5" r="1.8" fill="var(--sobre-terracota)" />
      </svg>
      <div className="leading-none">
        <span className={cx('block font-titulo font-bold text-tinta', compacta ? 'text-titular' : 'text-encabezado')}>Cívika</span>
        {!compacta && <span className="mt-0.5 block text-sm text-tinta-suave">Panel de gestión</span>}
      </div>
    </div>
  )
}
