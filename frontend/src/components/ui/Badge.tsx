import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

export type BadgeVariant = 'neutral' | 'brand' | 'success' | 'hint' | 'ai' | 'danger'

const variants: Record<BadgeVariant, string> = {
  neutral: 'bg-code text-ink-2',
  brand: 'bg-brand-soft text-brand',
  success: 'bg-success-soft text-success',
  hint: 'bg-hint-soft text-hint',
  ai: 'bg-ai-soft text-ai',
  danger: 'bg-danger-soft text-danger',
}

export function Badge({
  variant = 'neutral',
  className,
  ...rest
}: HTMLAttributes<HTMLSpanElement> & { variant?: BadgeVariant }) {
  return (
    <span
      {...rest}
      className={cn(
        'inline-flex h-6 items-center gap-1 whitespace-nowrap rounded-full px-2 text-[12px] font-semibold',
        variants[variant],
        className,
      )}
    />
  )
}
