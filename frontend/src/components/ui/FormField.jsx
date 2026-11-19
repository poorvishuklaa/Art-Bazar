import { forwardRef, useId } from 'react'

/** Label + input + error message, wired for react-hook-form's register(). */
const FormField = forwardRef(function FormField({ label, error, hint, as = 'input', className = '', ...props }, ref) {
  const id = useId()
  const Tag = as
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-gray-700">
        {label}
      </label>
      <Tag
        id={id}
        ref={ref}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
        className={`block w-full rounded-lg border bg-white px-3.5 py-2.5 text-ink outline-none transition placeholder:text-gray-400 focus:ring-2 ${
          error ? 'border-red-400 focus:ring-red-200' : 'border-gray-300 focus:border-brand-500 focus:ring-brand-100'
        }`}
        {...props}
      />
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 text-sm text-red-600">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${id}-hint`} className="mt-1.5 text-xs text-gray-500">
            {hint}
          </p>
        )
      )}
    </div>
  )
})

export default FormField
