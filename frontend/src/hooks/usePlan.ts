import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { Plan, PlanTask } from '../lib/types'

export function useActivePlan() {
  return useQuery({
    queryKey: ['plan', 'active'],
    queryFn: async (): Promise<Plan | null> => {
      const res = await api.get<Plan>('/plans/active', { validateStatus: (s) => s === 200 || s === 204 })
      return res.status === 204 ? null : res.data
    },
  })
}

export interface CreatePlanInput {
  examName: string
  examDate: string
  hoursPerDay: number
  subjectIds: number[]
}

export function useCreatePlan() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: CreatePlanInput) => (await api.post<Plan>('/plans', input)).data,
    onSuccess: (plan) => qc.setQueryData(['plan', 'active'], plan),
  })
}

/** Optimistic tick: updates immediately and rolls back if the server refuses. */
export function useToggleTask() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: { taskId: number; done: boolean }) =>
      (await api.patch<PlanTask>(`/plans/tasks/${input.taskId}`, { done: input.done })).data,
    onMutate: async ({ taskId, done }) => {
      await qc.cancelQueries({ queryKey: ['plan', 'active'] })
      const previous = qc.getQueryData<Plan | null>(['plan', 'active'])
      if (previous) {
        const days = previous.days.map((day) => ({
          ...day,
          tasks: day.tasks.map((t) => (t.id === taskId ? { ...t, done } : t)),
        }))
        const all = days.flatMap((d) => d.tasks)
        const next: Plan = {
          ...previous,
          days,
          progress: { done: all.filter((t) => t.done).length, total: all.length },
        }
        qc.setQueryData<Plan | null>(['plan', 'active'], next)
      }
      return { previous }
    },
    onError: (_error, _vars, context) => {
      if (context?.previous) qc.setQueryData(['plan', 'active'], context.previous)
    },
    onSettled: () => void qc.invalidateQueries({ queryKey: ['plan', 'active'] }),
  })
}
