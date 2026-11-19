const Spinner = ({ className = 'h-6 w-6' }) => (
  <span
    role="status"
    aria-label="Loading"
    className={`inline-block animate-spin rounded-full border-2 border-brand-500 border-t-transparent ${className}`}
  />
)

export default Spinner
