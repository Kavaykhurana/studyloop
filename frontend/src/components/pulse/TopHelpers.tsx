import { Link } from 'react-router'
import type { Pulse } from '../../lib/types'
import { Avatar } from '../ui/Avatar'

export function TopHelpers({ helpers }: { helpers: Pulse['topHelpers'] }) {
  if (helpers.length === 0) return <p className="text-ink-2">No accepted answers in this period yet.</p>
  return (
    <ol className="flex flex-col gap-1">
      {helpers.map((h, i) => (
        <li key={h.id}>
          <Link
            to={`/app/profile/${h.id}`}
            className="flex items-center gap-3 rounded-input px-2 py-1.5 hover:bg-surface-2"
          >
            <span className="w-4 text-center font-mono text-[13px] text-ink-3">{i + 1}</span>
            <Avatar name={h.name} id={h.id} />
            <span className="min-w-0 flex-1">
              <span className="block truncate text-[14px] font-medium leading-5 text-ink">{h.name}</span>
              <span className="block text-[12px] leading-4 text-ink-3">
                {h.acceptedInPeriod} accepted · <span className="font-mono">{h.reputation}</span> rep
              </span>
            </span>
          </Link>
        </li>
      ))}
    </ol>
  )
}
