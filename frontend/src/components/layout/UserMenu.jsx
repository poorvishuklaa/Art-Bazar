import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { linksFor } from '../../utils/roles'

const UserMenu = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    if (!open) return
    const close = (e) => {
      if (e.type === 'keydown' ? e.key === 'Escape' : !ref.current?.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', close)
    return () => {
      document.removeEventListener('mousedown', close)
      document.removeEventListener('keydown', close)
    }
  }, [open])

  const handleLogout = async () => {
    setOpen(false)
    await logout()
    navigate('/')
  }

  const initial = user.name.trim().charAt(0).toUpperCase()

  return (
    <div ref={ref} className="relative hidden sm:block">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
        className="flex items-center gap-2 rounded-full border border-gray-200 py-1 pr-4 pl-1 hover:border-brand-500"
      >
        <span className="grid h-8 w-8 place-items-center rounded-full bg-brand-500 text-sm font-semibold text-white">
          {initial}
        </span>
        <span className="max-w-[8rem] truncate text-sm text-slate-brand">{user.name.split(' ')[0]}</span>
      </button>

      {open && (
        <div role="menu" className="absolute right-0 mt-2 w-56 overflow-hidden rounded-xl bg-white py-1 shadow-lg ring-1 ring-gray-100">
          <div className="border-b border-gray-100 px-4 py-3">
            <p className="truncate text-sm font-medium">{user.name}</p>
            <p className="truncate text-xs text-gray-500">{user.email}</p>
            <p className="mt-1 text-[11px] font-semibold tracking-wider text-brand-500 uppercase">{user.role.toLowerCase()}</p>
          </div>
          {linksFor(user).map((l) => (
            <Link key={l.to} role="menuitem" to={l.to} onClick={() => setOpen(false)} className="block px-4 py-2 text-sm hover:bg-brand-50">
              {l.label}
            </Link>
          ))}
          <button role="menuitem" type="button" onClick={handleLogout} className="block w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50">
            Sign out
          </button>
        </div>
      )}
    </div>
  )
}

export default UserMenu
