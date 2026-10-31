import { CheckCircle2 } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { SimilarDoubtsPanel } from '../components/doubts/SimilarDoubtsPanel'
import { Markdown } from '../components/doubts/Markdown'
import { SnapUploader } from '../components/doubts/SnapUploader'
import { SubjectTag } from '../components/doubts/SubjectTag'
import { Button, ButtonLink } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { Skeleton } from '../components/ui/Skeleton'
import { Tabs } from '../components/ui/Tabs'
import { Textarea } from '../components/ui/Textarea'
import { useCreateDoubt, useSimilar } from '../hooks/useDoubt'
import { useSubjects } from '../hooks/useSubjects'
import { errorMessage } from '../lib/api'
import { cn } from '../lib/cn'
import type { SimilarItem, SnapResult } from '../lib/types'
import { usePageTitle } from '../lib/usePageTitle'

type Step = 1 | 2 | 3
type Mode = 'type' | 'snap'

function StepIndicator({ step }: { step: Step }) {
  const labels = ['Describe', 'Check radar', 'Post']
  return (
    <ol className="flex flex-wrap items-center gap-2 text-[13px]" aria-label="Progress">
      {labels.map((label, i) => {
        const n = i + 1
        return (
          <li
            key={label}
            aria-current={n === step ? 'step' : undefined}
            className={cn(
              'inline-flex items-center gap-2 rounded-full px-3 py-1 font-medium',
              n === step ? 'bg-brand text-white' : n < step ? 'bg-brand-soft text-brand' : 'bg-code text-ink-3',
            )}
          >
            <span className="font-mono">{n}</span> {label}
          </li>
        )
      })}
    </ol>
  )
}

