import type { InputHTMLAttributes, ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { controlClasses, Field, type FieldChrome } from './Field'

interface InputProps extends InputHTMLAttributes<HTMLInputElement>, FieldChrome {
  trailing?: ReactNode
  containerClassName?: string
}

export function Input({ label, helper, error, trailing, containerClassName, className, ...rest }: InputProps) {
  return (
    <Field label={label} helper={helper} error={error} className={containerClassName}>
      {({ id, describedBy, invalid }) => (
        <div className="relative">
          <input
            id={id}
            aria-invalid={invalid || undefined}
            aria-describedby={describedBy}
            {...rest}
            className={cn(controlClasses(invalid), 'h-10 px-3', trailing ? 'pr-11' : undefined, className)}
          />
          {trailing && <div className="absolute inset-y-0 right-1 flex items-center">{trailing}</div>}
        </div>
      )}
    </Field>
  )
}
