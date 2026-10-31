import { cn } from '../../lib/cn'
import type { SubjectRef, TopicRef } from '../../lib/types'

export function SubjectTag({
  subject,
  topic,
  className,
}: {
  subject: SubjectRef
  topic?: TopicRef | null
  className?: string
}) {
  return (
    <span className={cn('inline-flex items-center gap-1.5 text-[12px] leading-4 text-ink-2', className)}>
      <span className="size-2 rounded-full" style={{ backgroundColor: subject.colorHex }} aria-hidden="true" />
      {subject.code}
      {topic ? ` · ${topic.name}` : ''}
    </span>
  )
}
