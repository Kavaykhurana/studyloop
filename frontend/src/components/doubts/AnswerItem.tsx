import { CheckCircle2, Star } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { useAcceptAnswer, useRateAnswer } from '../../hooks/useDoubt'
import { errorMessage } from '../../lib/api'
import { cn } from '../../lib/cn'
import { relativeTime, yearBranch } from '../../lib/format'
import type { Answer } from '../../lib/types'
import { Avatar } from '../ui/Avatar'
import { Button } from '../ui/Button'
import { Modal } from '../ui/Modal'
import { Markdown } from './Markdown'

export function AnswerItem({
  answer,
  doubtId,
  canAccept,
  canRate,
}: {
  answer: Answer
  doubtId: number
  canAccept: boolean
  canRate: boolean
}) {
  const accept = useAcceptAnswer(doubtId)
  const rate = useRateAnswer(doubtId)
  const [confirming, setConfirming] = useState(false)
  const [hover, setHover] = useState(0)
  const shown = hover || answer.helpfulRating || 0

  const onAccept = () =>
    accept.mutate(answer.id, {
      onSuccess: () => {
        setConfirming(false)
        toast.success('Answer accepted. This doubt is now part of the campus knowledge base.')
      },
      onError: (e) => {
        setConfirming(false)
        toast.error(errorMessage(e))
      },
    })

  return (
    <article
      className={cn(
        'rounded-card border p-4',
        answer.isAccepted ? 'border-success/30 bg-success-soft' : 'border-line bg-surface',
      )}
    >
      <header className="mb-3 flex flex-wrap items-center gap-3">
        <Avatar name={answer.author.name} id={answer.author.id} />
        <div className="min-w-0 flex-1">
          <Link to={`/app/profile/${answer.author.id}`} className="text-[14px] font-semibold text-ink hover:underline">
            {answer.author.name}
          </Link>
          <p className="text-[12px] text-ink-3">
            {yearBranch(answer.author.yearOfStudy, answer.author.branch)} · {answer.author.reputation} reputation ·{' '}
            {relativeTime(answer.createdAt)}
          </p>
        </div>
        {answer.isAccepted && (
          <span className="inline-flex items-center gap-1 text-[13px] font-semibold text-success">
            <CheckCircle2 className="size-4" aria-hidden="true" /> Accepted
          </span>
        )}
      </header>
      <Markdown>{answer.bodyMd}</Markdown>

      {canAccept && !answer.isAccepted && (
        <div className="mt-3">
          <Button variant="secondary" size="sm" onClick={() => setConfirming(true)}>
            Accept answer
          </Button>
        </div>
      )}

      {answer.isAccepted && (
        <RatingRow
          rating={answer.helpfulRating}
          shown={shown}
          onHover={setHover}
          editable={canRate || undefined}
          onRate={(stars) => rate.mutate({ answerId: answer.id, stars }, { onError: (e) => toast.error(errorMessage(e)) })}
        />
      )}

      <Modal
        open={confirming}
        title={`Accept ${answer.author.name}'s answer?`}
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              Cancel
            </Button>
            <Button onClick={onAccept} loading={accept.isPending}>
              Accept answer
            </Button>
          </>
        }
      >
        This closes the doubt and gives them +15 reputation.
      </Modal>
    </article>
  )
}

function RatingRow({
  rating,
  shown,
  onHover,
  onRate,
  editable,
}: {
  rating: number | null
  shown: number
  onHover: (n: number) => void
  onRate: (n: number) => void
  editable: true | undefined
}) {
  return (
    <div className="mt-3 flex flex-wrap items-center gap-2 text-[13px] text-ink-2">
      {editable ? <span>How helpful was it?</span> : <span>{rating ? 'Rated by the asker' : 'Not rated yet'}</span>}
      <div className="flex" onMouseLeave={() => onHover(0)}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button
            key={n}
            type="button"
            disabled={!editable}
            aria-label={`${n} star${n > 1 ? 's' : ''}`}
            aria-pressed={rating === n}
            onMouseEnter={() => editable && onHover(n)}
            onClick={() => onRate(n)}
            className="inline-flex size-8 items-center justify-center disabled:cursor-default"
          >
            <Star
              className={cn('size-5', n <= shown ? 'fill-[#f59e0b] text-[#f59e0b]' : 'text-ink-3')}
              aria-hidden="true"
            />
          </button>
        ))}
      </div>
    </div>
  )
}
