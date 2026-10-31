import { Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import type { DoubtFilters as Filters } from '../../hooks/useDoubts'
import { useSubjects } from '../../hooks/useSubjects'
import { Button } from '../ui/Button'
import { Chip } from '../ui/Chip'
import { Input } from '../ui/Input'
import { Segmented } from '../ui/Tabs'
import { Select } from '../ui/Select'

type StatusChoice = 'ALL' | 'OPEN' | 'SOLVED'

export function DoubtFilters({
  value,
  onChange,
}: {
  value: Filters
  onChange: (next: Filters) => void
}) {
  const subjects = useSubjects()
  const [search, setSearch] = useState(value.q ?? '')

  useEffect(() => {
    const trimmed = search.trim()
    const handle = window.setTimeout(() => {
      const next = trimmed.length >= 2 ? trimmed : undefined
      if (next !== value.q) onChange({ ...value, q: next })
    }, 300)
    return () => window.clearTimeout(handle)
  }, [search, value, onChange])

  const topics = subjects.data?.find((s) => s.id === value.subjectId)?.topics ?? []

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end gap-3">
        <Segmented<StatusChoice>
          label="Status"
          value={value.status ?? 'ALL'}
          onChange={(id) => onChange({ ...value, status: id === 'ALL' ? undefined : id })}
          items={[
            { id: 'ALL', label: 'All' },
            { id: 'OPEN', label: 'Open' },
            { id: 'SOLVED', label: 'Solved' },
          ]}
        />
        <Select
          aria-label="Subject"
          value={value.subjectId ?? ''}
          onChange={(e) =>
            onChange({ ...value, subjectId: e.target.value ? Number(e.target.value) : undefined, topicId: undefined })
          }
          containerClassName="w-44"
        >
          <option value="">All subjects</option>
          {subjects.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.code} · {s.name}
            </option>
          ))}
        </Select>
        <Select
          aria-label="Topic"
          value={value.topicId ?? ''}
          disabled={!value.subjectId}
          onChange={(e) => onChange({ ...value, topicId: e.target.value ? Number(e.target.value) : undefined })}
          containerClassName="w-48"
        >
          <option value="">All topics</option>
          {topics.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </Select>
        <Chip selected={Boolean(value.matchedToMe)} onClick={() => onChange({ ...value, matchedToMe: !value.matchedToMe })}>
          Matched to me
        </Chip>
      </div>
      <Input
        aria-label="Search doubts"
        placeholder="Search doubts (2+ characters)"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        trailing={<Search className="mr-2 size-4 text-ink-3" aria-hidden="true" />}
        maxLength={100}
      />
    </div>
  )
}

export function ClearFilters({ onClear }: { onClear: () => void }) {
  return (
    <Button variant="secondary" onClick={onClear}>
      Clear filters
    </Button>
  )
}
