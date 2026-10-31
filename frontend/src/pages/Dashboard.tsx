import { MessageCircleQuestion } from 'lucide-react'
import { Link } from 'react-router'
import { DoubtCard, DoubtCardSkeleton } from '../components/doubts/DoubtCard'
import { ButtonLink } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorState } from '../components/ui/ErrorState'
import { ProgressBar } from '../components/ui/ProgressBar'
import { Skeleton } from '../components/ui/Skeleton'
import { useDoubtList } from '../hooks/useDoubts'
import { useActivePlan, useToggleTask } from '../hooks/usePlan'
import { usePulse } from '../hooks/usePulse'
import { useUser } from '../lib/auth'
import { firstName, greeting, percent, todayIST } from '../lib/format'
import { usePageTitle } from '../lib/usePageTitle'

function DoubtList({ filters, empty }: { filters: { mine?: boolean; matchedToMe?: boolean }; empty: React.ReactNode }) {
  const list = useDoubtList(filters, 5)
  if (list.isPending) return <div className="flex flex-col gap-3">{[0, 1].map((i) => <DoubtCardSkeleton key={i} />)}</div>
  if (list.isError) return <ErrorState message="We couldn't load these doubts." onRetry={() => void list.refetch()} />
  if (list.data.items.length === 0) return <Card>{empty}</Card>
  return (
    <div className="flex flex-col gap-3">
      {list.data.items.map((d) => (
        <DoubtCard key={d.id} doubt={d} />
      ))}
    </div>
  )
}

function TodaysPlan() {
  const plan = useActivePlan()
  const toggle = useToggleTask()
  if (plan.isPending) return <Skeleton className="h-40" />
  if (plan.isError) return <ErrorState message="We couldn't load your plan." onRetry={() => void plan.refetch()} />
  if (!plan.data) {
    return (
      <Card>
        <EmptyState
          icon={MessageCircleQuestion}
          title="No study plan yet"
          body="Tell us your exam date and we'll build a day-by-day plan."
          action={<ButtonLink to="/app/plan">Make a plan</ButtonLink>}
        />
      </Card>
    )
  }
  const today = plan.data.days.find((d) => d.date === todayIST())
  return (
    <Card className="flex flex-col gap-3">
      <div>
        <h2 className="t-h3">Today's plan</h2>
        <p className="text-[13px] text-ink-2">{plan.data.examName}</p>
      </div>
      <ProgressBar value={plan.data.progress.done} max={plan.data.progress.total} label="Plan progress" />
      <p className="text-[12px] text-ink-3">
        {plan.data.progress.done}/{plan.data.progress.total} tasks done
      </p>
      {today ? (
        <ul className="flex flex-col gap-2">
          {today.tasks.map((t) => (
            <li key={t.id} className="flex items-start gap-2 text-[14px] leading-5">
              <input
                type="checkbox"
                checked={t.done}
                onChange={(e) => toggle.mutate({ taskId: t.id, done: e.target.checked })}
                aria-label={`Mark done: ${t.title}`}
                className="mt-0.5 size-5 shrink-0 accent-[#4338ca]"
              />
              <span className={t.done ? 'text-ink-3 line-through' : 'text-ink'}>{t.title}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-[14px] text-ink-2">Nothing scheduled for today.</p>
      )}
      <Link to="/app/plan" className="text-[13px] font-medium text-brand">
        Open full plan
      </Link>
    </Card>
  )
}

function CampusThisWeek() {
  const pulse = usePulse(7)
  if (pulse.isPending) return <Skeleton className="h-36" />
  if (pulse.isError) return <ErrorState message="We couldn't load campus numbers." onRetry={() => void pulse.refetch()} />
  const t = pulse.data.totals
  return (
    <Card className="flex flex-col gap-3">
      <h2 className="t-h3">Campus this week</h2>
      <dl className="grid grid-cols-3 gap-2">
        <div>
          <dt className="text-[12px] text-ink-3">Posted</dt>
          <dd className="font-mono text-[20px]">{t.doubtsPosted}</dd>
        </div>
        <div>
          <dt className="text-[12px] text-ink-3">Solved</dt>
          <dd className="font-mono text-[20px]">{percent(t.solvedRate)}</dd>
        </div>
        <div>
          <dt className="text-[12px] text-ink-3">First reply</dt>
          <dd className="font-mono text-[20px]">
            {t.medianFirstAnswerMinutes === null ? '—' : `${Math.round(t.medianFirstAnswerMinutes)}m`}
          </dd>
        </div>
      </dl>
      {pulse.data.isSeededDemo && <p className="text-[12px] text-ink-3">Demo campus data (seeded)</p>}
      <Link to="/app/pulse" className="text-[13px] font-medium text-brand">
        Open Pulse
      </Link>
    </Card>
  )
}

export function Dashboard() {
  usePageTitle('Home')
  const user = useUser()
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="t-h1">
          {greeting()}, {firstName(user.name)}
        </h1>
        <p className="text-ink-2">
          {user.reputation} reputation · {user.campus.shortName}
        </p>
      </div>
      <div className="grid gap-6 lg:grid-cols-3">
        <section className="flex flex-col gap-3 lg:col-span-2 lg:col-start-1 lg:row-start-1" aria-labelledby="mine">
          <h2 id="mine" className="t-h2">
            Your doubts
          </h2>
          <DoubtList
            filters={{ mine: true }}
            empty={
              <EmptyState
                icon={MessageCircleQuestion}
                title="No doubts yet"
                body="When you're stuck, ask — the radar might already have the answer."
                action={<ButtonLink to="/app/ask">Ask a doubt</ButtonLink>}
              />
            }
          />
        </section>
        <div className="lg:col-start-3 lg:row-start-1">
          <TodaysPlan />
        </div>
        <section className="flex flex-col gap-3 lg:col-span-2 lg:col-start-1 lg:row-start-2" aria-labelledby="matched">
          <h2 id="matched" className="t-h2">
            Doubts matched to you
          </h2>
          <DoubtList
            filters={{ matchedToMe: true }}
            empty={
              <EmptyState
                icon={MessageCircleQuestion}
                title="Nothing matched to you right now"
                body="Add your strong subjects so StudyLoop can route doubts to you."
                action={
                  <ButtonLink to="/onboarding" variant="secondary">
                    Edit subjects
                  </ButtonLink>
                }
              />
            }
          />
        </section>
        <div className="lg:col-start-3 lg:row-start-2">
          <CampusThisWeek />
        </div>
      </div>
    </div>
  )
}
