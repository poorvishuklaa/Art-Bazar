import { useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchAdminOrders } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import { formatDate, formatPrice } from '../../utils/format'
import AdminNav from '../../components/admin/AdminNav'
import OrderStatusBadge from '../../components/order/OrderStatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import { usePageTitle } from '../../hooks/usePageTitle'

const statuses = [
  ['', 'All orders'], ['PLACED', 'Awaiting payment'], ['PAID', 'Paid'], ['PARTIALLY_SHIPPED', 'Partly shipped'],
  ['SHIPPED', 'Shipped'], ['DELIVERED', 'Delivered'], ['CANCELLED', 'Cancelled'],
]

const AdminOrders = () => {
  usePageTitle('Orders · Admin')
  const [params, setParams] = useSearchParams()
  const status = params.get('status') ?? ''
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)
  const orders = useQuery({
    queryKey: ['admin', 'orders', status, page],
    queryFn: () => fetchAdminOrders({ status, page }),
    placeholderData: keepPreviousData,
  })

  return (
    <div className="py-10">
      <AdminNav />
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <h1 className="text-3xl font-medium">Orders</h1>
        <select
          aria-label="Order status"
          value={status}
          onChange={(e) => setParams(e.target.value ? { status: e.target.value } : {})}
          className="rounded-full border border-gray-300 bg-white px-4 py-2 text-sm outline-none focus:border-brand-500"
        >
          {statuses.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
        </select>
      </div>

      <div className="mt-6">
        {orders.isPending ? (
          <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : orders.isError ? (
          <Alert type="error">{getErrorMessage(orders.error)}</Alert>
        ) : orders.data.content.length === 0 ? (
          <EmptyState title="No orders here" />
        ) : (
          <>
            <div className="overflow-x-auto rounded-2xl bg-white ring-1 ring-gray-100">
              <table className="w-full min-w-[640px] text-sm">
                <thead>
                  <tr className="border-b border-gray-100 text-left text-gray-500">
                    <th className="px-4 py-3 font-medium">Order</th>
                    <th className="px-4 py-3 font-medium">Buyer</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 text-right font-medium">Items</th>
                    <th className="px-4 py-3 text-right font-medium">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {orders.data.content.map((o) => (
                    <tr key={o.id}>
                      <td className="px-4 py-3">
                        <p className="font-medium">#{o.id}</p>
                        <p className="text-xs text-gray-500">{formatDate(o.createdAt)}</p>
                      </td>
                      <td className="px-4 py-3">
                        <p>{o.buyerName}</p>
                        <p className="text-xs text-gray-500">{o.buyerEmail}</p>
                      </td>
                      <td className="px-4 py-3"><OrderStatusBadge order={o} /></td>
                      <td className="px-4 py-3 text-right tabular-nums">{o.itemCount}</td>
                      <td className="px-4 py-3 text-right font-medium tabular-nums">{formatPrice(o.totalAmount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={orders.data.page}
              totalPages={orders.data.totalPages}
              onChange={(p) => setParams({ ...(status ? { status } : {}), ...(p ? { page: String(p + 1) } : {}) })}
            />
          </>
        )}
      </div>
    </div>
  )
}

export default AdminOrders
