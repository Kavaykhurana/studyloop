import { Lock, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { toast } from 'sonner'
import { useNextHint } from '../../hooks/useHints'
import { useResolveByHint } from '../../hooks/useDoubt'
import { errorCode, errorMessage } from '../../lib/api'
import { cn } from '../../lib/cn'
import type { DoubtDetail, Hint } from '../../lib/types'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Modal } from '../ui/Modal'
import { Markdown } from './Markdown'

const LABELS = ['Nudge', 'Approach', 'First step', 'Full solution']

function HintRung({ hint }: { hint: Hint }) {
  return (
    <div className="rounded-r-input border-l-[3px] border-hint-line bg-hint-soft p-4">
      <div className="mb-2 flex flex-wrap items-center gap-2">
        <span className="text-[13px] font-semibold text-hint">
          {hint.level} · {LABELS[hint.level - 1]}
        </span>
        {hint.confidence === 'MEDIUM' && <Badge variant="hint">Assumption made</Badge>}
        {hint.confidence === 'LOW' && <Badge variant="hint">Needs clarification</Badge>}
      </div>
      <Markdown>{hint.contentMd}</Markdown>
      {hint.citedDoubts.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-2">
          {hint.citedDoubts.map((c) => (
            <Link
              key={c.id}
              to={`/app/doubts/${c.id}`}
              title={c.title}
              className="inline-flex items-center rounded-full border border-hint-line bg-surface px-2 text-[12px] font-semibold leading-5 text-hint hover:bg-hint-soft"
            >
              From #{c.id}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

function RungSkeleton() {
  return (
    <div className="rounded-r-input border-l-[3px] border-hint-line bg-hint-soft p-4" aria-live="polite">
      <p className="mb-3 text-[13px] text-hint">Thinking through your doubt…</p>
      <div className="skeleton mb-2 h-4 w-full" />
      <div className="skeleton mb-2 h-4 w-11/12" />
      <div className="skeleton h-4 w-2/3" />
    </div>
  )
}

export function HintLadder({ doubt }: { doubt: DoubtDetail }) {
  const next = useNextHint(doubt.id)
  const resolve = useResolveByHint(doubt.id)
  const [confirming, setConfirming] = useState(false)
  const [busyMessage, setBusyMessage] = useState<string | null>(null)
  const unlocked = doubt.hints.levelUnlocked
  const target = unlocked + 1

  const unlock = () => {
    setBusyMessage(null)
    next.mutate(undefined, {
      onError: (e) => {
        if (errorCode(e) === 'AI_UNAVAILABLE') setBusyMessage(errorMessage(e))
        else toast.error(errorMessage(e))
      },
    })
  }

  const rungs = [1, 2, 3, 4].map((level) => {
    const hint = doubt.hints.items.find((h) => h.level === level)
    if (hint) return <HintRung key={level} hint={hint} />
    if (level === target) {
      if (next.isPending) return <RungSkeleton key={level} />
      return (
        <div key={level} className="flex flex-col items-start gap-2 rounded-input border border-line bg-surface-2 p-4">
          {level === 4 && (
            <p className="text-[13px] text-ink-2">
              Tried the earlier hints? The full solution is most useful after you've attempted step 3.
            </p>
          )}
          {busyMessage && (
            <p role="alert" className="rounded-input bg-danger-soft px-3 py-2 text-[13px] text-danger">
              {busyMessage}
            </p>
          )}
          <Button variant="secondary" onClick={unlock}>
            {busyMessage ? 'Try again' : level === 4 ? 'Show full solution' : `Unlock hint ${level}`}
          </Button>
        </div>
      )
    }
    return (
      <div
        key={level}
        className={cn('flex items-center gap-2 rounded-input border border-dashed border-line px-4 py-3 text-ink-3')}
      >
        <Lock className="size-4" aria-hidden="true" />
        <span className="text-[13px]">
          {level} · {LABELS[level - 1]}
        </span>
      </div>
    )
  })

  return (
    <Card className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="t-h3">Hint ladder</h2>
        <Badge variant="ai">
          <Sparkles className="size-3" aria-hidden="true" /> AI · grounded in campus answers
        </Badge>
      </div>
      {rungs}
      {doubt.permissions.canResolveByHint && (
        <div>
          <Button variant="ghost" onClick={() => setConfirming(true)}>
            Hints solved it — close my doubt
          </Button>
        </div>
      )}
      <Modal
        open={confirming}
        title="Mark this doubt as solved with hints?"
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              Cancel
            </Button>
            <Button
              loading={resolve.isPending}
              onClick={() =>
                resolve.mutate(undefined, {
                  onSuccess: () => {
                    setConfirming(false)
                    toast.success('Marked as solved with hints.')
                  },
                  onError: (e) => {
                    setConfirming(false)
                    toast.error(errorMessage(e))
                  },
                })
              }
            >
              Mark solved
            </Button>
          </>
        }
      >
        Seniors won't be asked to answer it anymore.
      </Modal>
    </Card>
  )
}
