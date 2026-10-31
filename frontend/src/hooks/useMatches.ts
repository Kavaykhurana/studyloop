import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { Match } from '../lib/types'

export function useRefreshMatches(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => (await api.post<Match[]>(`/doubts/${doubtId}/matches/refresh`)).data,
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['doubt', doubtId] }),
  })
}
