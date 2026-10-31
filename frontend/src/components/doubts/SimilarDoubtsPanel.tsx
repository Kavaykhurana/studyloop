import { ExternalLink } from 'lucide-react'
import { toast } from 'sonner'
import { useDeflection } from '../../hooks/useDoubt'
import { errorMessage } from '../../lib/api'
import type { SimilarItem } from '../../lib/types'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { SubjectTag } from './SubjectTag'

export function SimilarDoubtsPanel({
  items,
  queryText,
  onDeflected,
}: {
  items: SimilarItem[]
  queryText: string
  onDeflected: () => void
}) {
  const deflect = useDeflection()

  const solved = (item: SimilarItem) =>
    deflect.mutate(
      { sourceDoubtId: item.doubtId, queryText: queryText.slice(0, 500), similarity: item.similarity },
      {
        onSuccess: onDeflected,
        onError: (e) => toast.error(errorMessage(e)),
      },
    )

  return (
    <section aria-labelledby="radar-heading" className="flex flex-col gap-3">
      <h2 id="radar-heading" className="t-h2">
        Already solved on campus
      </h2>
      {items.map((item) => (
        <Card key={item.doubtId} className="flex flex-col gap-2">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <SubjectTag subject={item.subject} topic={item.topic} />
            <span className="rounded-full bg-success-soft px-2 font-mono text-[12px] font-medium leading-6 text-success">
              {Math.round(item.similarity * 100)}% match
            </span>
          </div>
          <h3 className="text-[16px] font-semibold leading-6">{item.title}</h3>
          <p className="line-clamp-3 text-[14px] leading-5 text-ink-2">{item.acceptedAnswerExcerpt}</p>
          <p className="text-[12px] text-ink-3">Answered by {item.acceptedAnswerAuthor}</p>
          <div className="mt-1 flex flex-wrap gap-2">
            <Button onClick={() => solved(item)} loading={deflect.isPending}>
              This solved my doubt
            </Button>
            <a
              href={`/app/doubts/${item.doubtId}`}
              target="_blank"
              rel="noreferrer noopener"
              className="inline-flex h-10 items-center gap-2 rounded-input border border-line bg-surface px-4 text-[14px] font-medium text-ink hover:bg-surface-2"
            >
              View full answer <ExternalLink className="size-4" aria-hidden="true" />
            </a>
          </div>
        </Card>
      ))}
    </section>
  )
}
