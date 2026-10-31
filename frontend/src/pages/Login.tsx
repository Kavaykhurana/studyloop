import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { AuthCard } from '../components/layout/AuthCard'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { applyFieldErrors, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { safeNext } from '../lib/safeNext'
import { usePageTitle } from '../lib/usePageTitle'

const schema = z.object({
  email: z.string().trim().min(1, 'Enter your email.').email('must be a valid email'),
  password: z.string().min(1, 'Enter your password.'),
})
type Values = z.infer<typeof schema>

export function Login() {
  usePageTitle('Sign in')
  const { login, demo } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [show, setShow] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [demoBusy, setDemoBusy] = useState(false)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      await login(values.email, values.password)
      navigate(safeNext(params.get('next')), { replace: true })
    } catch (e) {
      if (!applyFieldErrors(e, setError, ['email', 'password'])) setFormError(errorMessage(e))
    }
  })

  const tryDemo = async () => {
    setDemoBusy(true)
    try {
      await demo('learner')
      navigate('/app', { replace: true })
    } catch {
      toast.error('Demo is waking up — try again in a few seconds.')
    } finally {
      setDemoBusy(false)
    }
  }

  return (
    <AuthCard title="Sign in" footer={
      <>
        <p>
          New here?{' '}
          <Link to="/signup" className="font-medium text-brand underline underline-offset-2">
            Create an account
          </Link>
        </p>
        <p>
          Just looking?{' '}
          <button type="button" onClick={() => void tryDemo()} disabled={demoBusy} className="font-medium text-brand underline underline-offset-2 disabled:opacity-50">
            Try the demo as a learner.
          </button>
        </p>
      </>
    }>
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        {formError && (
          <p role="alert" className="rounded-input bg-danger-soft px-3 py-2 text-[14px] text-danger">
            {formError}
          </p>
        )}
        <Input label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Input
          label="Password"
          type={show ? 'text' : 'password'}
          autoComplete="current-password"
          error={errors.password?.message}
          trailing={
            <button
              type="button"
              aria-label={show ? 'Hide password' : 'Show password'}
              onClick={() => setShow((v) => !v)}
              className="inline-flex size-8 items-center justify-center rounded-md text-ink-3 hover:text-ink"
            >
              {show ? <EyeOff className="size-4" aria-hidden="true" /> : <Eye className="size-4" aria-hidden="true" />}
            </button>
          }
          {...register('password')}
        />
        <Button type="submit" size="lg" loading={isSubmitting}>
          Sign in
        </Button>
      </form>
    </AuthCard>
  )
}
