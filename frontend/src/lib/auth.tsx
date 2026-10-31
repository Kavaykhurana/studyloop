import { useQuery, useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { api, hadSession, refreshSession, rememberSession, setAccessToken, setAuthListener } from './api'
import type { AuthResponse, UserMe } from './types'

type Status = 'loading' | 'authed' | 'anon'
export type Persona = 'learner' | 'helper'

export interface SignupInput {
  name: string
  email: string
  password: string
  yearOfStudy?: number
  branch?: string
}

interface AuthContextValue {
  status: Status
  user: UserMe | null
  login: (email: string, password: string) => Promise<UserMe>
  signup: (input: SignupInput) => Promise<UserMe>
  demo: (persona: Persona) => Promise<UserMe>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const [status, setStatus] = useState<Status>(() => (hadSession() ? 'loading' : 'anon'))

  const apply = useCallback(
    (auth: AuthResponse): UserMe => {
      setAccessToken(auth.accessToken)
      rememberSession(true)
      queryClient.setQueryData(['me'], auth.user)
      setStatus('authed')
      return auth.user
    },
    [queryClient],
  )

  const lost = useCallback(() => {
    setAccessToken(null)
    rememberSession(false)
    queryClient.clear()
    setStatus('anon')
    toast.error('Your session ended. Please sign in again.')
    const next = encodeURIComponent(window.location.pathname + window.location.search)
    navigate(`/login?next=${next}`, { replace: true })
  }, [navigate, queryClient])

  useEffect(() => {
    setAuthListener({ onRefreshed: (auth) => queryClient.setQueryData(['me'], auth.user), onLost: lost })
    return () => setAuthListener(null)
  }, [lost, queryClient])

  useEffect(() => {
    if (!hadSession()) return undefined
    let cancelled = false
    refreshSession().then(
      (auth) => {
        if (!cancelled) apply(auth)
      },
      () => {
        rememberSession(false)
        if (!cancelled) setStatus('anon')
      },
    )
    return () => {
      cancelled = true
    }
  }, [apply])

  const me = useQuery({
    queryKey: ['me'],
    queryFn: async () => (await api.get<UserMe>('/me')).data,
    enabled: status === 'authed',
  })

  const login = useCallback(
    async (email: string, password: string) =>
      apply((await api.post<AuthResponse>('/auth/login', { email, password })).data),
    [apply],
  )
  const signup = useCallback(
    async (input: SignupInput) => apply((await api.post<AuthResponse>('/auth/signup', input)).data),
    [apply],
  )
  const demo = useCallback(
    async (persona: Persona) => {
      const user = apply((await api.post<AuthResponse>('/auth/demo', { persona })).data)
      await queryClient.invalidateQueries()
      return user
    },
    [apply, queryClient],
  )
  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // The local session is cleared below either way; the server-side family expires on its own.
    }
    // Leave the protected page first: clearing the session while still on it would make ProtectedRoute redirect to
    // /login?next=... and override this navigation.
    await navigate('/', { replace: true })
    setAccessToken(null)
    rememberSession(false)
    queryClient.clear()
    setStatus('anon')
  }, [navigate, queryClient])

  const value = useMemo<AuthContextValue>(
    () => ({ status, user: me.data ?? null, login, signup, demo, logout }),
    [status, me.data, login, signup, demo, logout],
  )

  useEffect(() => {
    document.documentElement.dataset.route = location.pathname
  }, [location.pathname])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}

export function useUser(): UserMe {
  const { user } = useAuth()
  if (!user) throw new Error('useUser requires a signed-in user')
  return user
}
