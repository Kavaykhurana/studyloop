import { Clock, Layers, Sparkles, Star, Trophy } from 'lucide-react'
import { useId, useState } from 'react'
import { Link } from 'react-router'
import { yearBranch } from '../../lib/format'
import type { Match } from '../../lib/types'
import { Avatar } from '../ui/Avatar'
import { MatchBreakdown } from './MatchBreakdown'

function reasonIcon(reason: string) {
  if (reason.startsWith('Solved')) return Sparkles
  if (reason.startsWith('Rates')) return Star
  if (reason.includes('reputation')) return Trophy
  if (reason.startsWith('Active')) return Clock
  return Layers
}

export function MatchCard({ match }: { match: Match }) {
  const [open, setOpen] = useState(false)
  const panelId = useId()
  return (
    <div className="rounded-input border border-line bg-surface p-3">
      <div className="flex items-center gap-3">
        <Avatar name={match.helper.name} id={match.helper.id} />
        <div className="min-w-0 flex-1">
          <Link to={`/app/profile/${match.helper.id}`} className="block truncate text-[14px] font-semibold text-ink hover:underline">
            {match.helper.name}
          </Link>
          <p className="text-[12px] text-ink-3">{yearBranch(match.helper.yearOfStudy, match.helper.branch)}</p>
        </div>
        <span
          className="rounded-full bg-brand-soft px-2 font-mono text-[13px] font-medium leading-6 text-brand"
          aria-label={`Match score ${match.score}`}
        >
          {match.score}
        </span>
      </div>
      {match.reasons.length > 0 && (
        <ul className="mt-2 flex flex-col gap-1">
          {match.reasons.slice(0, 3).map((reason) => {
            const Icon = reasonIcon(reason)
            return (
              <li key={reason} className="flex items-start gap-2 text-[13px] leading-5 text-ink-2">
                <Icon className="mt-0.5 size-3.5 shrink-0 text-brand" aria-hidden="true" />
                {reason}
              </li>
            )
          })}
        </ul>
      )}
      <button
        type="button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((v) => !v)}
        className="mt-2 text-[13px] font-medium text-brand hover:underline"
      >
        Why this match?
      </button>
      <div id={panelId} hidden={!open}>
        {open && <MatchBreakdown values={match.breakdown} />}
      </div>
    </div>
  )
}
