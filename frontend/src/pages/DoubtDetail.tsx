import { X } from 'lucide-react'
import { useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { AnswerComposer } from '../components/doubts/AnswerComposer'
import { AnswerItem } from '../components/doubts/AnswerItem'
import { HintLadder } from '../components/doubts/HintLadder'
import { MatchCard } from '../components/doubts/MatchCard'
import { Markdown } from '../components/doubts/Markdown'
import { StatusPill } from '../components/doubts/StatusPill'
import { SubjectTag } from '../components/doubts/SubjectTag'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/ErrorState'
import { Modal } from '../components/ui/Modal'
import { Skeleton } from '../components/ui/Skeleton'
import { useDeleteDoubt, useDoubt } from '../hooks/useDoubt'
import { useRefreshMatches } from '../hooks/useMatches'
import { errorCode, errorMessage } from '../lib/api'
import { difficultyLabel, relativeTime, yearBranch } from '../lib/format'
import { usePageTitle } from '../lib/usePageTitle'

function DetailSkeleton() {
  return (
    <div className="grid gap-6 lg:grid-cols-3" aria-hidden="true">
      <div className="flex flex-col gap-4 lg:col-span-2">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-10 w-3/4" />
        <Skeleton className="h-40" />
        <Skeleton className="h-56" />
      </div>
      <Skeleton className="h-64" />
    </div>
  )
}

export function DoubtDetail() {
  const { id: raw } = useParams()
  const id = Number(raw)
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const doubt = useDoubt(id)
  const refresh = useRefreshMatches(id)
  const remove = useDeleteDoubt(id)
  const [confirmDelete, setConfirmDelete] = useState(false)
  usePageTitle(doubt.data?.title ?? 'Doubt')

  if (!Number.isFinite(id)) return <ErrorState message="That doubt link doesn't look right." />
  if (doubt.isPending) return <DetailSkeleton />
  if (doubt.isError) {
    return (
      <ErrorState
        message={errorCode(doubt.error) === 'NOT_FOUND' ? "We couldn't find that doubt." : errorMessage(doubt.error)}
        onRetry={errorCode(doubt.error) === 'NOT_FOUND' ? undefined : () => void doubt.refetch()}
      />
    )
  }

  const d = doubt.data
  const notified = d.matches.filter((m) => m.rank <= 3 && m.score >= 25).length
  const showNew = params.get('new') === '1'
  const difficulty = difficultyLabel(d.difficulty)

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="flex min-w-0 flex-col gap-5 lg:col-span-2">
        <header className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <SubjectTag subject={d.subject} topic={d.topic} />
            <StatusPill status={d.status} resolution={d.resolution} />
            {difficulty && <Badge>{difficulty}</Badge>}
            {d.source === 'SNAP' && <Badge variant="ai">Snapped</Badge>}
          </div>
          <h1 className="t-h1">{d.title}</h1>
          <p className="text-[13px] text-ink-3">
            Asked by {d.author.name} · {yearBranch(d.author.yearOfStudy, d.author.branch)} · {relativeTime(d.createdAt)}
          </p>
        </header>

        {showNew && (
          <div className="flex items-start justify-between gap-3 rounded-input bg-brand-soft px-4 py-3 text-[14px] text-ink">
            <p>
              Posted. We've notified {notified} {notified === 1 ? 'senior' : 'seniors'} who've solved similar doubts. While
              you wait, try a hint.
            </p>
            <button
              type="button"
              aria-label="Dismiss"
              onClick={() => {
                params.delete('new')
                setParams(params, { replace: true })
              }}
              className="shrink-0 rounded-md p-1 text-ink-3 hover:text-ink"
            >
              <X className="size-4" aria-hidden="true" />
            </button>
          </div>
        )}

        <Card>
          <Markdown>{d.bodyMd}</Markdown>
        </Card>

        <HintLadder doubt={d} />

        <section aria-labelledby="answers" className="flex flex-col gap-3">
          <h2 id="answers" className="t-h2">
            {d.answers.length} {d.answers.length === 1 ? 'answer' : 'answers'}
          </h2>
          {d.answers.length === 0 ? (
            <Card className="text-ink-2">No answers yet. Matched seniors have been notified.</Card>
          ) : (
            d.answers.map((a) => (
              <AnswerItem
                key={a.id}
                answer={a}
                doubtId={d.id}
                canAccept={d.permissions.canAccept}
                canRate={d.permissions.isAuthor}
              />
            ))
          )}
          {d.permissions.canAnswer && <AnswerComposer doubtId={d.id} />}
        </section>
      </div>

      <aside className="flex min-w-0 flex-col gap-3" aria-labelledby="matched">
        <h2 id="matched" className="t-h3">
          Matched seniors
        </h2>
        {d.matches.length === 0 ? (
          <Card className="text-[14px] text-ink-2">
            No seniors matched yet. Your doubt is in the open feed, and the hint ladder is ready.
          </Card>
        ) : (
          d.matches.slice(0, 5).map((m) => <MatchCard key={m.helper.id} match={m} />)
        )}
        {d.permissions.canRefreshMatches && (
          <Button
            variant="ghost"
            loading={refresh.isPending}
            onClick={() =>
              refresh.mutate(undefined, {
                onSuccess: () => toast.success('Updated your matches.'),
                onError: (e) => toast.error(errorMessage(e)),
              })
            }
          >
            Find more seniors
          </Button>
        )}
        {d.permissions.canDelete && (
          <Button variant="ghost" className="text-danger hover:bg-danger-soft" onClick={() => setConfirmDelete(true)}>
            Delete doubt
          </Button>
        )}
      </aside>

      <Modal
        open={confirmDelete}
        title="Delete this doubt?"
        onClose={() => setConfirmDelete(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirmDelete(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              loading={remove.isPending}
              onClick={() =>
                remove.mutate(undefined, {
                  onSuccess: () => navigate('/app/doubts', { replace: true }),
                  onError: (e) => {
                    setConfirmDelete(false)
                    toast.error(errorMessage(e))
                  },
                })
              }
            >
              Delete doubt
            </Button>
          </>
        }
      >
        This can't be undone. Only doubts without answers can be deleted.
      </Modal>
    </div>
  )
}
