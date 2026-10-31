import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../../lib/auth'
import { FullScreenSpinner } from './ProtectedRoute'

export function PublicOnlyRoute() {
  const { status } = useAuth()
  if (status === 'loading') return <FullScreenSpinner />
  if (status === 'authed') return <Navigate to="/app" replace />
  return <Outlet />
}
