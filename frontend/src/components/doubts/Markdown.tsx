import { Link } from 'react-router'
import ReactMarkdown from 'react-markdown'
import rehypeHighlight from 'rehype-highlight'
import rehypeSanitize, { defaultSchema } from 'rehype-sanitize'
import remarkGfm from 'remark-gfm'
import { cn } from '../../lib/cn'

const CITATION = /\[#(\d+)\]/g
const INTERNAL_DOUBT = /^\/app\/doubts\/\d+$/

/** [#42] becomes a Markdown link that the renderer draws as a citation chip. */
export function withCitationLinks(text: string): string {
  return text.replace(CITATION, (_match, id: string) => `[#${id}](/app/doubts/${id})`)
}

export function Markdown({ children, className }: { children: string; className?: string }) {
  return (
    <div className={cn('md break-words', className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[[rehypeSanitize, defaultSchema], [rehypeHighlight, { detect: false, ignoreMissing: true }]]}
        components={{
          a: ({ href, children: label }) =>
            href && INTERNAL_DOUBT.test(href) ? (
              <Link to={href} className="cite">
                {label}
              </Link>
            ) : (
              <a href={href} target="_blank" rel="noreferrer noopener">
                {label}
              </a>
            ),
        }}
      >
        {withCitationLinks(children)}
      </ReactMarkdown>
    </div>
  )
}
