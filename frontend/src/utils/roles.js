/** Role-restricted areas; must match the <ProtectedRoute roles> in App.jsx. */
const restrictedAreas = [
  { area: '/admin', roles: ['ADMIN'] },
  { area: '/artist', roles: ['ARTIST'] }, // not /artists/:id, which is public
  { area: '/cart', roles: ['BUYER', 'ARTIST'] },
  { area: '/checkout', roles: ['BUYER', 'ARTIST'] },
  { area: '/orders', roles: ['BUYER', 'ARTIST'] },
]

/** True for the area itself and anything below it: "/artist" and "/artist/x", but not "/artists". */
const inArea = (path, area) => path === area || path.startsWith(`${area}/`)

export const canVisit = (user, path) =>
  restrictedAreas.every((a) => !inArea(path, a.area) || a.roles.includes(user?.role))

/** After sign-in: back to where they were heading if their role may go there, else their home. */
export const destinationAfterLogin = (user, from) => (from && canVisit(user, from) ? from : homeFor(user))

/** Where each role lands after signing in, unless they were heading somewhere specific. */
export const homeFor = (user) => {
  if (user?.role === 'ADMIN') return '/admin'
  if (user?.role === 'ARTIST') return '/artist'
  return '/'
}

const roleLinks = {
  ARTIST: [
    { to: '/artist', label: 'My artworks' },
    { to: '/artist/orders', label: 'Orders to ship' },
    { to: '/artist/sales', label: 'Sales' },
    { to: '/orders', label: 'My purchases' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Admin dashboard' },
    { to: '/admin/artists', label: 'Artist applications' },
  ],
  BUYER: [{ to: '/orders', label: 'My orders' }],
}

/** Buyers and artists can buy; admins only moderate. */
export const canShop = (user) => user?.role === 'BUYER' || user?.role === 'ARTIST'

/** Menu links for the signed-in user: their role's links, then account settings. */
export const linksFor = (user) => (user ? [...(roleLinks[user.role] ?? []), { to: '/account', label: 'Account settings' }] : [])
