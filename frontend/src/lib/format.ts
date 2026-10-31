import { formatDistanceToNowStrict, format, parseISO } from 'date-fns'

export function relativeTime(iso: string): string {
  return `${formatDistanceToNowStrict(parseISO(iso))} ago`
}

export function firstName(name: string): string {
  return name.split(' ')[0] ?? name
}

/** Greeting based on the hour in India Standard Time, whatever the device timezone is. */
export function greeting(now: Date = new Date()): string {
  const hour = Number(
    new Intl.DateTimeFormat('en-GB', { hour: 'numeric', hour12: false, timeZone: 'Asia/Kolkata' }).format(now),
  )
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export function percent(fraction: number): string {
  return `${Math.round(fraction * 100)}%`
}

export function difficultyLabel(difficulty: number | null): string | null {
  if (difficulty === 1) return 'Easy'
  if (difficulty === 2) return 'Medium'
  if (difficulty === 3) return 'Hard'
  return null
}

export function examDate(iso: string): string {
  return format(parseISO(iso), 'EEE, d MMM')
}

export function dayLabel(iso: string, index: number): string {
  return index === 0 ? `Today · ${format(parseISO(iso), 'd MMM')}` : format(parseISO(iso), 'EEE · d MMM')
}

export function chartDate(iso: string): string {
  return format(parseISO(iso), 'd MMM')
}

export function yearBranch(year: number | null, branch: string | null): string {
  return [year ? `Year ${year}` : null, branch].filter(Boolean).join(' · ')
}

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? '') : ''
  return (first + last).toUpperCase()
}

export function isoDate(date: Date): string {
  return format(date, 'yyyy-MM-dd')
}

/** Today's date (yyyy-MM-dd) in India Standard Time, matching how plans are dated on the server. */
export function todayIST(now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(now)
}
