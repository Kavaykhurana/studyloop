import { cn } from '../../lib/cn'

export interface TabItem<T extends string> {
  id: T
  label: string
}

export function Tabs<T extends string>({
  items,
  value,
  onChange,
  label,
  className,
}: {
  items: TabItem<T>[]
  value: T
  onChange: (id: T) => void
  label: string
  className?: string
}) {
  return (
    <div role="tablist" aria-label={label} className={cn('inline-flex rounded-input bg-code p-1', className)}>
      {items.map((item) => (
        <button
          key={item.id}
          type="button"
          role="tab"
          aria-selected={item.id === value}
          onClick={() => onChange(item.id)}
          className={cn(
            'min-h-8 rounded-[8px] px-3 text-[13px] font-medium transition-colors duration-[120ms]',
            item.id === value ? 'bg-surface text-ink shadow-card' : 'text-ink-2 hover:text-ink',
          )}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}

/** Segmented single-choice control (radio semantics) used for status filters and ranges. */
export function Segmented<T extends string | number>({
  items,
  value,
  onChange,
  label,
  className,
}: {
  items: { id: T; label: string }[]
  value: T
  onChange: (id: T) => void
  label: string
  className?: string
}) {
  return (
    <div role="radiogroup" aria-label={label} className={cn('inline-flex rounded-input bg-code p-1', className)}>
      {items.map((item) => (
        <button
          key={String(item.id)}
          type="button"
          role="radio"
          aria-checked={item.id === value}
          onClick={() => onChange(item.id)}
          className={cn(
            'min-h-8 rounded-[8px] px-3 text-[13px] font-medium transition-colors duration-[120ms]',
            item.id === value ? 'bg-surface text-ink shadow-card' : 'text-ink-2 hover:text-ink',
          )}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
