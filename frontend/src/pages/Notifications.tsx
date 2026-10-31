import { useInfiniteQuery } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { useNavigate } from 'react-router'
import { notificationIcon } from '../components/layout/NotificationBell'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { EmptyState } from '../components/ui/EmptyState'
import { ErrorState } from '../components/ui/ErrorState'
import { Skeleton } from '../components/ui/Skeleton'
import { useMarkAllRead, useMarkRead } from '../hooks/useNotifications'
import { api } from '../lib/api'
import { cn } from '../lib/cn'
import { relativeTime } from '../lib/format'
import type { AppNotification, PageResponse } from '../lib/types'
import { usePageTitle } from '../lib/usePageTitle'

export function Notifications() {
  usePageTitle('Notifications')
  const navigate = useNavigate()
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const list = useInfiniteQuery({
    queryKey: ['notifications', 'all'],
    initialPageParam: 0,
    queryFn: async ({ pageParam }) =>
      (await api.get<PageResponse<AppNotification>>('/notifications', { params: { page: pageParam, size: 20 } })).data,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  })
  const items = list.data?.pages.flatMap((p) => p.items) ?? []
  const anyUnread = items.some((n) => !n.read)

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="t-h1">Notifications</h1>
        <Button variant="secondary" disabled={!anyUnread} onClick={() => markAll.mutate()}>
          Mark all as read
        </Button>
      </div>
      {list.isPending && [0, 1, 2].map((i) => <Skeleton key={i} className="h-16" />)}
      {list.isError && <ErrorState message="We couldn't load your notifications." onRetry={() => void list.refetch()} />}
      {list.isSuccess && items.length === 0 && (
        <Card>
          <EmptyState icon={Bell} title="You're all caught up." body="New matches and answers will show up here." />
        </Card>
      )}
      <ul className="flex flex-col gap-2">
        {items.map((n) => {
          const Icon = notificationIcon[n.type]
          return (
            <li key={n.id}>
              <button
                type="button"
                onClick={() => {
                  if (!n.read) markRead.mutate(n.id)
                  navigate(`/app/doubts/${n.doubtId}`)
                }}
                className={cn(
                  'flex w-full items-start gap-3 rounded-card border border-line p-4 text-left transition-shadow hover:shadow-hover',
                  n.read ? 'bg-surface' : 'bg-brand-soft',
                )}
              >
                <Icon className="mt-0.5 size-5 shrink-0 text-brand" aria-hidden="true" />
                <span className="min-w-0 flex-1">
                  <span className="block text-[14px] leading-5 text-ink">{n.message}</span>
                  <span className="block text-[12px] text-ink-3">{relativeTime(n.createdAt)}</span>
                </span>
                {!n.read && <span className="mt-1.5 size-2 shrink-0 rounded-full bg-brand" aria-label="Unread" />}
              </button>
            </li>
          )
        })}
      </ul>
      {list.hasNextPage && (
        <div className="flex justify-center">
          <Button variant="secondary" onClick={() => void list.fetchNextPage()} loading={list.isFetchingNextPage}>
            Load more
          </Button>
        </div>
      )}
    </div>
  )
}
