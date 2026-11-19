import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchArtistOrders, updateFulfilment } from '../../api/artistSales'
import { getErrorMessage } from '../../api/client'
import { formatDate, formatPrice } from '../../utils/format'
import { useSalesSummary } from '../../hooks/useSalesSummary'
import ArtistNav from '../../components/artist/ArtistNav'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import { usePageTitle } from '../../hooks/usePageTitle'

const partBadge = {
  PENDING: ['To ship', 'bg-amber-50 text-amber-800 ring-amber-200'],
  SHIPPED: ['Shipped', 'bg-sky-50 text-sky-800 ring-sky-200'],
  DELIVERED: ['Delivered', 'bg-green-50 text-green-700 ring-green-200'],
  CANCELLED: ['Cancelled', 'bg-gray-100 text-gray-600 ring-gray-200'],
}

const nextStep = {
  PENDING: { status: 'SHIPPED', label: 'Mark as shipped', confirm: 'Mark these items as shipped?', body: 'The buyer will see that your part of the order is on its way.' },
  SHIPPED: { status: 'DELIVERED', label: 'Mark as delivered', confirm: 'Mark these items as delivered?', body: 'Do this once the buyer has received the parcel. It cannot be undone.' },
}

/** Orders containing the signed-in artist's work: what to post, and where. */
const ArtistOrders = () => {
  usePageTitle('Orders to ship')
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const filter = params.get('filter') === 'all' ? 'all' : 'to_ship'
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)
  const [pending, setPending] = useState(null) // { order, step }
  const { data: summary } = useSalesSummary()

  const orders = useQuery({
    queryKey: ['artist-orders', filter, page],
    queryFn: () => fetchArtistOrders(filter, page),
    placeholderData: keepPreviousData,
  })

  const fulfil = useMutation({
    mutationFn: ({ order, step }) => updateFulfilment(order.orderId, step.status),
    onSuccess: () => {
      setPending(null)
      queryClient.invalidateQueries({ queryKey: ['artist-orders'] })
      queryClient.invalidateQueries({ queryKey: ['sales-summary'] })
    },
    onError: () => setPending(null),
  })

  const tab = (key, label) => (
    <button
      type="button"
      role="tab"
      aria-selected={filter === key}
      onClick={() => setParams(key === 'to_ship' ? {} : { filter: key })}
      className={`rounded-full px-4 py-1.5 text-sm transition ${filter === key ? 'bg-ink text-white' : 'text-gray-600 ring-1 ring-gray-200 hover:ring-gray-400'}`}
    >
      {label}
    </button>
  )

  return (
    <div className="py-10">
      <ArtistNav toShip={summary?.ordersToShip} />
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-medium">Orders</h1>
          <p className="mt-1 text-gray-500">Paid orders that include your artworks. Oldest first, so you can ship in order.</p>
        </div>
        <div role="tablist" className="flex gap-2">
          {tab('to_ship', 'To ship')}
          {tab('all', 'All orders')}
        </div>
      </div>

      <div className="mt-6 space-y-4">
        {fulfil.isError && <Alert type="error">{getErrorMessage(fulfil.error)}</Alert>}
        {orders.isPending ? (
          <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : orders.isError ? (
          <Alert type="error">{getErrorMessage(orders.error)}</Alert>
        ) : orders.data.content.length === 0 ? (
          <EmptyState title={filter === 'to_ship' ? 'Nothing to ship right now' : 'No orders yet'}>
            {filter === 'to_ship' ? 'New paid orders for your artworks will appear here.' : 'When buyers purchase your work, the orders show up here.'}
          </EmptyState>
        ) : (
          <>
            {orders.data.content.map((o) => {
              const [badge, badgeCls] = partBadge[o.myStatus]
              const step = o.orderStatus !== 'CANCELLED' ? nextStep[o.myStatus] : null
              const s = o.shipping
              return (
                <article key={o.orderId} className="rounded-2xl bg-white ring-1 ring-gray-100">
                  <header className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-5 py-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-medium">Order #{o.orderId}</span>
                      <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${badgeCls}`}>{badge}</span>
                      {o.paymentStatus === 'REFUNDED' && <span className="text-xs text-gray-500">Buyer cancelled · refunded</span>}
                    </div>
                    <span className="text-sm text-gray-500">Paid {formatDate(o.paidAt ?? o.createdAt)}</span>
                  </header>
                  <div className="grid gap-5 p-5 md:grid-cols-[1fr_240px]">
                    <ul className="space-y-3">
                      {o.items.map((i) => (
                        <li key={i.id} className="flex items-center gap-3">
                          <img src={i.imageUrl} alt="" className="h-14 w-14 shrink-0 rounded-lg object-cover" />
                          <div className="min-w-0 flex-1">
                            {i.artworkId ? (
                              <Link to={`/artworks/${i.artworkId}`} className="line-clamp-1 font-medium hover:text-brand-500">{i.title}</Link>
                            ) : (
                              <span className="line-clamp-1 font-medium">{i.title}</span>
                            )}
                            <p className="text-sm text-gray-500">{formatPrice(i.unitPrice)} × {i.quantity}</p>
                          </div>
                          <span className="shrink-0 text-sm font-medium">{formatPrice(i.lineTotal)}</span>
                        </li>
                      ))}
                      <li className="flex justify-between border-t border-gray-100 pt-3 text-sm font-semibold">
                        <span>Your earnings from this order</span>
                        <span>{formatPrice(o.myTotal)}</span>
                      </li>
                    </ul>
                    <div className="rounded-xl bg-gray-50 p-4 text-sm">
                      <p className="mb-1 font-medium">Ship to</p>
                      <p>{s.name}</p>
                      <p className="text-gray-600">{s.address}</p>
                      <p className="text-gray-600">{s.city}, {s.state} {s.pincode}</p>
                      <p className="text-gray-600">{s.phone}</p>
                    </div>
                  </div>
                  {step && (
                    <footer className="flex justify-end border-t border-gray-100 px-5 py-3">
                      <button
                        type="button"
                        onClick={() => setPending({ order: o, step })}
                        className="rounded-full bg-brand-500 px-5 py-2 text-sm font-medium text-white hover:bg-brand-600"
                      >
                        {step.label}
                      </button>
                    </footer>
                  )}
                </article>
              )
            })}
            <Pagination
              page={orders.data.page}
              totalPages={orders.data.totalPages}
              onChange={(p) => setParams({ ...(filter === 'all' ? { filter } : {}), ...(p ? { page: String(p + 1) } : {}) })}
            />
          </>
        )}
      </div>

      <ConfirmDialog
        open={!!pending}
        title={pending?.step.confirm}
        confirmLabel={pending?.step.label}
        busy={fulfil.isPending}
        onCancel={() => setPending(null)}
        onConfirm={() => fulfil.mutate(pending)}
      >
        {pending?.step.body}
      </ConfirmDialog>
    </div>
  )
}

export default ArtistOrders
