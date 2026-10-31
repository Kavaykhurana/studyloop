import type { SelectHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'
import { controlClasses, Field, type FieldChrome } from './Field'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement>, FieldChrome {
  containerClassName?: string
}

export function Select({ label, helper, error, containerClassName, className, children, ...rest }: SelectProps) {
  return (
    <Field label={label} helper={helper} error={error} className={containerClassName}>
      {({ id, describedBy, invalid }) => (
        <select
          id={id}
          aria-invalid={invalid || undefined}
          aria-describedby={describedBy}
          {...rest}
          className={cn(controlClasses(invalid), 'h-10 px-3', className)}
        >
          {children}
        </select>
      )}
    </Field>
  )
}
