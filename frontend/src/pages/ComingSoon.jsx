import { Link } from 'react-router-dom'

/** Placeholder for routes that are built in later phases (login, cart, ...). */
const ComingSoon = ({ title }) => (
  <div className="py-24 text-center">
    <h1 className="text-3xl font-medium">{title}</h1>
    <p className="mt-3 text-gray-500">This page is on its way.</p>
    <Link to="/" className="mt-8 inline-block rounded-full bg-brand-500 px-6 py-2.5 text-white hover:bg-brand-600">
      Back to home
    </Link>
  </div>
)

export default ComingSoon
