import { zodResolver } from '@hookform/resolvers/zod'
import { addDays } from 'date-fns'
import { Minus, Plus } from 'lucide-react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { useSubjects } from '../../hooks/useSubjects'
import type { CreatePlanInput } from '../../hooks/usePlan'
import { applyFieldErrors } from '../../lib/api'
import { isoDate } from '../../lib/format'
import type { UserMe } from '../../lib/types'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { Chip } from '../ui/Chip'
import { Input } from '../ui/Input'

const schema = z.object({
  examName: z.string().trim().min(3, 'must be at least 3 characters').max(100, 'must be at most 100 characters'),
  examDate: z.string().min(1, 'Pick the exam date.'),
  hoursPerDay: z.number().min(0.5).max(12),
  subjectIds: z.array(z.number()).min(1, 'Pick at least one subject.').max(4, 'Pick at most four subjects.'),
})
type Values = z.infer<typeof schema>

export function PlanForm({
  user,
  loading,
  onSubmit,
}: {
  user: UserMe
  loading: boolean
  onSubmit: (input: CreatePlanInput, onServerError: (e: unknown) => void) => void
}) {
  const subjects = useSubjects()
  const today = new Date()
  const defaults = user.subjects.filter((s) => s.kind === 'NEEDS_HELP').map((s) => s.subjectId)
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { examName: '', examDate: '', hoursPerDay: 2, subjectIds: defaults.slice(0, 4) },
  })

  return (
    <Card>
      <form
        className="flex flex-col gap-5"
        noValidate
        onSubmit={handleSubmit((values) =>
          onSubmit(values, (e) => applyFieldErrors(e, setError, ['examName', 'examDate', 'hoursPerDay', 'subjectIds'])),
        )}
      >
        <Input label="Exam name" placeholder="DBMS Mid-Sem" error={errors.examName?.message} {...register('examName')} />
        <Input
          label="Exam date"
          type="date"
          min={isoDate(addDays(today, 1))}
          max={isoDate(addDays(today, 60))}
          error={errors.examDate?.message}
          {...register('examDate')}
        />
        <Controller
          control={control}
          name="hoursPerDay"
          render={({ field }) => (
            <div className="flex flex-col gap-1">
              <span id="hours-label" className="text-[13px] font-medium text-ink-2">
                Hours per day
              </span>
              <div className="flex items-center gap-3" role="group" aria-labelledby="hours-label">
                <Button
                  variant="secondary"
                  aria-label="Fewer hours"
                  onClick={() => field.onChange(Math.max(0.5, field.value - 0.5))}
                >
                  <Minus className="size-4" aria-hidden="true" />
                </Button>
                <output className="w-14 text-center font-mono text-[18px]" aria-live="polite">
                  {field.value.toFixed(1)}
                </output>
                <Button
                  variant="secondary"
                  aria-label="More hours"
                  onClick={() => field.onChange(Math.min(12, field.value + 0.5))}
                >
                  <Plus className="size-4" aria-hidden="true" />
                </Button>
              </div>
            </div>
          )}
        />
        <Controller
          control={control}
          name="subjectIds"
          render={({ field }) => (
            <fieldset className="flex flex-col gap-2">
              <legend className="text-[13px] font-medium text-ink-2">Subjects (1 to 4)</legend>
              <div className="flex flex-wrap gap-2">
                {subjects.data?.map((s) => {
                  const selected = field.value.includes(s.id)
                  return (
                    <Chip
                      key={s.id}
                      selected={selected}
                      onClick={() =>
                        field.onChange(selected ? field.value.filter((id) => id !== s.id) : [...field.value, s.id])
                      }
                    >
                      <span className="size-2 rounded-full" style={{ backgroundColor: s.colorHex }} aria-hidden="true" />
                      {s.code}
                    </Chip>
                  )
                })}
              </div>
              {errors.subjectIds?.message && (
                <p role="alert" className="text-[13px] text-danger">
                  {errors.subjectIds.message}
                </p>
              )}
            </fieldset>
          )}
        />
        <Button type="submit" size="lg" loading={loading}>
          {loading ? 'Building your plan…' : 'Build my plan'}
        </Button>
      </form>
    </Card>
  )
}
