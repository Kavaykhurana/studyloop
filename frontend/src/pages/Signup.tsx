import { zodResolver } from '@hookform/resolvers/zod'
import { Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { AuthCard } from '../components/layout/AuthCard'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { Select } from '../components/ui/Select'
import { applyFieldErrors, errorMessage } from '../lib/api'
import { useAuth } from '../lib/auth'
import { usePageTitle } from '../lib/usePageTitle'

const schema = z.object({
  name: z.string().trim().min(2, 'must be at least 2 characters').max(80, 'must be at most 80 characters'),
  email: z.string().trim().min(1, 'Enter your college email.').email('must be a valid email'),
  password: z
    .string()
    .min(8, 'Use 8+ characters with at least one letter and one number.')
    .max(72, 'must be at most 72 characters')
    .regex(/[A-Za-z]/, 'Use 8+ characters with at least one letter and one number.')
    .regex(/\d/, 'Use 8+ characters with at least one letter and one number.'),
  yearOfStudy: z.string().optional(),
  branch: z.string().trim().max(40, 'must be at most 40 characters').optional(),
})
type Values = z.infer<typeof schema>

export function Signup() {
  usePageTitle('Create account')
  const { signup, demo } = useAuth()
  const navigate = useNavigate()
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
      await signup({
        name: values.name,
        email: values.email,
        password: values.password,
        ...(values.yearOfStudy ? { yearOfStudy: Number(values.yearOfStudy) } : {}),
        ...(values.branch ? { branch: values.branch } : {}),
      })
      navigate('/onboarding', { replace: true })
    } catch (e) {
      if (!applyFieldErrors(e, setError, ['name', 'email', 'password', 'yearOfStudy', 'branch'])) {
        setFormError(errorMessage(e))
      }
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
    <AuthCard
      title="Create your account"
      footer={
        <>
          <p>
            Already have an account?{' '}
            <Link to="/login" className="font-medium text-brand underline underline-offset-2">
              Sign in
            </Link>
          </p>
          <p>
            Just looking?{' '}
            <button type="button" onClick={() => void tryDemo()} disabled={demoBusy} className="font-medium text-brand underline underline-offset-2 disabled:opacity-50">
              Try the demo as a learner.
            </button>
          </p>
        </>
      }
    >
      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-4">
        {formError && (
          <p role="alert" className="rounded-input bg-danger-soft px-3 py-2 text-[14px] text-danger">
            {formError}
          </p>
        )}
        <Input label="Full name" autoComplete="name" error={errors.name?.message} {...register('name')} />
        <Input
          label="College email"
          type="email"
          autoComplete="email"
          helper="Use your college email to join your campus. Others join the Open Campus."
          error={errors.email?.message}
          {...register('email')}
        />
        <Input
          label="Password"
          type={show ? 'text' : 'password'}
          autoComplete="new-password"
          helper="8+ characters, at least one letter and one number"
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
        <div className="grid grid-cols-2 gap-3">
          <Select label="Year of study" error={errors.yearOfStudy?.message} {...register('yearOfStudy')}>
            <option value="">Optional</option>
            {[1, 2, 3, 4, 5].map((y) => (
              <option key={y} value={y}>
                Year {y}
              </option>
            ))}
          </Select>
          <Input label="Branch" placeholder="Optional" error={errors.branch?.message} {...register('branch')} />
        </div>
        <Button type="submit" size="lg" loading={isSubmitting}>
          Create account
        </Button>
      </form>
    </AuthCard>
  )
}
