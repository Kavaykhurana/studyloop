import type { HTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  interactive?: boolean
}

export function Card({ interactive = false, className, ...rest }: CardProps) {
  return (
    <div
      {...rest}
      className={cn(
        'rounded-card border border-line bg-surface p-4 shadow-card md:p-5',
        interactive && 'transition-shadow hover:border-line-strong hover:shadow-hover',
        className,
      )}
    />
  )
}
