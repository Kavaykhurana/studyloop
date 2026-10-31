import { Outlet } from 'react-router'
import { useUser } from '../../lib/auth'
import { DemoBanner } from './DemoBanner'
import { BottomTabBar, Navbar } from './Navbar'

export function AppShell() {
  const user = useUser()
  return (
    <div className="min-h-dvh">
      <DemoBanner user={user} />
      <Navbar user={user} />
      <main className="container-page pb-28 pt-6 md:pb-12">
        <Outlet />
      </main>
      <BottomTabBar />
    </div>
  )
}
