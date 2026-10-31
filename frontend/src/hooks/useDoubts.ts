import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { DoubtSummary, PageResponse } from '../lib/types'

export interface DoubtFilters {
  status?: 'OPEN' | 'SOLVED'
  subjectId?: number
  topicId?: number
  mine?: boolean
  matchedToMe?: boolean
  q?: string
}

function params(filters: DoubtFilters, page: number, size: number) {
  return {
    ...(filters.status ? { status: filters.status } : {}),
    ...(filters.subjectId ? { subjectId: filters.subjectId } : {}),
    ...(filters.topicId ? { topicId: filters.topicId } : {}),
    ...(filters.mine ? { mine: true } : {}),
    ...(filters.matchedToMe ? { matchedToMe: true } : {}),
    ...(filters.q ? { q: filters.q } : {}),
    page,
    size,
  }
}

/** Infinite feed with a "Load more" button. */
export function useDoubtFeed(filters: DoubtFilters) {
  return useInfiniteQuery({
    queryKey: ['doubts', filters],
    initialPageParam: 0,
    queryFn: async ({ pageParam }) =>
      (await api.get<PageResponse<DoubtSummary>>('/doubts', { params: params(filters, pageParam, 20) })).data,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  })
}

/** A single short page, used by dashboard panels. */
export function useDoubtList(filters: DoubtFilters, size = 5) {
  return useQuery({
    queryKey: ['doubts', 'list', filters, size],
    queryFn: async () =>
      (await api.get<PageResponse<DoubtSummary>>('/doubts', { params: params(filters, 0, size) })).data,
  })
}
