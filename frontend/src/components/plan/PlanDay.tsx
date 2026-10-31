import { dayLabel } from '../../lib/format'
import type { PlanDay as PlanDayType } from '../../lib/types'
import { PlanTaskItem } from './PlanTaskItem'

export function PlanDay({
  day,
  onToggle,
}: {
  day: PlanDayType
  onToggle: (taskId: number, done: boolean) => void
}) {
  return (
    <section
      aria-label={`Day ${day.dayIndex + 1}`}
      className="flex w-full shrink-0 flex-col gap-2 rounded-card border border-line bg-surface-2 p-3 md:w-[260px]"
    >
      <header>
        <p className="t-micro text-brand">{dayLabel(day.date, day.dayIndex)}</p>
        <h3 className="text-[15px] font-semibold leading-5 text-ink">{day.focus}</h3>
        <p className="font-mono text-[12px] text-ink-3">{day.totalMinutes} min planned</p>
      </header>
      <ul className="flex flex-col gap-2">
        {day.tasks.map((task) => (
          <PlanTaskItem key={task.id} task={task} onToggle={(done) => onToggle(task.id, done)} />
        ))}
      </ul>
    </section>
  )
}
