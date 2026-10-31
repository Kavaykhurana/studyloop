import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type {
  AcceptResult,
  Answer,
  DoubtDetail,
  DoubtSummary,
  SimilarItem,
  SnapResult,
} from '../lib/types'

export function useDoubt(id: number) {
  return useQuery({
    queryKey: ['doubt', id],
    queryFn: async () => (await api.get<DoubtDetail>(`/doubts/${id}`)).data,
  })
}

export interface CreateDoubtInput {
  title: string
  bodyMd: string
  subjectId?: number
  source: 'TEXT' | 'SNAP'
}

export function useCreateDoubt() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: CreateDoubtInput) => (await api.post<DoubtDetail>('/doubts', input)).data,
    onSuccess: (doubt) => {
      qc.setQueryData(['doubt', doubt.id], doubt)
      void qc.invalidateQueries({ queryKey: ['doubts'] })
    },
  })
}

export function useSimilar() {
  return useMutation({
    mutationFn: async (input: { title: string; bodyMd?: string; subjectId?: number }) =>
      (await api.post<{ items: SimilarItem[] }>('/doubts/similar', input)).data.items,
  })
}

export function useDeflection() {
  return useMutation({
    mutationFn: async (input: { sourceDoubtId: number; queryText: string; similarity: number }) => {
      await api.post('/doubts/deflections', input)
    },
  })
}

export function useSnap() {
  return useMutation({
    mutationFn: async (image: Blob) => {
      const form = new FormData()
      form.append('image', image, 'question.jpg')
      return (await api.post<SnapResult>('/ai/snap', form)).data
    },
  })
}

export function usePostAnswer(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (bodyMd: string) => (await api.post<Answer>(`/doubts/${doubtId}/answers`, { bodyMd })).data,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['doubt', doubtId] })
      void qc.invalidateQueries({ queryKey: ['doubts'] })
    },
  })
}

export function useAcceptAnswer(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (answerId: number) => (await api.put<AcceptResult>(`/answers/${answerId}/accept`)).data,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['doubt', doubtId] })
      void qc.invalidateQueries({ queryKey: ['doubts'] })
      void qc.invalidateQueries({ queryKey: ['me'] })
    },
  })
}

export function useRateAnswer(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (input: { answerId: number; stars: number }) =>
      (await api.put<Answer>(`/answers/${input.answerId}/rating`, { stars: input.stars })).data,
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['doubt', doubtId] }),
  })
}

export function useResolveByHint(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => (await api.post<DoubtSummary>(`/doubts/${doubtId}/resolve-by-hint`)).data,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['doubt', doubtId] })
      void qc.invalidateQueries({ queryKey: ['doubts'] })
    },
  })
}

export function useDeleteDoubt(doubtId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      await api.delete(`/doubts/${doubtId}`)
    },
    onSuccess: () => {
      qc.removeQueries({ queryKey: ['doubt', doubtId] })
      void qc.invalidateQueries({ queryKey: ['doubts'] })
    },
  })
}
