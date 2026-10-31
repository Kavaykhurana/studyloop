import { useState } from 'react'
import { toast } from 'sonner'
import { PlanDay } from '../components/plan/PlanDay'
import { PlanForm } from '../components/plan/PlanForm'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { ErrorState } from '../components/ui/ErrorState'
import { Modal } from '../components/ui/Modal'
import { ProgressBar } from '../components/ui/ProgressBar'
import { Skeleton } from '../components/ui/Skeleton'
import { useActivePlan, useCreatePlan, useToggleTask } from '../hooks/usePlan'
import { errorMessage } from '../lib/api'
import { useUser } from '../lib/auth'
import { examDate } from '../lib/format'
import { usePageTitle } from '../lib/usePageTitle'

export function Plan() {
  usePageTitle('Study plan')
  const user = useUser()
  const plan = useActivePlan()
  const create = useCreatePlan()
  const toggle = useToggleTask()
  const [replacing, setReplacing] = useState(false)
  const [confirming, setConfirming] = useState(false)

  if (plan.isPending) return <Skeleton className="h-80" />
  if (plan.isError) return <ErrorState message="We couldn't load your plan." onRetry={() => void plan.refetch()} />

  const p = plan.data
  if (!p || replacing) {
    return (
      <div className="mx-auto flex max-w-xl flex-col gap-4">
        <h1 className="t-h1">Build a study plan</h1>
        <p className="text-ink-2">
          Pick your exam and how much time you have. We'll use your weak subjects and your own recent doubts.
        </p>
        <PlanForm
          user={user}
          loading={create.isPending}
          onSubmit={(input, onServerError) =>
            create.mutate(input, {
              onSuccess: () => setReplacing(false),
              onError: (e) => {
                onServerError(e)
                toast.error(errorMessage(e))
              },
            })
          }
        />
        {replacing && (
          <Button variant="ghost" onClick={() => setReplacing(false)}>
            Keep my current plan
          </Button>
        )}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="t-h1">
            {p.examName} · {examDate(p.examDate)}
          </h1>
          <p className="mt-1 max-w-2xl text-ink-2">{p.summary}</p>
        </div>
        <Button variant="secondary" onClick={() => setConfirming(true)}>
          Make a new plan
        </Button>
      </div>
      <Card className="flex flex-col gap-2">
        <ProgressBar value={p.progress.done} max={p.progress.total} label="Plan progress" />
        <p className="text-[13px] text-ink-2">
          {p.progress.done}/{p.progress.total} tasks
        </p>
      </Card>
      <div className="flex flex-col gap-3 md:flex-row md:overflow-x-auto md:pb-2">
        {p.days.map((day) => (
          <PlanDay
            key={day.dayIndex}
            day={day}
            onToggle={(taskId, done) =>
              toggle.mutate({ taskId, done }, { onError: (e) => toast.error(errorMessage(e)) })
            }
          />
        ))}
      </div>
      <Modal
        open={confirming}
        title="Make a new plan?"
        onClose={() => setConfirming(false)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(false)}>
              Cancel
            </Button>
            <Button
              onClick={() => {
                setConfirming(false)
                setReplacing(true)
              }}
            >
              Continue
            </Button>
          </>
        }
      >
        This replaces your current plan.
      </Modal>
    </div>
  )
}
