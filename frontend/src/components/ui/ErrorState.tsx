import { Button } from './Button'
import { cn } from '../../lib/cn'

export function ErrorState({
  message = 'Something went wrong.',
  onRetry,
  className,
}: {
  message?: string
  onRetry?: () => void
  className?: string
}) {
  return (
    <div role="alert" className={cn('flex flex-col items-start gap-3 rounded-card bg-danger-soft p-4 text-danger', className)}>
      <p className="text-[14px] font-medium">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry} className="text-ink">
          Try again
        </Button>
      )}
    </div>
  )
}
