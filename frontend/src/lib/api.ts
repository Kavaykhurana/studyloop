import axios, { isAxiosError, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiProblem, AuthResponse } from './types'

/** The access token lives only in this module's memory, never in localStorage or sessionStorage. */
let accessToken: string | null = null

export function setAccessToken(token: string | null): void {
  accessToken = token
}

const HINT_KEY = 'sl_had_session'

/**
 * A boolean breadcrumb (never a token) saying this browser has signed in before. The refresh cookie is HttpOnly,
 * so without this hint every anonymous visit would make a doomed refresh call and log a 401 in the console.
 */
export function hadSession(): boolean {
  try {
    return window.localStorage.getItem(HINT_KEY) === '1'
  } catch {
    return false
  }
}

export function rememberSession(active: boolean): void {
  try {
    if (active) window.localStorage.setItem(HINT_KEY, '1')
    else window.localStorage.removeItem(HINT_KEY)
  } catch {
    // storage can be blocked (private mode); the hint is only an optimisation
  }
}

interface AuthListener {
  onRefreshed: (auth: AuthResponse) => void
  onLost: () => void
}

let listener: AuthListener | null = null

export function setAuthListener(next: AuthListener | null): void {
  listener = next
}

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'X-Requested-With': 'StudyLoop' },
})

function isAuthEndpoint(url: string | undefined): boolean {
  return url?.startsWith('/auth/') ?? false
}

api.interceptors.request.use((config) => {
  if (accessToken && !isAuthEndpoint(config.url)) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

let inFlightRefresh: Promise<AuthResponse> | null = null

/** One refresh at a time: concurrent callers share the same request (refresh tokens rotate on every use). */
export function refreshSession(): Promise<AuthResponse> {
  if (!inFlightRefresh) {
    inFlightRefresh = api
      .post<AuthResponse>('/auth/refresh')
      .then((res) => {
        setAccessToken(res.data.accessToken)
        return res.data
      })
      .finally(() => {
        inFlightRefresh = null
      })
  }
  return inFlightRefresh
}

type Retriable = InternalAxiosRequestConfig & { _retried?: boolean }

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as Retriable | undefined
    if (error.response?.status === 401 && original && !isAuthEndpoint(original.url) && !original._retried) {
      original._retried = true
      try {
        const auth = await refreshSession()
        listener?.onRefreshed(auth)
        return api.request(original)
      } catch {
        setAccessToken(null)
        listener?.onLost()
      }
    }
    return Promise.reject(error)
  },
)

export function problemOf(error: unknown): ApiProblem | null {
  if (isAxiosError<ApiProblem>(error) && error.response?.data && typeof error.response.data === 'object') {
    return error.response.data
  }
  return null
}

/** The user-facing message: the server's `detail`, or a friendly fallback for network failures. */
export function errorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  const problem = problemOf(error)
  if (problem?.detail) return problem.detail
  if (isAxiosError(error) && !error.response) return 'Cannot reach StudyLoop right now. Check your connection.'
  return fallback
}

export function errorCode(error: unknown): string | null {
  return problemOf(error)?.code ?? null
}

/** Maps server field errors onto react-hook-form fields; returns true if at least one field matched. */
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  knownFields: readonly string[],
): boolean {
  const problem = problemOf(error)
  let matched = false
  for (const item of problem?.errors ?? []) {
    if (knownFields.includes(item.field)) {
      setError(item.field as Path<T>, { type: 'server', message: item.message })
      matched = true
    }
  }
  return matched
}
