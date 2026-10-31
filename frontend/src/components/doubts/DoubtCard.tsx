import { Link } from 'react-router'
import { relativeTime, yearBranch } from '../../lib/format'
import type { DoubtSummary } from '../../lib/types'
import { Card } from '../ui/Card'
import { StatusPill } from './StatusPill'
import { SubjectTag } from './SubjectTag'

export function DoubtCard({ doubt }: { doubt: DoubtSummary }) {
  const who = [doubt.author.name.split(' ')[0], doubt.author.yearOfStudy ? `Year ${doubt.author.yearOfStudy}` : null]
    .filter(Boolean)
    .join(', ')
  return (
    <Link to={`/app/doubts/${doubt.id}`} className="block rounded-card">
      <Card interactive className="flex flex-col gap-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <SubjectTag subject={doubt.subject} topic={doubt.topic} />
          <StatusPill status={doubt.status} resolution={doubt.resolution} />
        </div>
        <h3 className="text-[16px] font-semibold leading-6 text-ink">{doubt.title}</h3>
        <p className="line-clamp-2 text-[14px] leading-5 text-ink-2">{doubt.excerpt}</p>
        <p className="text-[12px] text-ink-3" title={yearBranch(doubt.author.yearOfStudy, doubt.author.branch)}>
          {doubt.answerCount} {doubt.answerCount === 1 ? 'answer' : 'answers'} · {who} · {relativeTime(doubt.createdAt)}
        </p>
      </Card>
    </Link>
  )
}

export function DoubtCardSkeleton() {
  return (
    <Card className="flex flex-col gap-3" aria-hidden="true">
      <div className="skeleton h-4 w-40" />
      <div className="skeleton h-5 w-3/4" />
      <div className="skeleton h-4 w-full" />
      <div className="skeleton h-3 w-1/3" />
    </Card>
  )
}
