import type { ReactNode } from 'react'
import { Card } from '../ui/Card'
import { Logo } from './Logo'

export function AuthCard({ title, children, footer }: { title: string; children: ReactNode; footer: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col items-center px-4 py-10">
      <Logo />
      <Card className="mt-8 w-full max-w-[420px] !p-6">
        <h1 className="t-h1 mb-5">{title}</h1>
        {children}
      </Card>
      <div className="mt-5 flex max-w-[420px] flex-col items-center gap-1 text-center text-[13px] text-ink-2">{footer}</div>
    </div>
  )
}
