import { QueryClientProvider } from '@tanstack/react-query'
import { createBrowserRouter, Outlet, RouterProvider } from 'react-router'
import { Toaster } from 'sonner'
import { AppShell } from './components/layout/AppShell'
import { ProtectedRoute } from './components/layout/ProtectedRoute'
import { PublicOnlyRoute } from './components/layout/PublicOnlyRoute'
import { Spinner } from './components/ui/Spinner'
import { AuthProvider } from './lib/auth'
import { queryClient } from './lib/queryClient'
import { Landing } from './pages/Landing'
import { Login } from './pages/Login'
import { NotFound } from './pages/NotFound'
import { Signup } from './pages/Signup'

function RouteFallback() {
  return (
    <div className="flex min-h-dvh items-center justify-center text-[var(--color-brand)]">
      <Spinner className="size-8" label="Loading page" />
    </div>
  )
}

function RootLayout() {
  return (
    <AuthProvider>
      <Outlet />
      <Toaster position="bottom-right" richColors closeButton />
    </AuthProvider>
  )
}

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    hydrateFallbackElement: <RouteFallback />,
    children: [
      { path: '/', element: <Landing /> },
      {
        element: <PublicOnlyRoute />,
        children: [
          { path: '/login', element: <Login /> },
          { path: '/signup', element: <Signup /> },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [
          { path: '/onboarding', lazy: async () => ({ Component: (await import('./pages/Onboarding')).Onboarding }) },
          {
            path: '/app',
            element: <AppShell />,
            children: [
              { index: true, lazy: async () => ({ Component: (await import('./pages/Dashboard')).Dashboard }) },
              { path: 'ask', lazy: async () => ({ Component: (await import('./pages/Ask')).Ask }) },
              { path: 'doubts', lazy: async () => ({ Component: (await import('./pages/Feed')).Feed }) },
              {
                path: 'doubts/:id',
                lazy: async () => ({ Component: (await import('./pages/DoubtDetail')).DoubtDetail }),
              },
              { path: 'plan', lazy: async () => ({ Component: (await import('./pages/Plan')).Plan }) },
              { path: 'pulse', lazy: async () => ({ Component: (await import('./pages/Pulse')).Pulse }) },
              { path: 'profile/:id', lazy: async () => ({ Component: (await import('./pages/Profile')).Profile }) },
              {
                path: 'notifications',
                lazy: async () => ({ Component: (await import('./pages/Notifications')).Notifications }),
              },
            ],
          },
        ],
      },
      { path: '*', element: <NotFound /> },
    ],
  },
])

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
