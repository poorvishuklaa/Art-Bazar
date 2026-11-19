import { Link, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchOrders } from '../../api/orders'
import { getErrorMessage } from '../../api/client'
import { formatDate, formatPrice } from '../../utils/format'
import OrderStatusBadge from '../../components/order/OrderStatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'

const Orders = () => {
  const [params, setParams] = useSearchParams()
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['orders', page],
    queryFn: () => fetchOrders(page),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="mx-auto max-w-3xl py-10">
      <h1 className="text-3xl font-medium">My orders</h1>
      <div className="mt-8">
        {isPending ? (
          <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : isError ? (
          <Alert type="error">{getErrorMessage(error)}</Alert>
        ) : data.content.length === 0 ? (
          <EmptyState title="No orders yet">
            <Link to="/artworks" className="font-medium text-brand-500 hover:underline">Browse artworks</Link> to place your first order.
          </EmptyState>
        ) : (
          <>
            <ul className="space-y-3">
              {data.content.map((o) => (
                <li key={o.id}>
                  <Link to={`/orders/${o.id}`} className="flex items-center gap-4 rounded-2xl bg-white p-4 ring-1 ring-gray-100 transition hover:ring-brand-500">
                    {o.firstItemImageUrl && <img src={o.firstItemImageUrl} alt="" className="h-16 w-16 shrink-0 rounded-xl object-cover" />}
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="font-medium">Order #{o.id}</span>
                        <OrderStatusBadge order={o} />
                      </div>
                      <p className="mt-1 line-clamp-1 text-sm text-gray-500">
                        {o.firstItemTitle}
                        {o.itemCount > 1 && ` and ${o.itemCount - 1} more`} · {formatDate(o.createdAt)}
                      </p>
                    </div>
                    <span className="shrink-0 font-semibold">{formatPrice(o.totalAmount)}</span>
                  </Link>
                </li>
              ))}
            </ul>
            <Pagination page={data.page} totalPages={data.totalPages} onChange={(p) => setParams(p === 0 ? {} : { page: String(p + 1) })} />
          </>
        )}
      </div>
    </div>
  )
}

export default Orders
