import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import logo from '../../assets/logo.png'
import { BagIcon, CloseIcon, MenuIcon, SearchIcon } from '../ui/Icons'
import { useAuth } from '../../hooks/useAuth'
import UserMenu from './UserMenu'
import { linksFor } from '../../utils/roles'

const links = [
  { to: '/', label: 'Home' },
  { to: '/artworks', label: 'Artworks' },
  { to: '/#explore', label: 'Categories' },
  { to: '/#footer', label: 'Contact' },
]

const Navbar = () => {
  const [open, setOpen] = useState(false)
  const { user, isAuthenticated, status, logout } = useAuth()
  const navigate = useNavigate()

  const handleMobileLogout = async () => {
    setOpen(false)
    await logout()
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-40 border-b border-gray-100 bg-white/90 backdrop-blur">
      <nav className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-2" onClick={() => setOpen(false)}>
          <img src={logo} alt="Art Bazaar" className="h-11 w-11 object-contain" />
          <span className="text-xl font-semibold text-brand-500">Art Bazaar</span>
        </Link>

        <ul className="hidden items-center gap-8 text-[17px] text-slate-brand md:flex">
          {links.map((l) => (
            <li key={l.label}>
              <NavLink
                to={l.to}
                end={l.to === '/'}
                className={({ isActive }) =>
                  `pb-1 transition hover:text-brand-500 ${
                    isActive && !l.to.includes('#') ? 'border-b-2 border-sky-brand text-brand-500' : ''
                  }`
                }
              >
                {l.label}
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="flex items-center gap-4 sm:gap-6">
          <button
            type="button"
            aria-label="Search artworks"
            onClick={() => navigate('/artworks', { state: { focusSearch: Date.now() } })}
            className="text-slate-brand hover:text-brand-500"
          >
            <SearchIcon className="h-6 w-6" />
          </button>
          <Link to="/cart" aria-label="Cart" className="text-slate-brand hover:text-brand-500">
            <BagIcon className="h-6 w-6" />
          </Link>
          {isAuthenticated ? (
            <UserMenu />
          ) : (
            status !== 'loading' && (
              <Link
                to="/login"
                className="hidden rounded-full border border-brand-500 px-6 py-2 text-slate-brand transition hover:bg-brand-50 sm:inline-block"
              >
                Sign in
              </Link>
            )
          )}
          <button
            type="button"
            className="text-slate-brand md:hidden"
            aria-label={open ? 'Close menu' : 'Open menu'}
            aria-expanded={open}
            onClick={() => setOpen((o) => !o)}
          >
            {open ? <CloseIcon className="h-7 w-7" /> : <MenuIcon className="h-7 w-7" />}
          </button>
        </div>
      </nav>

      {open && (
        <ul className="space-y-1 border-t border-gray-100 px-4 py-3 md:hidden">
          {links.map((l) => (
            <li key={l.label}>
              <Link to={l.to} onClick={() => setOpen(false)} className="block rounded-lg px-3 py-2 text-slate-brand hover:bg-brand-50">
                {l.label}
              </Link>
            </li>
          ))}
          {isAuthenticated ? (
            <>
              {linksFor(user).map((l) => (
                <li key={l.to}>
                  <Link to={l.to} onClick={() => setOpen(false)} className="block rounded-lg px-3 py-2 text-slate-brand hover:bg-brand-50">
                    {l.label}
                  </Link>
                </li>
              ))}
              <li className="mt-2 border-t border-gray-100 pt-2">
                <p className="px-3 text-xs text-gray-500">Signed in as {user.email}</p>
                <button type="button" onClick={handleMobileLogout} className="mt-1 block w-full rounded-lg px-3 py-2 text-left font-medium text-red-600 hover:bg-red-50">
                  Sign out
                </button>
              </li>
            </>
          ) : (
            <li>
              <Link to="/login" onClick={() => setOpen(false)} className="block rounded-lg px-3 py-2 font-medium text-brand-500 hover:bg-brand-50">
                Sign in
              </Link>
            </li>
          )}
        </ul>
      )}
    </header>
  )
}

export default Navbar
