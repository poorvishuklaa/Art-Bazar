import { Link } from 'react-router-dom'

const NotFound = () => (
  <div className="py-24 text-center">
    <p className="text-6xl font-semibold text-brand-500">404</p>
    <h1 className="mt-4 text-2xl font-medium">Page not found</h1>
    <p className="mt-2 text-gray-500">The page you are looking for does not exist.</p>
    <Link to="/" className="mt-8 inline-block rounded-full bg-brand-500 px-6 py-2.5 text-white hover:bg-brand-600">
      Back to home
    </Link>
  </div>
)

export default NotFound
