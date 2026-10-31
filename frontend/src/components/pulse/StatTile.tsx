import { Card } from '../ui/Card'

export function StatTile({ label, value, note }: { label: string; value: string; note?: string }) {
  return (
    <Card className="flex flex-col gap-1 !p-4">
      <p className="font-mono text-[28px] font-medium leading-9 text-ink">{value}</p>
      <p className="text-[13px] leading-5 text-ink-2">{label}</p>
      {note && <p className="text-[12px] text-ink-3">{note}</p>}
    </Card>
  )
}

export function StatTileSkeleton() {
  return (
    <Card className="flex flex-col gap-2 !p-4" aria-hidden="true">
      <div className="skeleton h-8 w-20" />
      <div className="skeleton h-4 w-32" />
    </Card>
  )
}
