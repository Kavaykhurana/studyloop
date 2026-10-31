import { Lightbulb, Radar, RefreshCcw, Users, type LucideIcon } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { Logo } from '../components/layout/Logo'
import { Button, ButtonLink } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Skeleton } from '../components/ui/Skeleton'
import { usePublicStats } from '../hooks/usePublicStats'
import { useAuth, type Persona } from '../lib/auth'
import { percent } from '../lib/format'
import { usePageTitle } from '../lib/usePageTitle'

const STEPS: { icon: LucideIcon; title: string; body: string }[] = [
  { icon: Radar, title: 'Duplicate radar', body: 'See if a senior already solved it before you post.' },
  { icon: Lightbulb, title: 'Hint ladder', body: 'Nudge, approach, first step, full solution. You choose when.' },
  {
    icon: Users,
    title: 'Smart routing',
    body: "Matched to peers by what they've actually solved, with the reasons shown.",
  },
  {
    icon: RefreshCcw,
    title: 'Knowledge that compounds',
    body: "Every accepted answer makes the next student's hints better.",
  },
]

function StatsStrip() {
  const stats = usePublicStats()
  if (stats.isError) return null
  if (stats.isPending) {
    return (
      <div className="mt-10 grid gap-3 sm:grid-cols-3" aria-hidden="true">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-20" />
        ))}
      </div>
    )
  }
  const s = stats.data
  const tiles = [
    { value: String(s.doubtsSolved), label: 'doubts solved' },
    { value: percent(s.selfServeRate), label: 'solved without waiting for a peer' },
    { value: s.medianFirstAnswerMinutes === null ? '—' : String(Math.round(s.medianFirstAnswerMinutes)), label: 'min median time to first answer' },
  ]
  return (
    <div className="mt-10">
      <div className="grid gap-3 sm:grid-cols-3">
        {tiles.map((t) => (
          <Card key={t.label} className="!p-4">
            <p className="font-mono text-[28px] font-medium leading-9">{t.value}</p>
            <p className="text-[13px] text-ink-2">{t.label}</p>
          </Card>
        ))}
      </div>
      {s.isSeededDemo && <p className="mt-2 text-[12px] text-ink-3">Demo campus data (seeded)</p>}
    </div>
  )
}

export function Landing() {
  usePageTitle()
  const { status, demo } = useAuth()
  const navigate = useNavigate()
  const [busy, setBusy] = useState<Persona | null>(null)
  const github = import.meta.env.VITE_GITHUB_URL as string | undefined

  const start = async (persona: Persona) => {
    setBusy(persona)
    try {
      await demo(persona)
      navigate('/app')
    } catch {
      toast.error('Demo is waking up — try again in a few seconds.')
    } finally {
      setBusy(null)
    }
  }

  return (
    <div className="min-h-dvh">
      <header className="container-page flex h-16 items-center justify-between">
        <Logo />
        {status === 'authed' ? (
          <ButtonLink to="/app" variant="secondary" size="sm">
            Open StudyLoop
          </ButtonLink>
        ) : (
          <Link to="/login" className="text-[14px] font-medium text-ink-2 hover:text-ink">
            Sign in
          </Link>
        )}
      </header>

      <main className="container-page pb-16 pt-10 sm:pt-16">
        <p className="t-micro text-brand">NexHack 3.0 · Education &amp; Student Innovation</p>
        <h1 className="t-display mt-3 max-w-3xl">Stuck at 11 PM? Your campus already knows the answer.</h1>
        <p className="mt-4 max-w-[620px] text-[17px] leading-7 text-ink-2">
          StudyLoop checks if your doubt is already solved, gives you hints before answers, and connects you to the senior
          who has solved problems like yours.
        </p>
        <div className="mt-8 flex flex-wrap items-center gap-3">
          <Button size="lg" onClick={() => void start('learner')} loading={busy === 'learner'} disabled={busy !== null}>
            Try as a learner
          </Button>
          <Button
            size="lg"
            variant="secondary"
            onClick={() => void start('helper')}
            loading={busy === 'helper'}
            disabled={busy !== null}
          >
            Try as a helper
          </Button>
        </div>
        <p className="mt-3 text-[13px] text-ink-2">
          or{' '}
          <Link to="/signup" className="font-medium text-brand underline underline-offset-2">
            create your own account
          </Link>
        </p>

        <StatsStrip />

        <section aria-labelledby="loop-heading" className="mt-16">
          <h2 id="loop-heading" className="t-h2 mb-4">
            How the loop works
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {STEPS.map(({ icon: Icon, title, body }) => (
              <Card key={title} className="flex flex-col gap-2">
                <span className="inline-flex size-10 items-center justify-center rounded-input bg-brand-soft text-brand">
                  <Icon className="size-5" aria-hidden="true" />
                </span>
                <h3 className="t-h3">{title}</h3>
                <p className="text-[14px] leading-5 text-ink-2">{body}</p>
              </Card>
            ))}
          </div>
        </section>
      </main>

      <footer className="container-page pb-10 text-[13px] text-ink-3">
        Built in 48 hours for NexHack 3.0 · Java Spring Boot · React · pgvector · Gemini
        {github ? (
          <>
            {' · '}
            <a href={github} className="underline underline-offset-2" target="_blank" rel="noreferrer noopener">
              GitHub
            </a>
          </>
        ) : null}
      </footer>
    </div>
  )
}
