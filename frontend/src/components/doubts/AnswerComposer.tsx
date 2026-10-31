import { useState } from 'react'
import { toast } from 'sonner'
import { usePostAnswer } from '../../hooks/useDoubt'
import { applyFieldErrors, errorMessage } from '../../lib/api'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Tabs } from '../ui/Tabs'
import { Textarea } from '../ui/Textarea'
import { Markdown } from './Markdown'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'

const schema = z.object({
  bodyMd: z.string().trim().min(20, 'must be at least 20 characters').max(10000, 'must be at most 10000 characters'),
})
type Values = z.infer<typeof schema>

export function AnswerComposer({ doubtId }: { doubtId: number }) {
  const post = usePostAnswer(doubtId)
  const [tab, setTab] = useState<'write' | 'preview'>('write')
  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors },
  } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { bodyMd: '' } })
  const body = useWatch({ control, name: 'bodyMd' })

  const onSubmit = handleSubmit((values) =>
    post.mutate(values.bodyMd, {
      onSuccess: () => {
        reset()
        setTab('write')
        toast.success('Answer posted. The asker has been notified.')
      },
      onError: (e) => {
        if (!applyFieldErrors(e, setError, ['bodyMd'])) toast.error(errorMessage(e))
      },
    }),
  )

  return (
    <Card>
      <form onSubmit={onSubmit} className="flex flex-col gap-3" noValidate>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="t-h3">Your answer</h3>
          <Tabs
            label="Editor mode"
            value={tab}
            onChange={setTab}
            items={[
              { id: 'write', label: 'Write' },
              { id: 'preview', label: 'Preview' },
            ]}
          />
        </div>
        {tab === 'write' ? (
          <Textarea
            aria-label="Your answer"
            placeholder="Explain the why, not just the what."
            error={errors.bodyMd?.message}
            helper="Explain the why, not just the what. Code blocks welcome."
            {...register('bodyMd')}
          />
        ) : (
          <div className="min-h-[140px] rounded-input border border-line bg-surface-2 p-3">
            {body.trim() ? <Markdown>{body}</Markdown> : <p className="text-ink-3">Nothing to preview yet.</p>}
          </div>
        )}
        <div className="flex items-center justify-between gap-3">
          <span className="text-[12px] text-ink-3">{body.length} / 10000</span>
          <Button type="submit" loading={post.isPending}>
            Post answer
          </Button>
        </div>
      </form>
    </Card>
  )
}
