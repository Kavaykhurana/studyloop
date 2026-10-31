import { Home, ListChecks, MessagesSquare, Plus, Radar } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, NavLink } from 'react-router'
import { useAuth } from '../../lib/auth'
import { cn } from '../../lib/cn'
import type { UserMe } from '../../lib/types'
import { Avatar } from '../ui/Avatar'
import { ButtonLink } from '../ui/Button'
import { Logo } from './Logo'
import { NotificationBell } from './NotificationBell'

const links = [
  { to: '/app', label: 'Home', icon: Home, end: true },
  { to: '/app/doubts', label: 'Doubts', icon: MessagesSquare, end: false },
  { to: '/app/plan', label: 'Plan', icon: ListChecks, end: false },
  { to: '/app/pulse', label: 'Pulse', icon: Radar, end: false },
]

function AvatarMenu({ user }: { user: UserMe }) {
  const { logout } = useAuth()
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)

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

  return (
    <div ref={root} className="relative">
      <button
        type="button"
        aria-label="Account menu"
        aria-expanded={open}
        aria-haspopup="true"
        onClick={() => setOpen((v) => !v)}
        className="inline-flex size-10 items-center justify-center rounded-full"
      >
        <Avatar name={user.name} id={user.id} size="md" />
      </button>
      {open && (
        <div className="absolute right-0 z-40 mt-2 w-48 rounded-card border border-line bg-surface p-1 shadow-hover">
          <p className="truncate px-3 py-2 text-[13px] text-ink-3">{user.name}</p>
          <Link
            to={`/app/profile/${user.id}`}
            onClick={() => setOpen(false)}
            className="block rounded-input px-3 py-2 text-[14px] text-ink hover:bg-surface-2"
          >
            Profile
          </Link>
          <button
            type="button"
            onClick={() => {
              setOpen(false)
              void logout()
            }}
            className="block w-full rounded-input px-3 py-2 text-left text-[14px] text-ink hover:bg-surface-2"
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}

export function Navbar({ user }: { user: UserMe }) {
  return (
    <header className="sticky top-0 z-30 h-16 border-b border-line bg-surface">
      <div className="container-page flex h-full items-center justify-between gap-4">
        <Logo to="/app" />
        <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              className={({ isActive }) =>
                cn(
                  'rounded-input px-3 py-2 text-[14px] font-medium transition-colors',
                  isActive ? 'bg-brand-soft text-brand' : 'text-ink-2 hover:bg-surface-2',
                )
              }
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="flex items-center gap-1 sm:gap-2">
          <ButtonLink to="/app/ask" size="md">
            Ask a doubt
          </ButtonLink>
          <NotificationBell />
          <AvatarMenu user={user} />
        </div>
      </div>
    </header>
  )
}

export function BottomTabBar() {
  const item = (to: string, label: string, Icon: typeof Home, end: boolean) => (
    <NavLink
      key={to}
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          'flex min-h-10 flex-1 flex-col items-center justify-center gap-0.5 text-[11px] font-medium',
          isActive ? 'text-brand' : 'text-ink-3',
        )
      }
    >
      <Icon className="size-5" aria-hidden="true" />
      {label}
    </NavLink>
  )
  return (
    <nav
      aria-label="Main"
      className="fixed inset-x-0 bottom-0 z-30 flex h-16 items-center border-t border-line bg-surface pb-[env(safe-area-inset-bottom)] md:hidden"
    >
      {item('/app', 'Home', Home, true)}
      {item('/app/doubts', 'Doubts', MessagesSquare, false)}
      <div className="flex flex-1 justify-center">
        <Link
          to="/app/ask"
          aria-label="Ask a doubt"
          className="-mt-6 inline-flex size-12 items-center justify-center rounded-full bg-brand text-white shadow-hover"
        >
          <Plus className="size-6" aria-hidden="true" />
        </Link>
      </div>
      {item('/app/plan', 'Plan', ListChecks, false)}
      {item('/app/pulse', 'Pulse', Radar, false)}
    </nav>
  )
}
