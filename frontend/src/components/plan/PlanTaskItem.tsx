import { Link } from 'react-router'
import { cn } from '../../lib/cn'
import type { PlanTask, PlanTaskKind } from '../../lib/types'
import { Badge, type BadgeVariant } from '../ui/Badge'
import { SubjectTag } from '../doubts/SubjectTag'

const KIND: Record<PlanTaskKind, { label: string; variant: BadgeVariant }> = {
  LEARN: { label: 'Learn', variant: 'brand' },
  PRACTICE: { label: 'Practice', variant: 'success' },
  REVISE: { label: 'Revise', variant: 'hint' },
  ASK_PEER: { label: 'Ask a peer', variant: 'ai' },
}

export function PlanTaskItem({
  task,
  onToggle,
}: {
  task: PlanTask
  onToggle: (done: boolean) => void
}) {
  const kind = KIND[task.kind]
  return (
    <li className="flex items-start gap-3 rounded-input border border-line bg-surface p-3">
      <input
        type="checkbox"
        checked={task.done}
        onChange={(e) => onToggle(e.target.checked)}
        aria-label={`Mark done: ${task.title}`}
        className="mt-1 size-5 shrink-0 accent-[#4338ca]"
      />
      <div className="min-w-0 flex-1">
        <p className={cn('text-[14px] leading-5 text-ink', task.done && 'text-ink-3 line-through')}>
          {task.kind === 'ASK_PEER' ? (
            <Link to="/app/ask" className="underline underline-offset-2">
              {task.title}
            </Link>
          ) : (
            task.title
          )}
        </p>
        <div className="mt-1.5 flex flex-wrap items-center gap-2">
          <Badge variant={kind.variant}>{kind.label}</Badge>
          <span className="font-mono text-[12px] text-ink-3">{task.minutes} min</span>
          <SubjectTag subject={task.subject} topic={task.topic} />
        </div>
      </div>
    </li>
  )
}
