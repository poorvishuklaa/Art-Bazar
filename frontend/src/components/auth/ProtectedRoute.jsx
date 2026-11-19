import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import Spinner from '../ui/Spinner'

/**
 * Guards child routes. Anonymous users go to /login (and come back afterwards);
 * signed-in users without a permitted role see a 403 message.
 */
const ProtectedRoute = ({ roles }) => {
  const { user, status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (roles && !roles.includes(user.role)) {
    return (
      <div className="py-24 text-center">
        <p className="text-5xl font-semibold text-brand-500">403</p>
        <h1 className="mt-4 text-2xl font-medium">You don&apos;t have access to this page</h1>
      </div>
    )
  }
  return <Outlet />
}

export default ProtectedRoute
