import { useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { SubjectWithTopics } from '../lib/types'

export function useSubjects() {
  return useQuery({
    queryKey: ['subjects'],
    queryFn: async () => (await api.get<SubjectWithTopics[]>('/subjects')).data,
    staleTime: Infinity,
  })
}
