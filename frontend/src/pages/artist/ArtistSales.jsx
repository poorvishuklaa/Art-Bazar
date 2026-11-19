import { Link } from 'react-router-dom'
import { useSalesSummary } from '../../hooks/useSalesSummary'
import { getErrorMessage } from '../../api/client'
import { formatPrice } from '../../utils/format'
import ArtistNav from '../../components/artist/ArtistNav'
import RevenueChart from '../../components/artist/RevenueChart'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

const StatTile = ({ label, value, note, to }) => {
  const body = (
    <>
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
      {note && <p className="mt-1 text-xs text-gray-500">{note}</p>}
    </>
  )
  const cls = 'block rounded-2xl bg-white p-5 ring-1 ring-gray-100'
  return to ? <Link to={to} className={`${cls} transition hover:ring-brand-500`}>{body}</Link> : <div className={cls}>{body}</div>
}

/** Sales overview for the signed-in artist. Only paid, uncancelled sales count as revenue. */
const ArtistSales = () => {
  const { data, isPending, isError, error } = useSalesSummary()

  return (
    <div className="py-10">
      <ArtistNav toShip={data?.ordersToShip} />
      <h1 className="text-3xl font-medium">Sales</h1>
      <p className="mt-1 text-gray-500">Paid sales only. Cancelled and refunded orders are not counted.</p>

      {isPending ? (
        <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
      ) : isError ? (
        <div className="mt-6"><Alert type="error">{getErrorMessage(error)}</Alert></div>
      ) : (
        <>
          <div className="mt-8 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatTile label="Total revenue" value={formatPrice(data.totalRevenue)} note={`${data.itemsSold} ${data.itemsSold === 1 ? 'item' : 'items'} sold`} />
            <StatTile label="Last 30 days" value={formatPrice(data.revenueLast30Days)} note={`${data.itemsSoldLast30Days} ${data.itemsSoldLast30Days === 1 ? 'item' : 'items'} sold`} />
            <StatTile label="Orders to ship" value={data.ordersToShip} note={data.ordersToShip ? 'Waiting for you' : 'All caught up'} to="/artist/orders" />
            <StatTile label="Listed artworks" value={data.activeListings} to="/artist" />
          </div>

          <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_340px]">
            <section className="rounded-2xl bg-white p-6 ring-1 ring-gray-100">
              <h2 className="font-medium">Revenue, last 6 months</h2>
              <div className="mt-6">
                <RevenueChart months={data.monthly} />
              </div>
            </section>

            <section className="rounded-2xl bg-white p-6 ring-1 ring-gray-100">
              <h2 className="font-medium">Best sellers</h2>
              {data.topArtworks.length === 0 ? (
                <p className="mt-4 text-sm text-gray-500">Your best-selling pieces will show up here after your first sale.</p>
              ) : (
                <ol className="mt-4 space-y-3">
                  {data.topArtworks.map((t, i) => (
                    <li key={t.title} className="flex items-center gap-3 text-sm">
                      <span className="w-5 shrink-0 text-gray-400 tabular-nums">{i + 1}</span>
                      <span className="min-w-0 flex-1">
                        <span className="line-clamp-1 font-medium">{t.title}</span>
                        <span className="text-gray-500">{t.quantity} sold</span>
                      </span>
                      <span className="shrink-0 font-medium tabular-nums">{formatPrice(t.revenue)}</span>
                    </li>
                  ))}
                </ol>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  )
}

export default ArtistSales
