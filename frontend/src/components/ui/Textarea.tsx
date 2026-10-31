import { useCallback, useEffect, useRef, type ComponentProps } from 'react'
import { cn } from '../../lib/cn'
import { controlClasses, Field, type FieldChrome } from './Field'

interface TextareaProps extends ComponentProps<'textarea'>, FieldChrome {
  containerClassName?: string
}

const MAX_HEIGHT = 480

export function Textarea({ label, helper, error, containerClassName, className, onInput, ref, ...rest }: TextareaProps) {
  const inner = useRef<HTMLTextAreaElement | null>(null)

  const grow = useCallback(() => {
    const el = inner.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, MAX_HEIGHT)}px`
  }, [])

  useEffect(grow, [grow, rest.value])

  return (
    <Field label={label} helper={helper} error={error} className={containerClassName}>
      {({ id, describedBy, invalid }) => (
        <textarea
          id={id}
          aria-invalid={invalid || undefined}
          aria-describedby={describedBy}
          {...rest}
          ref={(node) => {
            inner.current = node
            if (typeof ref === 'function') ref(node)
            else if (ref) ref.current = node
          }}
          onInput={(e) => {
            grow()
            onInput?.(e)
          }}
          className={cn(controlClasses(invalid), 'min-h-[140px] resize-y px-3 py-2 leading-6', className)}
        />
      )}
    </Field>
  )
}
