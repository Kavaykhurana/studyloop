import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { chartDate } from '../../lib/format'
import type { Pulse } from '../../lib/types'

export function DailyChart({ daily }: { daily: Pulse['daily'] }) {
  const data = daily.map((d) => ({ ...d, label: chartDate(d.date) }))
  return (
    <div className="h-[240px] w-full" role="img" aria-label="Doubts posted and solved per day">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e3dc" />
          <XAxis dataKey="label" tick={{ fontSize: 12, fill: '#858b96' }} interval="preserveStartEnd" />
          <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#858b96' }} />
          <Tooltip cursor={{ fill: '#f3f2ee' }} />
          <Legend wrapperStyle={{ fontSize: 12 }} />
          <Bar dataKey="posted" name="Posted" fill="#c7cbf5" radius={[4, 4, 0, 0]} isAnimationActive={false} />
          <Bar dataKey="solved" name="Solved" fill="#4338ca" radius={[4, 4, 0, 0]} isAnimationActive={false} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
