import { Bell, CheckCircle2, MessageSquare, UserPlus } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useMarkAllRead, useMarkRead, useNotificationList, useUnreadCount } from '../../hooks/useNotifications'
import { cn } from '../../lib/cn'
import { relativeTime } from '../../lib/format'
import type { AppNotification, NotificationType } from '../../lib/types'

export const notificationIcon: Record<NotificationType, typeof Bell> = {
  MATCHED: UserPlus,
  NEW_ANSWER: MessageSquare,
  ANSWER_ACCEPTED: CheckCircle2,
}

export function NotificationBell() {
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()
  const count = useUnreadCount()
  const list = useNotificationList(0, 8, open)
  const markRead = useMarkRead()
  const markAll = useMarkAllRead()
  const unread = count.data ?? 0

  useEffect(() => {
    if (!open) return
    const onDown = (e: MouseEvent) => {
      if (root.current && !root.current.contains(e.target as Node)) setOpen(false)
    }
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onDown)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onDown)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const openNotification = (n: AppNotification) => {
    if (!n.read) markRead.mutate(n.id)
    setOpen(false)
    navigate(`/app/doubts/${n.doubtId}`)
  }

  return (
    <div ref={root} className="relative">
      <button
        type="button"
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-expanded={open}
        aria-haspopup="true"
        onClick={() => setOpen((v) => !v)}
        className="relative inline-flex size-10 items-center justify-center rounded-input text-ink-2 hover:bg-surface-2"
      >
        <Bell className="size-5" aria-hidden="true" />
        {unread > 0 && (
          <span className="absolute right-1 top-1 inline-flex min-w-4 items-center justify-center rounded-full bg-danger px-1 text-[10px] font-semibold leading-4 text-white">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>
      {open && (
        <div className="absolute right-0 z-40 mt-2 w-[min(360px,calc(100vw-32px))] rounded-card border border-line bg-surface shadow-hover">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <h2 className="t-h3 !text-[16px]">Notifications</h2>
            <button
              type="button"
              onClick={() => markAll.mutate()}
              disabled={unread === 0}
              className="text-[13px] font-medium text-brand disabled:opacity-40"
            >
              Mark all as read
            </button>
          </div>
          <ul className="max-h-[380px] overflow-y-auto">
            {list.isPending && <li className="px-4 py-6 text-center text-ink-3">Loading…</li>}
            {list.data?.items.length === 0 && <li className="px-4 py-6 text-center text-ink-2">You're all caught up.</li>}
            {list.data?.items.map((n) => {
              const Icon = notificationIcon[n.type]
              return (
                <li key={n.id}>
                  <button
                    type="button"
                    onClick={() => openNotification(n)}
                    className={cn(
                      'flex w-full items-start gap-3 px-4 py-3 text-left hover:bg-surface-2',
                      !n.read && 'bg-brand-soft hover:bg-brand-soft',
                    )}
                  >
                    <Icon className="mt-0.5 size-4 shrink-0 text-brand" aria-hidden="true" />
                    <span className="min-w-0">
                      <span className="block text-[14px] leading-5 text-ink">{n.message}</span>
                      <span className="block text-[12px] text-ink-3">{relativeTime(n.createdAt)}</span>
                    </span>
                  </button>
                </li>
              )
            })}
          </ul>
          <div className="border-t border-line px-4 py-2 text-center">
            <Link to="/app/notifications" onClick={() => setOpen(false)} className="text-[13px] font-medium text-brand">
              See all
            </Link>
          </div>
        </div>
      )}
    </div>
  )
}
