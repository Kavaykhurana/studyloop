import { useNavigate } from 'react-router'
import type { Pulse } from '../../lib/types'
import { SubjectTag } from '../doubts/SubjectTag'

export function ConfusionMap({ map }: { map: Pulse['confusionMap'] }) {
  const navigate = useNavigate()
  const max = Math.max(1, ...map.flatMap((s) => s.topics.map((t) => t.count)))
  return (
    <div className="flex flex-col gap-5">
      <p className="text-[13px] text-ink-2">Where students on campus are getting stuck.</p>
      {map.map((entry) => (
        <section key={entry.subject.id} aria-label={entry.subject.name} className="flex flex-col gap-2">
          <SubjectTag subject={entry.subject} className="font-semibold text-ink" />
          <ul className="flex flex-col gap-1.5">
            {entry.topics.map((t) => (
              <li key={t.topicId}>
                <button
                  type="button"
                  onClick={() => navigate(`/app/doubts?subjectId=${entry.subject.id}&topicId=${t.topicId}`)}
                  className="group flex w-full flex-col gap-1 rounded-input px-1 py-1 text-left hover:bg-surface-2"
                >
                  <span className="text-[13px] text-ink-2">
                    {t.name} · {t.count} ({t.openCount} open)
                  </span>
                  <span className="block h-3 w-full overflow-hidden rounded-full bg-code">
                    <span
                      className="block h-full rounded-full"
                      style={{
                        width: `${Math.max(6, Math.round((t.count / max) * 100))}%`,
                        backgroundColor: entry.subject.colorHex,
                        opacity: 0.25 + 0.75 * (t.count / max),
                      }}
                    />
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  )
}
