import type { ButtonHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  selected?: boolean
}

export function Chip({ selected = false, className, ...rest }: ChipProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      {...rest}
      className={cn(
        'inline-flex min-h-10 items-center gap-2 rounded-full border px-3 text-[13px] font-medium transition-colors duration-[120ms]',
        selected ? 'border-brand bg-brand-soft text-brand' : 'border-line bg-surface text-ink-2 hover:bg-surface-2',
        className,
      )}
    />
  )
}
