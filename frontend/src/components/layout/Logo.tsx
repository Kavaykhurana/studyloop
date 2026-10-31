import { Link } from 'react-router'

export function LogoMark({ size = 24 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2" y="6.5" width="13" height="11" rx="5.5" stroke="#4338CA" strokeWidth="2.2" />
      <rect x="9" y="6.5" width="13" height="11" rx="5.5" stroke="#4338CA" strokeOpacity="0.55" strokeWidth="2.2" />
    </svg>
  )
}

export function Logo({ to = '/' }: { to?: string }) {
  return (
    <Link to={to} className="inline-flex items-center gap-2 rounded-md" aria-label="StudyLoop home">
      <LogoMark />
      <span className="font-display text-[20px] font-bold leading-none tracking-tight text-ink">StudyLoop</span>
    </Link>
  )
}
