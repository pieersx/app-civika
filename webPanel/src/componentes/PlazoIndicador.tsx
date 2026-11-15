import { CircleCheck, Clock, TriangleAlert } from 'lucide-react'
import { cx } from '../lib/cx'
import type { Plazo } from '../lib/plazos'

/** Plazo con ícono y palabra: «Vencido hace 3 días» nunca depende solo del color. */
export function PlazoIndicador({ plazo, className }: { plazo: Plazo; className?: string }) {
  const { situacion } = plazo
  if (situacion === 'sin_plazo') return <span className={cx('text-sm text-tinta-suave', className)}>{plazo.texto}</span>
  const vencido = situacion === 'vencido'
  const Icono = vencido || situacion === 'cerrado_fuera_de_plazo' ? TriangleAlert : situacion === 'por_vencer' ? Clock : CircleCheck
  const color = vencido ? 'font-bold text-error' : situacion === 'por_vencer' ? 'font-bold text-atencion-texto' : 'text-tinta-suave'
  return (
    <span className={cx('inline-flex items-center gap-1 text-sm', color, className)}>
      <Icono aria-hidden="true" className="size-4 shrink-0" />
      {plazo.texto}
    </span>
  )
}
