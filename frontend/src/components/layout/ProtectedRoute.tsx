import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../../lib/auth'
import { Spinner } from '../ui/Spinner'

export function FullScreenSpinner() {
  return (
    <div className="flex min-h-dvh items-center justify-center bg-bg text-brand">
      <Spinner className="size-8" label="Loading StudyLoop" />
    </div>
  )
}

export function ProtectedRoute() {
  const { status, user } = useAuth()
  const location = useLocation()
  if (status === 'loading' || (status === 'authed' && !user)) return <FullScreenSpinner />
  if (status === 'anon') {
    const next = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?next=${next}`} replace />
  }
  if (user && !user.onboarded && location.pathname !== '/onboarding') {
    return <Navigate to="/onboarding" replace />
  }
  return <Outlet />
}
