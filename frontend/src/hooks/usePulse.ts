import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { Pulse } from '../lib/types'

export function usePulse(days: 7 | 14 | 30) {
  return useQuery({
    queryKey: ['pulse', days],
    queryFn: async () => (await api.get<Pulse>('/pulse', { params: { days } })).data,
  })
}
