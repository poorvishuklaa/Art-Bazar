import { NavLink } from 'react-router-dom'

const tabs = [
  { to: '/artist', label: 'My artworks', end: true },
  { to: '/artist/orders', label: 'Orders' },
  { to: '/artist/sales', label: 'Sales' },
]

/** Sub-navigation shared by the artist pages. */
const ArtistNav = ({ toShip }) => (
  <nav aria-label="Artist" className="mb-8 flex gap-1 overflow-x-auto border-b border-gray-200">
    {tabs.map((t) => (
      <NavLink
        key={t.to}
        to={t.to}
        end={t.end}
        className={({ isActive }) =>
          `-mb-px flex shrink-0 items-center gap-2 border-b-2 px-4 py-2.5 text-sm font-medium transition ${
            isActive ? 'border-brand-500 text-brand-500' : 'border-transparent text-gray-500 hover:text-ink'
          }`
        }
      >
        {t.label}
        {t.to === '/artist/orders' && toShip > 0 && (
          <span className="rounded-full bg-brand-500 px-1.5 text-[11px] leading-5 font-semibold text-white">{toShip}</span>
        )}
      </NavLink>
    ))}
  </nav>
)

export default ArtistNav
