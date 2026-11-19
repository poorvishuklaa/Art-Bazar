import { Link } from 'react-router-dom'
import logo from '../../assets/logo.png'

const AuthCard = ({ title, subtitle, children, footer }) => (
  <div className="flex justify-center py-12 sm:py-16">
    <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-sm ring-1 ring-gray-100 sm:p-8">
      <Link to="/" className="mx-auto mb-6 block w-fit">
        <img src={logo} alt="Art Bazaar" className="h-14 w-14 object-contain" />
      </Link>
      <h1 className="text-center text-2xl font-semibold">{title}</h1>
      {subtitle && <p className="mt-2 text-center text-sm text-gray-500">{subtitle}</p>}
      <div className="mt-8">{children}</div>
      {footer && <div className="mt-6 text-center text-sm text-gray-600">{footer}</div>}
    </div>
  </div>
)

export default AuthCard
