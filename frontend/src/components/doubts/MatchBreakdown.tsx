import type { MatchBreakdownValues } from '../../lib/types'

const ROWS: { key: keyof MatchBreakdownValues; label: string; weight: string }[] = [
  { key: 'semantic', label: 'Similar doubts solved', weight: '40%' },
  { key: 'declared', label: 'Self-rated strength', weight: '20%' },
  { key: 'reputation', label: 'Subject reputation', weight: '20%' },
  { key: 'recency', label: 'Recent activity', weight: '20%' },
  { key: 'load', label: 'Current load', weight: '−15' },
]

export function MatchBreakdown({ values }: { values: MatchBreakdownValues }) {
  return (
    <ul className="mt-2 flex flex-col gap-2 rounded-input bg-surface-2 p-3">
      {ROWS.map((row) => {
        const value = values[row.key]
        const negative = row.key === 'load'
        return (
          <li key={row.key} className="text-[12px]">
            <div className="mb-1 flex justify-between text-ink-2">
              <span>
                {row.label} <span className="text-ink-3">{row.weight}</span>
              </span>
              <span className="font-mono">{negative && value > 0 ? '−' : ''}{value.toFixed(2)}</span>
            </div>
            <div className="h-1.5 overflow-hidden rounded-full bg-code" aria-hidden="true">
              <div
                className={negative ? 'h-full bg-danger' : 'h-full bg-brand'}
                style={{ width: `${Math.round(Math.max(0, Math.min(1, value)) * 100)}%` }}
              />
            </div>
          </li>
        )
      })}
    </ul>
  )
}
