import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { PublicStats } from '../lib/types'

export function usePublicStats() {
  return useQuery({
    queryKey: ['publicStats'],
    queryFn: async () => (await api.get<PublicStats>('/public/stats')).data,
    retry: 1,
  })
}
