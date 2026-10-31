import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { toast } from 'sonner'
import { api } from '../lib/api'
import type { AppNotification, PageResponse } from '../lib/types'

/** Unread count, polled every 15 seconds (deliberately not WebSockets: it survives proxies and free tiers). */
export function useUnreadCount(enabled = true) {
  const query = useQuery({
    queryKey: ['notifications', 'count'],
    queryFn: async () => (await api.get<{ count: number }>('/notifications/unread-count')).data.count,
    refetchInterval: 15_000,
    refetchIntervalInBackground: false,
    staleTime: 0,
    enabled,
  })
  const previous = useRef<number | null>(null)
  const qc = useQueryClient()
  useEffect(() => {
    const count = query.data
    if (count === undefined) return
    if (previous.current !== null && count > previous.current) {
      void api
        .get<PageResponse<AppNotification>>('/notifications', { params: { page: 0, size: 1 } })
        .then((res) => {
          const newest = res.data.items[0]
          if (newest) toast(newest.message)
          void qc.invalidateQueries({ queryKey: ['notifications'], exact: false })
        })
    }
    previous.current = count
  }, [query.data, qc])
  return query
}

export function useNotificationList(page: number, size: number, enabled = true) {
  return useQuery({
    queryKey: ['notifications', page, size],
    queryFn: async () =>
      (await api.get<PageResponse<AppNotification>>('/notifications', { params: { page, size } })).data,
    enabled,
  })
}

export function useMarkRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => {
      await api.post(`/notifications/${id}/read`)
    },
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['notifications'] }),
  })
}

export function useMarkAllRead() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      await api.post('/notifications/read-all')
    },
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['notifications'] }),
  })
}
