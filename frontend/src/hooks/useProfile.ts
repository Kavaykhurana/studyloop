import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { PublicProfile, SubjectKind, UserMe } from '../lib/types'

export function useProfile(id: number) {
  return useQuery({
    queryKey: ['profile', id],
    queryFn: async () => (await api.get<PublicProfile>(`/users/${id}`)).data,
  })
}

export function useUpdateProfile() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: { name: string; yearOfStudy?: number | null; branch?: string; bio?: string }) =>
      (await api.put<UserMe>('/me', input)).data,
    onSuccess: (user) => {
      qc.setQueryData(['me'], user)
      void qc.invalidateQueries({ queryKey: ['profile', user.id] })
    },
  })
}

export function useUpdateSubjects() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (subjects: { subjectId: number; kind: SubjectKind; level: number }[]) =>
      (await api.put<UserMe>('/me/subjects', { subjects })).data,
    onSuccess: (user) => {
      qc.setQueryData(['me'], user)
      void qc.invalidateQueries({ queryKey: ['doubts'] })
    },
  })
}
