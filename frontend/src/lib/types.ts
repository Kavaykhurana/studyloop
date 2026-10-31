export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface ApiProblem {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  code: string
  traceId?: string
  errors?: { field: string; message: string }[]
}

export interface UserBrief {
  id: number
  name: string
  yearOfStudy: number | null
  branch: string | null
  reputation: number
}

export interface SubjectRef {
  id: number
  code: string
  name: string
  colorHex: string
}

export interface TopicRef {
  id: number
  name: string
}

export interface SubjectWithTopics extends SubjectRef {
  topics: TopicRef[]
}

export type SubjectKind = 'STRONG' | 'NEEDS_HELP'

export interface MySubject {
  subjectId: number
  code: string
  name: string
  kind: SubjectKind
  level: number
}

export interface UserMe {
  id: number
  name: string
  email: string
  campus: { id: number; name: string; shortName: string }
  yearOfStudy: number | null
  branch: string | null
  bio: string | null
  reputation: number
  onboarded: boolean
  isDemo: boolean
  subjects: MySubject[]
}

export interface AuthResponse {
  accessToken: string
  expiresIn: number
  user: UserMe
}

export type DoubtStatus = 'OPEN' | 'SOLVED'
export type Resolution = 'PEER_ANSWER' | 'AI_HINT'
export type DoubtSource = 'TEXT' | 'SNAP'

export interface DoubtSummary {
  id: number
  title: string
  excerpt: string
  subject: SubjectRef
  topic: TopicRef | null
  difficulty: number | null
  status: DoubtStatus
  resolution: Resolution | null
  source: DoubtSource
  author: UserBrief
  answerCount: number
  createdAt: string
  solvedAt: string | null
}

export interface Answer {
  id: number
  doubtId: number
  bodyMd: string
  author: UserBrief
  isAccepted: boolean
  helpfulRating: number | null
  createdAt: string
  acceptedAt: string | null
  isMine: boolean
}

export interface MatchBreakdownValues {
  semantic: number
  declared: number
  reputation: number
  recency: number
  load: number
}

export interface Match {
  helper: UserBrief
  rank: number
  score: number
  breakdown: MatchBreakdownValues
  reasons: string[]
}

export interface Hint {
  level: number
  contentMd: string
  citedDoubts: { id: number; title: string }[]
  confidence: 'HIGH' | 'MEDIUM' | 'LOW'
}

export interface DoubtDetail {
  id: number
  title: string
  subject: SubjectRef
  topic: TopicRef | null
  difficulty: number | null
  status: DoubtStatus
  resolution: Resolution | null
  source: DoubtSource
  author: UserBrief
  answerCount: number
  createdAt: string
  solvedAt: string | null
  bodyMd: string
  viewCount: number
  firstAnswerAt: string | null
  answers: Answer[]
  matches: Match[]
  hints: { levelUnlocked: number; items: Hint[] }
  permissions: {
    isAuthor: boolean
    canAnswer: boolean
    canAccept: boolean
    canResolveByHint: boolean
    canDelete: boolean
    canRefreshMatches: boolean
  }
}

export interface SimilarItem {
  doubtId: number
  title: string
  subject: SubjectRef
  topic: TopicRef | null
  similarity: number
  acceptedAnswerExcerpt: string
  acceptedAnswerAuthor: string
  solvedAt: string
}

export interface SnapResult {
  title: string
  bodyMd: string
  subjectId: number | null
  topicId: number | null
  confidence: 'HIGH' | 'MEDIUM' | 'LOW'
}

export interface HintNext {
  hint: Hint
  levelUnlocked: number
  fromCache: boolean
}

export interface AcceptResult {
  doubtId: number
  answerId: number
  awarded: { helper: number; author: number }
}

export type NotificationType = 'MATCHED' | 'NEW_ANSWER' | 'ANSWER_ACCEPTED'

export interface AppNotification {
  id: number
  type: NotificationType
  message: string
  doubtId: number
  actor: UserBrief | null
  read: boolean
  createdAt: string
}

export type PlanTaskKind = 'LEARN' | 'PRACTICE' | 'REVISE' | 'ASK_PEER'

export interface PlanTask {
  id: number
  title: string
  minutes: number
  kind: PlanTaskKind
  subject: SubjectRef
  topic: TopicRef | null
  done: boolean
}

export interface PlanDay {
  dayIndex: number
  date: string
  focus: string
  totalMinutes: number
  tasks: PlanTask[]
}

export interface Plan {
  id: number
  examName: string
  examDate: string
  hoursPerDay: number
  summary: string
  createdAt: string
  progress: { done: number; total: number }
  days: PlanDay[]
}

export interface Pulse {
  campus: string
  days: number
  isSeededDemo: boolean
  totals: {
    doubtsPosted: number
    solved: number
    solvedRate: number
    solvedByPeers: number
    solvedByHints: number
    deflections: number
    selfServeRate: number
    medianFirstAnswerMinutes: number | null
    activeHelpers: number
  }
  daily: { date: string; posted: number; solved: number }[]
  confusionMap: {
    subject: SubjectRef
    topics: { topicId: number; name: string; count: number; openCount: number }[]
  }[]
  topHelpers: { id: number; name: string; acceptedInPeriod: number; reputation: number }[]
  ai: { calls: number; successRate: number; p50LatencyMs: number | null }
}

export interface PublicStats {
  doubtsSolved: number
  selfServeRate: number
  medianFirstAnswerMinutes: number | null
  isSeededDemo: boolean
}

export interface PublicProfile {
  user: UserBrief
  bio: string | null
  strongSubjects: { code: string; level: number }[]
  subjectReputation: { code: string; reputation: number }[]
  stats: { answers: number; accepted: number; avgHelpful: number | null }
  recentAccepted: DoubtSummary[]
}
