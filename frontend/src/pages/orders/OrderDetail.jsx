import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { cancelOrder, fetchOrder } from '../../api/orders'
import { getErrorMessage } from '../../api/client'
import { formatDate, formatPrice } from '../../utils/format'
import OrderStatusBadge from '../../components/order/OrderStatusBadge'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import NotFound from '../NotFound'
import { usePageTitle } from '../../hooks/usePageTitle'

/** Per-artwork delivery progress, shown once an order is paid (each artist ships separately). */
const itemStatus = {
  PENDING: ['Preparing', 'text-gray-500'],
  SHIPPED: ['Shipped', 'text-sky-700'],
  DELIVERED: ['Delivered', 'text-green-700'],
  CANCELLED: ['Cancelled', 'text-gray-400'],
}

const OrderDetail = () => {
  const { id } = useParams()
  usePageTitle(`Order #${id}`)
  const location = useLocation()
  const queryClient = useQueryClient()
  const [justPaid] = useState(() => location.state?.justPaid)
  const [confirming, setConfirming] = useState(false)

  const { data: order, isPending, isError, error } = useQuery({ queryKey: ['order', id], queryFn: () => fetchOrder(id) })

  const cancel = useMutation({
    mutationFn: () => cancelOrder(id),
    onSuccess: (o) => {
      setConfirming(false)
      queryClient.setQueryData(['order', id], o)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['artworks'] })
    },
    onError: () => setConfirming(false),
  })

  if (isPending) {
    return <div className="grid place-items-center py-32"><Spinner className="h-8 w-8" /></div>
  }
  if (isError) {
    return error.response?.status === 404 ? <NotFound /> : <div className="py-12"><Alert type="error">{getErrorMessage(error)}</Alert></div>
  }

  const awaitingPayment = order.status === 'PLACED' && order.paymentStatus !== 'PAID'
  const s = order.shipping

  return (
    <div className="mx-auto max-w-3xl py-10">
      <Link to="/orders" className="text-sm text-gray-500 hover:text-brand-500">← My orders</Link>
      <div className="mt-3 flex flex-wrap items-center gap-3">
        <h1 className="text-3xl font-medium">Order #{order.id}</h1>
        <OrderStatusBadge order={order} />
      </div>
      <p className="mt-1 text-sm text-gray-500">Placed on {formatDate(order.createdAt)}</p>

      <div className="mt-6 space-y-3">
        {justPaid && order.paymentStatus === 'PAID' && (
          <Alert type="success">
            <p className="font-medium">Payment successful. Thank you!</p>
            <p className="mt-1">The artists will now prepare and ship your order. Reference: <span className="font-mono">{order.paymentReference}</span></p>
          </Alert>
        )}
        {awaitingPayment && (
          <Alert type="warning">
            <p className="font-medium">This order isn&apos;t paid yet</p>
            <p className="mt-1">
              Your items are reserved until {new Date(order.payBy).toLocaleTimeString('en-IN', { timeStyle: 'short' })}; after that the order is cancelled automatically.{' '}
              <Link to={`/orders/${order.id}/pay`} className="font-medium underline">Pay now</Link>
            </p>
          </Alert>
        )}
        {cancel.isError && <Alert type="error">{getErrorMessage(cancel.error)}</Alert>}
      </div>

      <section className="mt-8 rounded-2xl bg-white ring-1 ring-gray-100">
        <ul className="divide-y divide-gray-100">
          {order.items.map((i) => (
            <li key={i.id} className="flex gap-4 p-4">
              <img src={i.imageUrl} alt="" className="h-20 w-20 shrink-0 rounded-xl object-cover" />
              <div className="min-w-0 flex-1">
                {i.artworkId ? (
                  <Link to={`/artworks/${i.artworkId}`} className="font-medium hover:text-brand-500">{i.title}</Link>
                ) : (
                  <span className="font-medium">{i.title}</span>
                )}
                <p className="text-sm text-gray-500">
                  by <Link to={`/artists/${i.artistId}`} className="hover:text-brand-500">{i.artistName}</Link> · {formatPrice(i.unitPrice)} × {i.quantity}
                </p>
                {order.paymentStatus === 'PAID' && (
                  <p className={`mt-1 text-xs font-medium ${itemStatus[i.status][1]}`}>
                    {itemStatus[i.status][0]}
                    {i.artworkId && i.status !== 'CANCELLED' && (
                      <Link to={`/artworks/${i.artworkId}#reviews`} className="ml-3 font-normal text-brand-500 hover:underline">
                        Rate this piece
                      </Link>
                    )}
                  </p>
                )}
              </div>
              <span className="shrink-0 font-medium">{formatPrice(i.lineTotal)}</span>
            </li>
          ))}
        </ul>
        <div className="flex justify-between border-t border-gray-100 p-4 font-semibold">
          <span>Total</span>
          <span>{formatPrice(order.totalAmount)}</span>
        </div>
      </section>

      <div className="mt-6 grid gap-6 sm:grid-cols-2">
        <section className="rounded-2xl bg-gray-50 p-5 text-sm">
          <h2 className="mb-2 font-medium">Shipping to</h2>
          <p>{s.name}</p>
          <p className="text-gray-600">{s.address}</p>
          <p className="text-gray-600">{s.city}, {s.state} {s.pincode}</p>
          <p className="text-gray-600">{s.phone}</p>
        </section>
        <section className="rounded-2xl bg-gray-50 p-5 text-sm">
          <h2 className="mb-2 font-medium">Payment</h2>
          {order.paymentStatus === 'PAID' || order.paymentStatus === 'REFUNDED' ? (
            <>
              <p>{order.paymentMethod === 'UPI' ? 'UPI' : 'Card'} · {order.paymentStatus === 'REFUNDED' ? 'Refunded' : 'Paid'}</p>
              <p className="font-mono text-xs text-gray-500">{order.paymentReference}</p>
            </>
          ) : (
            <p className="text-gray-600">{order.status === 'CANCELLED' ? 'Not charged' : order.paymentStatus === 'FAILED' ? 'Last attempt failed' : 'Not paid yet'}</p>
          )}
        </section>
      </div>

      {order.cancellable && (
        <div className="mt-8 flex flex-wrap gap-3">
          {awaitingPayment && (
            <Link to={`/orders/${order.id}/pay`} className="rounded-full bg-brand-500 px-6 py-2.5 font-medium text-white hover:bg-brand-600">Pay now</Link>
          )}
          <button type="button" onClick={() => setConfirming(true)} className="rounded-full px-6 py-2.5 text-red-600 ring-1 ring-red-200 hover:bg-red-50">
            Cancel order
          </button>
        </div>
      )}

      <ConfirmDialog
        open={confirming}
        title="Cancel this order?"
        confirmLabel="Yes, cancel order"
        danger
        busy={cancel.isPending}
        onCancel={() => setConfirming(false)}
        onConfirm={() => cancel.mutate()}
      >
        {order.paymentStatus === 'PAID'
          ? 'Your payment will be refunded and the items will go back on sale.'
          : 'The items will go back on sale. You have not been charged.'}
      </ConfirmDialog>
    </div>
  )
}

export default OrderDetail
