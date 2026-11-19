import { useEffect, useRef } from 'react'

/** Accessible confirm modal built on <dialog>. */
const ConfirmDialog = ({ open, title, children, confirmLabel = 'Confirm', danger, busy, onConfirm, onCancel }) => {
  const ref = useRef(null)

  useEffect(() => {
    const d = ref.current
    if (!d) return
    if (open && !d.open) d.showModal()
    if (!open && d.open) d.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      onCancel={(e) => {
        e.preventDefault()
        onCancel()
      }}
      className="m-auto w-[min(28rem,calc(100vw-2rem))] rounded-2xl p-0 shadow-xl backdrop:bg-black/40"
    >
      <div className="p-6">
        <h2 className="text-lg font-semibold">{title}</h2>
        <div className="mt-2 text-sm text-gray-600">{children}</div>
        <div className="mt-6 flex justify-end gap-3">
          <button type="button" onClick={onCancel} className="rounded-full px-5 py-2 text-sm ring-1 ring-gray-200 hover:bg-gray-50">
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className={`rounded-full px-5 py-2 text-sm font-medium text-white disabled:opacity-60 ${
              danger ? 'bg-red-600 hover:bg-red-700' : 'bg-brand-500 hover:bg-brand-600'
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </dialog>
  )
}

export default ConfirmDialog
