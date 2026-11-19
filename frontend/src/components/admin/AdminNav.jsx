import { NavLink } from 'react-router-dom'

const tabs = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/artists', label: 'Artist applications' },
  { to: '/admin/users', label: 'Users' },
  { to: '/admin/artworks', label: 'Artworks' },
  { to: '/admin/categories', label: 'Categories' },
  { to: '/admin/orders', label: 'Orders' },
]

const AdminNav = () => (
  <nav aria-label="Admin" className="mb-8 flex gap-1 overflow-x-auto border-b border-gray-200 [&::-webkit-scrollbar]:hidden">
    {tabs.map((t) => (
      <NavLink
        key={t.to}
        to={t.to}
        end={t.end}
        className={({ isActive }) =>
          `-mb-px shrink-0 border-b-2 px-4 py-2.5 text-sm font-medium transition ${
            isActive ? 'border-brand-500 text-brand-500' : 'border-transparent text-gray-500 hover:text-ink'
          }`
        }
      >
        {t.label}
      </NavLink>
    ))}
  </nav>
)

export default AdminNav
