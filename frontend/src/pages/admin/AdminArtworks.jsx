import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAdminArtworks, setAdminArtworkStatus } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import { formatPrice } from '../../utils/format'
import AdminNav from '../../components/admin/AdminNav'
import SearchBox from '../../components/admin/SearchBox'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import { usePageTitle } from '../../hooks/usePageTitle'

const filters = [['', 'Live & hidden'], ['ACTIVE', 'Listed'], ['HIDDEN', 'Hidden'], ['REMOVED', 'Taken down']]
const badge = {
  ACTIVE: ['Listed', 'bg-green-50 text-green-700 ring-green-200'],
  HIDDEN: ['Hidden', 'bg-gray-100 text-gray-600 ring-gray-200'],
  REMOVED: ['Taken down', 'bg-red-50 text-red-700 ring-red-200'],
}
const actions = {
  ACTIVE: [{ to: 'HIDDEN', label: 'Hide' }, { to: 'REMOVED', label: 'Take down', danger: true }],
  HIDDEN: [{ to: 'ACTIVE', label: 'Restore' }, { to: 'REMOVED', label: 'Take down', danger: true }],
  REMOVED: [{ to: 'HIDDEN', label: 'Reinstate as hidden' }],
}

const AdminArtworks = () => {
  usePageTitle('Artworks · Admin')
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const status = params.get('status') ?? ''
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)
  const [pending, setPending] = useState(null) // { artwork, action }

  const update = (changes) => setParams((prev) => {
    const next = new URLSearchParams(prev)
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)))
    if (!('page' in changes)) next.delete('page')
    return next
  }, { replace: true })

  const list = useQuery({
    queryKey: ['admin', 'artworks', q, status, page],
    queryFn: () => fetchAdminArtworks({ q, status, page }),
    placeholderData: keepPreviousData,
  })

  const change = useMutation({
    mutationFn: ({ artwork, action }) => setAdminArtworkStatus(artwork.id, action.to),
    onSuccess: () => {
      setPending(null)
      queryClient.invalidateQueries({ queryKey: ['admin'] })
      queryClient.invalidateQueries({ queryKey: ['artworks'] })
    },
    onError: () => setPending(null),
  })

  const run = (artwork, action) => (action.danger ? setPending({ artwork, action }) : change.mutate({ artwork, action }))

  return (
    <div className="py-10">
      <AdminNav />
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-medium">Artworks</h1>
          <p className="mt-1 text-gray-500">Hide or take down listings that break the marketplace rules.</p>
        </div>
        <SearchBox value={q} onChange={(v) => update({ q: v })} placeholder="Title or description" label="Search artworks" />
      </div>

      <div role="tablist" className="mt-6 flex flex-wrap gap-2">
        {filters.map(([v, l]) => (
          <button
            key={v}
            role="tab"
            aria-selected={status === v}
            onClick={() => update({ status: v })}
            className={`rounded-full px-4 py-1.5 text-sm transition ${status === v ? 'bg-ink text-white' : 'text-gray-600 ring-1 ring-gray-200 hover:ring-gray-400'}`}
          >
            {l}
          </button>
        ))}
      </div>

      <div className="mt-6 space-y-4">
        {change.isError && <Alert type="error">{getErrorMessage(change.error)}</Alert>}
        {list.isPending ? (
          <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : list.isError ? (
          <Alert type="error">{getErrorMessage(list.error)}</Alert>
        ) : list.data.content.length === 0 ? (
          <EmptyState title="No artworks here" />
        ) : (
          <>
            <ul className="divide-y divide-gray-100 rounded-2xl bg-white ring-1 ring-gray-100">
              {list.data.content.map((a) => {
                const [label, cls] = badge[a.status]
                const busy = change.isPending && change.variables?.artwork.id === a.id
                return (
                  <li key={a.id} className={`flex flex-col gap-3 p-4 sm:flex-row sm:items-center ${busy ? 'opacity-60' : ''}`}>
                    <img src={a.imageUrl} alt="" className="h-16 w-16 shrink-0 rounded-xl object-cover" />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        {a.status === 'ACTIVE' ? (
                          <Link to={`/artworks/${a.id}`} className="font-medium hover:text-brand-500">{a.title}</Link>
                        ) : (
                          <span className="font-medium">{a.title}</span>
                        )}
                        <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${cls}`}>{label}</span>
                      </div>
                      <p className="text-sm text-gray-500">
                        by {a.artist.name} · {a.category.name} · {formatPrice(a.price)} · {a.stock} in stock
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-wrap gap-2">
                      {actions[a.status].map((act) => (
                        <button
                          key={act.to}
                          type="button"
                          disabled={busy}
                          onClick={() => run(a, act)}
                          className={`rounded-full px-3 py-1.5 text-sm ring-1 ${act.danger ? 'text-red-600 ring-red-200 hover:bg-red-50' : 'ring-gray-200 hover:bg-gray-50'}`}
                        >
                          {act.label}
                        </button>
                      ))}
                    </div>
                  </li>
                )
              })}
            </ul>
            <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={(p) => update({ page: p ? String(p + 1) : '' })} />
          </>
        )}
      </div>

      <ConfirmDialog
        open={!!pending}
        title={`Take down “${pending?.artwork.title}”?`}
        confirmLabel="Take down"
        danger
        busy={change.isPending}
        onCancel={() => setPending(null)}
        onConfirm={() => change.mutate(pending)}
      >
        It disappears from the shop and from the artist&apos;s dashboard. Past orders are not affected. You can reinstate it later.
      </ConfirmDialog>
    </div>
  )
}

export default AdminArtworks
