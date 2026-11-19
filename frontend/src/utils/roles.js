/** Role-restricted areas; must match the <ProtectedRoute roles> in App.jsx. */
const restrictedAreas = [
  { prefix: '/admin', roles: ['ADMIN'] },
  { prefix: '/artist', roles: ['ARTIST'] },
]

export const canVisit = (user, path) =>
  restrictedAreas.every((a) => !path.startsWith(a.prefix) || a.roles.includes(user?.role))

/** After sign-in: back to where they were heading if their role may go there, else their home. */
export const destinationAfterLogin = (user, from) => (from && canVisit(user, from) ? from : homeFor(user))

/** Where each role lands after signing in, unless they were heading somewhere specific. */
export const homeFor = (user) => {
  if (user?.role === 'ADMIN') return '/admin/artists'
  if (user?.role === 'ARTIST') return '/artist'
  return '/'
}

const roleLinks = {
  ARTIST: [{ to: '/artist', label: 'Artist space' }],
  ADMIN: [{ to: '/admin/artists', label: 'Artist applications' }],
  BUYER: [],
}

/** Extra menu links for the signed-in user's role. */
export const linksFor = (user) => roleLinks[user?.role] ?? []
