import { useState } from 'react'
import { toast } from 'sonner'
import { useAuth } from '../../lib/auth'
import { errorMessage } from '../../lib/api'
import type { UserMe } from '../../lib/types'

export function DemoBanner({ user }: { user: UserMe }) {
  const { demo } = useAuth()
  const [busy, setBusy] = useState(false)
  if (!user.isDemo) return null
  const isLearner = user.email.startsWith('aanya')
  const otherName = isLearner ? 'Rohan Mehta (helper)' : 'Aanya Sharma (learner)'

  const switchPersona = async () => {
    setBusy(true)
    try {
      await demo(isLearner ? 'helper' : 'learner')
    } catch (error) {
      toast.error(errorMessage(error, 'Demo is waking up — try again in a few seconds.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex min-h-9 flex-wrap items-center justify-center gap-x-3 bg-brand-soft px-4 py-1.5 text-center text-[13px] text-ink-2">
      <span>
        You're exploring a demo campus as <strong className="font-semibold text-ink">{user.name}</strong> with seeded data.
      </span>
      <button
        type="button"
        onClick={switchPersona}
        disabled={busy}
        className="font-semibold text-brand underline underline-offset-2 hover:text-brand-hover disabled:opacity-50"
      >
        Switch to {otherName}
      </button>
    </div>
  )
}
