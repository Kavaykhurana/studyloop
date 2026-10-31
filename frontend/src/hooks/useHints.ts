import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { HintNext } from '../lib/types'

export function useNextHint(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => (await api.post<HintNext>(`/doubts/${doubtId}/hints/next`)).data,
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['doubt', doubtId] }),
  })
}
