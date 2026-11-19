const EmptyState = ({ title, children }) => (
  <div className="rounded-2xl bg-gray-50 px-6 py-14 text-center">
    <p className="font-medium text-ink">{title}</p>
    {children && <div className="mt-2 text-sm text-gray-500">{children}</div>}
  </div>
)

export default EmptyState