export function Ask() {
  usePageTitle('Ask a doubt')
  const navigate = useNavigate()
  const subjects = useSubjects()
  const similar = useSimilar()
  const create = useCreateDoubt()

  const [step, setStep] = useState<Step>(1)
  const [mode, setMode] = useState<Mode>('type')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [subjectId, setSubjectId] = useState<number | undefined>()
  const [source, setSource] = useState<'TEXT' | 'SNAP'>('TEXT')
  const [preview, setPreview] = useState(false)
  const [transcribed, setTranscribed] = useState(false)
  const [items, setItems] = useState<SimilarItem[]>([])
  const [note, setNote] = useState<string | null>(null)
  const [deflected, setDeflected] = useState(false)
  const [postError, setPostError] = useState<string | null>(null)

  const canContinue = title.trim().length >= 10 && body.trim().length >= 20
  const subject = subjects.data?.find((s) => s.id === subjectId)

  const onSnap = (result: SnapResult) => {
    setTitle(result.title)
    setBody(result.bodyMd)
    if (result.subjectId) setSubjectId(result.subjectId)
    setSource('SNAP')
    setTranscribed(true)
    setMode('type')
  }

  const checkRadar = () => {
    setStep(2)
    setNote(null)
    similar.mutate(
      { title: title.trim(), bodyMd: body.trim(), ...(subjectId ? { subjectId } : {}) },
      {
        onSuccess: (found) => {
          setItems(found)
          if (found.length === 0) {
            setNote('Nothing similar solved yet — you might be the first to ask.')
            setStep(3)
          }
        },
        onError: () => {
          setNote("We couldn't check the radar just now, so you can post directly.")
          setStep(3)
        },
      },
    )
  }

  const post = () => {
    setPostError(null)
    create.mutate(
      { title: title.trim(), bodyMd: body.trim(), ...(subjectId ? { subjectId } : {}), source },
      {
        onSuccess: (doubt) => navigate(`/app/doubts/${doubt.id}?new=1`),
        onError: (e) => setPostError(errorMessage(e)),
      },
    )
  }

  if (deflected) {
    return (
      <Card className="mx-auto mt-6 flex max-w-lg flex-col items-center gap-3 py-10 text-center">
        <CheckCircle2 className="size-12 text-success" aria-hidden="true" />
        <h1 className="t-h2">Glad it helped.</h1>
        <p className="text-ink-2">That's one less doubt in the queue.</p>
        <div className="mt-2 flex flex-wrap justify-center gap-2">
          <ButtonLink to="/app">Back to home</ButtonLink>
          <Button
            variant="secondary"
            onClick={() => {
              setDeflected(false)
              setStep(1)
              setTitle('')
              setBody('')
              setItems([])
              setSource('TEXT')
              setTranscribed(false)
            }}
          >
            Ask something else
          </Button>
        </div>
      </Card>
    )
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-5">
      <div>
        <h1 className="t-h1">Ask a doubt</h1>
        <div className="mt-3">
          <StepIndicator step={step} />
        </div>
      </div>

      {step === 1 && (
        <Card className="flex flex-col gap-4">
          <Tabs<Mode>
            label="How do you want to ask?"
            value={mode}
            onChange={setMode}
            items={[
              { id: 'type', label: 'Type it' },
              { id: 'snap', label: 'Snap it' },
            ]}
          />
          {mode === 'snap' ? (
            <SnapUploader onResult={onSnap} />
          ) : (
            <>
              {transcribed && (
                <p className="rounded-input bg-ai-soft px-3 py-2 text-[14px] text-ai">
                  Transcribed by AI — check it matches your question before posting.
                </p>
              )}
              <Input
                label="Title"
                value={title}
                maxLength={160}
                placeholder="e.g. Why does my BST insert become O(n) on sorted input?"
                helper={`${title.length}/160`}
                onChange={(e) => setTitle(e.target.value)}
              />
              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <span className="text-[12px] text-ink-3">Markdown and code blocks supported</span>
                  <Button variant="ghost" size="sm" onClick={() => setPreview((v) => !v)}>
                    {preview ? 'Edit' : 'Preview'}
                  </Button>
                </div>
                {preview ? (
                  <div className="min-h-[140px] rounded-input border border-line bg-surface-2 p-3">
                    {body.trim() ? <Markdown>{body}</Markdown> : <p className="text-ink-3">Nothing to preview yet.</p>}
                  </div>
                ) : (
                  <Textarea
                    aria-label="Describe your doubt"
                    value={body}
                    maxLength={8000}
                    placeholder="What are you trying to do, what did you try, and where are you stuck?"
                    onChange={(e) => setBody(e.target.value)}
                  />
                )}
              </div>
              <Select
                label="Subject"
                value={subjectId ?? ''}
                onChange={(e) => setSubjectId(e.target.value ? Number(e.target.value) : undefined)}
              >
                <option value="">Let StudyLoop decide</option>
                {subjects.data?.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.code} · {s.name}
                  </option>
                ))}
              </Select>
              <div>
                <Button size="lg" disabled={!canContinue} onClick={checkRadar}>
                  Continue
                </Button>
              </div>
            </>
          )}
        </Card>
      )}

      {step === 2 && similar.isPending && (
        <div className="flex flex-col gap-3" aria-live="polite">
          <p className="text-ink-2">Checking what seniors have already solved…</p>
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-32" />
          ))}
        </div>
      )}

      {step === 2 && !similar.isPending && items.length > 0 && (
        <>
          <SimilarDoubtsPanel items={items} queryText={title} onDeflected={() => setDeflected(true)} />
          <div className="flex flex-wrap gap-2">
            <Button size="lg" onClick={() => setStep(3)}>
              None of these — post my doubt
            </Button>
            <Button size="lg" variant="ghost" onClick={() => setStep(1)}>
              Back
            </Button>
          </div>
        </>
      )}

      {step === 3 && (
        <>
          {note && <p className="rounded-input bg-brand-soft px-3 py-2 text-[14px] text-ink-2">{note}</p>}
          <Card className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center gap-2">
              {subject ? (
                <SubjectTag subject={subject} />
              ) : (
                <span className="text-[12px] text-ink-3">Subject chosen by StudyLoop</span>
              )}
              {source === 'SNAP' && <span className="rounded-full bg-ai-soft px-2 text-[12px] font-semibold text-ai">Snapped</span>}
            </div>
            <h2 className="t-h3">{title}</h2>
            <Markdown>{body}</Markdown>
          </Card>
          {postError && (
            <p role="alert" className="rounded-input bg-danger-soft px-3 py-2 text-[14px] text-danger">
              {postError}
            </p>
          )}
          <div className="flex flex-wrap gap-2">
            <Button size="lg" onClick={post} loading={create.isPending}>
              {create.isPending ? 'Posting… finding seniors' : 'Post doubt'}
            </Button>
            <Button size="lg" variant="ghost" onClick={() => setStep(1)} disabled={create.isPending}>
              Edit
            </Button>
          </div>
          <p className="text-[13px] text-ink-3">
            Your doubt is visible only to students on your campus. <Link to="/app/doubts" className="underline">Browse doubts</Link>
          </p>
        </>
      )}
    </div>
  )
}
