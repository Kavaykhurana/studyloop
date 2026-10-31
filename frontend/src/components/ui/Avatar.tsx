import { cn } from '../../lib/cn'
import { initials } from '../../lib/format'

const COLORS = ['#4F46E5', '#0E7490', '#B45309', '#7C3AED', '#BE185D', '#15803D']

const SIZES = { sm: 'size-6 text-[10px]', md: 'size-8 text-[12px]', lg: 'size-10 text-[14px]', xl: 'size-16 text-[22px]' }

export function Avatar({
  name,
  id,
  size = 'md',
  className,
}: {
  name: string
  id: number
  size?: keyof typeof SIZES
  className?: string
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold text-white',
        SIZES[size],
        className,
      )}
      style={{ backgroundColor: COLORS[id % 6] }}
    >
      {initials(name)}
    </span>
  )
}
