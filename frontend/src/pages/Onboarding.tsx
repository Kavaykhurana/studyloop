import { useState } from 'react'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Logo } from '../components/layout/Logo'
import { ErrorState } from '../components/ui/ErrorState'
import { Skeleton } from '../components/ui/Skeleton'
import { Segmented } from '../components/ui/Tabs'
import { useUpdateSubjects } from '../hooks/useProfile'
import { useSubjects } from '../hooks/useSubjects'
import { errorMessage } from '../lib/api'
import { useUser } from '../lib/auth'
import { cn } from '../lib/cn'
import type { SubjectKind } from '../lib/types'
import { usePageTitle } from '../lib/usePageTitle'

type Choice = SubjectKind | 'SKIP'
interface Row {
  kind: Choice
  level: number
}

export function Onboarding() {
  usePageTitle('Your subjects')
  const user = useUser()
  const subjects = useSubjects()
  const update = useUpdateSubjects()
  const navigate = useNavigate()
  const [rows, setRows] = useState<Record<number, Row>>(() =>
    Object.fromEntries(user.subjects.map((s) => [s.subjectId, { kind: s.kind, level: s.level }])),
  )
  const [error, setError] = useState<string | null>(null)

  const set = (id: number, patch: Partial<Row>) =>
    setRows((r) => ({ ...r, [id]: { kind: r[id]?.kind ?? 'SKIP', level: r[id]?.level ?? 3, ...patch } }))

  const submit = () => {
    const chosen = Object.entries(rows).filter(([, r]) => r.kind !== 'SKIP')
    if (chosen.length === 0) {
      setError('Pick at least one subject.')
      return
    }
    setError(null)
    update.mutate(
      chosen.map(([id, r]) => ({ subjectId: Number(id), kind: r.kind as SubjectKind, level: r.level })),
      {
        onSuccess: () => navigate('/app', { replace: true }),
        onError: (e) => toast.error(errorMessage(e)),
      },
    )
  }

  return (
    <div className="container-page max-w-3xl py-8">
      <Logo to="/app" />
      <h1 className="t-h1 mt-8">What are you strong at, and where do you need help?</h1>
      <p className="mt-2 text-ink-2">
        We use this to route doubts to the right seniors and to find the right help for you.
      </p>
      <div className="mt-6 flex flex-col gap-3">
        {subjects.isPending && [0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-24" />)}
        {subjects.isError && <ErrorState message="We couldn't load the subjects." onRetry={() => void subjects.refetch()} />}
        {subjects.data?.map((s) => {
          const row = rows[s.id] ?? { kind: 'SKIP' as Choice, level: 3 }
          return (
            <Card key={s.id} className="flex flex-col gap-3">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="size-2.5 rounded-full" style={{ backgroundColor: s.colorHex }} aria-hidden="true" />
                  <span className="font-medium">{s.name}</span>
                </div>
                <Segmented<Choice>
                  label={`${s.name} role`}
                  value={row.kind}
                  onChange={(kind) => set(s.id, { kind })}
                  items={[
                    { id: 'STRONG', label: 'Strong' },
                    { id: 'NEEDS_HELP', label: 'Need help' },
                    { id: 'SKIP', label: 'Skip' },
                  ]}
                />
              </div>
              {row.kind !== 'SKIP' && (
                <div>
                  <p className="mb-1 text-[13px] text-ink-2">How confident? 1 = lost, 5 = can teach it</p>
                  <div className="flex gap-2" role="radiogroup" aria-label={`${s.name} confidence`}>
                    {[1, 2, 3, 4, 5].map((level) => (
                      <button
                        key={level}
                        type="button"
                        role="radio"
                        aria-checked={row.level === level}
                        onClick={() => set(s.id, { level })}
                        className={cn(
                          'size-10 rounded-full border text-[14px] font-medium transition-colors',
                          row.level === level
                            ? 'border-brand bg-brand text-white'
                            : 'border-line bg-surface text-ink-2 hover:bg-surface-2',
                        )}
                      >
                        {level}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </Card>
          )
        })}
      </div>
      {error && (
        <p role="alert" className="mt-3 text-[14px] text-danger">
          {error}
        </p>
      )}
      <div className="mt-6">
        <Button size="lg" onClick={submit} loading={update.isPending}>
          Save and continue
        </Button>
      </div>
    </div>
  )
}
