import type { DoubtStatus, Resolution } from '../../lib/types'
import { Badge } from '../ui/Badge'

export function StatusPill({ status, resolution }: { status: DoubtStatus; resolution: Resolution | null }) {
  if (status === 'OPEN') return <Badge variant="brand">Open</Badge>
  if (resolution === 'AI_HINT') return <Badge variant="ai">Solved with hints</Badge>
  return <Badge variant="success">Solved by a senior</Badge>
}
