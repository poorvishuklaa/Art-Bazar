import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../hooks/useAuth'
import { deleteArtwork, fetchMyArtworks, setArtworkStatus } from '../../api/artworks'
import { getErrorMessage } from '../../api/client'
import { formatPrice } from '../../utils/format'
import Alert from '../../components/ui/Alert'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import Spinner from '../../components/ui/Spinner'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import ArtistNav from '../../components/artist/ArtistNav'
import { useSalesSummary } from '../../hooks/useSalesSummary'
import { usePageTitle } from '../../hooks/usePageTitle'

const StatusBadge = ({ status }) =>
  status === 'ACTIVE' ? (
    <span className="rounded-full bg-green-50 px-2.5 py-0.5 text-xs font-medium text-green-700 ring-1 ring-green-200">Listed</span>
  ) : (
    <span className="rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600 ring-1 ring-gray-200">Hidden</span>
  )

const ArtistDashboard = () => {
  usePageTitle('My artworks')
  const { user, reloadUser } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)
  const [toDelete, setToDelete] = useState(null)
  const [actionError, setActionError] = useState('')
  // Read once on mount: the history entry is cleared below, but the message stays on screen.
  const [flash] = useState(() => location.state?.flash)

  // Pick up an approval that happened since sign-in.
  useEffect(() => {
    if (!user.artistApproved) reloadUser().catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Show the "saved" message once, not again after a reload.
  useEffect(() => {
    if (flash) navigate(location.pathname + location.search, { replace: true, state: null })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const mine = useQuery({
    queryKey: ['my-artworks', page],
    queryFn: () => fetchMyArtworks(page),
    placeholderData: keepPreviousData,
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['my-artworks'] })
    queryClient.invalidateQueries({ queryKey: ['artworks'] })
  }

  const toggle = useMutation({
    mutationFn: (a) => setArtworkStatus(a.id, a.status === 'ACTIVE' ? 'HIDDEN' : 'ACTIVE'),
    onSuccess: refresh,
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (a) => deleteArtwork(a.id),
    onSuccess: () => {
      setToDelete(null)
      refresh()
    },
    onError: (e) => {
      setToDelete(null)
      setActionError(getErrorMessage(e))
    },
  })

  const approved = user.artistApproved
  const { data: summary } = useSalesSummary()

  return (
    <div className="py-10">
      <ArtistNav toShip={summary?.ordersToShip} />
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-medium">My artworks</h1>
          <p className="mt-1 text-gray-500">
            Hello, {user.name.split(' ')[0]}.{' '}
            {approved && (
              <Link to={`/artists/${user.id}`} className="text-brand-500 hover:underline">
                View your public profile
              </Link>
            )}
          </p>
        </div>
        {approved && (
          <Link
            to="/artist/artworks/new"
            className="inline-flex items-center justify-center rounded-full bg-brand-500 px-6 py-2.5 font-medium text-white hover:bg-brand-600"
          >
            + Add artwork
          </Link>
        )}
      </div>

      <div className="mt-6 space-y-3">
        {!approved && (
          <Alert type="warning">
            <p className="font-medium">Your account is waiting for approval</p>
            <p className="mt-1">
              Our team reviews every new artist to keep the marketplace genuine. You can browse and buy in the meantime;
              adding artworks unlocks once you&apos;re approved.
            </p>
          </Alert>
        )}
        {flash && <Alert type="success">{flash}</Alert>}
        {actionError && <Alert type="error">{actionError}</Alert>}
      </div>

      <div className="mt-6">
        {mine.isPending ? (
          <div className="grid place-items-center py-16">
            <Spinner className="h-8 w-8" />
          </div>
        ) : mine.isError ? (
          <Alert type="error">{getErrorMessage(mine.error)}</Alert>
        ) : mine.data.content.length === 0 ? (
          <EmptyState title="You haven't added any artworks yet">
            {approved ? 'Add your first piece and it will appear in the shop straight away.' : 'Once approved, your artworks will show up here.'}
          </EmptyState>
        ) : (
          <>
            <ul className="divide-y divide-gray-100 overflow-hidden rounded-2xl bg-white ring-1 ring-gray-100">
              {mine.data.content.map((a) => (
                <li key={a.id} className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
                  <img src={a.imageUrl} alt="" className="h-20 w-20 shrink-0 rounded-xl object-cover ring-1 ring-gray-100" />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate font-medium">{a.title}</p>
                      <StatusBadge status={a.status} />
                    </div>
                    <p className="mt-1 text-sm text-gray-500">
                      {a.category.name} · {formatPrice(a.price)} ·{' '}
                      <span className={a.stock === 0 ? 'text-red-600' : ''}>
                        {a.stock === 0 ? 'Sold out' : `${a.stock} in stock`}
                      </span>
                    </p>
                  </div>
                  {approved && (
                    <div className="flex shrink-0 flex-wrap gap-2 text-sm">
                      {a.status === 'ACTIVE' && (
                        <Link to={`/artworks/${a.id}`} className="rounded-full px-3 py-1.5 ring-1 ring-gray-200 hover:bg-gray-50">
                          View
                        </Link>
                      )}
                      <Link to={`/artist/artworks/${a.id}/edit`} className="rounded-full px-3 py-1.5 ring-1 ring-gray-200 hover:bg-gray-50">
                        Edit
                      </Link>
                      <button
                        type="button"
                        disabled={toggle.isPending && toggle.variables?.id === a.id}
                        onClick={() => {
                          setActionError('')
                          toggle.mutate(a)
                        }}
                        className="rounded-full px-3 py-1.5 ring-1 ring-gray-200 hover:bg-gray-50 disabled:opacity-50"
                      >
                        {a.status === 'ACTIVE' ? 'Hide' : 'List'}
                      </button>
                      <button
                        type="button"
                        onClick={() => setToDelete(a)}
                        className="rounded-full px-3 py-1.5 text-red-600 ring-1 ring-red-200 hover:bg-red-50"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
            <Pagination
              page={mine.data.page}
              totalPages={mine.data.totalPages}
              onChange={(p) => setParams(p === 0 ? {} : { page: String(p + 1) })}
            />
          </>
        )}
      </div>

      <ConfirmDialog
        open={!!toDelete}
        title="Delete this artwork?"
        confirmLabel="Delete"
        danger
        busy={remove.isPending}
        onCancel={() => setToDelete(null)}
        onConfirm={() => remove.mutate(toDelete)}
      >
        &ldquo;{toDelete?.title}&rdquo; will be removed from the shop. This cannot be undone. To take it down temporarily,
        use Hide instead.
      </ConfirmDialog>
    </div>
  )
}

export default ArtistDashboard
