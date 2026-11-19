/** page is 0-based (as the API returns it). */
const Pagination = ({ page, totalPages, onChange }) => {
  if (totalPages <= 1) return null
  const btn = 'rounded-full px-4 py-2 text-sm ring-1 ring-gray-200 transition hover:ring-brand-500 disabled:opacity-40 disabled:hover:ring-gray-200'
  return (
    <nav aria-label="Pagination" className="mt-10 flex items-center justify-center gap-3">
      <button type="button" className={btn} disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </button>
      <span className="text-sm text-gray-500">
        Page {page + 1} of {totalPages}
      </span>
      <button type="button" className={btn} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </nav>
  )
}

export default Pagination
