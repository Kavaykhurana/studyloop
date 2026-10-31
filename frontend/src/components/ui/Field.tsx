import { useId, type ReactNode } from 'react'
import { cn } from '../../lib/cn'

export interface FieldChrome {
  label?: string
  helper?: string
  error?: string
}

export const controlClasses = (invalid: boolean): string =>
  cn(
    'w-full rounded-input border bg-surface text-[15px] text-ink placeholder:text-ink-3',
    'transition-colors duration-[120ms] hover:border-line-strong focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/25',
    invalid ? 'border-danger' : 'border-line',
  )

/** Label, control slot, error and helper text. The render prop receives ids for aria wiring. */
export function Field({
  label,
  helper,
  error,
  children,
  className,
}: FieldChrome & {
  className?: string
  children: (ids: { id: string; describedBy: string | undefined; invalid: boolean }) => ReactNode
}) {
  const id = useId()
  const describedBy = error ? `${id}-error` : helper ? `${id}-help` : undefined
  return (
    <div className={cn('flex flex-col gap-1', className)}>
      {label && (
        <label htmlFor={id} className="text-[13px] font-medium text-ink-2">
          {label}
        </label>
      )}
      {children({ id, describedBy, invalid: Boolean(error) })}
      {error ? (
        <p id={`${id}-error`} className="text-[13px] text-danger" role="alert">
          {error}
        </p>
      ) : helper ? (
        <p id={`${id}-help`} className="text-[12px] leading-4 text-ink-3">
          {helper}
        </p>
      ) : null}
    </div>
  )
}
