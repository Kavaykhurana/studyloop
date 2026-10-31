import { MessagesSquare } from 'lucide-react'
import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import { ClearFilters, DoubtFilters } from '../components/doubts/DoubtFilters'
import { DoubtCard, DoubtCardSkeleton } from '../components/doubts/DoubtCard'
import { Button, ButtonLink } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorState } from '../components/ui/ErrorState'
import { useDoubtFeed, type DoubtFilters as Filters } from '../hooks/useDoubts'
import { usePageTitle } from '../lib/usePageTitle'

function parse(params: URLSearchParams): Filters {
  const status = params.get('status')
  const subjectId = Number(params.get('subjectId'))
  const topicId = Number(params.get('topicId'))
  const q = params.get('q')
  return {
    ...(status === 'OPEN' || status === 'SOLVED' ? { status } : {}),
    ...(subjectId > 0 ? { subjectId } : {}),
    ...(topicId > 0 ? { topicId } : {}),
    ...(params.get('matchedToMe') === 'true' ? { matchedToMe: true } : {}),
    ...(q && q.length >= 2 ? { q } : {}),
  }
}

function serialise(filters: Filters): URLSearchParams {
  const params = new URLSearchParams()
  if (filters.status) params.set('status', filters.status)
  if (filters.subjectId) params.set('subjectId', String(filters.subjectId))
  if (filters.topicId) params.set('topicId', String(filters.topicId))
  if (filters.matchedToMe) params.set('matchedToMe', 'true')
  if (filters.q) params.set('q', filters.q)
  return params
}

export function Feed() {
  usePageTitle('Doubts')
  const [params, setParams] = useSearchParams()
  const filters = useMemo(() => parse(params), [params])
  const feed = useDoubtFeed(filters)
  const onChange = useCallback((next: Filters) => setParams(serialise(next), { replace: true }), [setParams])
  const hasFilters = Object.keys(filters).length > 0
  const items = feed.data?.pages.flatMap((p) => p.items) ?? []

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="t-h1">Doubts</h1>
        <ButtonLink to="/app/ask">Ask a doubt</ButtonLink>
      </div>
      <DoubtFilters value={filters} onChange={onChange} />
      {feed.isPending && (
        <div className="flex flex-col gap-3">
          {[0, 1, 2, 3].map((i) => (
            <DoubtCardSkeleton key={i} />
          ))}
        </div>
      )}
      {feed.isError && <ErrorState message="We couldn't load the doubts." onRetry={() => void feed.refetch()} />}
      {feed.isSuccess && items.length === 0 && (
        <EmptyState
          icon={MessagesSquare}
          title={hasFilters ? 'No doubts match these filters' : 'No doubts yet'}
          body={hasFilters ? 'Try a different subject, topic or search.' : 'Be the first to ask on your campus.'}
          action={hasFilters ? <ClearFilters onClear={() => onChange({})} /> : <ButtonLink to="/app/ask">Ask a doubt</ButtonLink>}
        />
      )}
      {items.length > 0 && (
        <div className="flex flex-col gap-3">
          {items.map((d) => (
            <DoubtCard key={d.id} doubt={d} />
          ))}
        </div>
      )}
      {feed.hasNextPage && (
        <div className="flex justify-center">
          <Button variant="secondary" onClick={() => void feed.fetchNextPage()} loading={feed.isFetchingNextPage}>
            Load more
          </Button>
        </div>
      )}
    </div>
  )
}
