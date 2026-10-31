import { ButtonLink } from '../components/ui/Button'
import { Logo } from '../components/layout/Logo'
import { usePageTitle } from '../lib/usePageTitle'

export function NotFound() {
  usePageTitle('Page not found')
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4 px-4 text-center">
      <Logo />
      <h1 className="t-h1 mt-4">This page took a wrong turn.</h1>
      <p className="text-ink-2">The link might be old, or the page might have moved.</p>
      <ButtonLink to="/">Go home</ButtonLink>
    </div>
  )
}
