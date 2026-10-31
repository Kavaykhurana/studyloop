import { useState } from 'react'
import { useParams } from 'react-router'
import { toast } from 'sonner'
import { DoubtCard } from '../components/doubts/DoubtCard'
import { Avatar } from '../components/ui/Avatar'
import { Badge } from '../components/ui/Badge'
import { Button, ButtonLink } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/ErrorState'
import { Input } from '../components/ui/Input'
import { Modal } from '../components/ui/Modal'
import { Select } from '../components/ui/Select'
import { Skeleton } from '../components/ui/Skeleton'
import { Textarea } from '../components/ui/Textarea'
import { useProfile, useUpdateProfile } from '../hooks/useProfile'
import { errorCode, errorMessage } from '../lib/api'
import { useUser } from '../lib/auth'
import { yearBranch } from '../lib/format'
import type { UserMe } from '../lib/types'
import { usePageTitle } from '../lib/usePageTitle'

function EditProfile({ me, open, onClose }: { me: UserMe; open: boolean; onClose: () => void }) {
  const update = useUpdateProfile()
  const [name, setName] = useState(me.name)
  const [year, setYear] = useState(me.yearOfStudy ? String(me.yearOfStudy) : '')
  const [branch, setBranch] = useState(me.branch ?? '')
  const [bio, setBio] = useState(me.bio ?? '')
  const [error, setError] = useState<string | null>(null)

  const save = () => {
    setError(null)
    update.mutate(
      { name: name.trim(), yearOfStudy: year ? Number(year) : null, branch: branch.trim(), bio: bio.trim() },
      {
        onSuccess: () => {
          toast.success('Profile updated.')
          onClose()
        },
        onError: (e) => setError(errorMessage(e)),
      },
    )
  }

  return (
    <Modal
      open={open}
      title="Edit profile"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={save} loading={update.isPending}>
            Save
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} maxLength={80} />
        <Select label="Year of study" value={year} onChange={(e) => setYear(e.target.value)}>
          <option value="">Not set</option>
          {[1, 2, 3, 4, 5].map((y) => (
            <option key={y} value={y}>
              Year {y}
            </option>
          ))}
        </Select>
        <Input label="Branch" value={branch} onChange={(e) => setBranch(e.target.value)} maxLength={40} />
        <Textarea label="Bio" value={bio} onChange={(e) => setBio(e.target.value)} maxLength={280} className="min-h-[96px]" />
        {error && (
          <p role="alert" className="text-[13px] text-danger">
            {error}
          </p>
        )}
      </div>
    </Modal>
  )
}

export function Profile() {
  const { id: raw } = useParams()
  const id = Number(raw)
  const me = useUser()
  const profile = useProfile(id)
  const [editing, setEditing] = useState(false)
  usePageTitle(profile.data?.user.name ?? 'Profile')

  if (profile.isPending) return <Skeleton className="h-72" />
  if (profile.isError) {
    return (
      <ErrorState
        message={errorCode(profile.error) === 'NOT_FOUND' ? "We couldn't find that profile." : errorMessage(profile.error)}
        onRetry={errorCode(profile.error) === 'NOT_FOUND' ? undefined : () => void profile.refetch()}
      />
    )
  }

  const p = profile.data
  const own = me.id === p.user.id
  return (
    <div className="flex flex-col gap-6">
      <Card className="flex flex-col gap-4 sm:flex-row sm:items-start">
        <Avatar name={p.user.name} id={p.user.id} size="xl" />
        <div className="min-w-0 flex-1">
          <h1 className="t-h1">{p.user.name}</h1>
          <p className="text-ink-2">{yearBranch(p.user.yearOfStudy, p.user.branch)}</p>
          {p.bio && <p className="mt-2 text-ink-2">{p.bio}</p>}
          <p className="mt-2 font-mono text-[18px]">{p.user.reputation} reputation</p>
        </div>
        {own && (
          <div className="flex flex-wrap gap-2">
            <span title={me.isDemo ? 'Demo accounts cannot edit their profile' : undefined}>
              <Button variant="secondary" disabled={me.isDemo} onClick={() => setEditing(true)}>
                Edit profile
              </Button>
            </span>
            <ButtonLink to="/onboarding" variant="secondary">
              Edit subjects
            </ButtonLink>
          </div>
        )}
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        <Card className="flex flex-col gap-3">
          <h2 className="t-h3">Strong subjects</h2>
          {p.strongSubjects.length === 0 ? (
            <p className="text-ink-2">No strong subjects listed.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {p.strongSubjects.map((s) => (
                <Badge key={s.code} variant="brand">
                  {s.code} · {s.level}/5
                </Badge>
              ))}
            </div>
          )}
          <h2 className="t-h3 mt-2">Subject reputation</h2>
          {p.subjectReputation.length === 0 ? (
            <p className="text-ink-2">No reputation earned yet.</p>
          ) : (
            <div className="flex flex-wrap gap-2">
              {p.subjectReputation.map((s) => (
                <Badge key={s.code} variant="success">
                  {s.code} · {s.reputation}
                </Badge>
              ))}
            </div>
          )}
        </Card>
        <Card className="grid grid-cols-3 gap-3">
          <div>
            <p className="font-mono text-[24px]">{p.stats.answers}</p>
            <p className="text-[13px] text-ink-2">Answers</p>
          </div>
          <div>
            <p className="font-mono text-[24px]">{p.stats.accepted}</p>
            <p className="text-[13px] text-ink-2">Accepted</p>
          </div>
          <div>
            <p className="font-mono text-[24px]">{p.stats.avgHelpful === null ? '—' : p.stats.avgHelpful.toFixed(1)}</p>
            <p className="text-[13px] text-ink-2">Avg helpful rating</p>
          </div>
        </Card>
      </div>

      <section className="flex flex-col gap-3" aria-labelledby="recent">
        <h2 id="recent" className="t-h2">
          Recent accepted answers
        </h2>
        {p.recentAccepted.length === 0 ? (
          <Card className="text-ink-2">No accepted answers yet.</Card>
        ) : (
          p.recentAccepted.map((d) => <DoubtCard key={d.id} doubt={d} />)
        )}
      </section>

      {own && editing && <EditProfile me={me} open onClose={() => setEditing(false)} />}
    </div>
  )
}
