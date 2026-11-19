import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchAdminStats } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import { formatPrice } from '../../utils/format'
import AdminNav from '../../components/admin/AdminNav'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import { usePageTitle } from '../../hooks/usePageTitle'

const Tile = ({ label, value, note, to }) => {
  const inner = (
    <>
      <p className="text-sm text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums">{value}</p>
      {note && <p className="mt-1 text-xs text-gray-500">{note}</p>}
    </>
  )
  const cls = 'block rounded-2xl bg-white p-5 ring-1 ring-gray-100'
  return to ? <Link to={to} className={`${cls} transition hover:ring-brand-500`}>{inner}</Link> : <div className={cls}>{inner}</div>
}

const AdminOverview = () => {
  usePageTitle('Admin overview')
  const { data: s, isPending, isError, error } = useQuery({ queryKey: ['admin', 'stats'], queryFn: fetchAdminStats })

  return (
    <div className="py-10">
      <AdminNav />
      <h1 className="text-3xl font-medium">Overview</h1>
      <p className="mt-1 text-gray-500">How the marketplace is doing. Sales figures count paid orders only.</p>
      {isPending ? (
        <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
      ) : isError ? (
        <div className="mt-6"><Alert type="error">{getErrorMessage(error)}</Alert></div>
      ) : (
        <>
          {s.pendingArtists > 0 && (
            <div className="mt-6">
              <Alert type="warning">
                {s.pendingArtists} {s.pendingArtists === 1 ? 'artist is' : 'artists are'} waiting for approval.{' '}
                <Link to="/admin/artists" className="font-medium underline">Review applications</Link>
              </Alert>
            </div>
          )}
          <h2 className="mt-8 text-sm font-semibold tracking-wider text-gray-500 uppercase">Sales</h2>
          <div className="mt-3 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <Tile label="Total sales" value={formatPrice(s.gmv)} note={`${s.paidOrders} paid orders`} to="/admin/orders" />
            <Tile label="Last 30 days" value={formatPrice(s.gmvLast30Days)} note={`${s.paidOrdersLast30Days} paid orders`} />
            <Tile label="Awaiting shipment" value={s.ordersAwaitingShipment} note="Paid, not fully shipped" to="/admin/orders?status=PAID" />
            <Tile label="Listed artworks" value={s.listedArtworks} to="/admin/artworks" />
          </div>
          <h2 className="mt-8 text-sm font-semibold tracking-wider text-gray-500 uppercase">People</h2>
          <div className="mt-3 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <Tile label="Buyers" value={s.buyers} to="/admin/users?role=BUYER" />
            <Tile label="Artists" value={s.artists} to="/admin/users?role=ARTIST" />
            <Tile label="Pending artists" value={s.pendingArtists} note={s.pendingArtists ? 'Need review' : 'All reviewed'} to="/admin/artists" />
          </div>
        </>
      )}
    </div>
  )
}

export default AdminOverview
