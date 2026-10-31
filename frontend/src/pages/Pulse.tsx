import { useState } from 'react'
import { ConfusionMap } from '../components/pulse/ConfusionMap'
import { DailyChart } from '../components/pulse/DailyChart'
import { StatTile, StatTileSkeleton } from '../components/pulse/StatTile'
import { TopHelpers } from '../components/pulse/TopHelpers'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/ErrorState'
import { Skeleton } from '../components/ui/Skeleton'
import { Segmented } from '../components/ui/Tabs'
import { usePulse } from '../hooks/usePulse'
import { percent } from '../lib/format'
import { usePageTitle } from '../lib/usePageTitle'

type Range = 7 | 14 | 30

export function Pulse() {
  usePageTitle('Campus Pulse')
  const [days, setDays] = useState<Range>(7)
  const pulse = usePulse(days)

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="t-h1">Campus Pulse{pulse.data ? ` · ${pulse.data.campus}` : ''}</h1>
        <Segmented<Range>
          label="Time range"
          value={days}
          onChange={setDays}
          items={[
            { id: 7, label: '7 days' },
            { id: 14, label: '14 days' },
            { id: 30, label: '30 days' },
          ]}
        />
      </div>

      {pulse.isPending && (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <StatTileSkeleton key={i} />
            ))}
          </div>
          <Skeleton className="h-64" />
        </>
      )}
      {pulse.isError && <ErrorState message="We couldn't load Campus Pulse." onRetry={() => void pulse.refetch()} />}

      {pulse.isSuccess && (
        <>
          {pulse.data.isSeededDemo && (
            <p className="rounded-input bg-hint-soft px-3 py-2 text-[13px] text-hint">
              This demo campus runs on seeded data so you can see the dashboard working.
            </p>
          )}
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
            <StatTile label="Doubts posted" value={String(pulse.data.totals.doubtsPosted)} />
            <StatTile label="Solved rate" value={percent(pulse.data.totals.solvedRate)} />
            <StatTile label="Solved without waiting for a peer" value={percent(pulse.data.totals.selfServeRate)} />
            <StatTile
              label="Median time to first answer"
              value={
                pulse.data.totals.medianFirstAnswerMinutes === null
                  ? '—'
                  : `${Math.round(pulse.data.totals.medianFirstAnswerMinutes)} min`
              }
            />
            <StatTile label="Duplicates deflected" value={String(pulse.data.totals.deflections)} />
            <StatTile label="Active helpers" value={String(pulse.data.totals.activeHelpers)} />
          </div>

          <Card className="flex flex-col gap-3">
            <h2 className="t-h3">Doubts per day</h2>
            <DailyChart daily={pulse.data.daily} />
          </Card>

          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="flex flex-col gap-3 lg:col-span-2">
              <h2 className="t-h3">Confusion map</h2>
              {pulse.data.confusionMap.length === 0 ? (
                <p className="text-ink-2">No doubts posted in this period yet.</p>
              ) : (
                <ConfusionMap map={pulse.data.confusionMap} />
              )}
            </Card>
            <div className="flex flex-col gap-6">
              <Card className="flex flex-col gap-3">
                <h2 className="t-h3">Top helpers</h2>
                <TopHelpers helpers={pulse.data.topHelpers} />
              </Card>
              <Card className="flex flex-col gap-1">
                <h2 className="t-h3">AI health</h2>
                <p className="font-mono text-[13px] text-ink-2">
                  {pulse.data.ai.calls === 0
                    ? 'No AI calls in this window yet'
                    : `${pulse.data.ai.calls} AI calls · ${Math.round(pulse.data.ai.successRate * 100)}% success · p50 ${pulse.data.ai.p50LatencyMs ?? '—'} ms`}
                </p>
              </Card>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
